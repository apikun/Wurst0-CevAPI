#!/usr/bin/env python3
"""Insert the 21 new hack registrations into Hacks.java alphabetically."""
import re

HACKS = "/home/z/my-project/modport/fork/src/main/java/net/wurst0/hacks/Hacks.java"
NEW = ["AirstrikePlusHack", "ArmorStandImagesHack", "AutoCommandHack",
    "AutoDisplaysHack", "AutoNamesHack", "AutoScoreboardHack",
    "AutoTextsHack", "AutoTitlesHack", "BoomPlusHack", "ExplosionAuraHack",
    "ForceOPBookHack", "ForceOPSignHack", "ForceTPHack", "HandOfGodHack",
    "MultiverseAnnihilatorHack", "NBTEditorHack", "OPServerKillModuleHack",
    "OPplayerTPmoduleHack", "SpawnRadiusHack", "UUIDBanHack", "VoiderPlusHack"]

lines = open(HACKS).read().split("\n")
existing = set()
out = []
for ln in lines:
    m = re.match(r"\s*add\(new (\w+)\(\)\);", ln)
    if m:
        existing.add(m.group(1))
    out.append((ln, m.group(1) if m else None))

for cls in NEW:
    if cls in existing:
        print("already registered:", cls)
        continue
    needle = "\tadd(new " + cls + "());"
    inserted = False
    for i, (ln, name) in enumerate(out):
        if name and name.lower() > cls.lower():
            out.insert(i, (needle, cls))
            inserted = True
            break
    if not inserted:
        # append at end of list section: find last add(...) line
        last = max(i for i, (ln, name) in enumerate(out) if name)
        out.insert(last + 1, (needle, cls))
    print("registered:", cls)

open(HACKS, "w").write("\n".join(ln for ln, _ in out))
print("total add() lines:", sum(1 for ln, n in out if n))
