/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FastPlaceHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class FastPlaceHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public FastPlaceHack()
	{
		super("FastPlace",
			"Removes the delay between placing or breaking blocks when"
				+ " holding down the mouse button.");
		setCategory(Category.BLOCKS);
	}

	@Override
	public void onUpdate()
	{
		if(mc.playerController != null)
			mc.playerController.blockHitDelay = 0;
	}
}
