/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.settings;

/**
 * A free-text setting. Edited through a text field in the ClickGUI
 * (click the row, type, press Enter or Esc to finish). Unblocks
 * hacks like ChatSpam/CommandSpam that need custom text input.
 */
public final class TextFieldSetting extends Setting
{
	private String value;
	private final int maxLength;

	public TextFieldSetting(String name, String value)
	{
		this(name, value, 256);
	}

	public TextFieldSetting(String name, String value, int maxLength)
	{
		super(name);
		this.value = value;
		this.maxLength = maxLength;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		if(value == null)
			value = "";
		if(value.length() > maxLength)
			value = value.substring(0, maxLength);
		this.value = value;
	}

	public int getMaxLength()
	{
		return maxLength;
	}
}
