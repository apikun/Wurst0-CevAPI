/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0;

import java.awt.Color;

public enum Category
{
	FAVORITES("Favorites", 0x909090),
	BLOCKS("Blocks", 0x6070d0),
	MOVEMENT("Movement", 0x40c0d0),
	COMBAT("Combat", 0xd05050),
	RENDER("Render", 0xd08030),
	INTEL("Intel", 0x40d070),
	TOOLS("Tools", 0xd0c040),
	CHAT("Chat", 0x50a0d0),
	FUN("Fun", 0xd090d0),
	ITEMS("Items", 0x50b050),
	OTHER("Other", 0xb0b0b0),
	CREATIVE_OP("Creative/Op", 0x80e080),
	COMMANDS("Commands", 0x808080);

	private final String name;
	private final int color;

	Category(String name, int color)
	{
		this.name = name;
		this.color = color;
	}

	public String getName()
	{
		return name;
	}

	public int getColor()
	{
		return color;
	}

	public Color asAwtColor()
	{
		return new Color(color);
	}
}
