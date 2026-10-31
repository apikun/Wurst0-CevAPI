/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NameProtectHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.feature.Hack;

/**
 * Replaces your own name (case-insensitive) in received chat with
 * "Player", so screenshots don't leak your username. Protects
 * received messages only; outgoing messages are unchanged.
 */
public final class NameProtectHack extends Hack
	implements ChatInputListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public NameProtectHack()
	{
		super("NameProtect",
			"Hides your name in chat messages (useful for screenshots"
				+ " and streaming).");
		setCategory(Category.CHAT);
	}

	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		String name = mc.getSession().getUsername();
		if(name == null || name.isEmpty())
			return;

		Pattern pattern = Pattern.compile(Pattern.quote(name),
			Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(event.getText());
		if(matcher.find())
			event.setText(matcher.replaceAll("Player"));
	}
}
