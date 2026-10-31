/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiProjectileHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityShulkerBullet;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Punches incoming fireballs (ghast blazes) and shulker bullets back
 * before they hit you, by attacking them like the original.
 */
public final class AntiProjectileHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 4, 2, 6, 0.5);
	private final SliderSetting cooldownSec =
		new SliderSetting("Cooldown", 0.5, 0, 2, 0.05);
	private final CheckboxSetting swing =
		new CheckboxSetting("Swing", true);

	private int cooldown;

	public AntiProjectileHack()
	{
		super("AntiProjectile",
			"Punches incoming fireballs and shulker bullets back"
				+ " before they hit you.");
		setCategory(Category.COMBAT);
		addSettings(range, cooldownSec, swing);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(cooldown > 0)
		{
			cooldown--;
			return;
		}

		List<Entity> entities =
			new ArrayList<Entity>(mc.world.loadedEntityList);
		double rangeSq = range.getValue() * range.getValue();

		Entity best = null;
		double bestDist = Double.MAX_VALUE;
		for(Entity e : entities)
		{
			if(!isThreat(e) || !e.isEntityAlive())
				continue;
			double dist = e.getDistanceSq(p.posX, p.posY, p.posZ);
			if(dist > rangeSq || dist >= bestDist)
				continue;
			if(!isFlyingTowardsPlayer(p, e))
				continue;
			bestDist = dist;
			best = e;
		}

		if(best == null)
			return;

		mc.playerController.attackEntity(p, best);
		if(swing.isChecked())
			p.swingArm(EnumHand.MAIN_HAND);
		cooldown = (int)Math.round(cooldownSec.getValue() * 20.0);
	}

	private boolean isThreat(Entity e)
	{
		return e instanceof EntityFireball
			|| e instanceof EntityShulkerBullet;
	}

	private boolean isFlyingTowardsPlayer(EntityPlayerSP p, Entity e)
	{
		// projectile must be moving (or aimed) towards us
		double mx = e.motionX, my = e.motionY, mz = e.motionZ;
		double speed = mx * mx + my * my + mz * mz;
		if(speed > 0.001)
		{
			Vec3d pos = new Vec3d(p.posX - e.posX,
				p.posY - e.posY, p.posZ - e.posZ);
			double dot = pos.x * mx + pos.y * my + pos.z * mz;
			return dot > 0; // moving in our direction
		}
		return true; // hovering fireballs still count
	}
}
