/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI UUIDBanHack (GPL-3.0-or-later)
 * The "Ignore friends" no-op compatibility option was dropped. Also
 * fixed the original's queueing bug: a single target player is now
 * banned when "All players" is off (the original's else-if nested
 * inside the all-players loop never ran it).
 */
package net.wurst0.hacks;

import java.util.ArrayDeque;
import java.util.Queue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

public final class UUIDBanHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting player =
                new TextFieldSetting("Player", "");
        private final TextFieldSetting command = new TextFieldSetting(
                "Ban command", "ban {player}", 256);
        private final CheckboxSetting allPlayers =
                new CheckboxSetting("All players", false);
        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 20, 0, 100, 1);

        private final Queue<String> queue = new ArrayDeque<String>();
        private int ticks;

        public UUIDBanHack()
        {
                super("UUIDBan",
                        "Queues ban commands for one player or every online"
                                + " player except you ({player} in the command"
                                + " template is replaced). Needs OP.");
                setCategory(Category.CREATIVE_OP);
                addSettings(player, command, allPlayers, delay);
        }

        @Override
        protected void onEnable()
        {
                queue.clear();

                EntityPlayerSP p = mc.player;
                if(mc.getConnection() != null && allPlayers.isChecked())
                {
                        String self =
                                p == null ? "" : p.getGameProfile().getName();
                        for(NetworkPlayerInfo info : mc.getConnection()
                                .getPlayerInfoMap())
                        {
                                String name =
                                        info.getGameProfile().getName();
                                if(name.equals(self))
                                        continue;
                                queue.add(command.getValue()
                                        .replace("{player}", name));
                        }
                }else if(!player.getValue().trim().isEmpty())
                {
                        queue.add(command.getValue().replace("{player}",
                                player.getValue().trim()));
                }

                ticks = delay.getValueI();
        }

        @Override
        protected void onDisable()
        {
                queue.clear();
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;
                if(queue.isEmpty())
                {
                        setEnabled(false);
                        return;
                }
                if(ticks++ < delay.getValueI())
                        return;
                ticks = 0;

                String value = queue.poll();
                while(value.startsWith("/"))
                        value = value.substring(1);
                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(value.length() + 1 <= 256)
                        p.sendChatMessage("/" + value);
        }
}
