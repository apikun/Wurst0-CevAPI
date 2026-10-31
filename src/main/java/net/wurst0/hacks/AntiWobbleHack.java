/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiWobbleHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.init.MobEffects;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * Removes the nausea effect client-side each tick, which stops the
 * screen wobble. The server can re-apply the potion, but the wobble
 * is client-simulated so this is enough to get rid of it.
 */
public final class AntiWobbleHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public AntiWobbleHack()
	{
		super("AntiWobble",
			"Removes the wobble effect from nausea client-side.");
		setCategory(Category.RENDER);
	}

	@Override
	public void onUpdate()
	{
		if(mc.player == null)
			return;

		mc.player.removeActivePotionEffect(MobEffects.NAUSEA);
	}
}
