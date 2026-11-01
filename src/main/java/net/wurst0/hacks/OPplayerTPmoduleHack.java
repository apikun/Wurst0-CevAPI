/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI OPplayerTPmoduleHack (GPL-3.0-or-later)
 * The "Ignore friends" no-op compatibility option was dropped (the
 * fork has no friends system).
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class OPplayerTPmoduleHack extends Hack
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting teleportPlayersToMe =
                new CheckboxSetting("TP players to you", false);
        private final CheckboxSetting disableIfNotOp =
                new CheckboxSetting("Disable if not OP", false);

        private int currentPlayer;

        public OPplayerTPmoduleHack()
        {
                super("OPplayerTPmodule",
                        "Teleports you to the next online player (or them to"
                                + " you); each activation moves one player"
                                + " further through the list. Needs OP.");
                setCategory(Category.CREATIVE_OP);
                addSettings(teleportPlayersToMe, disableIfNotOp);
        }

        @Override
        protected void onEnable()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                {
                        setEnabled(false);
                        return;
                }

                String self = p.getGameProfile().getName();
                Collection<NetworkPlayerInfo> infos =
                        mc.getConnection().getPlayerInfoMap();
                List<String> players = new ArrayList<String>();
                for(NetworkPlayerInfo info : infos)
                {
                        String name = info.getGameProfile().getName();
                        if(name.equals(self))
                                continue;
                        players.add(name);
                }
                if(players.isEmpty())
                {
                        setEnabled(false);
                        return;
                }

                if(currentPlayer >= players.size())
                        currentPlayer = 0;
                String target = players.get(currentPlayer++);

                String command;
                if(teleportPlayersToMe.isChecked())
                        command = "tp " + target + " " + self;
                else
                        command = "tp " + self + " " + target;

                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(command.length() + 1 <= 256)
                        p.sendChatMessage("/" + command);

                if(currentPlayer >= players.size())
                        currentPlayer = 0;

                if(disableIfNotOp.isChecked())
                        setEnabled(false);
        }
}
