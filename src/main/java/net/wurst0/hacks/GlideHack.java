/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI GlideHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class GlideHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting fallSpeed =
		new SliderSetting("Fall speed", 0.3, 0.05, 1, 0.05);

	public GlideHack()
	{
		super("Glide",
			"Slows down your fall like with a parachute.");
		setCategory(Category.MOVEMENT);
		addSetting(fallSpeed);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(p.onGround || p.isInWater() || p.isOnLadder())
			return;
		if(p.capabilities.isFlying || p.motionY >= 0)
			return;

		double max = -fallSpeed.getValue();
		if(p.motionY < max)
			p.motionY = max;
	}
}
