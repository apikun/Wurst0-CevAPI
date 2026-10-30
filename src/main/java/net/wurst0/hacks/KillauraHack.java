/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI KillauraHack (GPL-3.0-or-later)
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
		new String[]{"Distance", "Angle", "Health"}, 0);
	private final SliderSetting fov =
		new SliderSetting("FOV", 360, 30, 360, 10);
	private final CheckboxSetting rotate = new CheckboxSetting("Rotate", true);
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
		super("Killaura", "Automatically attacks entities around you.");
		setCategory(Category.COMBAT);
		addSettings(range, speed, priority, fov, rotate, swing,
			attackPlayers, attackPassives, checkLOS, renderTarget);
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

		List<EntityLivingBase> targets = getTargets(p);
		if(targets.isEmpty())
			return;

		target = targets.get(0);

		if(rotate.isChecked())
			rotateTo(p, target);

		double rangeSq = range.getValue() * range.getValue();
		if(p.getDistanceSq(target.posX, target.posY, target.posZ) > rangeSq)
			return;

		mc.playerController.attackEntity(p, target);
		if(swing.isChecked())
			p.swingArm(EnumHand.MAIN_HAND);

		cooldown = Math.max(1, (int)Math.round(20D / speed.getValue()));
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

	private List<EntityLivingBase> getTargets(EntityPlayerSP p)
	{
		List<EntityLivingBase> result = new ArrayList<EntityLivingBase>();
		double rangeSq = range.getValue() * range.getValue();
		float halfFov = fov.getValueF() / 2F;

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

			if(living instanceof EntityPlayer)
			{
				if(!attackPlayers.isChecked())
					continue;
			}else if(isPassive(living) && !attackPassives.isChecked())
				continue;

			if(p.getDistanceSq(e.posX, e.posY, e.posZ) > rangeSq)
				continue;
			if(halfFov < 180F && getAngleTo(p, e) > halfFov)
				continue;
			if(checkLOS.isChecked() && !p.canEntityBeSeen(e))
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
		else
			Collections.sort(list, Comparator.comparingDouble(
				e -> p.getDistanceSq(e.posX, e.posY, e.posZ)));
	}

	private boolean isPassive(EntityLivingBase e)
	{
		return e instanceof EntityAnimal || e instanceof EntityAmbientCreature
			|| e instanceof EntitySquid || e instanceof EntityVillager
			|| e instanceof EntityGolem;
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

	private void rotateTo(EntityPlayerSP p, Entity e)
	{
		AxisAlignedBB box = e.getEntityBoundingBox();
		double diffX = e.posX - p.posX;
		double diffY = (box.minY + box.maxY) / 2D - (p.posY + p.getEyeHeight());
		double diffZ = e.posZ - p.posZ;
		double hDist = Math.sqrt(diffX * diffX + diffZ * diffZ);
		p.rotationYaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
		p.rotationPitch =
			MathHelper.clamp((float)(-Math.toDegrees(Math.atan2(diffY, hDist))),
				-90F, 90F);
	}

	@Override
	protected void onDisable()
	{
		target = null;
		cooldown = 0;
	}
}
