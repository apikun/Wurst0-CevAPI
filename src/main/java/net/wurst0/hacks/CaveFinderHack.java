/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 * TODO: port logic from net/wurstclient/hacks/CaveFinderHack.java in the original
 * repo (MC 26.3 / Fabric). This stub only registers the hack so it
 * appears in the GUI; it currently does nothing when enabled.
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.feature.Hack;

public final class CaveFinderHack extends Hack
{
	public CaveFinderHack()
	{
		super("CaveFinder", "[not ported yet]");
		setCategory(Category.INTEL);
	}
}
