/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ExplosionAuraHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: "execute at @s run summon" became a plain
 * /summon at your own position.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class ExplosionAuraHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting delay =
                new SliderSetting("Explosion delay (ticks)", 5, 0, 100, 1);
        private final SliderSetting power =
                new SliderSetting("Power", 10, 1, 127, 1);
        private final CheckboxSetting attackClick =
                new CheckboxSetting("Attack click", false);
        private final CheckboxSetting repeat =
                new CheckboxSetting("Automatic", false);

        private int ticks;

        public ExplosionAuraHack()
        {
                super("ExplosionAura",
                        "Spawns primed powered creepers with Fuse:0 around you,"
                                + " either continuously or while attacking.");
                setCategory(Category.CREATIVE_OP);
                addSettings(delay, power, attackClick, repeat);
        }

        @Override
        protected void onEnable()
        {
                ticks = delay.getValueI();
        }

        @Override
        protected void onDisable()
        {
                ticks = 0;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;

                boolean attacking = mc.gameSettings.keyBindAttack.isKeyDown();
                if(attackClick.isChecked() && !attacking)
                        return;

                if(ticks++ < delay.getValueI())
                        return;
                ticks = 0;

                String command = "summon creeper ~ ~ ~ {powered:1b"
                        + ",ExplosionRadius:" + power.getValueI() + ",Fuse:0}";
                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(command.length() + 1 <= 256)
                        p.sendChatMessage("/" + command);
        }
}
