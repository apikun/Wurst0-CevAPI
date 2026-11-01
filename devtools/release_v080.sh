#!/bin/bash
# Push mc1.12.2, tag v0.8.0 and create the GitHub release with the jar.
# The token is read from ~/.git-credentials and is never printed.
set -e
cd /home/z/my-project/modport/fork

TOKEN=$(sed -n 's#https://[^:/]*:\([^@]*\)@github.com#\1#p' \
  ~/.git-credentials 2>/dev/null | head -1)
[ -n "$TOKEN" ] || { echo "no token available"; exit 1; }

AUTH="Authorization: Bearer $TOKEN"
REPO="apikun/Wurst0-CevAPI"

cat > /tmp/relbody.md <<'EOF'
Eighth release of the **Wurst0** 1.12.2 port (Wurst7-CevAPI -> Forge 1.12.2, Java 8).

## Install
1. Install Minecraft 1.12.2 with **Forge 1.12.2-14.23.5.2859** (or newer 14.23.5.x).
2. Drop `Wurst0-0.8.0.jar` into your `.minecraft/mods` folder.
3. In game: **Right Shift** opens the ClickGUI, `.help` lists all commands.

## What's new in v0.8.0
**50 more working hacks - now 233 of 317** (up from 183), stubs down to 84:

- **Render/Intel:** Overlay (dig progress), ChestSearch, PotEsp, OpenWaterEsp, XpGui, RenderAdjust, TunnelHoleStairEsp, NewerNewChunks, Mapa
- **Fun:** FunCreepers, NecoMode, Flicker, MileyCyrus
- **Elytra/Movement:** ElytraPitch, ElytraDive, ElytraBounce, ElytraWalk, EntityControl, BoatPhase, Teleport, SafeTp, PearlLauncher, StairMaker
- **Combat:** InfiniteReach (teleport-strike), Outreach, SpearAssist, TargetPlace
- **Blocks:** SuperInstaMine, AirPlace, BonemealAura, SilkOnly, PlaceNBreak, AutoBuild, InstaBuild, TreeBot, Protect (protection sphere shared by all nukers)
- **Intel:** CheatDetector, AntiCheatDetect, Telemetrics, CoordLogger, NoGoZone, SignHistory, RespawnBedStore
- **Inventory:** InventorySorter, FastFill, LootSorter, XCarry (lite), QuickShulker, AutoTrader, BookBot

## Notes
- GPLv3, source on the `mc1.12.2` branch.
- Port of Wurst7-CevAPI for legacy servers; use responsibly and follow server rules.
EOF

python3 - <<'PYEOF'
import json
body = open('/tmp/relbody.md').read()
json.dump({
  "tag_name": "v0.8.0",
  "target_commitish": "mc1.12.2",
  "name": "Wurst0 v0.8.0 - Minecraft 1.12.2",
  "body": body,
  "draft": False,
  "prerelease": False
}, open('/tmp/reljson.json', 'w'))
PYEOF

git push "https://x-access-token:${TOKEN}@github.com/${REPO}.git" mc1.12.2
git tag -f v0.8.0
git push "https://x-access-token:${TOKEN}@github.com/${REPO}.git" v0.8.0

RELEASE_ID=$(curl -s -H "$AUTH" -H "Content-Type: application/json" \
  -X POST "https://api.github.com/repos/${REPO}/releases" \
  -d @/tmp/reljson.json | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")

if [ -z "$RELEASE_ID" ]; then echo "release creation failed"; exit 1; fi
echo "release id: $RELEASE_ID"

curl -s -H "$AUTH" -H "Content-Type: application/octet-stream" \
  -X POST "https://uploads.github.com/repos/${REPO}/releases/${RELEASE_ID}/assets?name=Wurst0-0.8.0.jar" \
  --data-binary @build/libs/Wurst0-0.8.0.jar \
  -o /tmp/upload_result.json -w "upload http: %{http_code}\n"

python3 -c "import json; d=json.load(open('/tmp/upload_result.json')); print('asset:', d.get('name'), d.get('size'))"
