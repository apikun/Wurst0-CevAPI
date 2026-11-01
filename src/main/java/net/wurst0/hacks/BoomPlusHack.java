/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BoomPlusHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: the "execute at @s run" wrapper was dropped;
 * a plain /summon already runs at your own position.
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

public final class BoomPlusHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting entity =
                new TextFieldSetting("Entity", "fireball");
        private final TextFieldSetting name =
                new TextFieldSetting("Custom name", "Cevapi");
        private final SliderSetting power =
                new SliderSetting("Explosion power", 10, 1, 127, 1);
        private final SliderSetting speed =
                new SliderSetting("Speed", 5, 0, 10, 1);
        private final CheckboxSetting automatic =
                new CheckboxSetting("Automatic", false);
        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 3, 3, 20, 1);

        private int ticks;

        public BoomPlusHack()
        {
                super("Boom+",
                        "Summons a configurable explosive entity above your"
                                + " head, optionally repeating. 1.12.2 syntax.");
                setCategory(Category.CREATIVE_OP);
                addSettings(entity, name, power, speed, automatic, delay);
        }

        @Override
        protected void onEnable()
        {
                ticks = delay.getValueI();
                summon();
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
                        summon();
                }
        }

        private void summon()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;

                String command = "summon " + entity.getValue() + " ~ ~2 ~"
                        + " {CustomName:\"" + nbtEscape(name.getValue())
                        + "\",ExplosionPower:" + power.getValueI()
                        + ",Motion:[0.0,-" + speed.getValueI() + ".0,0.0]}";

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
