#!/bin/bash
# Push the mc1.12.2 branch, tag v0.4.0 and create the GitHub release
# with the built jar attached.
#
# Usage:  GITHUB_TOKEN=<fresh PAT> bash devtools/release_v040.sh
#
# The token is never printed or stored; it is only used for this one
# run via the environment. Prefer a fine-grained PAT limited to the
# apikun/Wurst0-CevAPI repo (contents: read/write).
set -e
cd /home/z/my-project/modport/fork

TOKEN="${GITHUB_TOKEN:-}"
if [ -z "$TOKEN" ]; then
  # fallback: extract from the remote URL if it embeds credentials
  TOKEN=$(git remote get-url origin \
    | sed -n 's#https://[^:/]*:\([^@]*\)@github.com/.*#\1#p')
fi
[ -n "$TOKEN" ] || { echo "no GITHUB_TOKEN set and remote has no embedded token"; exit 1; }

AUTH="Authorization: Bearer $TOKEN"

# 1. push branch + tag
git push "https://x-access-token:${TOKEN}@github.com/apikun/Wurst0-CevAPI.git" mc1.12.2
git tag -f v0.4.0
git push "https://x-access-token:${TOKEN}@github.com/apikun/Wurst0-CevAPI.git" v0.4.0

# 2. create release
RELEASE_JSON=$(cat <<'EOF'
{
  "tag_name": "v0.4.0",
  "target_commitish": "mc1.12.2",
  "name": "Wurst0 v0.4.0 - Minecraft 1.12.2",
  "body": "Third release of the **Wurst0** 1.12.2 port (Wurst7-CevAPI -> Forge 1.12.2, Java 8).\n\n## Install\n1. Install Minecraft 1.12.2 with **Forge 1.12.2-14.23.5.2859** (or newer 14.23.5.x).\n2. Drop `Wurst0-0.4.0.jar` into your `.minecraft/mods` folder.\n3. In game: **Right Shift** opens the ClickGUI, `.help` lists all 12 commands.\n\n## What's new in v0.4.0\n**34 newly working hacks** - 111 of 317 total, 7 new commands:\n\n**Render:** Freecam (fake player body, frozen real player), NoHurtcam, NoVignette, NoOverlay (pumpkin/portal), NoFog (liquid fog), NoShieldOverlay, RedstoneESP, DurabilityHUD, ProjectileESP, PearlESP, TrueSight (invisible entities)\n\n**Movement:** ElytraFlight (Normal/Hover), BoatFly, Dolphin, NoLevitation, AntiWaterPush, AntiHunger (packet-level, approximate)\n\n**Combat/Items:** AutoTotem (offhand swap), Restock, DuraSwap, AntiDrop, FeedAura, Tillaura, AutoClicker, AntiBlast (explosion protection), AutoLeave, Throw\n\n**Chat:** FancyChat (fullwidth unicode), NoPlayerChat, NameProtect\n\n**Meta:** Panic, GlobalToggle (keybindable kill switch), HideWurst (HUD off for screenshots), ClickGUI hack (bind the GUI to any key)\n\n**New commands:** `.vclip` `.jump` `.gm` `.setmode` `.setslider` `.setcheckbox` `.features`\n\n## Notes\n- Several hacks are marked approximate - true packet-cancelling (Velocity, AntiHunger sprint suppression, real Speed), block-hiding X-Ray and reach need the SpongeMixin phase, which is next on the roadmap.\n- GPLv3, source on the `mc1.12.2` branch.\n- Port of Wurst7-CevAPI for legacy servers; use responsibly and follow server rules.",
  "draft": false,
  "prerelease": false
}
EOF
)

RESP=$(curl -s -w "\n%{http_code}" -X POST \
  -H "$AUTH" -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/apikun/Wurst0-CevAPI/releases \
  -d "$RELEASE_JSON")
HTTP_CODE=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
echo "release create http_code=$HTTP_CODE"
RELEASE_ID=$(echo "$BODY" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || true)
if [ -z "$RELEASE_ID" ]; then
  RELEASE_ID=$(curl -s -H "$AUTH" \
    https://api.github.com/repos/apikun/Wurst0-CevAPI/releases/tags/v0.4.0 \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")
fi
[ -n "$RELEASE_ID" ] || { echo "could not resolve release id"; exit 1; }

# 3. upload jar
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "$AUTH" -H "Content-Type: application/java-archive" \
  "https://uploads.github.com/repos/apikun/Wurst0-CevAPI/releases/${RELEASE_ID}/assets?name=Wurst0-0.4.0.jar" \
  --data-binary @build/libs/Wurst0-0.4.0.jar)
echo "jar upload http_code=$HTTP_CODE"
echo "done - release id $RELEASE_ID"
