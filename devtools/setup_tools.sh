#!/bin/bash
# Setup build toolchain for Wurst0 1.12.2 (JDK8 toolchain, JDK25 runtime, Gradle 9.8.2, patched RFG)
set -x
cd /home/z/my-project/tools

# 1. Extract JDKs and Gradle
mkdir -p jdk8 jdk25
tar -xzf dl/jdk8.tar.gz -C jdk8 --strip-components=1
tar -xzf dl/jdk25.tar.gz -C jdk25 --strip-components=1
python3 -c "import zipfile; zipfile.ZipFile('dl/gradle.zip').extractall('.')"
chmod +x gradle-9.8.2/bin/gradle

# 2. Download original RFG jar from GTNH nexus
mkdir -p rfgdl
curl -sL -o rfg-orig.jar "https://nexus.gtnewhorizons.com/repository/public/com/gtnewhorizons/retrofuturagradle/2.0.2/retrofuturagradle-2.0.2.jar"

# 3. Patch RFG decompiler heap for 4GB sandbox -> local maven
cd /home/z/my-project/modport/fork
python3 devtools/patch_rfg_heap.py

# 4. Point gradle.properties at the extracted JDK 8
JDK8BIN=$(ls -d /home/z/my-project/tools/jdk8)
sed -i "s|org.gradle.java.installations.paths=.*|org.gradle.java.installations.paths=$JDK8BIN|" /home/z/my-project/modport/fork/gradle.properties

echo "TOOLCHAIN SETUP DONE"
