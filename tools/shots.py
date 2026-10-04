#!/usr/bin/env python3
"""
Headless OpenGL screenshot runs of the REAL game (needs Linux + Xvfb + Mesa llvmpipe + ImageMagick;
this is what the 'Visual' GitHub workflow uses - see .github/workflows/visual.yml).

For every scene the game is started with the built-in test hooks (-Dbc.auto=1 ... see Game.autoTest()),
takes one screenshot and exits. Scenes run for each jar given with --jar label=path (default: the original
Update 3.5 jar 'ref' and the freshly built 'new'), then a side-by-side composite and a difference number are
written to ci-results/shots/.

    python3 tools/shots.py                       # all scenes, ref + new
    python3 tools/shots.py --scenes day,title    # some scenes
    python3 tools/shots.py --jar new=build/blockcraft.jar   # only the new jar
"""
import argparse
import glob
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "ci-results", "shots")
TIMES = []

# Seed 12345: spawn is around x=133,z=71; there is a flat grass strip (surface y=66) around x=138..150, z=73..76.
COMMON = ["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=3", "-Dbc.lod=32"]
CAM = "-Dbc.cam=133,92,71,45,22"
TERRA = ["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.gen=1", "-Dbc.mode=1", "-Dbc.rd=4", "-Dbc.lod=64", "-Dbc.time=6000", "-Dbc.weather=calm"]
WXC = ["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=3", "-Dbc.lod=48"]
PLAIN = ["-Dbc.shaders=false"]


def sets(blocks):
    """bc.set hook: 'x,y,z,id;...' - blocks are placed at frame 70 of the run."""
    return "-Dbc.set=" + ";".join("%d,%d,%d,%d" % b for b in blocks)


def farm_blocks():
    """3 rows of crops in all 8 growth stages on wet farmland, a dry plot, an irrigation ditch and hay bales."""
    out = []
    gy = 66  # ground (grass) level of the strip
    for z in range(72, 80):
        out.append((137, gy, z, 50))          # water column west of the field: x=137
        for x in range(138, 150):
            out.append((x, gy + 1, z, 0))     # clear anything above
            out.append((x, gy + 2, z, 0))
    for i in range(8):
        x = 138 + i
        for z, base in ((73, 124), (74, 142), (75, 150)):
            out.append((x, gy, z, 141))       # wet farmland
            out.append((x, gy + 1, z, base + i))
        out.append((x, gy, 76, 123))          # dry, empty farmland row
        out.append((x, gy, 77, 123))
        out.append((x, gy + 1, 77, 124))      # dry farmland with a young wheat plant
    for x, z in ((147, 73), (148, 73), (147, 74), (148, 74)):
        out.append((x, gy + 1, z, 158))
    out.append((147, gy + 2, 73, 158))
    return out


def kitchen_blocks():
    """Update 3.7 showcase on the flat strip: stems in 8 stages + fruit, cake (7 bites), carved pumpkins and
    jack o'lanterns (4 facings), bookshelves, sugar cane beside water, and fires on wood / grass."""
    out = []
    gy = 66
    for z in range(72, 80):
        for x in range(136, 152):
            for y in range(gy + 1, gy + 5):
                out.append((x, y, z, 0))
    for z in (73, 74):
        out.append((137, gy, z, 50))                    # water west of the stems keeps the farmland wet
    for i in range(8):
        x = 138 + i
        out.append((x, gy, 73, 141))
        out.append((x, gy + 1, 73, 171 + i))            # pumpkin stems, stage 0..7
        out.append((x, gy, 74, 141))
        out.append((x, gy + 1, 74, 179 + i))            # melon stems, stage 0..7
    for x, z, b in ((146, 73, 161), (147, 73, 161), (146, 74, 170), (147, 74, 170), (148, 74, 170)):
        out.append((x, gy + 1, z, b))                   # fruit next to the stems
    for i in range(7):
        out.append((138 + i, gy + 1, 75, 187 + i))      # cake: whole ... one slice left
    out.append((146, gy + 1, 75, 161))
    for i in range(4):
        out.append((138 + i, gy + 1, 76, 162 + i))      # carved pumpkins, face on N / S / E / W
        out.append((143 + i, gy + 1, 76, 166 + i))      # jack o'lanterns
    for x in (148, 149):
        for y in (gy + 1, gy + 2):
            out.append((x, y, 76, 160))                 # bookshelves
    out.append((140, gy, 77, 50))                       # water: the cane next to it may stand
    out.append((144, gy, 77, 50))
    for y in (gy + 1, gy + 2, gy + 3):
        out.append((139, y, 77, 159))
    for y in (gy + 1, gy + 2):
        out.append((141, y, 77, 159))
    out.append((143, gy + 1, 77, 159))
    out.append((145, gy + 1, 77, 159))
    out.append((147, gy + 1, 77, 170))
    out.append((148, gy + 1, 77, 161))
    out.append((139, gy + 1, 78, 5))                    # fire on planks / bookshelf / wool / bare grass
    out.append((139, gy + 2, 78, 194))
    out.append((141, gy + 1, 78, 194))
    out.append((143, gy + 1, 78, 34))
    out.append((143, gy + 2, 78, 194))
    out.append((144, gy + 1, 78, 194))
    out.append((146, gy + 1, 78, 160))
    out.append((146, gy + 2, 78, 194))
    out.append((147, gy + 1, 78, 5))
    out.append((148, gy + 1, 78, 194))
    return out


def tree_blocks(x, gy, z, h=7, log=9, leaf=12):
    """A tree for the falling-tree scenes: a trunk of h logs on the ground cell (x, gy, z) and a small crown (the same shape as
    TestsFall.plant): layers h-1 and h are 5x5 without the corners, the top layer a plus."""
    out = []
    for k in range(1, h + 1):
        out.append((x, gy + k, z, log))
    top = gy + h
    for dy in (-1, 0, 1):
        r = 1 if dy == 1 else 2
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                skip = (abs(dx) + abs(dz) > 1) if dy == 1 else (abs(dx) == 2 and abs(dz) == 2)
                if skip or (dx == 0 and dz == 0 and dy <= 0):
                    continue
                out.append((x + dx, top + dy, z + dz, leaf))
    return out


def lumber_blocks():
    """Logs on their side (Update 3.10): oak / birch / spruce along x and along z, with one upright log of each for comparison."""
    out = []
    gy = 66
    for i in range(3):
        x = 139 + i * 4
        out.append((x, gy + 1, 74, 9 + i))              # upright, for comparison
        for k in range(1, 4):
            out.append((x + 1, gy + 1, 74 + k, 220 + i))   # along x
            out.append((x + 2, gy + 1, 74 + k, 223 + i))   # along z
        out.append((x + 1, gy + 2, 75, 220 + i))
        out.append((x + 2, gy + 2, 76, 223 + i))
    return out


