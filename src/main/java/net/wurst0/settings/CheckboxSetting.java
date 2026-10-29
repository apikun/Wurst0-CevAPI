/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.settings;

public final class CheckboxSetting extends Setting
{
	private boolean checked;

	public CheckboxSetting(String name, boolean checked)
	{
		super(name);
		this.checked = checked;
	}

	public boolean isChecked()
	{
		return checked;
	}

	public void setChecked(boolean checked)
	{
		this.checked = checked;
	}

	public void toggle()
	{
		checked = !checked;
	}
}
