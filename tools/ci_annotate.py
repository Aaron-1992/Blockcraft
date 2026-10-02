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
if len(text) > limit:
    text = "... (truncated, last %d chars)\n" % limit + text[-limit:]
text = text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
print("::%s title=%s::%s" % (level, title, text))
