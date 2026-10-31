/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.event.events;

/**
 * Fired before a vanilla HUD overlay element is drawn (bridged from
 * Forge's RenderGameOverlayEvent.Pre). Cancelling hides that element,
 * which is how NoVignette / NoOverlay work.
 */
public final class OverlayEvent
{
	private final String type;
	private boolean cancelled;

	public OverlayEvent(String type)
	{
		this.type = type;
	}

	public String getType()
	{
		return type;
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
