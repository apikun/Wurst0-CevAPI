/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI HideWurstHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.feature.Hack;

/**
 * The HUD (watermark + arraylist) checks this hack and hides itself
 * while it is enabled. Bind it to a key for quick screenshots.
 */
public final class HideWurstHack extends Hack
{
	public HideWurstHack()
	{
		super("HideWurst",
			"Hides the Wurst0 watermark and hack list, e.g. for"
				+ " screenshots or recording.");
		setCategory(Category.OTHER);
	}
}
