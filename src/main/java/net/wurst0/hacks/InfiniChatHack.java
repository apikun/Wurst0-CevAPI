/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI InfiniChatHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.util.ReflectUtils;

/**
 * Raises the 256-character limit of the vanilla chat input field to
 * 2000 characters while enabled. The server still only accepts what
 * it supports, but this lets you prepare long messages / paste text.
 */
public final class InfiniChatHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();
	private Field inputField;

	public InfiniChatHack()
	{
		super("InfiniChat",
			"Removes the 256-character limit from the chat input field"
				+ " (the server still only receives what it accepts).");
		setCategory(Category.CHAT);
	}

	@Override
	public void onUpdate()
	{
		if(!(mc.currentScreen instanceof GuiChat))
			return;

		try
		{
			if(inputField == null)
				inputField = ReflectUtils.field(GuiChat.class,
					"inputField", "field_146415_a");
			if(inputField == null)
				return;

			Object screen = mc.currentScreen;
			GuiTextField tf = (GuiTextField)inputField.get(screen);
			if(tf != null && tf.getMaxStringLength() < 2000)
				tf.setMaxStringLength(2000);
		}catch(Exception e)
		{
			// reflection failed on this screen - just do nothing
		}
	}
}
