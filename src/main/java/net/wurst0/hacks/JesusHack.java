/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI JesusHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class JesusHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting noSwim =
                new CheckboxSetting("Stay on surface", true);

        public JesusHack()
        {
                super("Jesus",
                        "Lets you walk on water by keeping you at the surface while"
                                + " you move through it.");
                setCategory(Category.MOVEMENT);
                addSetting(noSwim);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;
                if(!p.isInWater() || p.isSneaking() || p.isRiding())
                        return;

                if(!noSwim.isChecked())
                        return;

                p.fallDistance = 0;

                if(mc.gameSettings.keyBindJump.isKeyDown())
                        p.motionY = 0.3D;
                else if(p.moveForward != 0 || p.moveStrafing != 0)
                        p.motionY = 0.1D;
                else
                        p.motionY = 0.05D;
        }
}
