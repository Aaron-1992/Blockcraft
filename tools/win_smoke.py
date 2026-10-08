#!/usr/bin/env python3
"""
Windows smoke test (runs on a GitHub 'windows-latest' machine, see .github/workflows/windows-smoke.yml).

It installs the game exactly like a player would - the 3.5 base zip, then the freshly built update pack on top -
puts a software OpenGL implementation (Mesa llvmpipe, GitHub's Windows machines have no GPU) next to the bundled
javaw.exe, and starts the REAL Blockcraft.exe (Launch4j stub -> bundled JRE -> jar -> LWJGL Windows natives -> OpenGL)
with the game's built-in test hooks (-Dbc.* passed through JAVA_TOOL_OPTIONS). Screenshots and logs are copied to
ci-results/windows/.

    python tools/win_smoke.py [scene,scene,...]
"""
import glob
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT = os.path.join(ROOT, "ci-results", "windows")
sys.path.insert(0, HERE)


def log(msg):
    print(msg, flush=True)
    with open(os.path.join(OUT, "smoke.log"), "a", encoding="utf-8") as f:
        f.write(msg + "\n")



def newest_patch():
    """The update pack with the highest version number (3.10 is newer than 3.9, which plain sorting gets wrong)."""
    packs = glob.glob(os.path.join(ROOT, "releases", "Blockcraft-UPDATE*-patch.zip"))

    def key(path):
        m = re.search(r"UPDATE([0-9.]+)-patch\.zip$", path)
        return tuple(int(x) for x in m.group(1).split(".")) if m else (0,)

    return sorted(packs, key=key)[-1]


def install_game():
    base = os.path.join(ROOT, "Blockcraft-1.12-UPDATE3.5-windows.zip")
    patch = newest_patch()
    tmp = os.path.join(tempfile.gettempdir(), "bc-win-smoke")
    shutil.rmtree(tmp, ignore_errors=True)
    os.makedirs(tmp)
    with zipfile.ZipFile(base) as z:
        z.extractall(os.path.join(tmp, "install"))
    log("extracted base zip: %s" % os.path.basename(base))
    with zipfile.ZipFile(patch) as z:
        z.extractall(os.path.join(tmp, "patch"))
    log("extracted update pack: %s" % os.path.basename(patch))
    shutil.copytree(os.path.join(tmp, "patch", "Blockcraft"), os.path.join(tmp, "install", "Blockcraft"), dirs_exist_ok=True)
    return os.path.join(tmp, "install", "Blockcraft"), tmp


def add_mesa(game_dir, tmp):
    """Software OpenGL for the GPU-less runner: Mesa's opengl32.dll next to javaw.exe wins the DLL search."""
    headers = {"User-Agent": "blockcraft-ci"}
    if os.environ.get("GITHUB_TOKEN"):
        headers["Authorization"] = "Bearer " + os.environ["GITHUB_TOKEN"]
    rel = json.load(urllib.request.urlopen(urllib.request.Request("https://api.github.com/repos/pal1000/mesa-dist-win/releases/latest", headers=headers), timeout=60))
    asset = next(a for a in rel["assets"] if a["name"].startswith("mesa3d-") and a["name"].endswith("-release-msvc.7z"))
    log("Mesa: %s (%.0f MB)" % (asset["name"], asset["size"] / 1e6))
    arc = os.path.join(tmp, "mesa.7z")
    urllib.request.urlretrieve(asset["browser_download_url"], arc)
    ex = os.path.join(tmp, "mesa")
    subprocess.run(["7z", "x", "-y", "-o" + ex, arc], check=True, stdout=subprocess.DEVNULL)
    x64 = os.path.join(ex, "x64")
    bin_dir = os.path.join(game_dir, "jre", "bin")
    copied = []
    for name in ("opengl32.dll", "libgallium_wgl.dll", "libglapi.dll"):
        src = os.path.join(x64, name)
        if os.path.exists(src):
            shutil.copy(src, bin_dir)
            copied.append(name)
    log("copied next to javaw.exe: %s" % ", ".join(copied))
    if "opengl32.dll" not in copied:
        log("WARNING: no opengl32.dll found in the Mesa package; x64 contains: %s" % ", ".join(sorted(os.listdir(x64))[:40]))


def scenes():
    import shots  # tools/shots.py: SCENES with the -Dbc.* properties (same scenes as the Linux visual run)
    return shots.SCENES


