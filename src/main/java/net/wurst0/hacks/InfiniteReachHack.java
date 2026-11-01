/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI InfiniteReachHack (GPL-3.0-or-later)
 * Lite adaptation: attacks entities beyond normal reach by
 * teleporting next to them, hitting, and teleporting back.
 * Strict servers will rubber-band the position packets.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class InfiniteReachHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 32, 8, 96, 4);
        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 10, 2, 40, 1);
        private final CheckboxSetting requireLook =
                new CheckboxSetting("Require looking at", false);

        private int timer;

        public InfiniteReachHack()
        {
                super("InfiniteReach",
                        "Attacks entities way beyond normal reach by"
                                + " teleporting to them and back.");
                setCategory(Category.COMBAT);
                addSettings(range, delay, requireLook);
        }

        private EntityLivingBase findTarget(EntityPlayerSP p)
        {
                double bestSq = range.getValue() * range.getValue();
                EntityLivingBase best = null;

                for(Entity e : mc.world.loadedEntityList)
                {
                        if(!(e instanceof EntityLivingBase))
                                continue;
                        if(e == p || e.isDead)
                                continue;
                        EntityLivingBase living = (EntityLivingBase)e;
                        if(living.getHealth() <= 0)
                                continue;
                        if(living.getDistanceSq(p) > bestSq)
                                continue;
                        if(living.getDistanceSq(p) < 25)
                                continue; // normal reach handles these

                        if(requireLook.isChecked())
                        {
                                if(mc.objectMouseOver == null
                                        || mc.objectMouseOver.entityHit
                                                != living)
                                        continue;
                        }

                        bestSq = living.getDistanceSq(p);
                        best = living;
                }
                return best;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(--timer > 0)
                        return;

                EntityLivingBase target = findTarget(p);
                if(target == null)
                        return;

                timer = delay.getValueI();

                double ox = p.posX;
                double oy = p.posY;
                double oz = p.posZ;

                double dx = target.posX - ox;
                double dz = target.posZ - oz;
                double dist = MathHelper.sqrt(dx * dx + dz * dz);
                if(dist < 0.01)
                        return;

                double nx = target.posX - dx / dist * 2.5;
                double nz = target.posZ - dz / dist * 2.5;

                // face the target, jump in, hit, jump back
                float yaw = (float)(MathHelper.atan2(dz, dx)
                        * 180.0 / Math.PI) - 90F;
                p.rotationYaw = yaw;
                p.setPositionAndUpdate(nx, oy, nz);
                mc.playerController.attackEntity(p, target);
                p.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
                p.setPositionAndUpdate(ox, oy, oz);
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
        }
}
