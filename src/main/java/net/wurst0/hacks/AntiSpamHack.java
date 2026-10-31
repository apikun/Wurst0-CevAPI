/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiSpamHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.event.events.ChatInputEvent;
import net.wurst0.event.listeners.ChatInputListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class AntiSpamHack extends Hack implements ChatInputListener
{
	private final SliderSetting threshold =
		new SliderSetting("Repeat count", 3, 2, 10, 1);

	private String lastRaw;
	private int repeats;

	public AntiSpamHack()
	{
		super("AntiSpam",
			"Collapses repeated chat messages into a single message"
				+ " with a repeat counter.");
		setCategory(Category.CHAT);
		addSetting(threshold);
	}

	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		String text = event.getText();
		if(text == null || text.isEmpty())
			return;

		if(text.equals(lastRaw))
		{
			repeats++;
			if(repeats + 1 >= threshold.getValueI())
				event.setText(stripCount(text) + " (x"
					+ (repeats + 1) + ")");
			return;
		}

		lastRaw = text;
		repeats = 1;
	}

	private static String stripCount(String s)
	{
		int start = s.lastIndexOf(" (x");
		int end = s.lastIndexOf(')');
		if(start > 0 && end > start)
			return s.substring(0, start);
		return s;
	}

	@Override
	protected void onEnable()
	{
		lastRaw = null;
		repeats = 0;
	}
}
