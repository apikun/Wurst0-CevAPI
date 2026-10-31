/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI JetpackHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class JetpackHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting speed =
		new SliderSetting("Speed", 0.5, 0.1, 2, 0.05);

	public JetpackHack()
	{
		super("Jetpack",
			"Hold the jump key to fly upwards like with a jetpack.");
		setCategory(Category.MOVEMENT);
		addSetting(speed);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(!mc.gameSettings.keyBindJump.isKeyDown())
			return;
		if(p.isRiding())
			return;

		p.motionY = Math.min(p.motionY + 0.1F, speed.getValue());
	}
}
