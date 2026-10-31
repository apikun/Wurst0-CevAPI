/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.event.events;

/**
 * Fired when the vanilla first-person hand is about to be rendered.
 * Cancelling hides the hand (HandNoClip).
 */
public final class HandRenderEvent
{
	private boolean cancelled;

	public boolean isCancelled()
	{
		return cancelled;
	}

	public void setCancelled(boolean cancelled)
	{
		this.cancelled = cancelled;
	}
}
