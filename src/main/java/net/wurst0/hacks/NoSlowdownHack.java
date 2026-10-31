/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoSlowdownHack (GPL-3.0-or-later)
 * Approximate 1.12.2 implementation: compensates the item-use
 * slowdown by boosting motion each tick (no packet hooks).
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class NoSlowdownHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public NoSlowdownHack()
	{
		super("NoSlowdown",
			"Reduces the slowdown caused by eating, drinking and"
				+ " blocking. Approximate without packet hooks.");
		setCategory(Category.MOVEMENT);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;
		if(!p.isHandActive() || p.isRiding() || p.capabilities.isFlying)
			return;

		double motionX = p.motionX;
		double motionZ = p.motionZ;
		double hSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);

		// vanilla slows item use to ~20% speed; compensate while slow
		if(hSpeed > 1.0E-4 && hSpeed < 0.25)
		{
			double factor = 1.8;
			p.motionX *= factor;
			p.motionZ *= factor;

			// cap at sprint speed
			double newSpeed = hSpeed * factor;
			if(newSpeed > 0.3)
			{
				double cap = 0.3 / newSpeed;
				p.motionX *= cap;
				p.motionZ *= cap;
			}
		}
	}
}
