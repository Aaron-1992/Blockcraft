# Repository / game-folder layout (V6.34)

The repo had accumulated **1.3 GB** of old update-pack ZIPs and three loose
copies of full Windows downloads. V6.31 removes the 25 obsolete patch packs
(the canonical releases stay on the GitHub Releases page) and documents where
everything now lives, so the checkout is ~156 MB instead of ~1.3 GB.

## Source

| Path | What it is |
| --- | --- |
| `src/bc/` | All game Java sources. `Trailer.java` (montage), `Caves7.java` / `CaveStructures7.java` (V6.34 3D cave volume and integrated structures), `Caves6.java` / `CaveStructures6.java` (V6.32), `Caves5.java` / `CaveStructures5.java` (V6.31) — all kept so old saves keep their own carver, `Lod.java` (cross-fades). |
| `test/bc/` | Headless regression suites, one `Tests6xx.java` per milestone. |
| `tools/` | `build.py`, `runtests.py`, `glsl_check.py`, `make_release.py`, `verify_release.py`, `tilesheet.py`. |
| `res/` | Resources copied into `blockcraft.jar`. `res/sounds/soundbank.bin` is the big music bank; `res/sounds/foley/` holds the ten real CC0 WAV recordings + `CREDITS.txt`. |
| `package/` | Files shipped loose next to `Blockcraft.exe` (README, guides, the editable tilesheet, `.bat` shortcuts). |
| `textures/` | Source PNGs for the atlas and the one-sheet tilesheet. |
| `docs/` | `releases/v6.xx.md` notes, `releases/v6.31-trailer-plan.txt`, `screenshots/` headless previews, `FILE-LAYOUT.md` (this file). |
| `releases/` | Only the **current** update pack plus the three most recent ones and `6.16` (used by `perf.yml`). Full Windows builds are **not** committed here — they are release assets on GitHub. |
| `ci/release-trigger.txt` | One line: the version the Release workflow publishes. |
| Root `Blockcraft-1.12-UPDATE3.5-windows.zip` | The base kit (bundled JRE) that `make_release.py` composes the full download from — required, do not delete. |
| Root sample-world ZIPs | Example worlds referenced by the historical V6.9/V6.10 release notes. |

## Inside the extracted Windows game folder

```
Blockcraft/
  Blockcraft.exe, blockcraft.jar, jre/        the game
  README.txt, Trailer-guide.txt, ...         short guides (see README's index)
  tilesheet.png, tilesheet-default.png       one-sheet texture editor
  sounds/soundbank.bin                       music / foley bank
  sounds/foley/*.wav + CREDITS.txt           real CC0 recordings (editable)
  resourcepacks/, shaderpacks/, mods/        user content
  saves/                                     your worlds (never in the zip)
```

`%USERPROFILE%\.blockcraft112\` holds options, saves, `latest.log` and
`crash.log` — it survives every update.
