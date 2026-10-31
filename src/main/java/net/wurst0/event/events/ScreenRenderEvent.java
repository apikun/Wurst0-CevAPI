/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.event.events;

/**
 * Fired after the HUD is drawn, while still in screen space.
 * Used by 2D overlays such as Radar.
 */
public final class ScreenRenderEvent
{
	private final float partialTicks;

	public ScreenRenderEvent(float partialTicks)
	{
		this.partialTicks = partialTicks;
	}

	public float getPartialTicks()
	{
		return partialTicks;
	}
}
