/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PlayerMuteHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.TextFieldSetting;

/**
 * Hides all chat messages from one player. Matches the classic
 * "<Name> message" format used by vanilla and most plugins.
 */
public final class PlayerMuteHack extends Hack implements ChatInputListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final TextFieldSetting playerName =
		new TextFieldSetting("Player name", "");

	public PlayerMuteHack()
	{
		super("PlayerMute",
			"Hides all chat messages from the player set in the name"
				+ " field (click it, type, Enter).");
		setCategory(Category.CHAT);
		addSetting(playerName);
	}

	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		String muted = playerName.getValue().trim();
		if(muted.isEmpty())
			return;

		String text = event.getText();
		if(text == null)
			return;

		// vanilla format "<Name> text"; plugins keep the same shape
		String tag = "<" + muted + ">";
		if(text.toLowerCase().contains(tag.toLowerCase()))
			event.setCancelled(true);
	}
}
