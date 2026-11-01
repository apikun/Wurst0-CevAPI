/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoTitlesHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: /title, /title ... subtitle and /title ...
 * actionbar all exist in 1.12.2; the JSON text components are built
 * with the old {"text":...,"color":...} syntax. The "Exclude friends"
 * no-op compatibility option was dropped.
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

public final class AutoTitlesHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting excludeSelf =
                new CheckboxSetting("Exclude self", true);
        private final CheckboxSetting useDelay =
                new CheckboxSetting("Use command delay", false);
        private final SliderSetting delay =
                new SliderSetting("Command delay (ticks)", 2, 1, 20, 1);
        private final CheckboxSetting makeSubtitle =
                new CheckboxSetting("Make subtitle", true);
        private final CheckboxSetting makeActionbar =
                new CheckboxSetting("Make actionbar", true);
        private final TextFieldSetting title =
                new TextFieldSetting("Title", "Wurst0");
        private final TextFieldSetting subtitle =
                new TextFieldSetting("Subtitle", "1.12.2");
        private final TextFieldSetting actionbar =
                new TextFieldSetting("Actionbar", "Renovations in progress.");
        private final TextFieldSetting titleColor =
                new TextFieldSetting("Title color", "white");
        private final TextFieldSetting subtitleColor =
                new TextFieldSetting("Subtitle color", "green");
        private final TextFieldSetting actionbarColor =
                new TextFieldSetting("Actionbar color", "yellow");

        private final Queue<String> queue = new ArrayDeque<String>();
        private int ticks;

        public AutoTitlesHack()
        {
                super("AutoTitles",
                        "Sends /title, subtitle and actionbar commands to all"
                                + " players (or everyone but you).");
                setCategory(Category.CREATIVE_OP);
                addSettings(excludeSelf, useDelay, delay, makeSubtitle,
                        makeActionbar, title, subtitle, actionbar, titleColor,
                        subtitleColor, actionbarColor);
        }

        @Override
        protected void onEnable()
        {
                queue.clear();

                EntityPlayerSP p = mc.player;
                String target = "@a";
                if(excludeSelf.isChecked() && p != null)
                        target = "@a[name=!" + p.getGameProfile().getName() + "]";

                queue.add("title " + target + " times 10 70 20");
                queue.add("title " + target + " title "
                        + jsonText(title.getValue(), titleColor.getValue()));
                if(makeSubtitle.isChecked())
                        queue.add("title " + target + " subtitle "
                                + jsonText(subtitle.getValue(),
                                        subtitleColor.getValue()));
                if(makeActionbar.isChecked())
                        queue.add("title " + target + " actionbar "
                                + jsonText(actionbar.getValue(),
                                        actionbarColor.getValue()));

                ticks = useDelay.isChecked() ? 0 : delay.getValueI();
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
                if(useDelay.isChecked() && ticks++ < delay.getValueI())
                        return;
                ticks = 0;

                String command = queue.poll();
                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(command.length() + 1 <= 256)
                        p.sendChatMessage("/" + command);
        }

        private static String jsonText(String text, String color)
        {
                return "{\"text\":\"" + jsonEscape(text) + "\",\"color\":\""
                        + color.trim() + "\"}";
        }

        private static String jsonEscape(String s)
        {
                if(s == null)
                        return "";
                return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
}
