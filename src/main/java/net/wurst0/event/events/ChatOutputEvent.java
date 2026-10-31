/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.event.events;

/**
 * Fired when the client is about to send a chat message.
 * The text may be modified or the event cancelled.
 */
public final class ChatOutputEvent
{
	private String text;
	private boolean cancelled;

	public ChatOutputEvent(String text)
	{
		this.text = text;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
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