def quake_blocks():
    """Update 3.10: something for an earthquake to shake: a dirt plateau with torches, glass and flowers on top and around it."""
    out = []
    gy = 66
    for x in range(139, 151):
        for z in range(72, 82):
            for y in range(gy + 1, gy + 6):
                out.append((x, y, z, 0))
    for x in range(141, 148):
        for z in range(74, 81):
            for y in range(gy + 1, gy + 4):
                out.append((x, y, z, 3 if y < gy + 3 else 2))
    for x in (141, 143, 145, 147):
        out.append((x, gy + 4, 74, 32))                  # torches on the edge
        out.append((x, gy + 4, 80, 32))
    for z in (76, 78):
        out.append((141, gy + 4, z, 15))                 # glass
        out.append((147, gy + 4, z, 15))
        out.append((141, gy + 5, z, 15))
        out.append((147, gy + 5, z, 15))
    for x in range(139, 151, 2):
        out.append((x, gy + 1, 72, 30 + (x // 2) % 2))   # flowers at the foot
        out.append((x, gy + 1, 82, 29))
    return out


def firewall_blocks():
    """Update 3.9 fire models: flames on a plank wall (fire in front of it, nothing below), in a corner (two walls), hanging from a
    plank ceiling, and for comparison fires standing on grass and on planks (the X)."""
    out = []
    gy = 66
    for z in range(72, 80):
        for x in range(136, 152):
            for y in range(gy + 1, gy + 6):
                out.append((x, y, z, 0))
    for x in range(139, 144):
        for y in (gy + 1, gy + 2, gy + 3):
            out.append((x, y, 77, 5))                   # a plank wall, 5 wide and 3 high
    for x, y in ((140, gy + 2), (142, gy + 2), (141, gy + 3)):
        out.append((x, y, 76, 194))                     # fires on its front face (nothing below them)
    for y in (gy + 1, gy + 2, gy + 3):
        out.append((145, y, 77, 5))                     # an inside corner of planks
        out.append((146, y, 76, 5))
    out.append((145, gy + 2, 76, 194))                  # fire in the corner: two walls
    for x in (148, 149, 150):
        out.append((x, gy + 4, 76, 5))                  # a plank ceiling
    out.append((149, gy + 3, 76, 194))                  # fire hanging from it
    out.append((137, gy + 1, 75, 194))                  # on grass: the X
    out.append((138, gy + 1, 75, 5))
    out.append((138, gy + 2, 75, 194))                  # on planks: the X
    return out


def slabs_blocks():
    """Update 3.9: stairs and slabs upside down / in the top half. Row z=75: oak stairs right way up (facing 0-3) and upside down;
    row z=77: cobblestone slab (bottom), top slab, a bottom + a top slab side by side, stone-brick stairs both ways under a roof of top slabs."""
    out = []
    gy = 66
    for z in range(72, 80):
        for x in range(136, 152):
            for y in range(gy + 1, gy + 6):
                out.append((x, y, z, 0))
    for f in range(4):
        out.append((138 + f, gy + 1, 75, 93 + f))           # oak stairs, facing 0..3
        out.append((143 + f, gy + 1, 75, 195 + f))          # the same upside down
    out.append((138, gy + 1, 77, 114))                      # cobblestone slab, bottom half
    out.append((140, gy + 2, 77, 216))                      # cobblestone slab, top half, floating one block up
    out.append((142, gy + 1, 77, 114))                      # a bottom slab ...
    out.append((143, gy + 1, 77, 216))                      # ... next to a top slab
    for x in range(145, 150):
        out.append((x, gy + 3, 77, 217))                    # stone brick top slabs as a ceiling
    for f in range(4):
        out.append((145 + f, gy + 1, 77, 93 + 8 + f))       # stone brick stairs under the ceiling
        out.append((145 + f, gy + 2, 77, 195 + 8 + f))      # and upside down on top of them
    return out


def doors_blocks():
    """Update 3.11: every kind of door in the open (closed and open, all four facings, upper and lower half), one with a block behind it."""
    out = []
    gy = 66
    for z in range(72, 80):
        for x in range(136, 152):
            for y in range(gy + 1, gy + 5):
                out.append((x, y, z, 0))
    for f in range(4):
        out.append((138 + f * 2, gy + 1, 74, 73 + f))        # closed, lower half
        out.append((138 + f * 2, gy + 2, 74, 81 + f))        # closed, upper half
        out.append((138 + f * 2, gy + 1, 77, 77 + f))        # open
        out.append((138 + f * 2, gy + 2, 77, 85 + f))
    out.append((147, gy + 1, 74, 73))                          # a closed door with stone behind it (the old mesher lost its +z face)
    out.append((147, gy + 2, 74, 81))
    out.append((147, gy + 1, 75, 1))
    out.append((147, gy + 2, 75, 1))
    return out


def roof_blocks():
    """Update 3.11 (entity lighting): a flat strip cleared of everything, and a roof of leaves 3 blocks over its right half - the mobs below it
    stand in its shadow, the ones on the left in the sun."""
    out = []
    gy = 66
    for z in range(70, 80):
        for x in range(136, 152):
            for y in range(gy + 1, gy + 7):
                out.append((x, y, z, 0))
    for x in range(143, 152):
        for z in range(70, 80):
            out.append((x, gy + 4, z, 12))
    return out


def torch_blocks():
    """Night scene: a cleared strip with torches around where the mobs will stand."""
    out = []
    gy = 66
    for z in range(70, 80):
        for x in range(136, 152):
            for y in range(gy + 1, gy + 5):
                out.append((x, y, z, 0))
    out.append((143, gy + 1, 76, 32))
    out.append((148, gy + 1, 74, 32))
    return out


# name -> (props, only_new)
def portal_blocks(x, y, z, axis=0):
    """Update 3.12: an obsidian frame (4 x 5, corners left out) with the portal film in it; (x, y, z) = bottom corner of the 2 x 3 interior."""
    out = []
    dx, dz = (1, 0) if axis == 0 else (0, 1)
    for i in range(-1, 3):
        out.append((x + dx * i, y - 1, z + dz * i, 26))
        out.append((x + dx * i, y + 3, z + dz * i, 26))
    for j in range(3):
        out.append((x - dx, y + j, z - dz, 26))
        out.append((x + dx * 2, y + j, z + dz * 2, 26))
        for i in range(2):
            out.append((x + dx * i, y + j, z + dz * i, 1030 + axis))
    return out


def overworld_portal_blocks():
    """The flat grass strip (surface y=66): air around, a portal in the middle, some netherrack and soul sand and a magma block beside it."""
    out = []
    gy = 66
    for z in range(72, 80):
        for x in range(136, 152):
            for y in range(gy + 1, gy + 7):
                out.append((x, y, z, 0))
    out += portal_blocks(143, gy + 1, 75)
    for x in range(139, 148):
        out.append((x, gy, 73, 1024))
    out.append((140, gy, 74, 1025))
    out.append((141, gy, 74, 1028))
    out.append((146, gy + 1, 74, 1027))
    out.append((147, gy + 1, 74, 1027))
    return out


def color_wall():
    """Update 3.12: a wall of 16 colours x 5 kinds (concrete, concrete powder, terracotta, glazed terracotta, stained glass in front of a black / white checker) on the flat grass strip."""
    out = []
    gy = 66
    for z in range(72, 80):
        for x in range(134, 154):
            for y in range(gy + 1, gy + 9):
                out.append((x, y, z, 0))
    for c in range(16):
        x = 136 + c
        out.append((x, gy + 1, 77, 1050 + c))
        out.append((x, gy + 2, 77, 1066 + c))
        out.append((x, gy + 3, 77, 1083 + c))
        out.append((x, gy + 4, 77, 1099 + c))
        out.append((x, gy + 5, 78, 1050 if c % 2 == 0 else 1065))
        out.append((x, gy + 6, 78, 1065 if c % 2 == 0 else 1050))
        out.append((x, gy + 5, 77, 1116 + c))
        out.append((x, gy + 6, 77, 1116 + (c + 8) % 16))
    return out


def misc_color_blocks():
    """Clay, terracotta, the six blocks of metal and gem, emerald ore next to lapis ore, diamond ore and plain stone."""
    out = []
    gy = 66
    for z in range(72, 80):
        for x in range(134, 154):
            for y in range(gy + 1, gy + 9):
                out.append((x, y, z, 0))
    ids = [1115, 1082, 1132, 1133, 1134, 1135, 1136, 1137, 1138, 24, 22, 1]
    for i, bid in enumerate(ids):
        out.append((138 + i, gy + 1, 77, bid))
        out.append((138 + i, gy + 2, 77, bid))
    return out


NETHER = ["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=3", "-Dbc.dim=-1"]
END = ["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=3", "-Dbc.dim=1"]
# seed 12345: stronghold #0 has its portal room (11 x 11, floor y=25) centred on x=-688, z=-611
SH_CX, SH_CZ, SH_Y = -688, -611, 25


def stronghold_ring(eyes=True, portal=True):
    """Update 3.12: all twelve frames of the first stronghold's portal room with their eyes, and the portal in the pit."""
    out = []
    for k in (-1, 0, 1):
        for fx, fz in ((SH_CX - 2, SH_CZ + k), (SH_CX + 2, SH_CZ + k), (SH_CX + k, SH_CZ - 2), (SH_CX + k, SH_CZ + 2)):
            out.append((fx, SH_Y, fz, 1042 if eyes else 1041))
    if portal:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                out.append((SH_CX + dx, SH_Y, SH_CZ + dz, 1043))
    return out


# Software OpenGL is slow, so scenes use the plain renderer, few frames and a short render distance.
SCENES = {
    # ---- Update 3.12: the Nether (seed 12345: a lava lake with a wide cavern around x=30, z=-96; flat floor for a portal at x=22, y=41, z=-116)
    "nether-vista":       (NETHER + ["-Dbc.cam=30,48,-96,-90,4"] + PLAIN + ["-Dbc.shot=150"], True),
    "nether-vista-sh":    (NETHER + ["-Dbc.cam=30,48,-96,-90,4", "-Dbc.shot=150"], True),
    "nether-vista-west":  (NETHER + ["-Dbc.cam=30,48,-96,90,2"] + PLAIN + ["-Dbc.shot=150"], True),
    "nether-portal":      (NETHER + ["-Dbc.cam=23,43,-111,180,8", sets(portal_blocks(22, 41, -116))] + PLAIN + ["-Dbc.shot=150"], True),
    "nether-portal-sh":   (NETHER + ["-Dbc.cam=23,43,-111,180,8", sets(portal_blocks(22, 41, -116)), "-Dbc.shot=150"], True),
    "nether-mobs":        (NETHER + ["-Dbc.cam=23,43,-111,180,6", "-Dbc.summon=pigman,magmacube,blaze,pigman", "-Dbc.summonDist=6", "-Dbc.summonDy=-1.6"] + PLAIN + ["-Dbc.shot=150"], True),
    # a fortress of seed 12345 (NetherGen's own seed, not 12345): its hall (7 x 7, floor y 62) is centred on x=121, z=99; corridors run north, south and east/west from it
    "nether-fortress":     (NETHER + ["-Dbc.cam=121.5,63,102,180,3", "-Dbc.rd=4"] + PLAIN + ["-Dbc.shot=220"], True),
    "nether-fortress-b":   (NETHER + ["-Dbc.cam=116,63,99.5,-90,3", "-Dbc.rd=4"] + PLAIN + ["-Dbc.shot=220"], True),
    "nether-fortress-air": (NETHER + ["-Dbc.cam=121.5,84,70,0,38", "-Dbc.rd=4"] + PLAIN + ["-Dbc.shot=240"], True),
    "nether-ghast":       (NETHER + ["-Dbc.cam=30,48,-96,-90,2", "-Dbc.summon=ghast", "-Dbc.summonDist=14", "-Dbc.summonDy=0"] + PLAIN + ["-Dbc.shot=110"], True),
    # ---- Update 3.12: the End (the island is the same in every world: platform at x=62; the tallest pillar at x=42, z=0; pillar 3 (76 high) at x=-13, z=40)
    "end-island":         (END + ["-Dbc.cam=62.5,64,0.5,90,-18"] + PLAIN + ["-Dbc.shot=150"], True),
    "end-island-sh":      (END + ["-Dbc.cam=62.5,64,0.5,90,-18", "-Dbc.shot=150"], True),
    "end-crystal":        (END + ["-Dbc.cam=-12.5,70,28.5,0,-32"] + PLAIN + ["-Dbc.shot=110"], True),
    "end-dragon":         (END + ["-Dbc.cam=15.5,73,0.5,90,17", "-Dbc.dragon=perch"] + PLAIN + ["-Dbc.shot=110"], True),
    "end-dragon-side":    (END + ["-Dbc.cam=0.5,69.5,15.5,180,6", "-Dbc.dragon=perch"] + PLAIN + ["-Dbc.shot=110"], True),
    "end-dragon-sh":      (END + ["-Dbc.cam=15.5,73,0.5,90,17", "-Dbc.dragon=perch", "-Dbc.shot=110"], True),
    "end-enderman":       (END + ["-Dbc.cam=30.5,65,0.5,90,4", "-Dbc.summon=enderman,enderman,enderman", "-Dbc.summonDist=5", "-Dbc.summonDy=0"] + PLAIN + ["-Dbc.shot=110"], True),
    "stronghold":         (COMMON + ["-Dbc.cam=%.1f,%d,%.1f,90,28" % (SH_CX + 4.5, SH_Y + 1, SH_CZ + 0.5), "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=150"], True),
    "stronghold-portal":  (COMMON + ["-Dbc.cam=%.1f,%d,%.1f,90,28" % (SH_CX + 4.5, SH_Y + 1, SH_CZ + 0.5), "-Dbc.time=6000", sets(stronghold_ring())] + PLAIN + ["-Dbc.shot=150"], True),
    "stronghold-portal-sh": (COMMON + ["-Dbc.cam=%.1f,%d,%.1f,90,28" % (SH_CX + 4.5, SH_Y + 1, SH_CZ + 0.5), "-Dbc.time=6000", sets(stronghold_ring()), "-Dbc.shot=150"], True),
    # ---- Update 3.12: the villages (seed 12345: the village at x=173, z=76; well 172..174 z 75..77, smithy x 184..190 z 71..75, church x 172..176 z 96..104)
    # (the first cameras sat inside an oak: the south of the village is forest - these ones were checked with a ray caster over the generated chunks)
    "village-day":        (COMMON + ["-Dbc.cam=182.5,71.6,83.5,128,6", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=170"], True),
    "village-aerial":     (COMMON + ["-Dbc.cam=205.5,88,60.5,64,34", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=170"], True),
    "village-folk":       (COMMON + ["-Dbc.cam=182.5,71.6,83.5,128,8", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.summon=villager,villager,irongolem,villager,villager", "-Dbc.summonDist=5", "-Dbc.summonDy=-1.6"] + PLAIN + ["-Dbc.shot=110"], True),
    "village-folk-sh":    (COMMON + ["-Dbc.cam=182.5,71.6,83.5,128,8", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.summon=villager,villager,irongolem,villager,villager", "-Dbc.summonDist=5", "-Dbc.summonDy=-1.6", "-Dbc.shot=110"], True),
    "village-trade":      (COMMON + ["-Dbc.cam=182.5,71.6,83.5,128,8", "-Dbc.time=6000", "-Dbc.inv=trade", "-Dbc.prof=3"] + PLAIN + ["-Dbc.shot=130"], True),
    # ---- Update 3.12: the World of Color (flat grass strip: wall at z=77, camera looks +z)
    "colors-wall":        (COMMON + ["-Dbc.cam=143.5,70.2,66.5,0,6", "-Dbc.time=6000", "-Dbc.weather=calm", sets(color_wall())] + PLAIN + ["-Dbc.shot=170"], True),
    "colors-wall-sh":     (COMMON + ["-Dbc.cam=143.5,70.2,66.5,0,6", "-Dbc.time=6000", "-Dbc.weather=calm", sets(color_wall()), "-Dbc.shot=170"], True),
    "colors-misc":        (COMMON + ["-Dbc.cam=143.5,68.6,70.5,0,6", "-Dbc.time=6000", "-Dbc.weather=calm", sets(misc_color_blocks())] + PLAIN + ["-Dbc.shot=170"], True),
    # ---- Update 3.12: advancements (the screen on the L key, and the toast in the corner)
    "adv-screen":         (COMMON + ["-Dbc.cam=143.5,70.2,66.5,0,6", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.adv=screen", "-Dbc.advdone=9", "-Dbc.advtab=0"] + PLAIN + ["-Dbc.shot=130"], True),
    "adv-adventure":      (COMMON + ["-Dbc.cam=143.5,70.2,66.5,0,6", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.adv=screen", "-Dbc.advdone=40", "-Dbc.advtab=3"] + PLAIN + ["-Dbc.shot=130"], True),
    "adv-toast-long":     (COMMON + ["-Dbc.cam=143.5,70.2,66.5,0,6", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.adv=toast", "-Dbc.advdone=0", "-Dbc.advid=story/diamond_armor"] + PLAIN + ["-Dbc.shot=108"], True),
    "adv-toast":          (COMMON + ["-Dbc.cam=143.5,70.2,66.5,0,6", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.adv=toast", "-Dbc.advdone=0"] + PLAIN + ["-Dbc.shot=108"], True),
    # ---- Update 3.12: parrots (five colours sitting on the grass strip; two on the shoulders of the player seen from the front)
    "parrots":            (COMMON + ["-Dbc.cam=143.5,68.6,68.5,0,12", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.parrot=land", "-Dbc.summon=parrot,parrot,parrot,parrot,parrot", "-Dbc.summonDist=3.2", "-Dbc.summonDy=-1.6"] + PLAIN + ["-Dbc.shot=110"], True),
    "parrot-shoulder":    (COMMON + ["-Dbc.cam=143.5,67.2,70.5,0,0", "-Dbc.view=2", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.parrot=shoulder", "-Dbc.summon=parrot,parrot", "-Dbc.summonDist=1.0", "-Dbc.summonDy=0"] + PLAIN + ["-Dbc.shot=110"], True),
    "portal-day":         (COMMON + ["-Dbc.cam=143.5,69,70.5,0,6", "-Dbc.time=6000", "-Dbc.weather=calm", sets(overworld_portal_blocks())] + PLAIN + ["-Dbc.shot=170"], True),
    "portal-day-sh":      (COMMON + ["-Dbc.cam=143.5,69,70.5,0,6", "-Dbc.time=6000", "-Dbc.weather=calm", sets(overworld_portal_blocks()), "-Dbc.shot=170"], True),

    "title":          (["-Dbc.auto=1", "-Dbc.title=TITLE"], False),
    "day-plain":      (COMMON + [CAM, "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=100"], False),
    "mobs":           (COMMON + ["-Dbc.cam=140,70,60,0,48", "-Dbc.time=6000"] + PLAIN + ["-Dbc.summon=pig,cow,sheep,chicken,creeper", "-Dbc.shot=110"], False),
    "farm":           (COMMON + ["-Dbc.cam=143.5,71,68.5,0,30", "-Dbc.time=6000"] + PLAIN + [sets(farm_blocks()), "-Dbc.shot=170"], True),
    "farm-close":     (COMMON + ["-Dbc.cam=141.5,69.3,70.8,0,38", "-Dbc.time=6000"] + PLAIN + [sets(farm_blocks()), "-Dbc.shot=170"], True),
    # close-ups: camera at y=65.5 over the beach (surface top y=64), mobs appear 3.2 ahead and 1.3 below the camera
    "sheep36":        (COMMON + ["-Dbc.cam=140,65.5,60,0,22", "-Dbc.time=6000"] + PLAIN + ["-Dbc.summon=sheep,sheared:sheep,baby:sheep", "-Dbc.summonDist=3.2", "-Dbc.summonDy=-1.3", "-Dbc.shot=100"], True),
    "pigs36":         (COMMON + ["-Dbc.cam=140,65.5,60,0,22", "-Dbc.time=6000"] + PLAIN + ["-Dbc.summon=baby:pig,pig,baby:cow,cow", "-Dbc.summonDist=3.2", "-Dbc.summonDy=-1.3", "-Dbc.shot=100"], True),
    "inv-creative-2": (COMMON + [CAM] + PLAIN + ["-Dbc.inv=creative", "-Dbc.page=1", "-Dbc.shot=110"], True),
    "kitchen37":      (COMMON + ["-Dbc.cam=143.5,72.5,69.0,0,40", "-Dbc.time=6000"] + PLAIN + [sets(kitchen_blocks()), "-Dbc.fire=off", "-Dbc.shot=170"], True),
    "night37":        (COMMON + ["-Dbc.cam=145.5,70.0,71.0,0,34", "-Dbc.time=18000"] + PLAIN + [sets(kitchen_blocks()), "-Dbc.fire=off", "-Dbc.shot=170"], True),
    # ---- Update 3.8: weather. -Dbc.weather=calm|clear|rain|thunder|cell:dx,dz,radius (storm cell relative to the player),
    # -Dbc.bolt=dx,dz (a fresh lightning bolt every frame), -Dbc.biome=5 (snow), -Dbc.wtime=<weather ticks>. Camera yaw -90 looks east
    # (positive pitch looks down). The camera floats at y=125 so the terrain does not hide the horizon.
    "wx-fair":        (WXC + ["-Dbc.cam=133,95,71,-90,0", "-Dbc.weather=calm", "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=190"], True),
    "wx-storm":       (WXC + ["-Dbc.cam=133,95,71,-90,0", "-Dbc.weather=cell:1150,-60,620", "-Dbc.lightning=off", "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=190"], True),
    "wx-storm-far":   (WXC + ["-Dbc.cam=133,95,71,-90,0", "-Dbc.weather=cell:2400,-100,720", "-Dbc.lightning=off", "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=190"], True),
    "wx-storm-bolt":  (WXC + ["-Dbc.cam=133,95,71,-90,0", "-Dbc.weather=cell:1000,-60,620", "-Dbc.lightning=off", "-Dbc.bolt=650,-40", "-Dbc.time=12300"] + PLAIN + ["-Dbc.shot=190"], True),
    "wx-rain":        (WXC + ["-Dbc.cam=133,69,71,-70,2", "-Dbc.weather=rain", "-Dbc.lightning=off", "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=190"], True),
    "wx-overcast":    (WXC + ["-Dbc.cam=133,69,71,-90,-28", "-Dbc.weather=thunder", "-Dbc.lightning=off", "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=190"], True),
    "wx-snow":        (WXC + ["-Dbc.cam=133,69,71,-70,2", "-Dbc.weather=rain", "-Dbc.biome=5", "-Dbc.lightning=off", "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=190"], True),
    # the same sky in the original 3.5 jar and the new one (no weather hooks): is the shader sky still as good as before?
    "wx-compare-sh":  (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=2", "-Dbc.lod=24", "-Dbc.cam=133,95,71,-90,2", "-Dbc.time=6000", "-Dbc.shot=26"], False),
    "wx-storm-sh":    (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=2", "-Dbc.lod=24", "-Dbc.cam=133,95,71,-90,0", "-Dbc.weather=cell:1150,-60,620", "-Dbc.lightning=off", "-Dbc.time=6000", "-Dbc.shot=26"], True),
    "wx-storm-far-sh": (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=2", "-Dbc.lod=24", "-Dbc.cam=133,95,71,-90,0", "-Dbc.weather=cell:2400,-100,720", "-Dbc.lightning=off", "-Dbc.time=6000", "-Dbc.shot=26"], True),
    "wx-rain-sh":     (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=2", "-Dbc.lod=24", "-Dbc.cam=133,69,71,-70,2", "-Dbc.weather=rain", "-Dbc.lightning=off", "-Dbc.time=6000", "-Dbc.shot=26"], True),
    # ---- LOD inspection (Update 3.9 / roadmap step 2): rd = 4 chunks of real terrain, LOD beyond. Top-down shows the ring, the ground view shows the seam.
    "lod-top":        (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=3", "-Dbc.lod=16", "-Dbc.clouds=off", "-Dbc.cam=133,260,71,0,90", "-Dbc.weather=clear", "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=150"], True),
    "lod-top-sh":     (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=3", "-Dbc.lod=16", "-Dbc.clouds=off", "-Dbc.cam=133,260,71,0,90", "-Dbc.weather=clear", "-Dbc.time=6000", "-Dbc.shot=30"], True),
    "lod-edge":       (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=4", "-Dbc.lod=32", "-Dbc.cam=133,84,71,-45,12", "-Dbc.weather=calm", "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=150"], True),
    "lod-night":      (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=4", "-Dbc.lod=32", "-Dbc.cam=133,84,71,-45,12", "-Dbc.weather=calm", "-Dbc.time=18000"] + PLAIN + ["-Dbc.shot=150"], True),
    "lod-edge-sh":    (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=4", "-Dbc.lod=32", "-Dbc.cam=133,84,71,-45,12", "-Dbc.weather=calm", "-Dbc.time=6000", "-Dbc.shot=30"], True),
    # ---- Update 3.9: skins. bc.view=1|2 (third person behind / in front), bc.skin=demo, bc.emptyhand=1, bc.title=SKINS
    "skin-front":     (COMMON + ["-Dbc.cam=140.5,67,74.5,0,8", "-Dbc.view=2", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "skin-back":      (COMMON + ["-Dbc.cam=140.5,67,74.5,35,12", "-Dbc.view=1", "-Dbc.skin=demo", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "skin-first":     (COMMON + ["-Dbc.cam=140.5,67,74.5,20,25", "-Dbc.skin=demo", "-Dbc.emptyhand=1", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    # the held item in the hand, seen from the side (the body is turned 45 degrees away from the camera), and the inventory preview
    "skin-hold-a":    (COMMON + ["-Dbc.cam=140.5,67,74.5,0,8", "-Dbc.view=2", "-Dbc.bodyyaw=45", "-Dbc.skin=demo", "-Dbc.hold=316", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "skin-hold-b":    (COMMON + ["-Dbc.cam=140.5,67,74.5,0,8", "-Dbc.view=2", "-Dbc.bodyyaw=-45", "-Dbc.skin=demo", "-Dbc.hold=316", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "skin-hold-block": (COMMON + ["-Dbc.cam=140.5,67,74.5,0,8", "-Dbc.view=2", "-Dbc.bodyyaw=-45", "-Dbc.hold=1", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "inv-player":     (COMMON + [CAM] + PLAIN + ["-Dbc.inv=player", "-Dbc.skin=demo", "-Dbc.mouse=560,160", "-Dbc.shot=110"], True),
    "inv-player-def": (COMMON + [CAM] + PLAIN + ["-Dbc.inv=player", "-Dbc.mouse=120,60", "-Dbc.shot=110"], True),
    "treefall-30":    (COMMON + ["-Dbc.cam=145.5,71,63.0,0,4", "-Dbc.time=6000"] + PLAIN + ["-Dbc.fill=139,67,69,152,80,80,0", sets(tree_blocks(145, 66, 74)), "-Dbc.fell=145,67,74,1,0", "-Dbc.fellangle=30", "-Dbc.shot=150"], True),
    "treefall-65":    (COMMON + ["-Dbc.cam=145.5,71,63.0,0,4", "-Dbc.time=6000"] + PLAIN + ["-Dbc.fill=139,67,69,152,80,80,0", sets(tree_blocks(145, 66, 74)), "-Dbc.fell=145,67,74,1,0", "-Dbc.fellangle=65", "-Dbc.shot=150"], True),
    "treefall-down":  (COMMON + ["-Dbc.cam=148.0,72,63.0,0,24", "-Dbc.time=6000"] + PLAIN + ["-Dbc.fill=139,67,69,154,80,80,0", sets(tree_blocks(145, 66, 74)), "-Dbc.fell=145,67,74,1,0", "-Dbc.shot=240"], True),
    "logs310":        (COMMON + ["-Dbc.cam=145.5,70.5,70.0,0,24", "-Dbc.time=6000"] + PLAIN + ["-Dbc.fill=137,67,72,153,72,80,0", sets(lumber_blocks()), "-Dbc.shot=150"], True),
    "tornado-mid":    (COMMON + ["-Dbc.rd=4", "-Dbc.cam=143.5,70,62,0,-14", "-Dbc.time=6000", "-Dbc.disaster=tornado:0,54,1.1,0", "-Dbc.dfreeze=260", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=210"], True),
    "tornado-close":  (COMMON + ["-Dbc.cam=143.5,69,62,0,-12", "-Dbc.time=6000", "-Dbc.disaster=tornado:0,26,1.0,0", "-Dbc.dfreeze=260", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=210"], True),
    "tornado-night":  (COMMON + ["-Dbc.rd=4", "-Dbc.cam=143.5,70,62,0,-14", "-Dbc.time=18000", "-Dbc.disaster=tornado:0,54,1.1,0", "-Dbc.dfreeze=260", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=210"], True),
    "quake":          (COMMON + ["-Dbc.cam=144,72,66.5,0,34", "-Dbc.time=6000", "-Dbc.fill=139,67,72,150,72,82,0", sets(quake_blocks()), "-Dbc.disaster=quake:0,12,3,900", "-Dbc.dfreeze=200", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=230"], True),
    "meteors":        (COMMON + ["-Dbc.cam=143.5,70,62,0,-22", "-Dbc.time=18000", "-Dbc.disaster=meteor:30,80,70;meteor:-36,95,64;meteor:5,110,76", "-Dbc.dfreeze=34", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=170"], True),
    "meteor-crater":  (COMMON + ["-Dbc.cam=143.5,92,68,0,52", "-Dbc.time=18000", "-Dbc.disaster=meteor:0,30,30", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=230"], True),
    "options310":     (["-Dbc.auto=1", "-Dbc.title=OPTIONS"], True),
    # ---- Update 3.13: the new terrain (seed 12345, world type 1 = Default; the cameras were placed with test/bc/VoxView, which draws the same columns)
    "terra-badlands":    (TERRA + ["-Dbc.cam=704.5,90,149.5,150,14"] + PLAIN + ["-Dbc.shot=260"], True),
    "terra-mount":       (TERRA + ["-Dbc.cam=392.5,150,-303.5,200,8"] + PLAIN + ["-Dbc.shot=260"], True),
    "terra-snowtaiga":   (TERRA + ["-Dbc.cam=-291.5,96,273.5,90,14"] + PLAIN + ["-Dbc.shot=260"], True),
    "terra-mushroom":    (TERRA + ["-Dbc.cam=633.5,92,633.5,300,14"] + PLAIN + ["-Dbc.shot=260"], True),
    "terra-savanna":     (TERRA + ["-Dbc.cam=351.5,92,-25.5,0,14"] + PLAIN + ["-Dbc.shot=260"], True),
    "terra-jungle":      (TERRA + ["-Dbc.cam=24.5,118,-59.5,270,16"] + PLAIN + ["-Dbc.shot=260"], True),
    "terra-darkforest":  (TERRA + ["-Dbc.cam=190.5,96,-25.5,90,16"] + PLAIN + ["-Dbc.shot=260"], True),
    "terra-swamp":       (TERRA + ["-Dbc.cam=75.5,84,-490.5,90,14"] + PLAIN + ["-Dbc.shot=260"], True),
    # ---- Update 3.13: the new menus. The title turns over a real landscape (bc.pano=on: the panorama is off in the other scenes), the options have tabs,
    # the create-world screen shows a map of the world, the pause menu lies over the game.
    "title313":       (["-Dbc.auto=1", "-Dbc.pano=on", "-Dbc.rd=6", "-Dbc.lod=96", "-Dbc.title=TITLE"], True),
    "worlds313":      (["-Dbc.auto=1", "-Dbc.pano=on", "-Dbc.rd=4", "-Dbc.fakeworlds=1", "-Dbc.title=WORLDS"], True),
    "newworld313":    (["-Dbc.auto=1", "-Dbc.pano=on", "-Dbc.rd=4", "-Dbc.title=NEWWORLD"], True),
    "options313-0":   (["-Dbc.auto=1", "-Dbc.pano=on", "-Dbc.rd=4", "-Dbc.title=OPTIONS", "-Dbc.opttab=0"], True),
    "options313-1":   (["-Dbc.auto=1", "-Dbc.title=OPTIONS", "-Dbc.opttab=1"], True),
    "options313-3":   (["-Dbc.auto=1", "-Dbc.title=OPTIONS", "-Dbc.opttab=3"], True),
    "pause313":       (COMMON + [CAM, "-Dbc.world=1", "-Dbc.title=PAUSE"] + PLAIN, True),
    # the inventories of Update 3.13: creative tabs and search, a tooltip, the furnace and the crafting table with their drawn arrows
    "inv313-creative": (COMMON + [CAM] + PLAIN + ["-Dbc.inv=creative", "-Dbc.shot=110"], True),
    "inv313-search":   (COMMON + [CAM] + PLAIN + ["-Dbc.inv=creative", "-Dbc.tab=7", "-Dbc.search=pick", "-Dbc.shot=110"], True),
    "inv313-tip":      (COMMON + [CAM] + PLAIN + ["-Dbc.inv=creative", "-Dbc.tab=4", "-Dbc.mouse=489,381", "-Dbc.shot=110"], True),
    "inv313-furnace":  (COMMON + [CAM] + PLAIN + ["-Dbc.inv=furnace", "-Dbc.shot=110"], True),
    "inv313-craft":    (COMMON + [CAM] + PLAIN + ["-Dbc.inv=craft", "-Dbc.shot=110"], True),
    # ---- Update 3.11: single-sided faces. Spectator mode (bc.mode=3, given after COMMON's mode so it wins) with the camera inside the ground looking up:
    # the ground must be see-through from below.
    "under-map":      (COMMON + ["-Dbc.mode=3", "-Dbc.cam=140,45,70,-60,-35", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "under-map-sh":   (COMMON + ["-Dbc.mode=3", "-Dbc.cam=140,45,70,-60,-35", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.shot=110"], True),
    # the camera in a pool looks up at the surface: seen from below it must still be there
    "under-water":    (COMMON + ["-Dbc.cam=144.5,67.6,74.5,0,-55", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.fill=140,66,70,148,70,78,50"] + PLAIN + ["-Dbc.shot=130"], True),
    "doors311":       (COMMON + ["-Dbc.cam=144,71,70.0,0,22", "-Dbc.time=6000"] + PLAIN + [sets(doors_blocks()), "-Dbc.shot=150"], True),
    # the LOD shows what the world looks like after it was changed: a brick tower and a pit are made near the start, then the player flies away
    # (their chunks unload and are summarised) and looks back at them from a distance
    "lod-changes":    (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=3", "-Dbc.lod=32", "-Dbc.cam=133,92,71,-90,6", "-Dbc.weather=calm", "-Dbc.time=6000", "-Dbc.lodage=0.02",
                       "-Dbc.fill=150,60,60,161,140,71,16;166,50,60,178,90,72,0", "-Dbc.tp=110:-60,100,71"] + PLAIN + ["-Dbc.shot=260"], True),
    # the same place without any change: what the generator makes of it (to see what the tower and the pit replaced)
    "lod-changes-ref": (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=3", "-Dbc.lod=32", "-Dbc.cam=-60,100,71,-90,6", "-Dbc.weather=calm", "-Dbc.time=6000"] + PLAIN + ["-Dbc.shot=160"], True),
    # ---- Update 3.11: the hand (first person: the empty arm in the skin, a tool, a block, a torch, food; the swing and eating poses frozen half way; third person)
    "hand-empty":     (COMMON + ["-Dbc.cam=140.5,67,74.5,20,25", "-Dbc.skin=demo", "-Dbc.emptyhand=1", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "hand-empty-swing": (COMMON + ["-Dbc.cam=140.5,67,74.5,20,25", "-Dbc.skin=demo", "-Dbc.emptyhand=1", "-Dbc.handswing=0.4", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "hand-sword":     (COMMON + ["-Dbc.cam=140.5,67,74.5,20,25", "-Dbc.hold=316", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "hand-sword-swing": (COMMON + ["-Dbc.cam=140.5,67,74.5,20,25", "-Dbc.hold=316", "-Dbc.handswing=0.4", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "hand-block":     (COMMON + ["-Dbc.cam=140.5,67,74.5,20,25", "-Dbc.hold=4", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "hand-torch":     (COMMON + ["-Dbc.cam=140.5,67,74.5,20,25", "-Dbc.hold=32", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "hand-apple":     (COMMON + ["-Dbc.cam=140.5,67,74.5,20,25", "-Dbc.hold=270", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "hand-eat":       (COMMON + ["-Dbc.cam=140.5,67,74.5,20,25", "-Dbc.hold=270", "-Dbc.handeat=0.5", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "hand-sword-sh":  (COMMON + ["-Dbc.cam=140.5,67,74.5,20,25", "-Dbc.hold=316", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.shot=110"], True),
    "skin-hold-torch": (COMMON + ["-Dbc.cam=140.5,67,74.5,0,8", "-Dbc.view=2", "-Dbc.bodyyaw=45", "-Dbc.skin=demo", "-Dbc.hold=32", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "skin-hold-apple": (COMMON + ["-Dbc.cam=140.5,67,74.5,0,8", "-Dbc.view=2", "-Dbc.bodyyaw=45", "-Dbc.skin=demo", "-Dbc.hold=270", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    # ---- Update 3.11: entities lit like the terrain with shaders (the same scenes without shaders for comparison: -plain)
    "mobs-sh":        (COMMON + ["-Dbc.cam=140,70,60,0,48", "-Dbc.time=6000", "-Dbc.summon=pig,cow,sheep,chicken,creeper", "-Dbc.shot=110"], True),
    "mobs-shade-sh":  (COMMON + ["-Dbc.cam=145.5,68.6,67.5,0,20", "-Dbc.time=6000", "-Dbc.weather=calm", sets(roof_blocks()), "-Dbc.summon=pig,cow,sheep,chicken,creeper,pig,sheep", "-Dbc.summonDist=5.5", "-Dbc.summonDy=-1.6", "-Dbc.shot=150"], True),
    "mobs-shade-plain": (COMMON + ["-Dbc.cam=145.5,68.6,67.5,0,20", "-Dbc.time=6000", "-Dbc.weather=calm", sets(roof_blocks()), "-Dbc.summon=pig,cow,sheep,chicken,creeper,pig,sheep", "-Dbc.summonDist=5.5", "-Dbc.summonDy=-1.6"] + PLAIN + ["-Dbc.shot=150"], True),
    "mobs-torch-sh":  (COMMON + ["-Dbc.cam=145.5,68.6,67.5,0,20", "-Dbc.time=18000", "-Dbc.weather=calm", sets(torch_blocks()), "-Dbc.summon=pig,cow,sheep,chicken,creeper,pig,sheep", "-Dbc.summonDist=5.5", "-Dbc.summonDy=-1.6", "-Dbc.shot=150"], True),
    "mobs-torch-plain": (COMMON + ["-Dbc.cam=145.5,68.6,67.5,0,20", "-Dbc.time=18000", "-Dbc.weather=calm", sets(torch_blocks()), "-Dbc.summon=pig,cow,sheep,chicken,creeper,pig,sheep", "-Dbc.summonDist=5.5", "-Dbc.summonDy=-1.6"] + PLAIN + ["-Dbc.shot=150"], True),
    # ---- Update 3.11: water with shaders (depth colour, refraction, reflections, foam, ripples); the same view without shaders (-plain) for comparison
    "water-day-sh":   (COMMON + ["-Dbc.cam=133,72,71,45,12", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.shot=110"], True),
    "water-day-plain": (COMMON + ["-Dbc.cam=133,72,71,45,12", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "water-low-sh":   (COMMON + ["-Dbc.cam=-60,66,71,-90,4", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.shot=110"], True),
    "water-low-plain": (COMMON + ["-Dbc.cam=-60,66,71,-90,4", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "water-fast-sh":  (COMMON + ["-Dbc.waterq=1", "-Dbc.cam=-60,66,71,-90,4", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.shot=110"], True),
    "water-sunset-sh": (COMMON + ["-Dbc.cam=133,70,71,90,5", "-Dbc.time=11000", "-Dbc.weather=calm", "-Dbc.shot=110"], True),
    # ---- Update 3.11: animation (mobs caught mid-stride / grazing, the player running) and the new clouds
    "mobs-walk":      (COMMON + ["-Dbc.cam=140,65.5,60,0,22", "-Dbc.time=6000", "-Dbc.summon=pig,cow,sheep,baby:pig", "-Dbc.summonDist=3.2", "-Dbc.summonDy=-1.3", "-Dbc.walkphase=0.6,1.0"] + PLAIN + ["-Dbc.shot=100"], True),
    "mobs-graze":     (COMMON + ["-Dbc.cam=140,65.5,60,0,22", "-Dbc.time=6000", "-Dbc.summon=pig,cow,sheep,baby:cow", "-Dbc.summonDist=3.2", "-Dbc.summonDy=-1.3", "-Dbc.graze=1"] + PLAIN + ["-Dbc.shot=100"], True),
    "skin-run":       (COMMON + ["-Dbc.cam=140.5,67,74.5,0,8", "-Dbc.view=2", "-Dbc.bodyyaw=60", "-Dbc.skin=demo", "-Dbc.hold=316", "-Dbc.posewalk=0.6,1.0,1", "-Dbc.time=6000", "-Dbc.weather=calm"] + PLAIN + ["-Dbc.shot=110"], True),
    "wx-sunset-sh":   (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=2", "-Dbc.lod=24", "-Dbc.cam=133,95,71,90,4", "-Dbc.weather=calm", "-Dbc.time=11800", "-Dbc.shot=26"], True),
    "firewall":       (COMMON + ["-Dbc.cam=144,70,68.5,0,12", "-Dbc.time=6000"] + PLAIN + [sets(firewall_blocks()), "-Dbc.fire=off", "-Dbc.shot=170"], True),
    "firewall-night": (COMMON + ["-Dbc.cam=144,70,68.5,0,12", "-Dbc.time=18000"] + PLAIN + [sets(firewall_blocks()), "-Dbc.fire=off", "-Dbc.shot=170"], True),
    "slabs":          (COMMON + ["-Dbc.cam=144,70.2,70.2,0,22", "-Dbc.time=6000"] + PLAIN + [sets(slabs_blocks()), "-Dbc.shot=170"], True),
    # leaf modes over the same forest: 0 fancy / 1 smart / 2 fast (look from above into the canopy)
    "leaves-fancy": (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=4", "-Dbc.lod=32", "-Dbc.cam=133,84,71,-45,12", "-Dbc.weather=calm", "-Dbc.time=6000", "-Dbc.leaves=0"] + PLAIN + ["-Dbc.shot=110"], True),
    "leaves-smart": (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=4", "-Dbc.lod=32", "-Dbc.cam=133,84,71,-45,12", "-Dbc.weather=calm", "-Dbc.time=6000", "-Dbc.leaves=1"] + PLAIN + ["-Dbc.shot=110"], True),
    "leaves-fast": (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=4", "-Dbc.lod=32", "-Dbc.cam=133,84,71,-45,12", "-Dbc.weather=calm", "-Dbc.time=6000", "-Dbc.leaves=2"] + PLAIN + ["-Dbc.shot=110"], True),
    # the same scenes through the shader pipeline (the model, held items and the new stairs must look right there too)
    "skin-hold-sh":   (COMMON + ["-Dbc.cam=140.5,67,74.5,0,8", "-Dbc.view=2", "-Dbc.bodyyaw=-45", "-Dbc.skin=demo", "-Dbc.hold=316", "-Dbc.time=6000", "-Dbc.weather=calm", "-Dbc.shot=110"], True),
    "slabs-sh":       (COMMON + ["-Dbc.cam=144,70.2,70.2,0,22", "-Dbc.time=6000", sets(slabs_blocks()), "-Dbc.shot=170"], True),
    "pause-menu":     (COMMON + [CAM] + PLAIN + ["-Dbc.pause=1", "-Dbc.shot=110"], True),
    # the Skins screen in a narrow window (1030x720 = what the Windows test machine gave us: 343 GUI units) and at GUI scale 4 (320 x 180)
    "skin-screen-narrow": (["-Dbc.auto=1", "-Dbc.title=SKINS", "-Dbc.skin=demo", "-Dbc.win=1030x720"], True),
    "skin-screen-tiny":   (["-Dbc.auto=1", "-Dbc.title=SKINS", "-Dbc.skin=demo", "-Dbc.guiscale=4"], True),
    "skin-screen":    (["-Dbc.auto=1", "-Dbc.title=SKINS", "-Dbc.skin=demo"], True),
    # real network import of an account's skin (needs internet: the Windows smoke test machine has it) and the file dialog opening
    "skin-net":       (["-Dbc.auto=1", "-Dbc.title=SKINS", "-Dbc.skinuser=jeb_"], True),
    "skin-dialog":    (["-Dbc.auto=1", "-Dbc.title=SKINS", "-Dbc.skinbrowse=1"], True),
    "skin-screen-def": (["-Dbc.auto=1", "-Dbc.title=SKINS"], True),
    "day-shaders":    (["-Dbc.auto=1", "-Dbc.seed=12345", "-Dbc.mode=1", "-Dbc.rd=2", "-Dbc.lod=16", CAM, "-Dbc.time=6000", "-Dbc.shot=20"], True),
}
DEFAULT_SCENES = ["title", "day-plain", "mobs", "farm", "farm-close", "sheep36", "pigs36", "inv-creative-2"]


def run_scene(java, label, jar, name, props, timeout):
    home = tempfile.mkdtemp(prefix="bc-home-")
    env = dict(os.environ, HOME=home, LIBGL_ALWAYS_SOFTWARE="1", GALLIUM_DRIVER="llvmpipe", LP_NUM_THREADS="2")
    cmd = ["xvfb-run", "-a", "-s", "-screen 0 1280x800x24 +extension GLX +render -noreset",
           java, "-Xmx1g", "-Duser.home=" + home] + props + ["-jar", jar]
    t0 = time.time()
    try:
        r = subprocess.run(cmd, env=env, capture_output=True, text=True, timeout=timeout)
        status = "exit %d" % r.returncode
        err = (r.stderr or "")[-1500:]
    except subprocess.TimeoutExpired:
        status = "TIMEOUT"
        err = ""
    dt = time.time() - t0
    TIMES.append("%s/%s: %s in %.0fs" % (name, label, status, dt))
    shots = sorted(glob.glob(os.path.join(home, ".blockcraft112", "screenshots", "*.png")))
    log = os.path.join(home, ".blockcraft112", "latest.log")
    os.makedirs(os.path.join(OUT, "logs"), exist_ok=True)
    if os.path.exists(log):
        shutil.copy(log, os.path.join(OUT, "logs", "%s-%s.log" % (name, label)))
    elif err:
        with open(os.path.join(OUT, "logs", "%s-%s.log" % (name, label)), "w") as f:
            f.write(err)
    png = None
    if shots:
        png = os.path.join(OUT, "%s-%s.png" % (name, label))
        shutil.copy(shots[-1], png)
    shutil.rmtree(home, ignore_errors=True)
    print("  [%-4s] %-16s %s in %.0fs -> %s" % (label, name, status, dt, "ok" if png else "NO SCREENSHOT"), flush=True)
    return png


def im(*args):
    return subprocess.run(list(args), capture_output=True, text=True)


def diff_pct(a, b):
    """Percentage of pixels that differ by more than a small tolerance."""
    r = im("compare", "-metric", "AE", "-fuzz", "6%", a, b, "null:")
    txt = (r.stderr or r.stdout).strip().split()
    try:
        n = float(txt[0])
    except Exception:
        return None
    w, h = [int(x) for x in im("identify", "-format", "%w %h", a).stdout.split()]
    return 100.0 * n / (w * h)


def composite(paths, labels, out):
    args = ["convert"]
    for p, l in zip(paths, labels):
        args += ["(", p, "-resize", "800x", "-gravity", "NorthWest", "-fill", "yellow", "-stroke", "black",
                 "-strokewidth", "1", "-pointsize", "20", "-annotate", "+8+6", l, ")"]
    args += ["+append", "-quality", "78", out]
    im(*args)


def release_jar():
    """The blockcraft.jar inside the newest committed update pack (the exact artifact players get), if there is one."""
    if os.environ.get("BC_NEW_JAR") == "build":
        return None  # test the freshly built jar instead (ci/visual-trigger.txt, line 2: jar=build)
    packs = sorted(glob.glob(os.path.join(ROOT, "releases", "Blockcraft-UPDATE*-patch.zip")),
                   key=lambda pth: tuple(int(x) for x in re.search(r"UPDATE([0-9.]+)-patch", pth).group(1).split(".")))  # 3.10 > 3.9
    if not packs:
        return None
    import zipfile
    out = os.path.join(ROOT, "build", "release-blockcraft.jar")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with zipfile.ZipFile(packs[-1]) as z, open(out, "wb") as f:
        f.write(z.read("Blockcraft/blockcraft.jar"))
    print("new jar = Blockcraft/blockcraft.jar from " + os.path.basename(packs[-1]))
    return out


def write_report(rows):
    with open(os.path.join(OUT, "report.md"), "w") as f:
        f.write("# Visual run %s\n\n| scene | rendered by | differing pixels (ref vs new) |\n|---|---|---|\n" % time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()))
        for r in rows:
            f.write("| %s | %s | %s |\n" % (r["scene"], ", ".join(r["have"]) or "NOTHING", "n/a" if r["diff"] is None else "%.2f%%" % r["diff"]))
        f.write("\n## timings\n\n" + "\n".join("- " + t for t in TIMES) + "\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--scenes", default="all")
    ap.add_argument("--jar", action="append", help="label=path (repeatable)")
    ap.add_argument("--timeout", type=int, default=240)
    a = ap.parse_args()
    jars = {}
    if a.jar:
        for j in a.jar:
            k, v = j.split("=", 1)
            jars[k] = os.path.abspath(v)
    else:
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        import runtests as T  # reuses reference_jar()
        jars = {"ref": T.reference_jar(), "new": release_jar() or os.path.join(ROOT, "build", "blockcraft.jar")}
    java = shutil.which("java") or "java"
    names = DEFAULT_SCENES if a.scenes == "all" else [x.strip() for x in a.scenes.split(",") if x.strip()]
    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for name in names:
        props, only_new = SCENES[name]
        got = {}
        for label, jar in jars.items():
            if only_new and label != "new":
                continue
            got[label] = run_scene(java, label, jar, name, props, a.timeout)
        row = {"scene": name, "diff": None}
        have = [l for l in got if got[l]]
        if len(have) >= 1:
            composite([got[l] for l in have], have, os.path.join(OUT, "%s.jpg" % name))
        if "ref" in got and "new" in got and got["ref"] and got["new"]:
            row["diff"] = diff_pct(got["ref"], got["new"])
        row["have"] = have
        rows.append(row)
        write_report(rows)
        # keep the repo small: only the side-by-side JPEG is kept, full PNGs are deleted
        for l in got:
            if got[l] and os.path.exists(got[l]):
                os.remove(got[l])
    write_report(rows)
    print(open(os.path.join(OUT, "report.md")).read())


if __name__ == "__main__":
    main()
