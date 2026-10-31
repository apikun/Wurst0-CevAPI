/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiDropHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.feature.Hack;

/**
 * The actual drop cancellation lives in ForgeEvents.onItemToss(),
 * which cancels Forge's ItemTossEvent for this client player when
 * this hack is enabled (cancelling also suppresses the C07 packet).
 */
public final class AntiDropHack extends Hack
{
	public AntiDropHack()
	{
		super("AntiDrop",
			"Prevents you from dropping items by pressing the drop"
				+ " key (Q) or clicking them outside of an inventory.");
		setCategory(Category.ITEMS);
	}
}
