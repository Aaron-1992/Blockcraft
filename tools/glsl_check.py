#!/usr/bin/env python3
"""
Syntax check of every GLSL shader that is written as a Java string array in src/bc (ShaderSrc.java, Shaders.java, WeatherFx.java ...).

There is no GPU or glslang in the assistant's sandbox; this runs the shaders through a pure JavaScript GLSL parser (npm
@shaderfrog/glsl-parser) so that a missing semicolon or a misspelt keyword is found before a 10 minute CI round trip. It checks syntax only:
types, overload resolution and driver limits are still checked by the Visual workflow (Mesa) and the Windows smoke test.

    python3 tools/glsl_check.py            (needs node + npm; installs the parser into /tmp/glcheck on first use)
"""
import json
import os
import re
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORK = "/tmp/glcheck"


def java_string(lit):
    """The value of one Java string literal body (between the quotes)."""
    out = []
    i = 0
    while i < len(lit):
        c = lit[i]
        if c == "\\" and i + 1 < len(lit):
            n = lit[i + 1]
            out.append({"n": "\n", "t": "\t", '"': '"', "\\": "\\", "'": "'"}.get(n, n))
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def extract(path):
    """name -> source for every `static final String NAME = String.join("\\n", new CharSequence[]{ "...", ... });` in the file."""
    text = open(path, encoding="utf-8").read()
    found = {}
    for m in re.finditer(r"static final String (\w+) = String\.join\(\s*\"\\n\",\s*new CharSequence\[\]\{(.*?)\}\s*\);", text, re.S):
        body = m.group(2)
        lines = []
        for lit in re.finditer(r'"((?:[^"\\]|\\.)*)"', body):
            lines.append(java_string(lit.group(1)))
        src = "\n".join(lines)
        found[os.path.basename(path) + ":" + m.group(1)] = src
    # Some shaders are a shared declaration block plus their own main(), e.g.
    #   static final String CLOUD_FS = CLOUD_DECL + "\n" + String.join("\n", new CharSequence[]{ ... });
    # Check those too: a syntax error in the cloud body would otherwise go unnoticed.
    for m in re.finditer(r"static final String (\w+) = (\w+) \+ \"\\n\" \+ String\.join\(\s*\"\\n\",\s*new CharSequence\[\]\{(.*?)\}\s*\);", text, re.S):
        name, prefix, body = m.group(1), m.group(2), m.group(3)
        head = found.get(os.path.basename(path) + ":" + prefix)
        if head is None:
            continue
        lines = [java_string(lit.group(1)) for lit in re.finditer(r'"((?:[^"\\]|\\.)*)"', body)]
        found[os.path.basename(path) + ":" + name] = head + "\n" + "\n".join(lines)
    return found


def assemble(shaders):
    """The water shader is derived from the terrain shader at class-load time (ShaderSrc.deriveWater): do the same splice here."""
    key = "ShaderSrc.java:"
    if key + "WATER_DECL" in shaders and key + "WORLD_FS" in shaders:
        s = shaders[key + "WORLD_FS"]
        tail = "varying float vDepth;"
        decl = s.index(tail) + len(tail)
        frm = s.index("  if (vWater > 0.5) {")
        to = s.index("  float f = clamp((fogRange.y - vDepth)")
        shaders[key + "WATER_FS"] = s[:decl] + "\n" + shaders[key + "WATER_DECL"] + s[decl:frm] + shaders[key + "WATER_MAIN"] + s[to:]
    return {k: v for k, v in shaders.items() if "#version" in v}


def main():
    shaders = {}
    srcdir = os.path.join(ROOT, "src", "bc")
    for f in sorted(os.listdir(srcdir)):
        if f.endswith(".java"):
            shaders.update(extract(os.path.join(srcdir, f)))
    shaders = assemble(shaders)
    if not shaders:
        print("glsl_check: no shaders found")
        return 1
    if not os.path.exists(os.path.join(WORK, "node_modules", "@shaderfrog")):
        os.makedirs(WORK, exist_ok=True)
        subprocess.run(["npm", "init", "-y"], cwd=WORK, capture_output=True)
        r = subprocess.run(["npm", "install", "@shaderfrog/glsl-parser", "--silent"], cwd=WORK, capture_output=True, text=True)
        if r.returncode != 0:
            print("glsl_check: cannot install the parser:", r.stderr[-300:])
            return 2
    check = os.path.join(WORK, "check.js")
    if not os.path.exists(check):
        open(check, "w").write(CHECK_JS)
    with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False) as t:
        json.dump(shaders, t)
    r = subprocess.run(["node", check, t.name], cwd=WORK, capture_output=True, text=True)
    os.unlink(t.name)
    print(r.stdout.rstrip())
    if r.stderr.strip():
        print(r.stderr.strip()[-600:])
    return r.returncode


CHECK_JS = r"""
const { parser } = require('@shaderfrog/glsl-parser');
const fs = require('fs');
const shaders = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
let bad = 0;
for (const [name, src] of Object.entries(shaders)) {
  try {
    parser.parse(src, { quiet: true });
    console.log('ok   ', name, '(' + src.split('\n').length + ' lines)');
  } catch (e) {
    bad++;
    const loc = e.location ? ' at line ' + e.location.start.line + ' col ' + e.location.start.column : '';
    console.log('FAIL ', name, loc, '-', String(e.message).slice(0, 200));
    if (e.location) { const lines = src.split('\n'); console.log('       > ' + lines[e.location.start.line - 1]); }
  }
}
process.exit(bad ? 1 : 0);
"""

if __name__ == "__main__":
    sys.exit(main())
