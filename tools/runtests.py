#!/usr/bin/env python3
"""
Runs the headless tests (no GPU / display needed).

    python3 tools/runtests.py equivalence   fingerprint the logic code and compare with the ORIGINAL Update 3.5 jar
    python3 tools/runtests.py unit          game-logic tests (crafting, farming, ladders, buckets, TNT, beds, ...)
    python3 tools/runtests.py               both

`equivalence` only makes sense for code that was NOT intentionally changed since 3.5; once a feature changes
behaviour on purpose, the fingerprints of the touched parts are expected to differ (see docs/DEVELOPMENT.md).
"""
import os
import shutil
import subprocess
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build as B  # noqa: E402

ROOT = B.ROOT
BUILD = B.BUILD
TEST_CLASSES = os.path.join(BUILD, "test-classes")
LWJGL = os.path.join(ROOT, "libs", "lwjgl-bundle.jar")


def ensure_built():
    classes = os.path.join(BUILD, "classes")
    if not B.compile_java(classes, [os.path.join(ROOT, "src")], [LWJGL]):
        sys.exit("COMPILE FAILED")
    return classes


def build_tests(classes):
    if not B.compile_java(TEST_CLASSES, [os.path.join(ROOT, "test")], [classes, LWJGL]):
        sys.exit("TEST COMPILE FAILED")


def reference_jar():
    out = os.path.join(BUILD, "ref", "blockcraft-3.5.jar")
    if not os.path.exists(out):
        os.makedirs(os.path.dirname(out), exist_ok=True)
        with zipfile.ZipFile(B.BASE_ZIP) as z:
            with open(out, "wb") as f:
                f.write(z.read("Blockcraft/blockcraft.jar"))
    return out


def run_java(cp, main, *args, timeout=900):
    cmd = [B.java_exe(), "-Xmx2g", "-Djava.awt.headless=true", "-cp", os.pathsep.join(cp), main, *args]
    return subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)


def equivalence(classes):
    ref = reference_jar()
    old = run_java([TEST_CLASSES, ref], "bc.Equivalence")
    new = run_java([TEST_CLASSES, classes, LWJGL], "bc.Equivalence")
    for tag, r in (("reference 3.5 jar", old), ("rebuilt classes", new)):
        if r.returncode != 0:
            print("--- stdout (tail) ---\n" + r.stdout[-2500:])
            print("--- stderr (tail) ---\n" + r.stderr[-2500:])
            sys.exit("equivalence run failed for %s (exit code %s)" % (tag, r.returncode))
    a, b = old.stdout.splitlines(), new.stdout.splitlines()
    diffs = [(i, x, y) for i, (x, y) in enumerate(zip(a, b)) if x != y]
    if len(a) != len(b):
        print("line count differs: %d vs %d" % (len(a), len(b)))
    if diffs or len(a) != len(b):
        lines = []
        for i, x, y in diffs[:60]:
            lines.append("DIFF line %d\n  3.5 : %s\n  now : %s" % (i + 1, x, y))
        lines.append("equivalence: %d differing lines of %d" % (len(diffs), len(a)))
        report = "\n".join(lines)
        print(report)
        with open(os.path.join(BUILD, "equivalence-diff.txt"), "w") as f:
            f.write(report + "\n--- stderr (3.5) ---\n" + old.stderr[-3000:] + "\n--- stderr (now) ---\n" + new.stderr[-3000:])
        return False
    print("equivalence: IDENTICAL (%d fingerprint lines)" % len(a))
    return True


def unit(classes):
    if not os.path.exists(os.path.join(ROOT, "test", "bc", "GameTests.java")):
        print("unit: no GameTests yet")
        return True
    r = run_java([TEST_CLASSES, classes, LWJGL], "bc.GameTests")
    print(r.stdout)
    if r.returncode != 0:
        print(r.stderr[-3000:])
        return False
    return True


def main():
    what = sys.argv[1] if len(sys.argv) > 1 else "all"
    classes = ensure_built()
    build_tests(classes)
    ok = True
    if what in ("equivalence", "all"):
        ok &= equivalence(classes)
    if what in ("unit", "all"):
        ok &= unit(classes)
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
