/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FollowHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Automatically walks to the closest other player and keeps the set
 * distance. Same movement approach as FightBot (movementInput
 * override + rotation), pauses while a GUI is open.
 */
public final class FollowHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting distance =
		new SliderSetting("Distance", 3, 1, 16, 0.5);

	public FollowHack()
	{
		super("Follow",
			"Automatically follows the closest player and keeps the set"
				+ " distance.");
		setCategory(Category.MOVEMENT);
		addSetting(distance);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		EntityPlayer target = findTarget(p);
		if(target == null)
		{
			stopWalking(p);
			return;
		}

		double distSq =
			p.getDistanceSq(target.posX, target.posY, target.posZ);
		if(distSq > distance.getValue() * distance.getValue())
		{
			rotateTo(p, target);
			walkTo(p);

			// jump over obstacles in the way
			if(p.collidedHorizontally && p.onGround)
				p.motionY = 0.42;
		}else
			stopWalking(p);
	}

	private EntityPlayer findTarget(EntityPlayerSP p)
	{
		EntityPlayer best = null;
		double bestDist = Double.MAX_VALUE;

		for(EntityPlayer e : mc.world.playerEntities)
		{
			if(e == p || !e.isEntityAlive())
				continue;

			double dist = p.getDistanceSq(e.posX, e.posY, e.posZ);
			if(dist < bestDist)
			{
				bestDist = dist;
				best = e;
			}
		}
		return best;
	}

	private void walkTo(EntityPlayerSP p)
	{
		if(p.movementInput == null)
			return;
		if(mc.currentScreen != null)
			return;
		if(p.isRiding())
			return;

		p.movementInput.moveForward = 1;
	}

	private void stopWalking(EntityPlayerSP p)
	{
		if(p.movementInput != null)
			p.movementInput.moveForward = 0;
	}

	private void rotateTo(EntityPlayerSP p, EntityPlayer target)
	{
		double diffX = target.posX - p.posX;
		double diffZ = target.posZ - p.posZ;
		double hDist = Math.sqrt(diffX * diffX + diffZ * diffZ);

		p.rotationYaw =
			(float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
		p.rotationPitch = 0;
		MathHelper.clamp(p.rotationPitch, -90F, 90F);
	}

	@Override
	protected void onDisable()
	{
		if(mc.player != null)
			stopWalking(mc.player);
	}
}