def run_scene(game_dir, name, props, timeout):
    home = os.path.join(os.path.expanduser("~"), ".blockcraft112")
    shutil.rmtree(home, ignore_errors=True)
    env = dict(os.environ)
    env["GALLIUM_DRIVER"] = "llvmpipe"
    env["LP_NUM_THREADS"] = "2"
    env["MESA_GL_VERSION_OVERRIDE"] = "4.5COMPAT"
    exe = os.path.join(game_dir, "Blockcraft.exe")
    opts = " ".join(props)
    if len(opts) > 600:
        # The Launch4j stub probes `java -version` and chokes on a huge "Picked up JAVA_TOOL_OPTIONS" line (it seems to read the whole
        # output - that line plus the three version lines - into a 1 KB buffer: 888 characters hung the 3.10 treefall scenes), so scenes
        # with long properties start the bundled JRE directly, with the exe itself as the jar (it IS a jar with a stub in front).
        cmd = [os.path.join(game_dir, "jre", "bin", "java.exe"), "-Xmx2G"] + props + ["-jar", exe]
        how = "java.exe -jar Blockcraft.exe"
    else:
        cmd = [exe]
        env["JAVA_TOOL_OPTIONS"] = opts
        how = "Blockcraft.exe (Launch4j stub)"
    log("%s: starting via %s" % (name, how))
    t0 = time.time()
    p = subprocess.Popen(cmd, env=env, cwd=game_dir, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    try:
        out, _ = p.communicate(timeout=timeout)
        status = "exit code %s" % p.returncode
    except subprocess.TimeoutExpired:
        p.kill()
        out, _ = p.communicate()
        status = "TIMEOUT after %ds (killed)" % timeout
    if any("bc.gpuforce" in p for p in props):
        # Update 3.18: the game started itself a second time (the GPU preference: see GpuPref.relaunch) and the first process is gone: the picture
        # is taken by the second one, so wait for it
        deadline = time.time() + timeout
        while time.time() < deadline and not glob.glob(os.path.join(home, "screenshots", "*.png")):
            time.sleep(2)
        time.sleep(3)
        status += " (+ the second start)"
    dt = time.time() - t0
    for f in ("latest.log", "crash.log"):
        src = os.path.join(home, f)
        if os.path.exists(src):
            shutil.copy(src, os.path.join(OUT, "%s-%s" % (name, f)))
    shots = sorted(glob.glob(os.path.join(home, "screenshots", "*.png")))
    ok = False
    if shots:
        try:
            from PIL import Image
            im = Image.open(shots[-1]).convert("RGB")
            im.thumbnail((960, 540))
            im.save(os.path.join(OUT, "%s.jpg" % name), quality=80)
            ok = True
        except Exception as e:  # keep the PNG if PIL is missing
            shutil.copy(shots[-1], os.path.join(OUT, "%s.png" % name))
            ok = True
            log("  (could not make a JPEG: %s)" % e)
    log("%-16s %-28s %5.0fs  screenshot=%s" % (name, status, dt, "yes" if ok else "NO"))
    tail = (out or b"").decode("utf-8", "replace").strip().splitlines()[-6:]
    for ln in tail:
        log("    | " + ln[:200])
    return ok


def main():
    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(OUT)
    wanted = [s for s in (sys.argv[1].split(",") if len(sys.argv) > 1 else ["title", "day-plain", "farm"]) if s]
    game_dir, tmp = install_game()
    log("installed to %s" % game_dir)
    add_mesa(game_dir, tmp)
    sc = scenes()
    results = {}
    for name in wanted:
        props, _only_new = sc[name]
        results[name] = run_scene(game_dir, name, props, 300)
    try:
        # Update 3.18: the game writes its graphics preference for Windows ("Settings > Display > Graphics") for the javaw.exe of the bundled Java
        q = subprocess.run(["reg", "query", r"HKCU\Software\Microsoft\DirectX\UserGpuPreferences"], capture_output=True, text=True, timeout=30)
        lines = [l.strip() for l in (q.stdout or "").splitlines() if "java" in l.lower()]
        log("GPU preference entries in the registry (%d):" % len(lines))
        for l in lines:
            log("    | " + l)
        results["gpu_pref_in_registry"] = any("GpuPreference=2" in l for l in lines)
    except Exception as e:
        log("could not read the registry: %s" % e)
    log("RESULT: " + json.dumps(results))
    sys.exit(0 if any(results.values()) else 1)


if __name__ == "__main__":
    main()
