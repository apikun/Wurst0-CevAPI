#!/bin/bash
# Commit v0.9.1, push mc1.12.2, tag, and create the GitHub release with the jar.
# The token is read from ~/.git-credentials and is never printed.
set -e
cd /home/z/my-project/modport/fork

TOKEN=$(sed -n 's#https://[^:/]*:\([^@]*\)@github.com#\1#p' \
  ~/.git-credentials 2>/dev/null | head -1)
[ -n "$TOKEN" ] || { echo "no token available"; exit 1; }

AUTH="Authorization: Bearer $TOKEN"
REPO="apikun/Wurst0-CevAPI"

git add -A
git -c user.name="apikun" -c user.email="apikun@users.noreply.github.com" \
  commit -m "Wurst0 v0.9.1: Grim source-audit bypass suite (vital transaction passthrough, post-safe ordering, hold cap, reach shield) + Bedwars-safe Killaura v2 (team check, smooth aim, smart priority, hitbox range), AimAssist player-targeting fix"

git push origin mc1.12.2 2>&1 | grep -v "token" || true

git tag -f v0.9.1
git push origin v0.9.1 2>&1 | grep -v "token" || true

cat > /tmp/relbody091.md <<'EOF'
Ninth release of the **Wurst0** 1.12.2 port (Wurst7-CevAPI -> Forge 1.12.2, Java 8).

## Install
1. Install Minecraft 1.12.2 with **Forge 1.12.2-14.23.5.2859** (or newer 14.23.5.x).
2. Drop `Wurst0-0.9.1.jar` into your `.minecraft/mods` folder.
3. In game: **Right Shift** opens the ClickGUI, `.help` lists all commands.

## What's new in v0.9.1 - Grim bypass suite + Bedwars-safe aimbot

Built from a full source audit of [GrimAC's check suite](https://github.com/GrimAnticheat/Grim).

### GrimBypass v2 (check -> countermeasure)
- **Transaction passthrough** - `CPacketConfirmTransaction`/`CPacketKeepAlive`
  responses are never held by Blink/FakeLag/PacketDelay -> Grim's
  TransactionOrder and floating-transaction tracking see instant answers.
- **Post-safe ordering** - attack / use-item / dig / click-window / abilities /
  held-item / entity-action packets are released right after a transaction
  response (the slot vanilla uses) -> defeats Grim's **Post** check.
- **Hold cap** - cyclic flush bounds how long position packets stall during
  blinks -> defeats **BadPacketsR**.
- **Reach shield** - combat range clamped to 2.95 blocks (eye -> hitbox
  surface, same metric Grim's **Reach** uses) and the swing is always sent
  -> defeats **Reach** + **PacketOrderB** hit cancelling.
- **Rotation smoothing** - per-tick snap clamp, pitch clamped to [-90,90]
  -> defeats **AimModulo360** / **BadPacketsD**.
- **Timer cap** + flag alerts + auto-disable (from v0.9).

### Killaura v2 - the Bedwars-safe aimbot
- **Team check**: never hits your teammates - scoreboard team match
  (Hypixel Bedwars teams are scoreboard teams) with colored-nametag
  fallback. Shared in TeamUtils, also wired into KillauraLegit + AimAssist.
- **Smooth aim**: eased rotation stepping with attack-on-converge (30deg)
  - human-looking, no snapping.
- **Smart priority**: distance 0.4 + view angle 0.4 + missing health 0.2.
- **Hitbox-aware range/aim**: measures and aims at the closest hitbox point.
- AimAssist fix: it silently skipped all players before (useless on
  Bedwars) - now it assists on players too, "Ignore teammates" default on.

### Honest limits
Grim re-simulates movement, so Speed/Flight-style prediction cheats remain
detected. This suite hardens the *packet* surface (order, transactions,
reach, rotation, timer) - the part a 1.12.2 client actually controls.

## Notes
- GPLv3, source on the `mc1.12.2` branch.
- Port of Wurst7-CevAPI for legacy servers; use responsibly and follow server rules.
EOF

python3 - <<'PYEOF'
import json, os, urllib.request

token = None
for line in open(os.path.expanduser('~/.git-credentials')):
    if '@github.com' in line:
        token = line.split('//')[1].split('@')[0].split(':')[1]
        break

body = open('/tmp/relbody091.md').read()
data = json.dumps({
    'tag_name': 'v0.9.1',
    'target_commitish': 'mc1.12.2',
    'name': 'Wurst0 v0.9.1 - Grim bypass suite + Bedwars-safe aimbot',
    'body': body,
}).encode()

req = urllib.request.Request(
    'https://api.github.com/repos/apikun/Wurst0-CevAPI/releases',
    data=data, method='POST',
    headers={'Authorization': 'Bearer ' + token,
             'Content-Type': 'application/json'})
resp = json.load(urllib.request.urlopen(req))
rid = resp['id']
print('release id:', rid)

jar = open('build/libs/Wurst0-0.9.1.jar', 'rb').read()
req2 = urllib.request.Request(
    'https://uploads.github.com/repos/apikun/Wurst0-CevAPI/releases/'
    + str(rid) + '/assets?name=Wurst0-0.9.1.jar',
    data=jar, method='POST',
    headers={'Authorization': 'Bearer ' + token,
             'Content-Type': 'application/java-archive'})
resp2 = json.load(urllib.request.urlopen(req2))
print('asset:', resp2['name'], resp2['size'])
PYEOF
echo "DONE"
