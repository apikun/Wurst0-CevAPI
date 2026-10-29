/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.clickgui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiScreen;
import net.wurst0.WurstClient;

public final class ClickGuiScreen extends GuiScreen
{
	private final ClickGuiManager manager;

	public ClickGuiScreen()
	{
		manager = WurstClient.INSTANCE.getClickGuiManager();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		drawDefaultBackground();
		for(ClickGuiWindow window : manager.getWindows())
			window.render(mouseX, mouseY);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
		throws IOException
	{
		for(ClickGuiWindow window : manager.getWindows())
			window.mouseClicked(mouseX, mouseY, mouseButton);
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state)
	{
		for(ClickGuiWindow window : manager.getWindows())
			window.mouseReleased(mouseX, mouseY, state);
		super.mouseReleased(mouseX, mouseY, state);
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY,
		int clickedMouseButton, long timeSinceLastClick)
	{
		for(ClickGuiWindow window : manager.getWindows())
			window.mouseClickMove(mouseX, mouseY, clickedMouseButton,
				timeSinceLastClick);
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton,
			timeSinceLastClick);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException
	{
		if(keyCode == Keyboard.KEY_ESCAPE
			|| keyCode == Keyboard.KEY_RSHIFT)
		{
			mc.displayGuiScreen(null);
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public boolean doesGuiPauseGame()
	{
		return false;
	}
}
