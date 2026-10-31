/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FancyChatHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.event.events.ChatOutputEvent;
import net.wurst0.event.listeners.ChatOutputListener;
import net.wurst0.feature.Hack;

/**
 * Rewrites outgoing chat into fullwidth unicode, so your messages
 * are not picked up by most chat filters / spam bots.
 */
public final class FancyChatHack extends Hack implements ChatOutputListener
{
	public FancyChatHack()
	{
		super("FancyChat",
			"Rewrites your chat messages into fullwidth unicode to"
				+ " bypass simple chat filters.");
		setCategory(Category.CHAT);
	}

	@Override
	public void onSentMessage(ChatOutputEvent event)
	{
		String text = event.getText();
		if(text.startsWith("/"))
			return;

		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);
			if(c >= '!' && c <= '~')
				c = (char)(c - '!' + 0xFF01);
			else if(c == ' ')
				c = 0x3000;
			builder.append(c);
		}
		event.setText(builder.toString());
	}
}
