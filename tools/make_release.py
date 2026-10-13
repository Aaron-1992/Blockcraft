#!/usr/bin/env python3
"""
Assembles what goes onto the GitHub *Releases* page, from the files that are committed in the repository:

    releases/Blockcraft-UPDATE<ver>-patch.zip      (built by tools/build.py --patch, smoke-tested on Windows)
    Blockcraft-1.12-UPDATE3.5-windows.zip          (the base kit with the bundled Java)

and writes into build/release/:

    Blockcraft-1.12-UPDATE<ver>-windows.zip   the complete game: base kit + the files of the update pack.
                                              This is what a player downloads - extract anywhere, run Blockcraft.exe.
    Blockcraft-UPDATE<ver>-patch.zip          the small update pack (copied unchanged)
    release-notes.md                          the "what's new" text of that version + install instructions
    VERSION                                   e.g. "3.6"

It deliberately does NOT compile anything: the released bytes are exactly the committed (and tested) update pack.
Used by .github/workflows/release.yml; can be run locally too:  python3 tools/make_release.py [version]
"""
import glob
import hashlib
import os
import re
import shutil
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build as B  # noqa: E402

ROOT = B.ROOT
OUT = os.path.join(ROOT, "build", "release")


def version_key(path):
    m = re.search(r"UPDATE([0-9.]+)-patch\.zip$", path)
    return tuple(int(x) for x in m.group(1).split(".")) if m else (0,)


def find_patch(version=None):
    zips = glob.glob(os.path.join(ROOT, "releases", "Blockcraft-UPDATE*-patch.zip"))
    if version:
        zips = [z for z in zips if z.endswith("UPDATE%s-patch.zip" % version)]
    if not zips:
        print("Building update pack for version %s via tools/build.py --patch ..." % (version or "latest"))
        import subprocess
        subprocess.run([sys.executable, os.path.join(ROOT, "tools", "build.py"), "--patch"], check=True)
        zips = glob.glob(os.path.join(ROOT, "releases", "Blockcraft-UPDATE*-patch.zip"))
        if version:
            zips = [z for z in zips if z.endswith("UPDATE%s-patch.zip" % version)]
    if not zips:
        sys.exit("no update pack found in releases/ (version %s) - run: python3 tools/build.py --patch" % (version or "latest"))
    return sorted(zips, key=version_key)[-1]


def whats_new(version):
    """The 'WHAT'S NEW IN UPDATE x.y' or 'WHAT'S NEW IN V x.y' section of package/README.txt."""
    text = open(os.path.join(ROOT, "package", "README.txt"), encoding="utf-8", errors="replace").read().replace("\r\n", "\n")
    m = re.search(r"^WHAT'S NEW IN (?:UPDATE |V)%s\b.*?$(.*?)(?=^(?:WHAT'S NEW IN |NEW IN [0-9]|FIXES IN|CONTROLS)\b)" % re.escape(version), text, re.S | re.M)
    if not m:
        return "(see README.txt inside the zip)"
    heading = re.search(r"^WHAT'S NEW IN (?:UPDATE |V)%s\b.*?$" % re.escape(version), text, re.M).group(0).strip()
    return heading + "\n" + m.group(1).rstrip()


def notes(version, full_name, patch_name):
    return (
        "## Blockcraft V%(v)s\n\n"
        "### Install (Windows 64-bit, nothing else needed - Java is inside)\n\n"
        "1. Download **%(full)s** below (\"Assets\").\n"
        "2. Right-click it -> *Extract All...* into a **new folder** (anywhere you like).\n"
        "3. Open the extracted `Blockcraft` folder and run **Blockcraft.exe**.\n\n"
        "Your worlds and options live in `%%USERPROFILE%%\\.blockcraft112`, so they are **kept** when you use a new folder.\n"
        "If Windows SmartScreen warns about an unknown publisher: *More info -> Run anyway* (the game is not code-signed).\n"
        "If it doesn't start, run `Blockcraft-debug.bat` and read the messages (or open `%%USERPROFILE%%\\.blockcraft112\\crash.log`).\n\n"
        "*Small download instead?* `%(patch)s` (about 6 MB) only contains the changed files - extract it and copy the "
        "`Blockcraft` folder over an existing Blockcraft install of V3.5 or newer (\"Replace files\").\n\n"
        "### What's new\n\n```\n%(new)s\n```\n"
    ) % {"v": version, "full": full_name, "patch": patch_name, "new": whats_new(version)}


def main():
    version = sys.argv[1] if len(sys.argv) > 1 else None
    patch = find_patch(version)
    version = re.search(r"UPDATE([0-9.]+)-patch\.zip$", patch).group(1)
    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(OUT)

    # files of the update pack -> replacements inside the base kit
    rep = {}
    with zipfile.ZipFile(patch) as z:
        for i in z.infolist():
            if i.is_dir() or not i.filename.startswith("Blockcraft/"):
                continue  # HOW-TO-INSTALL-THIS-UPDATE.txt is only for the patch
            mode = (i.external_attr >> 16) & 0o777 or 0o644
            rep[i.filename] = (z.read(i), mode)
    for need in ("Blockcraft/Blockcraft.exe", "Blockcraft/blockcraft.jar", "Blockcraft/README.txt"):
        if need not in rep:
            sys.exit("update pack %s lacks %s" % (os.path.basename(patch), need))

    full_name = "Blockcraft-V%s-windows.zip" % version
    patch_name = "Blockcraft-V%s-patch.zip" % version
    full = B.compose_full_zip(os.path.join(OUT, full_name), rep)
    shutil.copy(patch, os.path.join(OUT, patch_name))

    # ---- self-check: the zip a player gets must be complete and contain exactly the tested files ----
    with zipfile.ZipFile(full) as z, zipfile.ZipFile(B.BASE_ZIP) as base:
        assert z.testzip() is None, "corrupt zip"
        names = set(z.namelist())
        missing = [n for n in base.namelist() if n not in names]
        assert not missing, "missing from the full zip: %s" % missing[:5]
        for n, (data, _mode) in rep.items():
            assert z.read(n) == data, "%s differs from the update pack" % n
        for n in base.namelist():
            if n not in rep and not n.endswith("/"):
                assert z.read(n) == base.read(n), "%s differs from the base kit" % n
        assert "Blockcraft/jre/bin/javaw.exe" in names, "bundled Java is missing"
        n_files = len([n for n in names if not n.endswith("/")])
    with zipfile.ZipFile(patch) as z:
        assert z.testzip() is None

    with open(os.path.join(OUT, "release-notes.md"), "w", encoding="utf-8") as f:
        f.write(notes(version, full_name, patch_name))
    with open(os.path.join(OUT, "VERSION"), "w") as f:
        f.write(version)

    for name in sorted(os.listdir(OUT)):
        p = os.path.join(OUT, name)
        h = hashlib.sha256(open(p, "rb").read()).hexdigest()[:16]
        print("[release] %-44s %8.2f MB  sha256 %s" % (name, os.path.getsize(p) / 1e6, h))
    print("[release] OK - Update %s, full zip has %d files, self-check passed" % (version, n_files))


if __name__ == "__main__":
    main()
