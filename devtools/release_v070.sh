#!/bin/bash
# Push mc1.12.2, tag v0.7.0 and create the GitHub release with the jar.
# The token is read from ~/.git-credentials and is never printed.
set -e
cd /home/z/my-project/modport/fork

TOKEN=$(sed -n 's#https://[^:/]*:\([^@]*\)@github.com#\1#p' \
  ~/.git-credentials 2>/dev/null | head -1)
[ -n "$TOKEN" ] || { echo "no token available"; exit 1; }

AUTH="Authorization: Bearer $TOKEN"
REPO="apikun/Wurst0-CevAPI"

RELEASE_JSON=$(cat <<'EOF'
{
  "tag_name": "v0.7.0",
  "target_commitish": "mc1.12.2",
  "name": "Wurst0 v0.7.0 - Minecraft 1.12.2",
  "body": "Seventh release of the **Wurst0** 1.12.2 port (Wurst7-CevAPI -> Forge 1.12.2, Java 8).\n\n## Install\n1. Install Minecraft 1.12.2 with **Forge 1.12.2-14.23.5.2859** (or newer 14.23.5.x).\n2. Drop `Wurst0-0.7.0.jar` into your `.minecraft/mods` folder.\n3. In game: **Right Shift** opens the ClickGUI, `.help` lists all 13 commands.\n\n## What's new in v0.7.0\n**31 more working hacks - now 183 of 308** (up from 152):\n\n- **Movement:** SafeWalk, AntiEntityPush, ClutchFall (auto water-bucket MLG)\n- **Items:** AutoLoot (walks to dropped items), AntiBreak (tool protection), KillPotion + TrollPotion (creative)\n- **Blocks:** Liquids (place blocks on water/lava), InstantBunker (emergency bunker), AirMiner (no-aim mining)\n- **Combat:** CrystalAura (end crystal killer), AntiProjectile (punch fireballs/shulker bullets back), ArrowDMG (arrow speed boost)\n- **Chat/Tools:** AutoChat, InstantCommand, ForceOP (password list), StaffMonitor (staff join alerts), .wp waypoints command\n- **Render/Intel:** DamageESP, SkyBuildESP (sky bases), SurfaceXray, MobOwners, MeasurementESP, Waypoints, NoPumpkin\n- **HUD:** ServerIntel, GameStats (FPS + ping)\n- **Other:** SoundMute, WebhookAlert (Discord), TooManyHax (hides features from the GUI), DamageDetect, PlayerSonar\n\n## Notes\n- GPLv3, source on the `mc1.12.2` branch.\n- Port of Wurst7-CevAPI for legacy servers; use responsibly and follow server rules.",
  "draft": false,
  "prerelease": false
}
EOF
)

git push "https://x-access-token:${TOKEN}@github.com/${REPO}.git" mc1.12.2
git tag -f v0.7.0
git push "https://x-access-token:${TOKEN}@github.com/${REPO}.git" v0.7.0

RELEASE_ID=$(curl -s -H "$AUTH" -H "Content-Type: application/json" \
  -X POST "https://api.github.com/repos/${REPO}/releases" \
  -d "$RELEASE_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")

if [ -z "$RELEASE_ID" ]; then echo "release creation failed"; exit 1; fi
echo "release id: $RELEASE_ID"

curl -s -H "$AUTH" -H "Content-Type: application/octet-stream" \
  -X POST "https://uploads.github.com/repos/${REPO}/releases/${RELEASE_ID}/assets?name=Wurst0-0.7.0.jar" \
  --data-binary @build/libs/Wurst0-0.7.0.jar \
  -o /tmp/upload_result.json -w "upload http: %{http_code}\n"

python3 -c "import json; d=json.load(open('/tmp/upload_result.json')); print('asset:', d.get('name'), d.get('size'))"
