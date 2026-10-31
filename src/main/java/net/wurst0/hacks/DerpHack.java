/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI DerpHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class DerpHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();
	private final Random rand = new Random();

	public DerpHack()
	{
		super("Derp",
			"Randomly spins your head around, because that looks funny.");
		setCategory(Category.FUN);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;

		p.rotationYaw += (rand.nextFloat() - 0.5F) * 30F;
		p.rotationPitch = (rand.nextFloat() - 0.5F) * 60F;
	}

	@Override
	protected void onDisable()
	{
		if(mc.player != null)
			mc.player.rotationPitch = 0;
	}
}
