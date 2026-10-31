#!/bin/bash
# Push the mc1.12.2 branch, tag v0.5.0 and create the GitHub release
# with the built jar attached.
#
# Usage:  GITHUB_TOKEN=<fresh PAT> bash devtools/release_v050.sh
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
git tag -f v0.5.0
git push "https://x-access-token:${TOKEN}@github.com/apikun/Wurst0-CevAPI.git" v0.5.0

# 2. create release
RELEASE_JSON=$(cat <<'EOF'
{
  "tag_name": "v0.5.0",
  "target_commitish": "mc1.12.2",
  "name": "Wurst0 v0.5.0 - Minecraft 1.12.2",
  "body": "Fourth release of the **Wurst0** 1.12.2 port (Wurst7-CevAPI -> Forge 1.12.2, Java 8).\n\n## Install\n1. Install Minecraft 1.12.2 with **Forge 1.12.2-14.23.5.2859** (or newer 14.23.5.x).\n2. Drop `Wurst0-0.5.0.jar` into your `.minecraft/mods` folder.\n3. In game: **Right Shift** opens the ClickGUI, `.help` lists all 12 commands.\n\n## What's new in v0.5.0\n**ClickGUI small-screen overhaul** - the headline feature of this release:\n- Windows now lay out in **columns that fit the screen** instead of cascading off it - the GUI is finally usable on small windows and high GUI scales.\n- Windows **scroll with the mouse wheel** (with a scrollbar), so big categories like Movement (30 hacks) are fully reachable on any screen.\n- Drag is clamped so windows can never get lost, overlapping windows no longer both react to clicks, clicking a window brings it to the front, and window width adapts to tiny screens.\n- Fixed: clicking a settings row toggled the hack instead of the setting; only the first feature's sliders could actually be dragged.\n- HUD arraylist is capped with `+N more` so it can't run off the bottom of small screens.\n\n**Free-text settings (new setting type)** - text fields are now editable directly in the ClickGUI (click, type, Enter). ChatSpam's fixed presets are replaced by any message you want. Saved in the config.\n\n**12 newly working hacks** - 123 of 317 total:\n- **ShearAura** (auto-shears sheep/mooshrooms, hotbar shear switch), **BonemealAura** (hold bone meal), **Follow** (follows the closest player)\n- **MobHealth** (crosshair entity health), **SignESP**, **LogoutSpots** (where players logged out)\n- **Kaboom** (dig-packet bursts), **NoWeb** (cobweb slowdown compensation), **InfiniChat** (2000-char chat input), **Derp**, **AntiWobble**, **RainbowUi**\n\n## Notes\n- Packet-level hacks (true Velocity, real Speed, packet-cancelling) and true X-Ray still need the SpongeMixin phase, next on the roadmap.\n- GPLv3, source on the `mc1.12.2` branch.\n- Port of Wurst7-CevAPI for legacy servers; use responsibly and follow server rules.",
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
    https://api.github.com/repos/apikun/Wurst0-CevAPI/releases/tags/v0.5.0 \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")
fi
[ -n "$RELEASE_ID" ] || { echo "could not resolve release id"; exit 1; }

# 3. upload jar
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "$AUTH" -H "Content-Type: application/java-archive" \
  "https://uploads.github.com/repos/apikun/Wurst0-CevAPI/releases/${RELEASE_ID}/assets?name=Wurst0-0.5.0.jar" \
  --data-binary @build/libs/Wurst0-0.5.0.jar)
echo "jar upload http_code=$HTTP_CODE"
echo "done - release id $RELEASE_ID"
