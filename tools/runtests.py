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


def run_java(cp, main, *args, timeout=900, props=()):
    cmd = [B.java_exe(), "-Xmx2g", "-Djava.awt.headless=true", *props, "-cp", os.pathsep.join(cp), main, *args]
    return subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)


def parse_fingerprint(text):
    """'label = value' -> {label: value};  'label threw X' -> {label: 'threw X'}."""
    d = {}
    for line in text.splitlines():
        if " = " in line:
            k, v = line.split(" = ", 1)
        elif " threw " in line:
            k, v = line.split(" threw ", 1)
            v = "threw " + v
        else:
            k, v = line, ""
        d[k] = v
    return d


def allowed_prefixes():
    path = os.path.join(ROOT, "test", "equivalence-allowed.txt")
    out = []
    if os.path.exists(path):
        for ln in open(path, encoding="utf-8"):
            ln = ln.rstrip("\n")
            if ln.strip() and not ln.lstrip().startswith("#"):
                out.append(ln.strip())
    return out


def equivalence(classes):
    """
    Fingerprints of the pure-logic subsystems must match the ORIGINAL 3.5 jar, except for the areas listed in
    test/equivalence-allowed.txt (changed on purpose since 3.5). Anything else that differs is a regression.
    """
    ref = reference_jar()
    old = run_java([TEST_CLASSES, ref], "bc.Equivalence")
    new = run_java([TEST_CLASSES, classes, LWJGL], "bc.Equivalence")
    for tag, r in (("reference 3.5 jar", old), ("rebuilt classes", new)):
        if r.returncode != 0:
            print("--- stdout (tail) ---\n" + r.stdout[-2500:])
            print("--- stderr (tail) ---\n" + r.stderr[-2500:])
            sys.exit("equivalence run failed for %s (exit code %s)" % (tag, r.returncode))
    a, b = parse_fingerprint(old.stdout), parse_fingerprint(new.stdout)
    allowed = allowed_prefixes()
    bad, tolerated, added = [], 0, 0
    for k in sorted(set(a) | set(b)):
        if a.get(k) == b.get(k):
            continue
        if k not in a:
            added += 1  # a brand-new fingerprint (new feature): always fine
            continue
        if any(k.startswith(p) for p in allowed):
            tolerated += 1
            continue
        bad.append("  %s\n    3.5 : %s\n    now : %s" % (k, a.get(k, "(absent)"), b.get(k, "(absent)")))
    if bad:
        report = "equivalence: %d UNEXPECTED differences vs the 3.5 jar:\n%s" % (len(bad), "\n".join(bad[:60]))
        print(report)
        with open(os.path.join(BUILD, "equivalence-diff.txt"), "w") as f:
            f.write(report + "\n--- stderr (3.5) ---\n" + old.stderr[-3000:] + "\n--- stderr (now) ---\n" + new.stderr[-3000:])
        return False
    # strict pass: with the Update 3.7 worldgen extras (sugar cane, pumpkin patches), the 3.9 smart leaves and the 3.11 back faces switched off, terrain generation, the
    # loaded world and the simulation must be IDENTICAL to Update 3.5 - proof that the extras are purely additive.
    strict = run_java([TEST_CLASSES, classes, LWJGL], "bc.Equivalence", props=["-Dbc.noextras=true", "-Dbc.leaves=0", "-Dbc.legacymesh=true", "-Dbc.legacyfluids=true"])
    if strict.returncode != 0:
        print("--- stderr (tail) ---\n" + strict.stderr[-2500:])
        sys.exit("strict equivalence run failed (exit code %s)" % strict.returncode)
    c = parse_fingerprint(strict.stdout)
    strict_bad = ["  %s\n    3.5 : %s\n    now : %s" % (k, a[k], c.get(k, "(absent)"))
                  for k in sorted(a) if k.startswith(("gen ", "world ", "tick", "mesh terrain")) and a[k] != c.get(k)]
    if strict_bad:
        print("equivalence (worldgen extras off): %d differences vs the 3.5 jar:\n%s" % (len(strict_bad), "\n".join(strict_bad[:40])))
        return False
    same = sum(1 for k in a if k in b and a[k] == b[k])
    print("equivalence: terrain / world / tick fingerprints are identical to 3.5 once the 3.7 worldgen extras are off")
    print("equivalence: OK - %d fingerprints identical to the 3.5 jar, %d changed on purpose (test/equivalence-allowed.txt), %d new" % (same, tolerated, added))
    return True


def unit(classes):
    if not os.path.exists(os.path.join(ROOT, "test", "bc", "GameTests.java")):
        print("unit: no GameTests yet")
        return True
    r = run_java([TEST_CLASSES, classes, LWJGL, os.path.join(ROOT, "res")], "bc.GameTests")
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
