/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.settings;

public abstract class Setting
{
	private final String name;

	public Setting(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
}
