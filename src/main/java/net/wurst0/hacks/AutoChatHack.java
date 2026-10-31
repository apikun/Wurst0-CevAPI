/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoChatHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Sends chat messages from a list at a fixed interval, rotating
 * through them. Messages are separated by "|". Useful for keeping
 * activity-based servers happy without typing.
 */
public final class AutoChatHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final TextFieldSetting messages =
		new TextFieldSetting("Messages", "hello|gg|nice build", 512);
	private final SliderSetting interval =
		new SliderSetting("Interval (s)", 30, 1, 600, 1);
	private final CheckboxSetting randomOrder =
		new CheckboxSetting("Random order", false);

	private int timer;
	private int index;

	public AutoChatHack()
	{
		super("AutoChat",
			"Sends chat messages from a list at a fixed interval.");
		setCategory(Category.CHAT);
		addSettings(messages, interval, randomOrder);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;

		if(timer > 0)
		{
			timer--;
			return;
		}

		String raw = messages.getValue();
		if(raw.trim().isEmpty())
			return;
		String[] list = raw.split("\\|");
		if(list.length == 0)
			return;

		String msg;
		if(randomOrder.isChecked())
			msg = list[java.util.concurrent.ThreadLocalRandom.current()
				.nextInt(list.length)].trim();
		else
		{
			msg = list[index % list.length].trim();
			index = (index + 1) % list.length;
		}
		if(!msg.isEmpty())
			p.sendChatMessage(msg);

		timer = (int)Math.round(interval.getValue() * 20.0);
	}

	@Override
	protected void onEnable()
	{
		timer = 0;
		index = 0;
	}
}
