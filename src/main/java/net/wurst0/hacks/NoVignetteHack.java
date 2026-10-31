/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoVignetteHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.event.events.OverlayEvent;
import net.wurst0.event.listeners.OverlayListener;
import net.wurst0.feature.Hack;

public final class NoVignetteHack extends Hack implements OverlayListener
{
	public NoVignetteHack()
	{
		super("NoVignette",
			"Removes the black vignette overlay around the edges of"
				+ " the screen.");
		setCategory(Category.RENDER);
	}

	@Override
	public void onOverlayRender(OverlayEvent event)
	{
		if("VIGNETTE".equals(event.getType()))
			event.setCancelled(true);
	}
}
