/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MobViewHack (GPL-3.0-or-later)
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
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Attaches the camera to the nearest mob (the real player keeps
 * moving with your controls, exactly like the original hack).
 * The camera resets automatically when the mob despawns.
 */
public final class MobViewHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 16, 4, 64, 4);

	public MobViewHack()
	{
		super("MobView",
			"Lets you see through the eyes of the nearest mob while"
				+ " you keep walking around.");
		setCategory(Category.FUN);
		addSetting(range);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		Entity mob = findMob(p);
		if(mob != null)
			mc.setRenderViewEntity(mob);
		else if(mc.getRenderViewEntity() != p)
			mc.setRenderViewEntity(p);
	}

	private Entity findMob(EntityPlayerSP p)
	{
		Entity best = null;
		double bestDist = range.getValue() * range.getValue();

		List<Entity> loaded = new ArrayList<Entity>(mc.world.loadedEntityList);
		for(Entity e : loaded)
		{
			if(!(e instanceof EntityLivingBase) || e == p)
				continue;
			if(e instanceof EntityPlayer || e instanceof EntityArmorStand)
				continue;
			if(!e.isEntityAlive())
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

	@Override
	protected void onDisable()
	{
		if(mc.player != null && mc.getRenderViewEntity() != mc.player)
			mc.setRenderViewEntity(mc.player);
	}
}
