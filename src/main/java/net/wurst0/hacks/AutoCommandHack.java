/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoCommandHack (GPL-3.0-or-later)
 * Lite port: the CevAPI macro name and local permission-level hint
 * settings were dropped (they have no client-side effect on 1.12.2).
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
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

public final class AutoCommandHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting commands = new TextFieldSetting(
                "Commands",
                "deop @a[name=!{player}];whitelist off;pardon {player};op {player}",
                512);
        private final ModeSetting mode =
                new ModeSetting("Mode", new String[]{"Manual", "Loop"}, 0);
        private final SliderSetting delay =
                new SliderSetting("Command delay (ticks)", 3, 3, 100, 1);
        private final CheckboxSetting disableOnFinish =
                new CheckboxSetting("Disable on finish", false);

        private final Queue<String> queue = new ArrayDeque<String>();
        private int ticks;
        private boolean finished;

        public AutoCommandHack()
        {
                super("AutoCommand",
                        "Runs your configured commands one by one with a delay,"
                                + " either once (Manual) or forever (Loop)."
                                + " {player} becomes your name.");
                setCategory(Category.CREATIVE_OP);
                addSettings(commands, mode, delay, disableOnFinish);
        }

        @Override
        protected void onEnable()
        {
                queue.clear();
                finished = false;
                ticks = 0;
                fillQueue();
        }

        @Override
        protected void onDisable()
        {
                queue.clear();
                finished = false;
                ticks = 0;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;

                if(queue.isEmpty())
                {
                        if(finished && !mode.getSelected().equalsIgnoreCase("Loop"))
                        {
                                if(disableOnFinish.isChecked())
                                        setEnabled(false);
                                return;
                        }
                        if(finished)
                                fillQueue();
                }
                if(queue.isEmpty())
                        return;

                if(ticks++ < delay.getValueI())
                        return;
                ticks = 0;

                String command = queue.poll();
                command = command.replace("{player}",
                        p.getGameProfile().getName());
                while(command.startsWith("/"))
                        command = command.substring(1).trim();
                if(command.trim().isEmpty())
                        return;

                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(command.length() + 1 > 256)
                {
                        queue.clear();
                        finished = true;
                        return;
                }
                p.sendChatMessage("/" + command);

                if(queue.isEmpty())
                        finished = true;
        }

        private void fillQueue()
        {
                queue.clear();
                String[] parts = commands.getValue().split(";");
                for(String part : parts)
                {
                        String trimmed = part.trim();
                        if(!trimmed.isEmpty())
                                queue.add(trimmed);
                }
                finished = false;
                ticks = delay.getValueI();
        }
}
