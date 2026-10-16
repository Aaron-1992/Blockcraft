# Native Create project — status, not a completion claim

**Target requested:** Create's systems inside Blockcraft, without Minecraft.
**Current status:** V6.15 visible processing and straight-belt milestone; **full Create is not implemented**.

Reference: [Create 6.0.10 for Minecraft 1.21.1](https://github.com/Creators-of-Create/Create/releases/tag/mc1.21.1-6.0.10), commit `ac0c444d9828da3453ae8cc65338e8de063286fb`. This pins the target rather than silently changing what “fully featured” means. Create add-ons are outside this core-mod baseline unless separately requested.

The [upstream license at this exact commit](https://github.com/Creators-of-Create/Create/blob/ac0c444d9828da3453ae8cc65338e8de063286fb/LICENSE.md) licenses code under MIT but reserves the assets. Only code-level behavior is referenced/adapted; no upstream textures, sounds, models, branding images or tutorial scenes are redistributed. Original Blockcraft visuals are used. Attribution/license ships with the game.

[reference-inventory.json](reference-inventory.json) records named literal registrations found in the pinned AllBlocks/AllItems sources. It deliberately does **not** pretend to expand generated families or prove recipe/behavior completeness. Every entry is either `not_implemented` or `native_partial`; none is certified parity.

## Tracked scope

| System | Current state | What is still required for parity |
|---|---|---|
| Rotation networks | Native signed-speed graph, shaft connectivity, same-plane cogs, diagonal 2:1 cogs, bevel gears, gearbox ports; tested | Exact upstream source propagation/fault behavior, broader layout/performance validation |
| Stress and control | Local-speed-weighted SU, overload recovery, clutch/gearshift, two gauges; tested | Configurable server values, exact controller behavior, rotation speed controller, adjustable chain drives |
| Generation | Creative motor and compact wheel with actual current-vector torque | Exact wheel geometry/flow scoring, large wheel, hand crank, windmill bearing/sails, steam engines/boilers |
| Processing | Overhead press stamps visible belt/depot batches; hand/drop-fed millstone, no normal machine screens | Basin/mixer, fan processes, crushing wheels, deployers, recipe/heat/fluid integration, sequenced assembly |
| Transport | Straight horizontal belts, visible saved stacks, passive depots, stop/reverse/block handling | Two-pulley connector construction, inclines/turns, funnels/tunnels, chutes, vaults, filters, mechanical arms and entity transport |
| Fluids/heat | Existing Blockcraft water is used only as initial wheel input | Fluid storage/transfer, pumps/pipes/valves, hose pulley, drains/spouts, blaze burners, boilers |
| Moving contraptions | Not implemented | Assembly, glue/chassis, transforms, moving collision/riding, bearings, pistons, gantries, pulleys/elevators, portable interfaces |
| Mobile work tools | Not implemented | Drills, saws, harvesters, ploughs, deployers and inventories while moving |
| Railways | Not implemented | Track geometry, bogeys, assembly, couplings, controls, signals/stations, schedules and save/network consistency |
| Logistics | Not implemented | Packages/packagers, stock links/tickers, addresses, requests, frogports/postboxes and crafting integration |
| Redstone/display | Ordinary redstone drives clutch/gearshift only | Redstone links, thresholds/sequencers, exact displays and observers/controllers |
| Schematics/tutorials | Not implemented | Safe schematic capture/paste/cannon, material accounting and in-game Ponder-style explanations |
| Materials/art/recipes | Alloy, sheets, flour, wrench; dedicated 32px component textures, constant-density UVs and frame-interpolated original models | Full progression/recipe set, decorative families, all original replacement art and accurate shapes |
| Persistence/network | Native machine inventories/progress saved; host edits only | Full client interaction, authoritative synchronization, moving structures and versioned migrations |
| Distribution | Playable V6.15 belt/visible-processing milestone, not a full-port release | Repeated native platform checks, representative full factories/trains and explicit parity acceptance |

## Implementation order

1. **Mechanical foundation and safe persistent processing** — this milestone, with explicit differences documented in [the player guide](../../package/Create-foundation.txt).
2. **Real item transport and processing** — straight belts/depots and visible pressing now work; next are connector placement, inclines, funnels, and basin/recipe infrastructure.
3. **Moving structure engine** — assembly, transforms, physics/collision, persistence and synchronization before promising working contraptions.
4. **Fluids, heat and automation** — expand generators and processing, including sequenced assembly.
5. **Railway and logistics systems** — build on movement/networking rather than fake animated train props.
6. **Schematics, tutorials, remaining registries/recipes/art, compatibility and parity audit.**

These are dependencies, not promised delivery dates. An implemented block name is not evidence of full behavior. Each subsystem needs executable tests, survival workflows, save/reload and native game checks before its status can become complete. The current 12-bit block-ID/property format and loaded-network budgets also need reevaluation as content grows.
