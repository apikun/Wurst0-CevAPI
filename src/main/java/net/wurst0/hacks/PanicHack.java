/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PanicHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.wurst0.Category;
import net.wurst0.WurstClient;
import net.wurst0.feature.Hack;

public final class PanicHack extends Hack
{
	public PanicHack()
	{
		super("Panic",
			"Instantly turns off all enabled hacks.");
		setCategory(Category.OTHER);
	}

	@Override
	protected void onEnable()
	{
		List<Hack> toDisable =
			new ArrayList<Hack>(WurstClient.INSTANCE.getHacks().getList());
		for(Hack hack : toDisable)
		{
			if(hack == this || !hack.isEnabled())
				continue;
			hack.setEnabled(false);
		}
		setEnabled(false);
	}
}
