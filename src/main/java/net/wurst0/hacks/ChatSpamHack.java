/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ChatSpamHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

public final class ChatSpamHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting message =
                new TextFieldSetting("Message", "Wurst0 on top!");
        private final SliderSetting interval =
                new SliderSetting("Interval (seconds)", 5, 1, 60, 1);
        private final CheckboxSetting addCounter =
                new CheckboxSetting("Add counter", true);

        private int timer;
        private int counter;

        public ChatSpamHack()
        {
                super("ChatSpam",
                        "Spams a chat message at a fixed interval. Set the"
                                + " text in the Message field (click it, type,"
                                + " Enter).");
                setCategory(Category.CHAT);
                addSettings(message, interval, addCounter);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;

                if(--timer > 0)
                        return;
                timer = interval.getValueI() * 20;

                String text = message.getValue();
                if(text.isEmpty())
                        return;
                if(addCounter.isChecked())
                        text = text + " (" + (++counter) + ")";

                p.sendChatMessage(text);
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
                counter = 0;
        }

        @Override
        protected void onDisable()
        {
                counter = 0;
        }
}
