#!/bin/bash
# Commit, push, tag and create the GitHub release for Wurst0 v0.2.0.
# The PAT is read from the git remote URL of the fork - never printed.
set -e
cd /home/z/my-project/modport/fork

# 1. commit
git add -A
git commit -m "Wurst0 v0.2.0: 37 more working hacks (combat/movement/world/render), RFG heap fixes for 4GB sandboxes

Combat: Killaura, KillauraLegit, Criticals (Packet/MiniJump/FullJump),
AntiKnockback (Velocity), AimAssist, AutoSoup, TriggerBot, AutoSword
Movement: NoFall packet mode, Jesus, ScaffoldWalk, BunnyHop, HighJump,
AntiVoid, FastLadder, AirWalk (air jumps)
World/Blocks: FastBreak, AutoEat, AutoFarm, AutoMine, AutoWalk,
BuildRandom, AreaNuker, NukerLegit, SpeedNuker, AntiAfk, AutoRespawn
Render: ItemESP, SpawnerESP, PortalESP, BedESP, BarrierESP, Breadcrumbs,
Trajectories, NoWeather, AntiFov + Box/Lines/Both tracers styles on
PlayerESP/MobESP
Build: LocalFiller maven repo for Central 429 workarounds, RFG decompile
heap patch moved into 1536M, settings.gradle repository notes" 2>/dev/null || echo "(nothing to commit)"

# 2. push branch
git push origin mc1.12.2

# 3. tag + push tag
git tag -f v0.2.0
git push origin v0.2.0

# 4. release via API - token extracted from remote URL, never printed
TOKEN=$(git remote get-url origin | sed -n 's#.*://[^@]*@\([^:]*\).*#\1#p')
# format is https://x-access-token:TOKEN@github.com/... - extract between : and @
TOKEN=$(git remote get-url origin | sed 's#https://x-access-token:##; s#@github.com.*##')
[ -n "$TOKEN" ] || { echo "no token found in remote"; exit 1; }

RELEASE_JSON=$(cat <<'EOF'
{
  "tag_name": "v0.2.0",
  "target_commitish": "mc1.12.2",
  "name": "Wurst0 v0.2.0 - Minecraft 1.12.2",
  "body": "Second release of the **Wurst0** 1.12.2 port (Wurst7-CevAPI -> Forge 1.12.2, Java 8).\n\n## Install\n1. Install Minecraft 1.12.2 with **Forge 1.12.2-14.23.5.2859** (or newer 14.23.5.x).\n2. Drop `Wurst0-0.2.0.jar` into your `.minecraft/mods` folder.\n3. In game: **Right Shift** opens the ClickGUI, `.help` lists commands.\n\n## What's new in v0.2.0\nThe core framework (ClickGUI, HUD, config saving, keybinds, 317 registered hacks) is joined by **37 newly working hacks** - 52 of 317 total:\n\n**Combat:** Killaura, KillauraLegit, Criticals (Packet/MiniJump/FullJump), AntiKnockback (Velocity), AimAssist, AutoSoup, TriggerBot, AutoSword\n\n**Movement:** NoFall (new packet mode), Jesus, ScaffoldWalk, BunnyHop, HighJump, AntiVoid, FastLadder, AirWalk\n\n**World/Blocks:** FastBreak, AutoEat, AutoFarm, AutoMine, AutoWalk, BuildRandom, AreaNuker, NukerLegit, SpeedNuker, AntiAfk, AutoRespawn\n\n**Render:** ItemESP, SpawnerESP, PortalESP, BedESP, BarrierESP, Breadcrumbs, Trajectories, NoWeather, AntiFov, plus new Box/Lines/Both tracer styles on PlayerESP/MobESP\n\n## Still stubbed\n~265 hacks are registered but not yet ported (they show `[not ported yet]`). True block-hiding X-Ray, Freecam and packet hooks (Velocity via packets, real Speed/Fly modes) need the SpongeMixin phase - next on the roadmap.\n\n## Notes\n- GPLv3, source on the `mc1.12.2` branch.\n- This is a port of Wurst7-CevAPI for legacy servers; use responsibly and follow server rules.",
  "draft": false,
  "prerelease": false
}
EOF
)

RESP=$(curl -s -w "\n%{http_code}" -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/apikun/Wurst0-CevAPI/releases \
  -d "$RELEASE_JSON")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
echo "release create http_code=$HTTP_CODE"
RELEASE_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || true)
if [ -z "$RELEASE_ID" ]; then
  # release may already exist -> fetch it
  RELEASE_ID=$(curl -s -H "Authorization: Bearer $TOKEN" \
    https://api.github.com/repos/apikun/Wurst0-CevAPI/releases/tags/v0.2.0 \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")
fi
echo "release id=$RELEASE_ID"

# 5. upload jar
UPLOAD_URL="https://uploads.github.com/repos/apikun/Wurst0-CevAPI/releases/$RELEASE_ID/assets?name=Wurst0-0.2.0.jar"
code=$(curl -s -o /tmp/upload_resp.json -w "%{http_code}" -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/java-archive" \
  --data-binary @build/libs/Wurst0-0.2.0.jar \
  "$UPLOAD_URL")
echo "upload http_code=$code"
python3 -c "import json; d=json.load(open('/tmp/upload_resp.json')); print('asset:', d.get('name'), d.get('state'), d.get('browser_download_url'))" 2>/dev/null || cat /tmp/upload_resp.json | head -5
echo "RELEASE DONE"
