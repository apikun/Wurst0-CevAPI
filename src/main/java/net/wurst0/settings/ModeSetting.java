/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.settings;

public final class ModeSetting extends Setting
{
	private final String[] modes;
	private int index;

	public ModeSetting(String name, String[] modes, int selected)
	{
		super(name);
		this.modes = modes;
		index = selected;
	}

	public String[] getModes()
	{
		return modes;
	}

	public String getSelected()
	{
		return modes[index];
	}

	public int getIndex()
	{
		return index;
	}

	public void setSelected(String mode)
	{
		for(int i = 0; i < modes.length; i++)
			if(modes[i].equalsIgnoreCase(mode))
			{
				index = i;
				return;
			}
	}

	public void cycle()
	{
		index = (index + 1) % modes.length;
	}
}
