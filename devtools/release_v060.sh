#!/bin/bash
# Push the mc1.12.2 branch, tag v0.6.0 and create the GitHub release
# with the built jar attached.
#
# Usage:  GITHUB_TOKEN=<fresh PAT> bash devtools/release_v060.sh
#
# The token is never printed or stored; it is only used for this one
# run via the environment. Prefer a fine-grained PAT limited to the
# apikun/Wurst0-CevAPI repo (contents: read/write).
set -e
cd /home/z/my-project/modport/fork

TOKEN="${GITHUB_TOKEN:-}"
if [ -z "$TOKEN" ]; then
  # fallback: extract from the credential store file (never echoed)
  TOKEN=$(sed -n 's#https://[^:/]*:\([^@]*\)@github.com#\1#p' \
    /home/z/my-project/scripts/.gitcred 2>/dev/null | head -1)
fi
[ -n "$TOKEN" ] || { echo "no GITHUB_TOKEN available"; exit 1; }

AUTH="Authorization: Bearer $TOKEN"

# 1. push branch + tag
git push "https://x-access-token:${TOKEN}@github.com/apikun/Wurst0-CevAPI.git" mc1.12.2
git tag -f v0.6.0
git push "https://x-access-token:${TOKEN}@github.com/apikun/Wurst0-CevAPI.git" v0.6.0

# 2. create release
RELEASE_JSON=$(cat <<'EOF'
{
  "tag_name": "v0.6.0",
  "target_commitish": "mc1.12.2",
  "name": "Wurst0 v0.6.0 - Minecraft 1.12.2",
  "body": "Fifth release of the **Wurst0** 1.12.2 port (Wurst7-CevAPI -> Forge 1.12.2, Java 8).\n\n## Install\n1. Install Minecraft 1.12.2 with **Forge 1.12.2-14.23.5.2859** (or newer 14.23.5.x).\n2. Drop `Wurst0-0.6.0.jar` into your `.minecraft/mods` folder.\n3. In game: **Right Shift** opens the ClickGUI, `.help` lists all 12 commands.\n\n## What's new in v0.6.0\n**30 more working hacks - now 153 of 317** (up from 123):\n\n- **Movement:** Sneak, CreativeFlight, InvWalk (walk while your inventory is open), NoClip (client-side), ExtraElytra (auto-swaps a dying elytra)\n- **Combat:** ClickAura (hits everything in reach, no rotation), BedBreakAura, PearlDetect (pearl incoming warning)\n- **Blocks:** VeinMiner (mines whole ore veins), Tunneller (2-tall tunnel ahead), AutoSign (auto-writes your text on every sign you open)\n- **Render/Intel:** HealthTags, BaseFinder (reveals man-made blocks / hidden bases), CaveFinder, MobSpawnESP, RoofEsp (nether roof), WorkstationEsp, LavaWaterEsp\n- **HUD/Tools:** EntityCount, ElytraInfo, NbtSizeCounter\n- **Chat:** CommandSpam, Mention (highlights when your name is said), PlayerMute\n- **Fun/Other:** HeadRoll, MobView (see through the nearest mob's eyes), Antisocial (auto-walks away from players), Reach (via the vanilla reach attribute), ItemGenerator (creative), UseItemSpam\n\n## Notes\n- Reach works through Forge's `generic.reachDistance` attribute, so it is clean and client-side only; server reach checks still apply.\n- CameraDistance could not be ported yet: in 1.12.2 the third-person camera distance is a compiler-inlined constant - it is the first candidate for the upcoming mixin phase.\n- GPLv3, source on the `mc1.12.2` branch.\n- Port of Wurst7-CevAPI for legacy servers; use responsibly and follow server rules.",
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
    https://api.github.com/repos/apikun/Wurst0-CevAPI/releases/tags/v0.6.0 \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))")
fi
[ -n "$RELEASE_ID" ] || { echo "could not resolve release id"; exit 1; }

# 3. upload jar
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "$AUTH" -H "Content-Type: application/java-archive" \
  "https://uploads.github.com/repos/apikun/Wurst0-CevAPI/releases/${RELEASE_ID}/assets?name=Wurst0-0.6.0.jar" \
  --data-binary @build/libs/Wurst0-0.6.0.jar)
echo "jar upload http_code=$HTTP_CODE"
echo "done - release id $RELEASE_ID"
