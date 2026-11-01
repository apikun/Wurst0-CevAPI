/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoNamesHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: "team modify" is 1.13+ syntax, so the classic
 * /scoreboard teams option color/prefix/suffix commands are used.
 * 1.12.2 limits team prefixes/suffixes to 16 characters (longer
 * input is truncated).
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

public final class AutoNamesHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private static final String TEAM = "cevapi_names";
        private static final int MAX_AFFIX = 16;

        private static final String[] COLORS = {"red", "gold", "yellow",
                "green", "aqua", "blue", "light_purple", "white"};

        private final TextFieldSetting prefix =
                new TextFieldSetting("Prefix", "Who is");
        private final TextFieldSetting suffix =
                new TextFieldSetting("Suffix", "?");
        private final TextFieldSetting color =
                new TextFieldSetting("Color", "red");
        private final CheckboxSetting targetSelf =
                new CheckboxSetting("Target self", true);
        private final CheckboxSetting rainbow =
                new CheckboxSetting("Rainbow", false);
        private final SliderSetting rainbowDelay =
                new SliderSetting("Rainbow delay (ticks)", 20, 1, 100, 1);
        private final SliderSetting commandDelay =
                new SliderSetting("Command delay (ticks)", 3, 3, 20, 1);

        private final Queue<String> queue = new ArrayDeque<String>();
        private int ticks;
        private int colorIndex;
        private int commandTicks;

        public AutoNamesHack()
        {
                super("AutoNames",
                        "Gives every player a team color, prefix and suffix"
                                + " via /scoreboard teams, optionally cycling"
                                + " colors like a rainbow.");
                setCategory(Category.CREATIVE_OP);
                addSettings(prefix, suffix, color, targetSelf, rainbow,
                        rainbowDelay, commandDelay);
        }

        @Override
        protected void onEnable()
        {
                ticks = rainbowDelay.getValueI();
                colorIndex = 0;
                commandTicks = commandDelay.getValueI();
                queue.clear();
                enqueue("scoreboard teams add " + TEAM);
                apply();
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

                if(!queue.isEmpty())
                {
                        if(commandTicks++ >= commandDelay.getValueI())
                        {
                                commandTicks = 0;
                                p.sendChatMessage("/" + queue.poll());
                        }
                        return;
                }

                if(rainbow.isChecked() && ++ticks >= rainbowDelay.getValueI())
                {
                        ticks = 0;
                        colorIndex = (colorIndex + 1) % COLORS.length;
                        apply();
                }
        }

        private void apply()
        {
                String c = rainbow.isChecked() ? COLORS[colorIndex]
                        : color.getValue().trim();
                enqueue("scoreboard teams option " + TEAM + " color " + c);
                enqueue("scoreboard teams option " + TEAM + " prefix "
                        + truncate(prefix.getValue()));
                enqueue("scoreboard teams option " + TEAM + " suffix "
                        + truncate(suffix.getValue()));
                if(targetSelf.isChecked())
                {
                        enqueue("scoreboard teams join " + TEAM + " @a");
                }else
                {
                        EntityPlayerSP p = mc.player;
                        String self =
                                p == null ? "" : p.getGameProfile().getName();
                        if(self.isEmpty())
                                enqueue("scoreboard teams join " + TEAM + " @a");
                        else
                                enqueue("scoreboard teams join " + TEAM
                                        + " @a[name=!" + self + "]");
                }
        }

        private void enqueue(String command)
        {
                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(command.length() + 1 <= 256)
                        queue.add(command);
        }

        private static String truncate(String s)
        {
                if(s == null)
                        return "";
                return s.length() > MAX_AFFIX ? s.substring(0, MAX_AFFIX) : s;
        }
}
