/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoHurtcamHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * Vanilla's hurtCameraEffect() skips entirely when maxHurtTime is 0,
 * so zeroing it each tick removes the camera shake and red flash
 * without any rendering hook.
 */
public final class NoHurtcamHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public NoHurtcamHack()
	{
		super("NoHurtcam",
			"Disables the red flash and camera shake when you get hurt.");
		setCategory(Category.RENDER);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;

		p.hurtTime = 0;
		p.maxHurtTime = 0;
	}
}
