/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MentionHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.init.SoundEvents;
import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Highlights chat lines that mention your name and plays a sound.
 */
public final class MentionHack extends Hack implements ChatInputListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting sound =
                new CheckboxSetting("Sound", true);

        public MentionHack()
        {
                super("Mention",
                        "Highlights chat messages that mention your name.");
                setCategory(Category.CHAT);
                addSetting(sound);
        }

        @Override
        public void onReceivedMessage(ChatInputEvent event)
        {
                if(mc.player == null)
                        return;

                String name = mc.player.getName();
                if(name == null || name.isEmpty())
                        return;

                String text = event.getText();
                if(text == null || text.length() < name.length())
                        return;

                if(!text.toLowerCase().contains(name.toLowerCase()))
                        return;
                if(text.startsWith("\u00a76"))
                        return; // already highlighted by us

                event.setText("\u00a76" + text);

                if(sound.isChecked())
                        mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F);
        }
}
