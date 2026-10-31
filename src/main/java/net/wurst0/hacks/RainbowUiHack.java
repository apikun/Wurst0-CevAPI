/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI RainbowUiHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.feature.Hack;

/**
 * The rendering hooks live in ClickGuiWindow (rainbow title bars) and
 * Hud (rainbow watermark) - they both check for this hack being
 * enabled and shift the hue over time.
 */
public final class RainbowUiHack extends Hack
{
	public RainbowUiHack()
	{
		super("RainbowUi",
			"Renders the ClickGUI title bars and the HUD watermark in"
				+ " rainbow colors.");
		setCategory(Category.FUN);
	}
}
