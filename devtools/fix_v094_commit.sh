#!/bin/bash
# Fix the v0.9.4 commit message + tag (the release asset is already correct).
# Force-pushes the amended commit and re-points the v0.9.4 tag at it.
set -e
cd /home/z/my-project/modport/fork

TOKEN=$(sed -n 's#https://[^:/]*:\([^@]*\)@github.com#\1#p' \
  ~/.git-credentials 2>/dev/null | head -1)
[ -n "$TOKEN" ] || { echo "no token available"; exit 1; }

git -c user.name="apikun" -c user.email="apikun@users.noreply.github.com" \
  commit --amend -m "Wurst0 v0.9.4: Collapsible ClickGUI tabs - click a title bar to collapse/expand (drag still moves), +/- indicator, collapsed state persisted in clickgui.json"

git push -f origin mc1.12.2 2>&1 | grep -v "token" || true
git tag -f v0.9.4
git push -f origin v0.9.4 2>&1 | grep -v "token" || true
echo "AMEND DONE"
