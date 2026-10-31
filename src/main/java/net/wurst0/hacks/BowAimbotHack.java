/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BowAimbotHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemBow;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class BowAimbotHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 50, 10, 100, 5);

	private EntityLivingBase target;

	public BowAimbotHack()
	{
		super("BowAimbot",
			"Automatically aims your bow at the nearest enemy while"
				+ " drawing it back. Prediction is approximate.");
		setCategory(Category.COMBAT);
		addSetting(range);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
		{
			target = null;
			return;
		}

		if(!p.isHandActive()
			|| !(p.getActiveItemStack().getItem() instanceof ItemBow))
		{
			target = null;
			return;
		}

		target = findTarget(p);
		if(target == null)
			return;

		aimAt(p, target);
	}

	private EntityLivingBase findTarget(EntityPlayerSP p)
	{
		EntityLivingBase best = null;
		double bestSq = range.getValue() * range.getValue();

		for(Entity e : mc.world.loadedEntityList)
		{
			if(!(e instanceof EntityLivingBase) || e == p)
				continue;

			EntityLivingBase living = (EntityLivingBase)e;
			if(!living.isEntityAlive() || living.getHealth() <= 0)
				continue;
			if(living instanceof EntityArmorStand)
				continue;

			double distSq = p.getDistanceSq(e.posX, e.posY, e.posZ);
			if(distSq > bestSq)
				continue;

			best = living;
			bestSq = distSq;
		}
		return best;
	}

	private void aimAt(EntityPlayerSP p, EntityLivingBase e)
	{
		// lead the target: rough estimate of arrow flight time
		double dist = MathHelper.sqrt(p.getDistanceSq(e.posX, e.posY,
			e.posZ));
		double ticks = dist / 3D; // fully-charged arrow ~3 blocks/tick

		double aimX = e.posX + (e.posX - e.prevPosX) * ticks;
		double aimY = e.getEntityBoundingBox().minY
			+ e.height / 2D + (e.posY - e.prevPosY) * ticks;
		double aimZ = e.posZ + (e.posZ - e.prevPosZ) * ticks;

		double diffX = aimX - p.posX;
		double diffY = aimY - (p.posY + p.getEyeHeight());
		double diffZ = aimZ - p.posZ;
		double hDist = Math.sqrt(diffX * diffX + diffZ * diffZ);

		p.rotationYaw =
			(float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
		float pitch = MathHelper
			.clamp((float)-Math.toDegrees(Math.atan2(diffY, hDist)),
				-90F, 90F);

		// simple arrow-drop compensation
		pitch -= (float)(dist * 0.035);

		p.rotationPitch = pitch;
	}

	@Override
	protected void onDisable()
	{
		target = null;
	}
}
