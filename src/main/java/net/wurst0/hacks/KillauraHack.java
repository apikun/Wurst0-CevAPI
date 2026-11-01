/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI KillauraHack (GPL-3.0-or-later)
 *
 * v2 upgrades:
 *  - Team check: never attacks players on your scoreboard team (Bedwars /
 *    Hypixel teams are scoreboard teams), with a colored-nametag fallback
 *    for servers that don't use teams. Your teammates are safe.
 *  - Smooth aim: eased rotation stepping instead of a hard snap, so flicks
 *    look human (and never trip Grim's AimModulo360 >320-degree snap check).
 *  - Hitbox-aware range: measures eye -> closest hitbox point, the same
 *    surface distance Grim's Reach check measures.
 *  - Grim reach shield: with GrimBypass active the attack range is clamped
 *    to 2.95 blocks, inside Grim's 3.0 interpolated-hitbox limit.
 *  - Smart priority: weighted distance + angle + health target scoring.
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

public final class KillauraHack extends Hack
        implements UpdateListener, RenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 4.5, 1, 6, 0.05);
        private final SliderSetting speed =
                new SliderSetting("Speed", 10, 0.5, 20, 0.5);
        private final ModeSetting priority = new ModeSetting("Priority",
                new String[]{"Smart", "Distance", "Angle", "Health"}, 0);
        private final SliderSetting fov =
                new SliderSetting("FOV", 360, 30, 360, 10);
        private final CheckboxSetting rotate = new CheckboxSetting("Rotate", true);
        private final CheckboxSetting smoothAim =
                new CheckboxSetting("Smooth aim", true);
        private final SliderSetting smoothness =
                new SliderSetting("Smoothness", 3, 1, 10, 1);
        private final CheckboxSetting teamCheck =
                new CheckboxSetting("Team check", true);
        private final CheckboxSetting swing = new CheckboxSetting("Swing", true);
        private final CheckboxSetting attackPlayers =
                new CheckboxSetting("Attack players", true);
        private final CheckboxSetting attackPassives =
                new CheckboxSetting("Attack passives", true);
        private final CheckboxSetting checkLOS =
                new CheckboxSetting("Check line of sight", false);
        private final CheckboxSetting renderTarget =
                new CheckboxSetting("Render target", true);

        private EntityLivingBase target;
        private int cooldown;

        public KillauraHack()
        {
                super("Killaura",
                        "Automatically attacks entities around you. Never"
                                + " hits your Bedwars teammates (team check),"
                                + " rotates smoothly and stays inside Grim's"
                                + " reach limit when GrimBypass is on.");
                setCategory(Category.COMBAT);
                addSettings(range, speed, priority, fov, rotate, smoothAim,
                        smoothness, teamCheck, swing, attackPlayers,
                        attackPassives, checkLOS, renderTarget);
        }

        @Override
        public void onUpdate()
        {
                target = null;
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(cooldown > 0)
                {
                        cooldown--;
                        return;
                }

                double effRange = GrimBypassHack.capAttackRange(
                        range.getValue());

                List<EntityLivingBase> targets = getTargets(p, effRange);
                if(targets.isEmpty())
                        return;

                target = targets.get(0);

                Vec3d aimPoint = bestAimPoint(p, target);
                float[] rot = getRotationsTo(p, aimPoint);

                boolean aimed = true;
                if(rotate.isChecked())
                        aimed = rotateTowards(p, rot[0], rot[1]);

                // Grim shield: PacketOrderB cancels attacks that never
                // swing, so always emit the animation when active.
                if(aimed && p.getDistanceSq(target.posX, target.posY,
                        target.posZ) <= effRange * effRange
                        && eyeRange(p, target) <= effRange)
                {
                        mc.playerController.attackEntity(p, target);
                        if(swing.isChecked() || GrimBypassHack.forceSwing())
                                p.swingArm(EnumHand.MAIN_HAND);

                        cooldown = Math.max(1, (int)Math.round(20D
                                / speed.getValue()));
                }
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(!renderTarget.isChecked() || target == null
                        || !target.isEntityAlive())
                        return;
                if(mc.player == null || mc.world == null)
                        return;

                RenderUtils.enable3D(2F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                RenderUtils.drawBox(target.getEntityBoundingBox(), 1F, 0F, 0F, 0.5F);
                RenderUtils.disable3D(false);
        }

        private List<EntityLivingBase> getTargets(EntityPlayerSP p,
                double effRange)
        {
                List<EntityLivingBase> result = new ArrayList<EntityLivingBase>();
                double rangeSq = effRange * effRange;
                float halfFov = fov.getValueF() / 2F;
                Vec3d eye = p.getPositionEyes(1.0F);

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
                        if(isTeammate(living))
                                continue;

                        if(living instanceof EntityPlayer)
                        {
                                if(!attackPlayers.isChecked())
                                        continue;
                        }else if(isPassive(living) && !attackPassives.isChecked())
                                continue;

                        // hitbox-surface distance, like Grim's Reach check
                        if(eye.distanceTo(closestBoxPoint(
                                living.getEntityBoundingBox(), eye)) > effRange)
                                continue;
                        if(halfFov < 180F && getAngleTo(p, living) > halfFov)
                                continue;
                        if(checkLOS.isChecked() && !p.canEntityBeSeen(living))
                                continue;

                        result.add(living);
                }

                sortTargets(p, result);
                return result;
        }

        private void sortTargets(EntityPlayerSP p, List<EntityLivingBase> list)
        {
                String mode = priority.getSelected();
                if("Angle".equals(mode))
                        Collections.sort(list,
                                Comparator.comparingDouble(e -> getAngleTo(p, e)));
                else if("Health".equals(mode))
                        Collections.sort(list,
                                Comparator.comparingDouble(EntityLivingBase::getHealth));
                else if("Smart".equals(mode))
                        Collections.sort(list, Comparator.comparingDouble(
                                e -> smartScore(p, e)));
                else
                        Collections.sort(list, Comparator.comparingDouble(
                                e -> p.getDistanceSq(e.posX, e.posY, e.posZ)));
        }

        /**
         * Lower is better. Normalized blend of distance, view angle and
         * missing health: kills the closest threat you're already looking
         * at, prefers low-health targets, no wild swings across the FOV.
         */
        private double smartScore(EntityPlayerSP p, EntityLivingBase e)
        {
                double effRange = GrimBypassHack.capAttackRange(
                        range.getValue());
                double dist = eyeRange(p, e) / Math.max(effRange, 0.01);
                double angle = getAngleTo(p, e) / 180D;
                double health = 1D - (e.getHealth()
                        / Math.max(e.getMaxHealth(), 0.01));
                return dist * 0.4 + angle * 0.4 + health * 0.2;
        }

        private boolean isPassive(EntityLivingBase e)
        {
                return e instanceof EntityAnimal || e instanceof EntityAmbientCreature
                        || e instanceof EntitySquid || e instanceof EntityVillager
                        || e instanceof EntityGolem;
        }

        /**
         * Bedwars-safe targeting: never attack players on the same
         * scoreboard team (Hypixel Bedwars teams are scoreboard teams).
         * Fallback for teamless servers: compare colored nametags, ignoring
         * the default white/gray, so uncolored names never count as team.
         */
        private boolean isTeammate(EntityLivingBase e)
        {
                if(!teamCheck.isChecked() || !(e instanceof EntityPlayer))
                        return false;

                EntityPlayer other = (EntityPlayer)e;
                Team mine = mc.player.getTeam();
                Team theirs = mc.world.getScoreboard()
                        .getPlayersTeam(other.getName());
                if(mine != null || theirs != null)
                        return mine != null && theirs != null
                                && mine.equals(theirs);

                String myColor = nameColor(mc.player);
                return !myColor.isEmpty()
                        && myColor.equals(nameColor(other));
        }

        private String nameColor(EntityPlayer pl)
        {
                String s = pl.getDisplayName().getFormattedText();
                int i = s.indexOf('\u00a7');
                if(i >= 0 && i + 1 < s.length())
                {
                        char c = Character.toLowerCase(s.charAt(i + 1));
                        boolean isColor = (c >= '0' && c <= '9')
                                || (c >= 'a' && c <= 'f');
                        if(isColor && c != 'f' && c != '7')
                                return String.valueOf(c);
                }
                return "";
        }

        private double getAngleTo(EntityPlayerSP p, Entity e)
        {
                AxisAlignedBB box = e.getEntityBoundingBox();
                Vec3d eye = p.getPositionEyes(1.0F);
                Vec3d look = p.getLook(1.0F);
                Vec3d toTarget = new Vec3d(e.posX - eye.x,
                        (box.minY + box.maxY) / 2D - eye.y, e.posZ - eye.z);
                double len = Math.sqrt(toTarget.x * toTarget.x + toTarget.y * toTarget.y + toTarget.z * toTarget.z);
                if(len < 1.0E-4)
                        return 0;
                double dot = (look.x * toTarget.x
                        + look.y * toTarget.y
                        + look.z * toTarget.z) / len;
                return Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1D, 1D)));
        }

        /** Eye -> closest point on the entity's hitbox. */
        private double eyeRange(EntityPlayerSP p, Entity e)
        {
                Vec3d eye = p.getPositionEyes(1.0F);
                return eye.distanceTo(
                        closestBoxPoint(e.getEntityBoundingBox(), eye));
        }

        private Vec3d closestBoxPoint(AxisAlignedBB box, Vec3d point)
        {
                return new Vec3d(
                        MathHelper.clamp(point.x, box.minX, box.maxX),
                        MathHelper.clamp(point.y, box.minY, box.maxY),
                        MathHelper.clamp(point.z, box.minZ, box.maxZ));
        }

        /**
         * Aim at the closest hitbox point when near, otherwise the body
         * center - keeps hits inside Grim's reach and lands more attacks.
         */
        private Vec3d bestAimPoint(EntityPlayerSP p, EntityLivingBase e)
        {
                Vec3d eye = p.getPositionEyes(1.0F);
                if(eyeRange(p, e) < 3.0)
                        return closestBoxPoint(e.getEntityBoundingBox(), eye);
                AxisAlignedBB box = e.getEntityBoundingBox();
                return new Vec3d(e.posX, (box.minY + box.maxY) / 2D, e.posZ);
        }

        private float[] getRotationsTo(EntityPlayerSP p, Vec3d point)
        {
                double diffX = point.x - p.posX;
                double diffY = point.y - (p.posY + p.getEyeHeight());
                double diffZ = point.z - p.posZ;
                double hDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
                float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX))
                        - 90F;
                float pitch = MathHelper.clamp(
                        (float)(-Math.toDegrees(Math.atan2(diffY, hDist))),
                        -90F, 90F);
                return new float[]{yaw, pitch};
        }

        /**
         * Eased rotation: proportional step with a hard cap per tick. Only
         * small snaps ever reach the wire, keeping Grim's AimModulo360
         * (yaw snap >320 degrees) and BadPacketsD (pitch bounds) happy.
         * Returns true once the aim has converged close enough to attack.
         */
        private boolean rotateTowards(EntityPlayerSP p, float targetYaw,
                float targetPitch)
        {
                float dYaw = MathHelper.wrapDegrees(targetYaw - p.rotationYaw);
                float dPitch = MathHelper.clamp(targetPitch, -90F, 90F)
                        - p.rotationPitch;

                if(!smoothAim.isChecked())
                {
                        p.rotationYaw = MathHelper.wrapDegrees(
                                p.rotationYaw + dYaw);
                        p.rotationPitch = MathHelper.clamp(
                                p.rotationPitch + dPitch, -90F, 90F);
                        return true;
                }

                float maxStep = Math.max(8F, 80F / smoothness.getValueF());
                float yawStep = MathHelper.clamp(dYaw * 0.45F, -maxStep,
                        maxStep);
                float pitchStep = MathHelper.clamp(dPitch * 0.45F, -maxStep,
                        maxStep);
                if(Math.abs(dYaw) < 0.25F)
                        yawStep = dYaw;
                if(Math.abs(dPitch) < 0.25F)
                        pitchStep = dPitch;

                p.rotationYaw = MathHelper.wrapDegrees(p.rotationYaw + yawStep);
                p.rotationPitch = MathHelper.clamp(
                        p.rotationPitch + pitchStep, -90F, 90F);

                // attack once the aim is close; small angle keeps hits on
                // target without waiting for a perfect lock
                return Math.abs(dYaw) < 30F && Math.abs(dPitch) < 30F;
        }

        @Override
        protected void onDisable()
        {
                target = null;
                cooldown = 0;
        }
}
