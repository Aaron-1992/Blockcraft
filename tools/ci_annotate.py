#!/usr/bin/env python3
"""
Turns a text file into a GitHub Actions annotation (::notice/::error) so its content is readable through the
check-run API even where the raw logs cannot be downloaded:

    python3 tools/ci_annotate.py <file> "<title>" [notice|error] [max_chars]
"""
import sys

path, title = sys.argv[1], sys.argv[2]
level = sys.argv[3] if len(sys.argv) > 3 else "notice"
limit = int(sys.argv[4]) if len(sys.argv) > 4 else 30000
try:
    text = open(path, encoding="utf-8", errors="replace").read()
except OSError as e:
    text = "(could not read %s: %s)" % (path, e)
# GitHub keeps only the first ~4000 characters of an annotation message: when the text is longer, put the interesting lines
# (everything that is not an "ok ..." line of the unit tests: failures, the summary, exceptions) in front of the tail.
if len(text) > min(limit, 3800):
    lines = text.splitlines()
    odd = [l for l in lines if not l.startswith("ok ") and not l.startswith("ok\t") and l.strip()]
    head = "\n".join(odd)
    if len(head) > 2600:
        head = head[:1300] + "\n...\n" + head[-1300:]
    tail = text[-1000:]
    text = "(not an 'ok' line, %d of %d lines)\n%s\n=== tail ===\n%s" % (len(odd), len(lines), head, tail)
text = text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
print("::%s title=%s::%s" % (level, title, text))
