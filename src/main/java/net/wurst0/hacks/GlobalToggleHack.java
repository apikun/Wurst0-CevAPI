/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI GlobalToggleHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.wurst0.Category;
import net.wurst0.WurstClient;
import net.wurst0.event.events.KeyEvent;
import net.wurst0.event.listeners.KeyPressListener;
import net.wurst0.feature.Hack;

/**
 * Bind this hack to a key with ".bind GlobalToggle <key>" to turn
 * off every hack at once while pressing that key. Disables itself
 * after use, like Panic.
 */
public final class GlobalToggleHack extends Hack
	implements KeyPressListener
{
	public GlobalToggleHack()
	{
		super("GlobalToggle",
			"Turns off all enabled hacks when the bound key is"
				+ " pressed. Use .bind GlobalToggle <key> to set the"
				+ " key.");
		setCategory(Category.OTHER);
	}

	@Override
	public void onKeyPress(int keyCode, boolean state)
	{
		if(!state)
			return;

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
