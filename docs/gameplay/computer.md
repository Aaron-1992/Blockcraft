# Buildable 4-bit computer and furniture facing — V6.8

The player-facing guide is packaged with both downloads: [Computer-guide.txt](../../package/Computer-guide.txt). It specifies the real wiring, ROM layout, opcodes, controls, crafting, reload/network behavior and limits.

Implementation: `Circuit` evaluates independent components and cable nets; `CircuitComputer` places a tested physical circuit but never executes instructions. `Facing` preserves directional furniture properties and applies chest rotation after lid hinging. `TestsComputer` exercises the assembled computer, changed programs, broken wires, automatic wraparound, save/load, partial unloading, network message application and directional rendering contracts.
