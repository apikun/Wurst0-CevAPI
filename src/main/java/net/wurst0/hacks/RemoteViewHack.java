/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI RemoteViewHack (GPL-3.0-or-later)
 * Lite adaptation: without camera mixins on 1.12.2, the camera is
 * swapped with mc.setRenderViewEntity() while enabled and restored
 * on disable; your own player stays where it is and the first-person
 * hand is hidden while viewing.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.events.HandRenderEvent;
import net.wurst0.event.listeners.HandRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * Lets you look through the eyes of the nearest living entity,
 * client-side only. Movement controls keep driving your own player,
 * so stay put while viewing.
 */
public final class RemoteViewHack extends Hack
	implements UpdateListener, HandRenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private Entity entity;
	private Entity savedView;

	public RemoteViewHack()
	{
		super("RemoteView",
			"Lets you see through the eyes of the nearest living entity."
				+ " Client-side only; your player cannot move while"
				+ " viewing.");
		setCategory(Category.RENDER);
	}

	@Override
	protected void onEnable()
	{
		if(mc.player == null || mc.world == null)
		{
			setEnabled(false);
			return;
		}

		// find the closest valid entity
		Entity best = null;
		double bestDist = Double.MAX_VALUE;
		for(int i = 0; i < mc.world.loadedEntityList.size(); i++)
		{
			Entity e = mc.world.loadedEntityList.get(i);
			if(e == mc.player || !(e instanceof EntityLivingBase))
				continue;

			if(e instanceof EntityArmorStand || e.isDead)
				continue;

			if(((EntityLivingBase)e).getHealth() <= 0F)
				continue;

			double dist = mc.player.getDistanceSq(e);
			if(dist < bestDist)
			{
				bestDist = dist;
				best = e;
			}
		}

		if(best == null)
		{
			message("\u00a7cCould not find a valid entity.");
			setEnabled(false);
			return;
		}

		entity = best;
		savedView = mc.getRenderViewEntity();
		mc.setRenderViewEntity(entity);
		message("Now viewing " + entity.getName() + ".");
	}

	@Override
	protected void onDisable()
	{
		if(entity != null)
		{
			message("No longer viewing " + entity.getName() + ".");

			if(mc.getRenderViewEntity() == entity)
			{
				if(savedView != null && savedView != entity
					&& !savedView.isDead)
					mc.setRenderViewEntity(savedView);
				else if(mc.player != null)
					mc.setRenderViewEntity(mc.player);
			}
		}

		entity = null;
		savedView = null;
	}

	@Override
	public void onUpdate()
	{
		if(mc.player == null || mc.world == null)
		{
			setEnabled(false);
			return;
		}

		// validate entity: dead, health gone, replaced by a reload or
		// unloaded from the world all end the view
		if(entity == null || entity.isDead
			|| mc.world.getEntityByID(entity.getEntityId()) != entity
			|| (entity instanceof EntityLivingBase
				&& ((EntityLivingBase)entity).getHealth() <= 0F))
		{
			setEnabled(false);
			return;
		}

		// keep the camera on the target
		if(mc.getRenderViewEntity() != entity)
			mc.setRenderViewEntity(entity);
	}

	@Override
	public void onHandRender(HandRenderEvent event)
	{
		if(entity != null)
			event.setCancelled(true);
	}

	private void message(String text)
	{
		EntityPlayerSP player = mc.player;
		if(player != null)
			player.sendMessage(new TextComponentString(text));
	}
}
