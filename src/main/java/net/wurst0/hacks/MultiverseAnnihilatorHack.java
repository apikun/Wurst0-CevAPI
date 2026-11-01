/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MultiverseAnnihilatorHack (GPL-3.0-or-later)
 * Extension over the original: Multiverse asks for /mv confirm after
 * each dangerous /mv delete, so an optional confirm step is queued
 * right after every delete command.
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

public final class MultiverseAnnihilatorHack extends Hack
        implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting worlds = new TextFieldSetting("Worlds",
                "", 512);
        private final CheckboxSetting autoConfirm =
                new CheckboxSetting("Auto confirm", true);
        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 10, 0, 100, 1);

        private final Queue<String> queue = new ArrayDeque<String>();
        private int ticks;

        public MultiverseAnnihilatorHack()
        {
                super("MultiverseAnnihilator",
                        "Deletes the configured Multiverse worlds with"
                                + " /mv delete and confirms each one with"
                                + " /mv confirm. Needs Multiverse + OP.");
                setCategory(Category.CREATIVE_OP);
                addSettings(worlds, autoConfirm, delay);
        }

        @Override
        protected void onEnable()
        {
                queue.clear();
                queue.add("mv list");
                for(String world : worlds.getValue().split(";"))
                {
                        String name = world.trim();
                        if(name.isEmpty())
                                continue;
                        queue.add("mv delete " + name);
                        if(autoConfirm.isChecked())
                                queue.add("mv confirm");
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

                p.sendChatMessage("/" + queue.poll());
        }
}
