/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoDisplaysHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: text_display/block_display entities (1.19.4+)
 * do not exist, so the floating texts are rendered as invisible
 * marker armor stands with CustomNameVisible riding a clearable
 * marker stand. Block displays cannot be replicated. The brightness
 * setting has no 1.12.2 equivalent and was dropped.
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

public final class AutoDisplaysHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private static final String MARKER = "CEVAPI_DISPLAY";

        private final TextFieldSetting text = new TextFieldSetting("Text",
                "Your server is being renovated!");
        private final SliderSetting distance =
                new SliderSetting("Distance", 3, 1, 16, 1);
        private final CheckboxSetting clearExisting =
                new CheckboxSetting("Clear existing", true);
        private final CheckboxSetting useDelay =
                new CheckboxSetting("Use command delay", false);
        private final SliderSetting delay =
                new SliderSetting("Command delay (ticks)", 2, 1, 20, 1);

        private final Queue<String> queue = new ArrayDeque<String>();
        private int ticks;

        public AutoDisplaysHack()
        {
                super("AutoDisplays",
                        "Summons floating text in front of and behind every"
                                + " player (armor-stand based 1.12.2 replacement"
                                + " for text displays).");
                setCategory(Category.CREATIVE_OP);
                addSettings(text, distance, clearExisting, useDelay, delay);
        }

        @Override
        protected void onEnable()
        {
                queue.clear();

                if(clearExisting.isChecked())
                {
                        // kill each marker stand and everything standing
                        // within 2 blocks of it (its text passenger)
                        queue.add("execute @e[type=armor_stand,name=" + MARKER
                                + "] ~ ~ ~ kill @e[type=armor_stand,r=2]");
                }

                String nbt = "{Invisible:1b,Marker:1b,NoGravity:1b"
                        + ",CustomName:\"" + MARKER + "\""
                        + ",Passengers:[{id:\"minecraft:armor_stand\""
                        + ",Invisible:1b,Marker:1b,NoGravity:1b"
                        + ",CustomNameVisible:1b,CustomName:\""
                        + nbtEscape(text.getValue()) + "\"}]}";
                queue.add("execute @a ~ ~ ~ summon armor_stand ~ ~1 ~-"
                        + distance.getValueI() + " " + nbt);
                queue.add("execute @a ~ ~ ~ summon armor_stand ~ ~1 ~"
                        + distance.getValueI() + " " + nbt);

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
                if(useDelay.isChecked() && ticks++ < delay.getValueI())
                        return;
                ticks = 0;

                String command = queue.poll();
                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(command.length() + 1 <= 256)
                        p.sendChatMessage("/" + command);
        }

        private static String nbtEscape(String s)
        {
                if(s == null)
                        return "";
                return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
}
