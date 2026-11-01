#!/bin/bash
# Commit v0.9.4, push mc1.12.2, tag, and create the GitHub release with the jar.
# The token is read from ~/.git-credentials and is never printed.
set -e
cd /home/z/my-project/modport/fork

TOKEN=$(sed -n 's#https://[^:/]*:\([^@]*\)@github.com#\1#p' \
  ~/.git-credentials 2>/dev/null | head -1)
[ -n "$TOKEN" ] || { echo "no token available"; exit 1; }

git add -A
git -c user.name="apikun" -c user.email="apikun@users.noreply.github.com" \
  commit -m "Wurst0 v0.9.4: Collapsible ClickGUI tabs - click a title bar to collapse/expand (drag still moves), +/- indicator, collapsed state persisted in clickgui.json"

git push origin mc1.12.2 2>&1 | grep -v "token" || true

git tag -f v0.9.4
git push origin v0.9.4 2>&1 | grep -v "token" || true

cat > /tmp/relbody094.md <<'EOF'
Eleventh release of the **Wurst0** 1.12.2 port (Wurst7-CevAPI -> Forge 1.12.2, Java 8).

## Install
1. Install Minecraft 1.12.2 with **Forge 1.12.2-14.23.5.2859** (or newer 14.23.5.x).
2. Drop `Wurst0-0.9.4.jar` into your `.minecraft/mods` folder.
3. In game: **Right Shift** opens the ClickGUI, the **Keybinds** button (top-left) or `.binds gui` opens the keybind editor, `.help` lists all commands.

## What's new in v0.9.4 - Collapsible tabs

You can now **collapse any category window down to just its title bar**:

- **Click the title bar** of a tab (Combat, Render, Movement, ...) to collapse it - click again to expand it.
- **Dragging** the title bar still moves the window (a clean click vs a 4px+ drag are told apart, same as Wurst's real ClickGUI).
- A **`+` / `-` indicator** next to the `[count]` badge shows whether the tab is collapsed.
- Collapsed tabs are **saved** in `config/wurst0/clickgui.json` and restored on restart, together with window positions and expanded sliders.

## Previous releases
- v0.9.3: ClickGUI layout fully stable between sessions (clicks no longer reshuffle panels, all positions persisted)
- v0.9.2: Keybinds GUI editor (Keybinds button / `.binds gui`)
- v0.9.1: Grim bypass suite + Bedwars-safe Killaura (team check, smooth aim)
- v0.9.0: +72 hacks (305/332 functional), dark/crimson ClickGUI theme

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

body = open('/tmp/relbody094.md').read()
data = json.dumps({
    'tag_name': 'v0.9.4',
    'target_commitish': 'mc1.12.2',
    'name': 'Wurst0 v0.9.4 - Collapsible tabs',
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

jar = open('build/libs/Wurst0-0.9.4.jar', 'rb').read()
req2 = urllib.request.Request(
    'https://uploads.github.com/repos/apikun/Wurst0-CevAPI/releases/'
    + str(rid) + '/assets?name=Wurst0-0.9.4.jar',
    data=jar, method='POST',
    headers={'Authorization': 'Bearer ' + token,
             'Content-Type': 'application/java-archive'})
resp2 = json.load(urllib.request.urlopen(req2))
print('asset:', resp2['name'], resp2['size'])
PYEOF
echo "DONE"
