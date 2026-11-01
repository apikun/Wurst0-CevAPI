/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AirstrikePlusHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: CustomName is a plain string here and the 1.13+
 * "execute at @s run" wrapper became a plain summon (own position) or
 * "execute @a ~ ~ ~ summon" for every player.
 */
package net.wurst0.hacks;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

public final class AirstrikePlusHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting entity =
                new TextFieldSetting("Entity", "fireball");
        private final TextFieldSetting customName =
                new TextFieldSetting("Custom name", "Cevapi");
        private final SliderSetting minRange =
                new SliderSetting("Minimum range", 0, 0, 100, 1);
        private final SliderSetting maxRange =
                new SliderSetting("Maximum range", 30, 1, 100, 1);
        private final SliderSetting height =
                new SliderSetting("Height above head", 20, -63, 255, 1);
        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 3, 3, 20, 1);
        private final SliderSetting concurrent =
                new SliderSetting("Concurrent spawns", 1, 1, 100, 1);
        private final SliderSetting power =
                new SliderSetting("Explosion power", 10, 1, 127, 1);
        private final CheckboxSetting everyone =
                new CheckboxSetting("Airstrike everyone", false);
        private final CheckboxSetting automatic =
                new CheckboxSetting("Automatic", true);

        private int ticks;
        private final Random random = new Random();

        public AirstrikePlusHack()
        {
                super("Airstrike+",
                        "Summons configurable explosive entities above you or"
                                + " above every player, using 1.12.2 /summon syntax.");
                setCategory(Category.CREATIVE_OP);
                addSettings(entity, customName, minRange, maxRange, height,
                        delay, concurrent, power, everyone, automatic);
        }

        @Override
        protected void onEnable()
        {
                ticks = delay.getValueI();
                strike();
        }

        @Override
        protected void onDisable()
        {
                ticks = 0;
        }

        @Override
        public void onUpdate()
        {
                if(!automatic.isChecked())
                        return;
                if(ticks++ >= delay.getValueI())
                {
                        ticks = 0;
                        strike();
                }
        }

        private void strike()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;

                for(int i = 0; i < concurrent.getValueI(); i++)
                {
                        int range = minRange.getValueI();
                        if(maxRange.getValueI() > range)
                                range += random.nextInt(maxRange.getValueI() - range + 1);
                        int x = random.nextInt(range * 2 + 1) - range;
                        int z = random.nextInt(range * 2 + 1) - range;
                        String nbt = "{CustomName:\""
                                + nbtEscape(customName.getValue())
                                + "\",ExplosionPower:" + power.getValueI() + "}";
                        String command;
                        if(everyone.isChecked())
                                command = "execute @a ~ ~ ~ summon "
                                        + entity.getValue() + " ~" + x + " ~"
                                        + height.getValueI() + " ~" + z + " " + nbt;
                        else
                                command = "summon " + entity.getValue() + " ~" + x
                                        + " ~" + height.getValueI() + " ~" + z + " "
                                        + nbt;
                        sendCommand(p, command);
                }
        }

        private void sendCommand(EntityPlayerSP p, String command)
        {
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
