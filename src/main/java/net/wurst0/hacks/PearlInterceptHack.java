/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PearlInterceptHack (GPL-3.0-or-later)
 * Lite adaptation: watches foreign ender pearls, predicts their
 * landing spot with 1.12.2 throwable physics (drag 0.99, gravity
 * 0.03/tick, launch speed 1.5) and throws one of your own pearls
 * onto that spot first.
 */
package net.wurst0.hacks;

import java.util.HashSet;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.ReflectCalls;

/**
 * When another player's ender pearl is in flight, predicts where it
 * will land and throws your own pearl onto the same spot, so you
 * arrive before (or instead of) them.
 */
public final class PearlInterceptHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting maxHorizontalError =
                new SliderSetting("Max horizontal error", 2, 1, 32, 0.5);
        private final SliderSetting cooldownTicks =
                new SliderSetting("Cooldown (ticks)", 10, 0, 60, 1);
        private final CheckboxSetting switchBack =
                new CheckboxSetting("Switch back", true);

        private final HashSet<UUID> handledPearls =
                new HashSet<UUID>();
        private int cooldown;
        private int restoreSlot = -1;
        private int restoreTimer;

        public PearlInterceptHack()
        {
                super("PearlIntercept",
                        "Watches incoming ender pearls and throws one of"
                                + " your own onto the predicted landing"
                                + " spot to intercept them.");
                setCategory(Category.COMBAT);
                addSettings(maxHorizontalError, cooldownTicks, switchBack);
        }

        @Override
        protected void onEnable()
        {
                handledPearls.clear();
                cooldown = 0;
                restoreSlot = -1;
                restoreTimer = 0;
        }

        @Override
        protected void onDisable()
        {
                handledPearls.clear();
                cooldown = 0;
                if(restoreSlot >= 0 && mc.player != null)
                {
                        mc.player.inventory.currentItem = restoreSlot;
                        if(mc.playerController != null)
                                mc.playerController.updateController();
                }
                restoreSlot = -1;
                restoreTimer = 0;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null
                        || mc.playerController == null)
                        return;

                if(restoreTimer > 0 && --restoreTimer == 0
                        && restoreSlot >= 0)
                {
                        p.inventory.currentItem = restoreSlot;
                        mc.playerController.updateController();
                        restoreSlot = -1;
                }

                if(cooldown > 0)
                        cooldown--;

                if(cooldown > 0 || p.openContainer != p.inventoryContainer)
                        return;
                if(findPearl(p) == -1)
                        return;

                if(handledPearls.size() > 256)
                        handledPearls.clear();

                for(Entity e : mc.world.loadedEntityList)
                {
                        if(!(e instanceof EntityEnderPearl))
                                continue;
                        EntityEnderPearl pearl = (EntityEnderPearl)e;
                        if(!pearl.isEntityAlive())
                                continue;

                        UUID pearlId = pearl.getUniqueID();
                        if(handledPearls.contains(pearlId))
                                continue;
                        handledPearls.add(pearlId);

                        EntityLivingBase thrower = pearl.getThrower();
                        if(thrower == p)
                                continue;

                        // fresh pearls report unstable/zero velocity;
                        // wait for trajectory data like the original
                        if(pearl.ticksExisted < 2)
                                continue;
                        if(pearl.motionX * pearl.motionX
                                + pearl.motionY * pearl.motionY
                                + pearl.motionZ * pearl.motionZ < 1.0E-4)
                                continue;

                        Vec3d landing = predictLanding(pearl);
                        if(landing == null)
                                continue;

                        if(tryThrow(p, landing))
                                return;
                }
        }

        /**
         * Simulates the rest of the pearl's flight using the vanilla
         * EntityThrowable tick order: move first, then apply drag
         * (0.99) and gravity (0.03/tick).
         */
        private Vec3d predictLanding(EntityEnderPearl pearl)
        {
                double x = pearl.posX;
                double y = pearl.posY;
                double z = pearl.posZ;
                double mx = pearl.motionX;
                double my = pearl.motionY;
                double mz = pearl.motionZ;

                for(int i = 0; i < 300; i++)
                {
                        x += mx;
                        y += my;
                        z += mz;

                        if(mc.world.getBlockState(
                                new BlockPos(x, y, z)).getMaterial()
                                        .blocksMovement())
                                return new Vec3d(x, y, z);

                        mx *= 0.99;
                        mz *= 0.99;
                        my = my * 0.99 - 0.03;

                        if(y < 0)
                                return null;
                }
                return null;
        }

        /**
         * Aims a pitch whose simulated landing point is within the
         * max horizontal error of the target, then throws.
         */
        private boolean tryThrow(EntityPlayerSP p, Vec3d target)
        {
                double dx = target.x - p.posX;
                double dz = target.z - p.posZ;
                double horizontal = Math.sqrt(dx * dx + dz * dz);
                if(horizontal < 0.5)
                        return false;

                float yaw = (float)(MathHelper.atan2(dz, dx)
                        * 180.0 / Math.PI) - 90F;
                double eyeY = p.posY + p.getEyeHeight();

                double speed = 1.5;
                float radToYaw = yaw * 0.017453292F;
                double dirX = -MathHelper.sin(radToYaw);
                double dirZ = MathHelper.cos(radToYaw);

                float bestPitch = Float.NaN;
                double bestError = Double.MAX_VALUE;
                for(float pitch = 80F; pitch >= -80F; pitch -= 1F)
                {
                        float rad = pitch * 0.017453292F;
                        double vx = dirX
                                * MathHelper.cos(rad) * speed;
                        double vz = dirZ
                                * MathHelper.cos(rad) * speed;
                        double vy = -MathHelper.sin(rad) * speed;

                        Vec3d landing = simulate(p.posX, eyeY,
                                p.posZ, vx, vy, vz, target.y);
                        if(landing == null)
                                continue;

                        double ex = landing.x - target.x;
                        double ez = landing.z - target.z;
                        double error = Math.sqrt(ex * ex + ez * ez);
                        if(error < bestError)
                        {
                                bestError = error;
                                bestPitch = pitch;
                        }
                }

                if(Float.isNaN(bestPitch)
                        || bestError > maxHorizontalError.getValue())
                        return false;

                p.rotationYaw = yaw;
                p.rotationPitch = bestPitch;

                int slot = findPearl(p);
                if(slot != -1 && p.inventory.currentItem != slot)
                {
                        if(switchBack.isChecked())
                                restoreSlot = p.inventory.currentItem;
                        p.inventory.currentItem = slot;
                        mc.playerController.updateController();
                }

                ReflectCalls.rightClickMouse();
                cooldown = cooldownTicks.getValueI();
                restoreTimer = 8;
                return true;
        }

        private Vec3d simulate(double x, double y, double z,
                double vx, double vy, double vz, double targetY)
        {
                for(int i = 0; i < 300; i++)
                {
                        x += vx;
                        y += vy;
                        z += vz;

                        if(mc.world.getBlockState(
                                new BlockPos(x, y, z)).getMaterial()
                                        .blocksMovement())
                                return new Vec3d(x, y, z);

                        vx *= 0.99;
                        vz *= 0.99;
                        vy = vy * 0.99 - 0.03;

                        if(y < targetY - 16 || y < 0)
                                return null;
                }
                return null;
        }

        private int findPearl(EntityPlayerSP p)
        {
                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack =
                                p.inventory.mainInventory.get(i);
                        if(!stack.isEmpty()
                                && stack.getItem() == Items.ENDER_PEARL)
                                return i;
                }
                return -1;
        }
}
