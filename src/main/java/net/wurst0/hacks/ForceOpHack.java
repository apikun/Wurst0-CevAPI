/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ForceOpHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Tries every password in the list against the server, one attempt
 * per interval, cycling through Login command / Register command /
 * plain chat. Lite port: no chat-prompt detection, fixed interval.
 */
public final class ForceOpHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting passwords =
                new TextFieldSetting("Passwords",
                        "comma,separated,example123", 512);
        private final ModeSetting mode = new ModeSetting("Mode",
                new String[]{"Login", "Register", "Say"}, 0);
        private final SliderSetting interval =
                new SliderSetting("Interval (s)", 5, 1, 60, 1);
        private final CheckboxSetting stopAfterList =
                new CheckboxSetting("Stop after list", true);

        private int timer;
        private int index;
        private boolean done;

        public ForceOpHack()
        {
                super("ForceOP",
                        "Tries all your passwords on a server until one of them"
                                + " works.");
                setCategory(Category.TOOLS);
                addSettings(passwords, mode, interval, stopAfterList);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || done)
                        return;

                if(timer > 0)
                {
                        timer--;
                        return;
                }

                String[] list = passwords.getValue().split(",");
                if(list.length == 0 || passwords.getValue().trim().isEmpty())
                {
                        done = true;
                        return;
                }

                if(index >= list.length)
                {
                        if(stopAfterList.isChecked())
                        {
                                done = true;
                                p.sendMessage(new TextComponentString(
                                        "\u00a7c[ForceOP]\u00a7r Password list exhausted."));
                                setEnabled(false);
                                done = false;
                                return;
                        }
                        index = 0;
                }

                String pw = list[index].trim();
                index++;
                if(pw.isEmpty())
                        return;

                if(mode.getSelected().equals("Login"))
                        p.sendChatMessage("/login " + pw);
                else if(mode.getSelected().equals("Register"))
                        p.sendChatMessage("/register " + pw + " " + pw);
                else
                        p.sendChatMessage(pw);

                timer = (int)Math.round(interval.getValue() * 20.0);
        }

        @Override
        protected void onEnable()
        {
                index = 0;
                done = false;
                timer = 0;
        }

        @Override
        protected void onDisable()
        {
                index = 0;
                done = false;
                timer = 0;
        }
}
