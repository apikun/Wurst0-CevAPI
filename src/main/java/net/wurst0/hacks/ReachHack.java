/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ReachHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Extends your interaction reach by raising the player's
 * generic.reachDistance attribute (EntityPlayer.REACH_DISTANCE).
 * Forge 1.12.2 routes PlayerControllerMP.getBlockReachDistance()
 * through this attribute, and EntityRenderer.getMouseOver uses the
 * same value for entity targeting - so breaking, placing and
 * attacking all work at the new range. Server-side reach checks
 * still apply.
 */
public final class ReachHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting distance =
		new SliderSetting("Distance", 6, 1, 10, 0.1);

	public ReachHack()
	{
		super("Reach",
			"Allows you to reach further when breaking blocks and"
				+ " attacking entities.");
		setCategory(Category.OTHER);
		addSetting(distance);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		p.getEntityAttribute(EntityPlayer.REACH_DISTANCE)
			.setBaseValue(distance.getValue());
	}

	@Override
	protected void onDisable()
	{
		if(mc.player != null)
			mc.player.getEntityAttribute(EntityPlayer.REACH_DISTANCE)
				.setBaseValue(5.0D);
	}
}
