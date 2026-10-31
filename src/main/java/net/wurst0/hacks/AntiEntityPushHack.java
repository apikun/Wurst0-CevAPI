/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiEntityPushHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * Cancels the small velocity kicks that other entities apply when they
 * push into you. Approximate port: the push is applied by vanilla
 * collision handling on both sides, so we detect the resulting small
 * horizontal motion while standing still and remove it again.
 */
public final class AntiEntityPushHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public AntiEntityPushHack()
	{
		super("AntiEntityPush",
			"Protects you from being pushed around by other entities.");
		setCategory(Category.MOVEMENT);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(!p.onGround)
			return;

		// only cancel while not intentionally moving
		if(p.movementInput != null && (p.movementInput.moveForward != 0
			|| p.movementInput.moveStrafe != 0))
			return;

		// must be touching an entity for the push to be an entity push
		if(!entityNearby(p))
			return;

		double hx = Math.abs(p.motionX);
		double hz = Math.abs(p.motionZ);
		// small horizontal kick with no vertical motion => push
		if(hx < 0.2 && hz < 0.2 && hx + hz > 0.001)
		{
			p.motionX = 0;
			p.motionZ = 0;
		}
	}

	private boolean entityNearby(EntityPlayerSP p)
	{
		List<Entity> entities =
			new ArrayList<Entity>(mc.world.loadedEntityList);
		for(Entity e : entities)
		{
			if(e == p || !(e instanceof EntityLivingBase))
				continue;
			if(!e.isEntityAlive())
				continue;
			double dx = e.posX - p.posX;
			double dy = e.posY - p.posY;
			double dz = e.posZ - p.posZ;
			if(dx * dx + dy * dy + dz * dz < 1.1)
				return true;
		}
		return false;
	}
}
