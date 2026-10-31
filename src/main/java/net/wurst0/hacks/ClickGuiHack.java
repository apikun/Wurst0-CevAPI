/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ClickGuiHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.wurst0.Category;
import net.wurst0.clickgui.ClickGuiScreen;
import net.wurst0.feature.Hack;

/**
 * Opens the ClickGUI when enabled. Because it disables itself right
 * away, you can bind it to any key with ".bind ClickGUI <key>"
 * (Right Shift is hardcoded in the keybind manager as well).
 */
public final class ClickGuiHack extends Hack
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public ClickGuiHack()
	{
		super("ClickGUI",
			"Opens the ClickGUI. Can be bound to a key of your"
				+ " choice with .bind.");
		setCategory(Category.OTHER);
	}

	@Override
	protected void onEnable()
	{
		if(mc.player == null)
		{
			setEnabled(false);
			return;
		}

		mc.displayGuiScreen(new ClickGuiScreen());
		setEnabled(false);
	}
}
