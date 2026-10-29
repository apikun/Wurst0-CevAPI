/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.event.events;

public final class KeyEvent
{
	private final int keyCode;
	private final boolean state;

	public KeyEvent(int keyCode, boolean state)
	{
		this.keyCode = keyCode;
		this.state = state;
	}

	public int getKeyCode()
	{
		return keyCode;
	}

	public boolean getState()
	{
		return state;
	}
}
