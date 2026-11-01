#!/usr/bin/env python3
"""Generate stub hack files for the 21 upstream hacks missing from the fork."""
import os, re

UP = "/home/z/my-project/modport/original/src/main/java/net/wurstclient"
FORK = "/home/z/my-project/modport/fork/src/main/java/net/wurst0/hacks"

def find_up(cls):
    import glob
    hits = glob.glob(os.path.join(UP, "hacks/**", cls + ".java"), recursive=True)
    return hits[0] if hits else None

MISSING = ["AirstrikePlusHack", "ArmorStandImagesHack", "AutoCommandHack",
    "AutoDisplaysHack", "AutoNamesHack", "AutoScoreboardHack",
    "AutoTextsHack", "AutoTitlesHack", "BoomPlusHack", "ExplosionAuraHack",
    "ForceOPBookHack", "ForceOPSignHack", "ForceTPHack", "HandOfGodHack",
    "MultiverseAnnihilatorHack", "NBTEditorHack", "OPServerKillModuleHack",
    "OPplayerTPmoduleHack", "SpawnRadiusHack", "UUIDBanHack", "VoiderPlusHack"]

CAT_MAP = {
    "CREATIVE_OP": "CREATIVE_OP", "OTHER": "OTHER", "TOOLS": "TOOLS",
    "CHAT": "CHAT", "RENDER": "RENDER", "BLOCKS": "BLOCKS",
    "MOVEMENT": "MOVEMENT", "COMBAT": "COMBAT", "ITEMS": "ITEMS",
    "FUN": "FUN", "INTEL": "INTEL",
}

TEMPLATE = '''/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * TODO: port logic from net/wurstclient/hacks/{cls}.java in the original
 * repo (MC 26.3 / Fabric). This stub only registers the hack so it
 * appears in the GUI; it currently does nothing when enabled.
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.feature.Hack;

public final class {cls} extends Hack
{{
        public {cls}()
        {{
                super("{name}", "{desc}");
                setCategory(Category.{cat});
        }}
}}
'''

for cls in MISSING:
    path = os.path.join(FORK, cls + ".java")
    if os.path.exists(path):
        print("exists, skip:", cls)
        continue
    up = find_up(cls)
    if up is None:
        print("NOT FOUND:", cls)
        continue
    src = open(up).read()
    # category
    m = re.search(r"Category\.([A-Z_]+)\)", src)
    upcat = m.group(1) if m else "OTHER"
    cat = CAT_MAP.get(upcat, "OTHER")
    # description: first super("Name", "...") string arg
    m2 = re.search(r'super\(\s*"[^"]*"\s*,\s*\n?\s*"((?:[^"\\]|\\.)*)"', src, re.S)
    desc = m2.group(1) if m2 else "not ported yet"
    # de-escape and truncate the java-concatenated description
    desc = desc.replace('"\n\t\t\t\t+ "', "").replace('\\"', "'")
    desc = re.sub(r"\s+", " ", desc).strip()
    if not desc.startswith("[not ported yet]"):
        desc = "[not ported yet] " + desc
    desc = desc[:170]
    # display name
    m3 = re.search(r'super\(\s*"([^"]*)"', src)
    name = m3.group(1) if m3 else re.sub(r"Hack$", "", cls)
    open(path, "w").write(TEMPLATE.format(cls=cls, name=name, desc=desc, cat=cat))
    print("generated:", cls, "->", cat, "|", name)
