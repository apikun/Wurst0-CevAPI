/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiWaterPushHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Approximate port: water flow pushes the player by directly editing
 * motion each tick, so we zero the horizontal motion while standing
 * still in water. When moving, the "keep out of depth" checkbox keeps
 * the player at the surface by damping the downward motion.
 */
public final class AntiWaterPushHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting keepOuttaWater =
                new CheckboxSetting("Keep out of depth", false);

        public AntiWaterPushHack()
        {
                super("AntiWaterPush",
                        "Prevents you from being pushed around by flowing water.");
                setCategory(Category.MOVEMENT);
                addSetting(keepOuttaWater);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || !p.isInWater())
                        return;

                boolean moving = p.movementInput.moveForward != 0
                        || p.movementInput.moveStrafe != 0;
                if(!moving)
                {
                        p.motionX = 0;
                        p.motionZ = 0;
                }

                if(keepOuttaWater.isChecked() && p.motionY < 0)
                        p.motionY = Math.max(p.motionY, -0.08);
        }
}
