#!/usr/bin/env python3
"""
Sets up a Java toolchain for building Blockcraft, even on a machine with NO JDK installed and
a locked-down network (only github.com / npm / PyPI reachable).

What it provides (in $BC_TOOLCHAIN, default /tmp/blockcraft-toolchain):
  jvm/            a real Java 25 runtime (from the `jdk4py` wheel on PyPI)   -> runs the compiler + tests
  ecj.jar         the Eclipse Java compiler, taken from the npm package @vscjava/java-language-server
  winjre/         lib/modules etc. of the *Windows* JRE that ships with the game (from the base zip),
                  so the code is compiled against exactly the Java 21 API the players will run
  fernflower.jar  (optional) IntelliJ decompiler, only needed to re-decompile an old jar

If a full JDK (javac) is already on PATH, nothing is downloaded and build.py just uses it.

Usage: python3 tools/bootstrap.py
"""
import glob
import json
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TC = os.environ.get("BC_TOOLCHAIN", os.path.join(tempfile.gettempdir(), "blockcraft-toolchain"))
BASE_ZIP = os.path.join(ROOT, "Blockcraft-1.12-UPDATE3.5-windows.zip")
JDK4PY = "jdk4py==25.0.2.1"
JDT_NPM = "@vscjava/java-language-server@0.1.2"


def run(cmd, **kw):
    print("  $", " ".join(cmd))
    subprocess.run(cmd, check=True, **kw)


def have_javac():
    from shutil import which
    return which("javac") is not None and which("java") is not None


def setup_jvm():
    java = os.path.join(TC, "jvm", "bin", "java")
    if os.path.exists(java):
        return
    print("[bootstrap] fetching a Java runtime from PyPI (jdk4py)...")
    with tempfile.TemporaryDirectory() as tmp:
        run([sys.executable, "-m", "pip", "download", "--no-deps", "--only-binary=:all:", "-q", "-d", tmp, JDK4PY])
        whl = glob.glob(os.path.join(tmp, "jdk4py-*.whl"))[0]
        ex = os.path.join(tmp, "x")
        with zipfile.ZipFile(whl) as z:
            z.extractall(ex)
        shutil.rmtree(os.path.join(TC, "jvm"), ignore_errors=True)
        os.makedirs(TC, exist_ok=True)
        shutil.copytree(os.path.join(ex, "jdk4py", "java-runtime"), os.path.join(TC, "jvm"))
    for f in glob.glob(os.path.join(TC, "jvm", "bin", "*")) + glob.glob(os.path.join(TC, "jvm", "lib", "jspawnhelper")):
        os.chmod(f, 0o755)


def setup_ecj():
    ecj = os.path.join(TC, "ecj.jar")
    ff = os.path.join(TC, "fernflower.jar")
    if os.path.exists(ecj) and os.path.exists(ff):
        return
    print("[bootstrap] fetching the Eclipse compiler (ECJ) from npm (%s)..." % JDT_NPM)
    with tempfile.TemporaryDirectory() as tmp:
        run(["npm", "pack", "--silent", JDT_NPM], cwd=tmp)
        tgz = glob.glob(os.path.join(tmp, "*.tgz"))[0]
        run(["tar", "-xzf", tgz, "-C", tmp])
        plugins = os.path.join(tmp, "package", "server", "plugins")
        shutil.copy(glob.glob(os.path.join(plugins, "org.eclipse.jdt.core.compiler.batch_*.jar"))[0], ecj)
        dec = glob.glob(os.path.join(plugins, "wrapped.com.jetbrains.intellij.java.java-decompiler-engine_*.jar"))
        if dec:
            shutil.copy(dec[0], ff)


def setup_winjre():
    dst = os.path.join(TC, "winjre")
    if os.path.exists(os.path.join(dst, "lib", "modules")):
        return
    if not os.path.exists(BASE_ZIP):
        sys.exit("base zip not found: %s" % BASE_ZIP)
    print("[bootstrap] extracting the Windows JRE class library from the base zip...")
    os.makedirs(dst, exist_ok=True)
    with zipfile.ZipFile(BASE_ZIP) as z:
        for name in ("lib/modules", "lib/jrt-fs.jar", "release"):
            data = z.read("Blockcraft/jre/" + name)
            p = os.path.join(dst, name)
            os.makedirs(os.path.dirname(p), exist_ok=True)
            with open(p, "wb") as f:
                f.write(data)


def main():
    os.makedirs(TC, exist_ok=True)
    if have_javac():
        print("[bootstrap] a JDK with javac is on PATH - using it; nothing to download.")
        return
    setup_jvm()
    setup_ecj()
    setup_winjre()
    info = {
        "java": os.path.join(TC, "jvm", "bin", "java"),
        "ecj": os.path.join(TC, "ecj.jar"),
        "winjre": os.path.join(TC, "winjre"),
        "fernflower": os.path.join(TC, "fernflower.jar"),
    }
    with open(os.path.join(TC, "toolchain.json"), "w") as f:
        json.dump(info, f, indent=2)
    print("[bootstrap] ready:", TC)


if __name__ == "__main__":
    main()
