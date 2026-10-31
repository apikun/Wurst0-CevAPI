/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI DolphinHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class DolphinHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public DolphinHack()
	{
		super("Dolphin",
			"Makes you swim faster in water, like a dolphin.");
		setCategory(Category.MOVEMENT);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || !p.isInWater())
			return;

		p.motionY += 0.04;
		p.motionX *= 1.1;
		p.motionZ *= 1.1;
	}
}
