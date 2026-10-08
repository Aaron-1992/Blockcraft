#!/usr/bin/env python3
"""
Bytecode-level comparison of two directories of .class files (see tools/ClassCompare.java), e.g. to check that a
decompile->recompile or a refactoring did not change what a method does:

    python3 tools/classcompare.py <dirA> <dirB> [class-name-filter]
    python3 tools/classcompare.py dump <file.class> <methodName>

Needs a Java 24+ runtime (java.lang.classfile) - the one tools/bootstrap.py downloads (Java 25) qualifies.
"""
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build as B  # noqa: E402

kind, tc = B.toolchain()
if kind != "ecj":
    java = B.shutil.which("java")
    out = os.path.join(B.BUILD, "cc")
    os.makedirs(out, exist_ok=True)
    subprocess.run(["javac", "--release", "25", "-d", out, os.path.join(B.ROOT, "tools", "ClassCompare.java")], check=True)
else:
    java = tc["java"]
    out = os.path.join(B.BUILD, "cc")
    os.makedirs(out, exist_ok=True)
    subprocess.run([java, "-jar", tc["ecj"], "-source", "25", "-target", "25", "-nowarn", "--system", os.path.dirname(os.path.dirname(java)),
                    "-d", out, os.path.join(B.ROOT, "tools", "ClassCompare.java")], check=True)
sys.exit(subprocess.run([java, "-cp", out, "ClassCompare"] + sys.argv[1:]).returncode)
