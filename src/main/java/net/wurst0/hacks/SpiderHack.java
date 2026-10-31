/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SpiderHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class SpiderHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting climbSpeed =
		new SliderSetting("Climb speed", 0.2, 0.05, 1, 0.05);

	public SpiderHack()
	{
		super("Spider",
			"Allows you to climb up walls like a spider.");
		setCategory(Category.MOVEMENT);
		addSetting(climbSpeed);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(!p.collidedHorizontally || p.onGround || p.isInWater()
			|| p.isOnLadder())
			return;

		p.motionY = climbSpeed.getValue();
	}
}
