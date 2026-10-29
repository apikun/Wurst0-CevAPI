/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.command;

public class CmdException extends Exception
{
	public CmdException()
	{
		super();
	}

	public CmdException(String message)
	{
		super(message);
	}
}
