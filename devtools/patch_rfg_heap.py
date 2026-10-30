#!/usr/bin/env python3
"""Patch the RetroFuturaGradle 2.0.2 plugin jar so the FernFlower decompiler
worker uses a heap that fits in a 4GB sandbox (3072M -> 2304M), and publish it
to a local maven repo that settings.gradle prefers via mavenLocal().
"""
import os
import shutil
import zipfile

SRC_JAR = "/home/z/my-project/tools/rfg-orig.jar"
DST_DIR = ("/home/z/my-project/tools/localmaven/com/gtnewhorizons/"
           "retrofuturagradle/2.0.2")
DST_JAR = os.path.join(DST_DIR, "retrofuturagradle-2.0.2.jar")

OLD = b"3072M"
NEW = b"2048M"

if not os.path.isfile(SRC_JAR):
    raise SystemExit("source jar not found: " + SRC_JAR)

os.makedirs(DST_DIR, exist_ok=True)

replacements = 0
with zipfile.ZipFile(SRC_JAR, "r") as zin, \
        zipfile.ZipFile(DST_JAR, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename.endswith("mcp/DecompileTask.class"):
            count = data.count(OLD)
            if count:
                data = data.replace(OLD, NEW)
                replacements += count
                print("patched %d occurrence(s) in %s" % (count, item.filename))
        zout.writestr(item, data)

if replacements == 0:
    raise SystemExit("no occurrences patched - aborting, jar NOT published")

shutil.copyfile(DST_JAR, DST_JAR + ".bak")
print("published patched plugin to", DST_JAR)
