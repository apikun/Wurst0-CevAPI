#!/bin/bash
# Commit v0.9.2, push mc1.12.2, tag, and create the GitHub release with the jar.
# The token is read from ~/.git-credentials and is never printed.
set -e
cd /home/z/my-project/modport/fork

TOKEN=$(sed -n 's#https://[^:/]*:\([^@]*\)@github.com#\1#p' \
  ~/.git-credentials 2>/dev/null | head -1)
[ -n "$TOKEN" ] || { echo "no token available"; exit 1; }

git add -A
git -c user.name="apikun" -c user.email="apikun@users.noreply.github.com" \
  commit -m "Wurst0 v0.9.2: KeybindsScreen GUI editor (ClickGUI button + .binds gui), ClickGUI layout persistence (clickgui.json) - window positions survive restarts"

git push origin mc1.12.2 2>&1 | grep -v "token" || true

git tag -f v0.9.2
git push origin v0.9.2 2>&1 | grep -v "token" || true

cat > /tmp/relbody092.md <<'EOF'
Tenth release of the **Wurst0** 1.12.2 port (Wurst7-CevAPI -> Forge 1.12.2, Java 8).

## Install
1. Install Minecraft 1.12.2 with **Forge 1.12.2-14.23.5.2859** (or newer 14.23.5.x).
2. Drop `Wurst0-0.9.2.jar` into your `.minecraft/mods` folder.
3. In game: **Right Shift** opens the ClickGUI, `.help` lists all commands.

## What's new in v0.9.2

### Keybind settings - now in the GUI
- New **KeybindsScreen**: every hack listed with its current bind. Click a
  row, press the key you want (Escape clears). Saves instantly to
  `config/wurst0/keybinds.json`.
- Open it from the **"Keybinds" button** in the ClickGUI's top-left corner,
  or type **`.binds gui`** in chat.
- Chat commands still work: `.bind <hack> <key>`, `.binds` (list).

### GUI layout persistence
- Dragged ClickGUI windows are now saved to
  `config/wurst0/clickgui.json` and restored on every open **and restart**.
- No more windows re-arranging themselves - the auto column layout only
  touches windows you never moved.

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

body = open('/tmp/relbody092.md').read()
data = json.dumps({
    'tag_name': 'v0.9.2',
    'target_commitish': 'mc1.12.2',
    'name': 'Wurst0 v0.9.2 - Keybinds GUI + persistent ClickGUI layout',
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

jar = open('build/libs/Wurst0-0.9.2.jar', 'rb').read()
req2 = urllib.request.Request(
    'https://uploads.github.com/repos/apikun/Wurst0-CevAPI/releases/'
    + str(rid) + '/assets?name=Wurst0-0.9.2.jar',
    data=jar, method='POST',
    headers={'Authorization': 'Bearer ' + token,
             'Content-Type': 'application/java-archive'})
resp2 = json.load(urllib.request.urlopen(req2))
print('asset:', resp2['name'], resp2['size'])
PYEOF
echo "DONE"
