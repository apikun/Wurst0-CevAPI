/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI HeadRollHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Rolls your head in a smooth sine wave while keeping your yaw, so
 * you look silly without spinning around. Other players see the
 * rotation because vanilla sends C05/C06 rotation packets.
 */
public final class HeadRollHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting speed =
		new SliderSetting("Speed", 10, 1, 20, 1);

	private int tick;

	public HeadRollHack()
	{
		super("HeadRoll",
			"Rolls your head smoothly up and down. Great for confusing"
				+ " other players.");
		setCategory(Category.FUN);
		addSetting(speed);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		tick++;
		float roll = (float)Math.sin(tick * speed.getValueF() * 0.05) * 80F;
		p.rotationPitch = Math.max(-90F, Math.min(90F, roll));
	}

	@Override
	protected void onDisable()
	{
		if(mc.player != null)
			mc.player.rotationPitch = 0;
	}
}
