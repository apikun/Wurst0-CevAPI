/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BunnyHopHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class BunnyHopHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        public BunnyHopHack()
        {
                super("BunnyHop",
                        "Automatically jumps whenever you touch the ground, so you"
                                + " never lose speed while sprinting.");
                setCategory(Category.MOVEMENT);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;
                if(!p.onGround || p.isSneaking() || p.isInWater() || p.isOnLadder())
                        return;
                if(p.moveForward <= 0)
                        return;

                p.motionY = p.isSprinting() ? 0.42D : 0.3994D;
                if(p.isSprinting())
                {
                        float yawRad = (float)Math.toRadians(p.rotationYaw);
                        p.motionX -= Math.sin(yawRad) * 0.2F;
                        p.motionZ += Math.cos(yawRad) * 0.2F;
                }
        }
}
