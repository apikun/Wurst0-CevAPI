/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoPlayerChatHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.events.ChatOutputEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.event.listeners.ChatOutputListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class NoPlayerChatHack extends Hack
	implements ChatInputListener, ChatOutputListener
{
	private final CheckboxSetting allowOwn =
		new CheckboxSetting("Allow own messages", true);

	private String lastSent = "";

	public NoPlayerChatHack()
	{
		super("NoPlayerChat",
			"Hides all chat messages from other players. Only system"
				+ " messages and (optionally) your own messages are"
				+ " shown.");
		setCategory(Category.CHAT);
		addSetting(allowOwn);
	}

	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		if(allowOwn.isChecked() && !lastSent.isEmpty()
			&& event.getText().contains(lastSent))
			return;

		event.setCancelled(true);
	}

	@Override
	public void onSentMessage(ChatOutputEvent event)
	{
		lastSent = event.getText();
		if(lastSent.length() > 48)
			lastSent = lastSent.substring(0, 48);
	}
}
