/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI UntouchableHack (GPL-3.0-or-later)
 * Lite adaptation: the original dodges with 26.x packet mechanics
 * (bundles / position-sync cues for maces, spears, crystals). No
 * maces/spears exist in 1.12.2, so this version keeps the core
 * intent - teleport out of reach of armed players before they hit
 * you - using client-side position teleports (same mechanism as
 * TeleportHack) plus an emergency escape on heavy damage.
 */
package net.wurst0.hacks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.events.DamageEvent;
import net.wurst0.event.listeners.DamageListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Keeps armed players (and optionally hostile mobs) out of hitting
 * range by teleporting away, with an emergency escape triggered by
 * heavy damage.
 */
public final class UntouchableHack extends Hack
        implements UpdateListener, DamageListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting detectionRange =
                new SliderSetting("Detection range", 12, 6, 32, 0.5);
        private final SliderSetting playerDistance =
                new SliderSetting("Keep distance", 7, 3, 16, 0.5);
        private final SliderSetting teleportDistance =
                new SliderSetting("Teleport distance", 6, 1, 12, 0.25);
        private final SliderSetting verticalScan =
                new SliderSetting("Vertical scan", 3, 0, 8, 1);
        private final SliderSetting teleportCooldown =
                new SliderSetting("Teleport cooldown (ticks)", 3, 1, 10, 1);
        private final CheckboxSetting avoidHostileMobs =
                new CheckboxSetting("Avoid hostile mobs", false);
        private final CheckboxSetting emergencyEscape =
                new CheckboxSetting("Emergency escape", true);
        private final SliderSetting emergencyDamage =
                new SliderSetting("Emergency damage (HP)", 10, 1, 40, 1);

        private int cooldownTicksLeft;
        private boolean escapeNextTick;

        public UntouchableHack()
        {
                super("Untouchable",
                        "Teleports you out of reach of armed players"
                                + " before they can hit you, and escapes"
                                + " perpendicular to the threat after"
                                + " heavy damage.");
                setCategory(Category.COMBAT);
                addSettings(detectionRange, playerDistance,
                        teleportDistance, verticalScan, teleportCooldown,
                        avoidHostileMobs, emergencyEscape, emergencyDamage);
        }

        @Override
        protected void onEnable()
        {
                cooldownTicksLeft = 0;
                escapeNextTick = false;
        }

        @Override
        protected void onDisable()
        {
                cooldownTicksLeft = 0;
                escapeNextTick = false;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(cooldownTicksLeft > 0)
                        cooldownTicksLeft--;

                if(escapeNextTick)
                {
                        escapeNextTick = false;
                        dodge(p, null);
                        return;
                }

                EntityLivingBase threat = findThreat(p);
                if(threat == null || cooldownTicksLeft > 0)
                        return;

                dodge(p, threat);
        }

        @Override
        public void onDamage(DamageEvent event)
        {
                if(!emergencyEscape.isChecked())
                        return;
                if(mc.player == null)
                        return;
                if(event.getAmount() >= emergencyDamage.getValue())
                        escapeNextTick = true;
        }

        private EntityLivingBase findThreat(EntityPlayerSP p)
        {
                double detectSq = detectionRange.getValue()
                        * detectionRange.getValue();
                double keepSq = playerDistance.getValue()
                        * playerDistance.getValue();

                EntityLivingBase nearest = null;
                double nearestSq = Double.MAX_VALUE;
                for(Entity e : mc.world.loadedEntityList)
                {
                        if(e == p || !e.isEntityAlive()
                                || !(e instanceof EntityLivingBase))
                                continue;

                        boolean armedPlayer =
                                e instanceof EntityPlayer
                                        && isArmed((EntityLivingBase)e);
                        boolean hostileMob =
                                avoidHostileMobs.isChecked()
                                        && e instanceof EntityMob;
                        if(!armedPlayer && !hostileMob)
                                continue;

                        double distSq = e.getDistanceSq(p);
                        if(distSq > detectSq)
                                continue;

                        // dodge only once the threat is actually
                        // within hitting distance
                        if(distSq > keepSq)
                                continue;

                        if(distSq < nearestSq)
                        {
                                nearestSq = distSq;
                                nearest = (EntityLivingBase)e;
                        }
                }
                return nearest;
        }

        private void dodge(EntityPlayerSP p, EntityLivingBase threat)
        {
                double baseAngle;
                if(threat != null)
                {
                        // away from the threat
                        baseAngle = Math.atan2(p.posZ - threat.posZ,
                                p.posX - threat.posX);
                }else
                {
                        // emergency: escape away from the look
                        // direction (yaw + 270 in atan2(z, x) space)
                        float yaw = p.rotationYaw;
                        baseAngle = Math.toRadians(yaw)
                                + Math.PI * 1.5;
                }

                int[] offsets = {0, 45, -45, 90, -90, 135, -135, 180};
                double maxDist = teleportDistance.getValue();
                int vertical = verticalScan.getValueI();

                for(int offsetDeg : offsets)
                {
                        double angle = baseAngle
                                + Math.toRadians(offsetDeg);
                        double dirX = Math.cos(angle);
                        double dirZ = Math.sin(angle);

                        for(double dist = maxDist; dist >= 1; dist -= 1)
                        {
                                double x = p.posX + dirX * dist;
                                double z = p.posZ + dirZ * dist;
                                int y = (int)Math.floor(p.posY);

                                for(int dy = 0; dy <= vertical; dy++)
                                {
                                        if(isSafeSpot(p, x, y + dy, z))
                                        {
                                                p.setPositionAndUpdate(x,
                                                        y + dy, z);
                                                cooldownTicksLeft =
                                                        teleportCooldown
                                                                .getValueI();
                                                return;
                                        }
                                        if(dy == 0)
                                                continue;
                                        if(isSafeSpot(p, x, y - dy, z))
                                        {
                                                p.setPositionAndUpdate(x,
                                                        y - dy, z);
                                                cooldownTicksLeft =
                                                        teleportCooldown
                                                                .getValueI();
                                                return;
                                        }
                                }
                        }
                }
                // nothing found: short cooldown so we don't spam scans
                cooldownTicksLeft = teleportCooldown.getValueI();
        }

        private boolean isSafeSpot(EntityPlayerSP p, double x, double y,
                double z)
        {
                BlockPos feet = new BlockPos(Math.floor(x), y,
                        Math.floor(z));
                BlockPos head = feet.up();
                BlockPos ground = feet.down();

                if(!mc.world.isAirBlock(feet)
                        || !mc.world.isAirBlock(head))
                        return false;

                IBlockState below = mc.world.getBlockState(ground);
                if(!below.getMaterial().isSolid())
                        return false;

                AxisAlignedBB box = new AxisAlignedBB(x - 0.4, y,
                        z - 0.4, x + 0.4, y + 1.9, z + 0.4);
                if(!mc.world.getEntitiesWithinAABBExcludingEntity(p, box)
                        .isEmpty())
                        return false;

                return true;
        }

        private static boolean isArmed(EntityLivingBase entity)
        {
                ItemStack held = entity.getHeldItemMainhand();
                if(held.isEmpty())
                        return false;
                return held.getItem() instanceof ItemSword
                        || held.getItem() instanceof ItemAxe
                        || held.getItem() instanceof ItemBow;
        }
}
