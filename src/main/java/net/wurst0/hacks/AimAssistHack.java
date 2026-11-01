/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AimAssistHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.TeamUtils;

public final class AimAssistHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 4.5, 1, 6, 0.05);
        private final SliderSetting fov = new SliderSetting("FOV", 90, 10, 180, 5);
        private final SliderSetting speed =
                new SliderSetting("Rotation speed", 10, 1, 30, 1);
        private final CheckboxSetting ignoreTeammates =
                new CheckboxSetting("Ignore teammates", true);
        private final CheckboxSetting onlyOnAttack =
                new CheckboxSetting("Only while attacking", true);

        public AimAssistHack()
        {
                super("AimAssist",
                        "Slowly rotates your view towards the nearest valid target"
                                + " - including players - so your aim looks natural"
                                + " while it helps you hit. Never drags your aim onto"
                                + " your Bedwars teammates (ignore teammates).");
                setCategory(Category.COMBAT);
                addSettings(range, fov, speed, ignoreTeammates, onlyOnAttack);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;
                if(onlyOnAttack.isChecked()
                        && !mc.gameSettings.keyBindAttack.isKeyDown())
                        return;

                double rangeSq = range.getValue() * range.getValue();
                float halfFov = fov.getValueF() / 2F;
                EntityLivingBase best = null;
                double bestAngle = Double.MAX_VALUE;

                List<Entity> loaded =
                        new ArrayList<Entity>(mc.world.loadedEntityList);
                for(Entity e : loaded)
                {
                        if(!(e instanceof EntityLivingBase) || e == p)
                                continue;

                        EntityLivingBase living = (EntityLivingBase)e;
                        if(!living.isEntityAlive() || living.getHealth() <= 0)
                                continue;
                        if(living instanceof EntityArmorStand)
                                continue;
                        if(ignoreTeammates.isChecked()
                                && TeamUtils.isTeammate(p, living))
                                continue;
                        if(p.getDistanceSq(e.posX, e.posY, e.posZ) > rangeSq)
                                continue;
                        if(!p.canEntityBeSeen(e))
                                continue;

                        double angle = getAngleTo(p, e);
                        if(angle > halfFov)
                                continue;
                        if(angle < bestAngle)
                        {
                                bestAngle = angle;
                                best = living;
                        }
                }

                if(best == null)
                        return;

                double diffX = best.posX - p.posX;
                double diffY = best.getEntityBoundingBox().minY + best.height * 0.5D
                        - (p.posY + p.getEyeHeight());
                double diffZ = best.posZ - p.posZ;
                double hDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
                float targetYaw =
                        (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
                float targetPitch = MathHelper.clamp(
                        (float)(-Math.toDegrees(Math.atan2(diffY, hDist))), -90F, 90F);

                float yawDiff = MathHelper.wrapDegrees(targetYaw - p.rotationYaw);
                float pitchDiff =
                        MathHelper.wrapDegrees(targetPitch - p.rotationPitch);
                float max = speed.getValueF();
                p.rotationYaw += Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), max);
                p.rotationPitch += Math.signum(pitchDiff)
                        * Math.min(Math.abs(pitchDiff), max);
        }

        private double getAngleTo(EntityPlayerSP p, Entity e)
        {
                Vec3d eye = p.getPositionEyes(1.0F);
                Vec3d look = p.getLook(1.0F);
                double diffY = e.getEntityBoundingBox().minY + e.height * 0.5D
                        - eye.y;
                Vec3d toTarget = new Vec3d(e.posX - eye.x, diffY,
                        e.posZ - eye.z);
                double len = Math.sqrt(toTarget.x * toTarget.x + toTarget.y * toTarget.y + toTarget.z * toTarget.z);
                if(len < 1.0E-4)
                        return 0;
                double dot = (look.x * toTarget.x
                        + look.y * toTarget.y
                        + look.z * toTarget.z) / len;
                return Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1D, 1D)));
        }
}
