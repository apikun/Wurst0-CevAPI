/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.keybinds;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.wurst0.WurstClient;
import net.wurst0.clickgui.ClickGuiScreen;
import net.wurst0.feature.Hack;

public final class KeybindManager
{
	/** Right Shift by default, matching Wurst's ClickGUI bind. */
	public static final int CLICK_GUI_KEY = Keyboard.KEY_RSHIFT;

	private final Minecraft mc = Minecraft.getMinecraft();

	public void handleKeyPress(int keyCode)
	{
		if(keyCode == Keyboard.KEY_NONE)
			return;

		if(keyCode == CLICK_GUI_KEY)
		{
			GuiScreen screen = mc.currentScreen;
			if(screen instanceof ClickGuiScreen)
				mc.displayGuiScreen(null);
			else if(screen == null && mc.player != null)
				mc.displayGuiScreen(new ClickGuiScreen());
			return;
		}

		for(Hack hack : WurstClient.INSTANCE.getHacks().getList())
			if(hack.getKeybind() == keyCode)
				hack.toggle();
	}
}
