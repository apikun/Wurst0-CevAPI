/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoTextsHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: text_display entities (1.19.4+) do not exist, so
 * the floating texts are rendered as invisible marker armor stands
 * with CustomNameVisible (legacy \u00a7 color codes).
 */
package net.wurst0.hacks;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

public final class AutoTextsHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private static final String[] COLOR_NAMES = {"black", "dark_blue",
                "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold",
                "gray", "blue", "green", "aqua", "red", "light_purple",
                "yellow", "white"};
        private static final String[] COLOR_CODES = {"\u00a70", "\u00a71",
                "\u00a72", "\u00a73", "\u00a74", "\u00a75", "\u00a76",
                "\u00a77", "\u00a78", "\u00a79", "\u00a7b", "\u00a7c",
                "\u00a7d", "\u00a7e", "\u00a7f"};

        private final TextFieldSetting texts = new TextFieldSetting("Texts",
                "Hello from Wurst0;1.12.2", 512);
        private final TextFieldSetting color =
                new TextFieldSetting("Color", "red");
        private final SliderSetting radius =
                new SliderSetting("Radius", 8, 1, 32, 1);
        private final SliderSetting height =
                new SliderSetting("Height", 0, -8, 32, 1);
        private final CheckboxSetting randomHeight =
                new CheckboxSetting("Height variation", false);
        private final SliderSetting delay =
                new SliderSetting("Spawn delay (ticks)", 3, 3, 20, 1);

        private final Queue<String> queue = new ArrayDeque<String>();
        private int ticks;
        private final Random random = new Random();

        public AutoTextsHack()
        {
                super("AutoTexts",
                        "Spawns floating text lines around you as invisible"
                                + " marker armor stands (1.12.2 replacement for"
                                + " text displays).");
                setCategory(Category.CREATIVE_OP);
                addSettings(texts, color, radius, height, randomHeight, delay);
        }

        @Override
        protected void onEnable()
        {
                queue.clear();
                String prefix = colorCode(color.getValue());
                int y = (int)height.getValue();

                String[] lines = texts.getValue().split(";");
                for(String line : lines)
                {
                        line = line.trim();
                        if(line.isEmpty())
                                continue;

                        int x = random.nextInt((int)radius.getValue() * 2 + 1)
                                - (int)radius.getValue();
                        int z = random.nextInt((int)radius.getValue() * 2 + 1)
                                - (int)radius.getValue();
                        String name = prefix
                                + line.replace("\"", "").replace("\\", "");
                        String command = "summon armor_stand ~" + x + " ~" + y
                                + " ~" + z
                                + " {Invisible:1b,Marker:1b,NoGravity:1b"
                                + ",CustomNameVisible:1b,CustomName:\""
                                + nbtEscape(name) + "\"}";

                        // 1.12.2 servers reject chat packets longer than 256 chars.
                        if(command.length() + 1 <= 256)
                                queue.add(command);
                        if(randomHeight.isChecked())
                                y++;
                }

                ticks = (int)delay.getValue();
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
                if(ticks++ < (int)delay.getValue())
                        return;
                ticks = 0;
                p.sendChatMessage("/" + queue.poll());
        }

        private static String colorCode(String name)
        {
                if(name == null)
                        return "";
                for(int i = 0; i < COLOR_NAMES.length; i++)
                        if(COLOR_NAMES[i].equalsIgnoreCase(name.trim()))
                                return COLOR_CODES[i];
                return "";
        }

        private static String nbtEscape(String s)
        {
                if(s == null)
                        return "";
                return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
}
