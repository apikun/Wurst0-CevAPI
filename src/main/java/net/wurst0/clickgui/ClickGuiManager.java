/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.clickgui;

import java.util.ArrayList;
import java.util.List;

import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.hacks.Hacks;

public final class ClickGuiManager
{
	private final List<ClickGuiWindow> windows = new ArrayList<ClickGuiWindow>();

	public ClickGuiManager(Hacks hacks)
	{
		int i = 0;
		for(Category category : Category.values())
		{
			if(category == Category.FAVORITES || category == Category.COMMANDS)
				continue;

			List<Hack> categoryHacks = new ArrayList<Hack>();
			for(Hack hack : hacks.getList())
				if(hack.getCategory() == category)
					categoryHacks.add(hack);

			if(categoryHacks.isEmpty())
				continue;

			int x = 8 + i * 26;
			int y = 8 + i * 26;
			windows.add(new ClickGuiWindow(category, x, y, categoryHacks));
			i++;
		}
	}

	public List<ClickGuiWindow> getWindows()
	{
		return windows;
	}
}
