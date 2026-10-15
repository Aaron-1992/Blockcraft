# V6.10: sound-enabled Block Drop and Shadow Signal

**Shadow Signal is an original 32-second silhouette animation and synthesized score, not the Bad Apple!! video/song.** No external footage, recording or character art is included in this new cartridge. The generator is [`tools/media/make_shadow_signal.py`](../../tools/media/make_shadow_signal.py); the shipped bounded BCV asset is in `res/media/`.

## Play

Install V6.10, then install the two **new**, non-overwriting saves in `Blockcraft-Sound-Machines.zip`. Use view distance 5+. Right-click the keypad with an empty hand, then Enter. Both worlds start paused.

* **Shadow Signal — Original Silhouette Cinema:** Space/P pauses; arrows seek five seconds; Enter/R restarts; M mutes; Escape leaves controls. A 64×48 joined-panel display and 32×24 ordinary redstone lamps show the same ROM-controlled frame. The lamps retain normal redstone afterglow; the panel wall is sharper. There are speakers at the console and beside the panel wall.
* **Block Drop — Sound Edition:** the V6.9 game with actual `TONE` instructions for movement, rotation, start/pause, locking, line clears and game over. Its controls are unchanged, plus M to mute.

Existing V6.9 world: back it up, look at a connected component in Creative, then `/arcadesound`. This recognizes only the exact supplied ROM, adds a speaker without replacing blocks, and **resets the current game**. Custom programs are refused rather than silently overwritten. `/arcade` now builds the sound edition; `/shadow` builds the new cinema.

The complete player/building/import/instruction guide is [`package/Sound-machines.txt`](../../package/Sound-machines.txt). The original raster architecture is documented in [raster.md](raster.md).

## No hidden movie/game controller

The physical CPU, Program ROM, RAM, GPU, cables, keypad and screens still drive the machine. The new **Raster Speaker** and **Media Cartridge** are reusable, craftable hardware, not special game-event detectors. The actual generic movie program is [`MediaPlayer.asm`](../../package/MediaPlayer.asm); the sound game is [`FallingBlocks-Sound.asm`](../../package/FallingBlocks-Sound.asm). The old program with audio disabled assembles to exactly the V6.9 bytecode.

* `TONE` (opcode 33): MIDI pitch register, millisecond duration register, waveform/volume register. Four synthesized waveforms, bounded polyphony and per-tick request budgets.
* `VIDEO` (opcode 34): frame register and flags register; bit 0 enables synchronized audio. The cartridge provides frame pixels and PCM, while the ROM controls the frame counter, pause, seek and loop.
* `IN` ports 32–35 report media width, height, frame count and fps. Old opcodes 0–32 are unchanged.
* The lamp adapter can sample at 1×/2×/4×/8×. It drives ordinary lamp blocks through actual pixel drivers and redstone power, not a renderer overlay.
* Same-color rectangles within each 4×4 panel merge their geometry, preserving every addressable pixel and four-way facing. Existing textures, flat sprite bow and held lighting were not redesigned.

## Sound and persistence

Mono 44.1 kHz data is mixed into the existing stereo SoundEngine before environmental processing. Speakers have distance rolloff, stereo pan, mute and quiet modes. An audible soundtrack ducks background music. Audio cannot consume data beyond the last submitted frame. A bounded frame/device-block buffer (under 62ms) avoids repeatedly discarding PCM tails at the 512-sample device / 2,205-sample video boundary; seeks and loops reset immediately. This is not zero-latency/sample-perfect physical-screen sync: device buffers, mesh updates, lamp afterglow and networking add delay. Simulation stalls can cause audio holds/stutters.

Native saves persist ROM/CPU/RAM/framebuffer state plus `media.properties` cartridge references. Custom data is content-addressed in `world/media/<SHA256>.bcv`; corrupt maps are preserved rather than overwritten. Imports are bounded, simple-name only and Creative-host only. See the guide for `Make-media.py` (PNG sequence + PCM16 WAV), `/mediaload` and file limits.

Only the host executes ROMs. Tone and media-position commands use the existing host-authoritative packet path; late receivers start at the host frame. Custom files must be copied separately to clients; no automatic large-media transport. The builtin original is in every V6.10 game. Packet-path tests do not imply a real two-PC multiplayer audition.

## World production

`tools/worlds/BuildSoundWorlds.java` creates the two native saves and verifies reloaded chunks, all cinema panel/lamp pixels, ROM equality, controls, PCM mixing, disconnected-bus behavior and non-mutating verification. `--verify` checks the **actual extracted distributed ZIP**, without generating replacements. `tools/worlds/pack_sound_worlds.py` adds editable ROMs, guides, the non-overwriting installer and per-file checksums, then writes the deterministic ZIP. The historical V6.9 builder refuses to create mislabeled worlds against V6.10 classes.

See [V6.10 verification](../releases/v6.10-verification.md) for release evidence and remaining limits.
