/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoFogHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.event.events.FogEvent;
import net.wurst0.event.listeners.FogListener;
import net.wurst0.feature.Hack;

/**
 * 1.12.2 limitation: Forge only fires FogDensity for the
 * exponential fog used inside liquids and certain blocks, so this
 * removes water/lava/blindness fog. The distance-based linear fog
 * cannot be removed without a mixin (not implemented yet).
 */
public final class NoFogHack extends Hack implements FogListener
{
	public NoFogHack()
	{
		super("NoFog",
			"Disables the fog experienced in water, lava and other"
				+ " blocks.");
		setCategory(Category.RENDER);
	}

	@Override
	public void onFogDensity(FogEvent event)
	{
		event.setDensity(0);
		event.setCancelled(true);
	}
}
