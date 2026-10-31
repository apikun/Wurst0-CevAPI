/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.event.events;

/**
 * Bridged from Forge's EntityViewRenderEvent.FogDensity. Cancelling
 * overrides the fog density with the value set here (NoFog sets 0).
 */
public final class FogEvent
{
	private float density;
	private boolean cancelled;

	public float getDensity()
	{
		return density;
	}

	public void setDensity(float density)
	{
		this.density = density;
	}

	public boolean isCancelled()
	{
		return cancelled;
	}

	public void setCancelled(boolean cancelled)
	{
		this.cancelled = cancelled;
	}
}
