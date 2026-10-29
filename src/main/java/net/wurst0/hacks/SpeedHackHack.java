/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SpeedHackHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class SpeedHackHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting speed =
		new SliderSetting("Speed", 1.3, 1.05, 2, 0.05);

	public SpeedHackHack()
	{
		super("SpeedHack",
			"Makes you walk faster (simple mode). Works best while"
				+ " running on the ground.");
		setCategory(Category.MOVEMENT);
		addSetting(speed);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(p.onGround && (p.movementInput.moveForward != 0
			|| p.movementInput.moveStrafe != 0))
		{
			p.motionX *= speed.getValueF();
			p.motionZ *= speed.getValueF();
		}
	}
}
