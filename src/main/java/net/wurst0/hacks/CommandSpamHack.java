/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI CommandSpamHack (GPL-3.0-or-later)
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
 * Spams a command at a fixed interval (like ChatSpam, but sends it
 * as a command). A leading "/" is added automatically if missing.
 */
public final class CommandSpamHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final TextFieldSetting command =
		new TextFieldSetting("Command", "/kit");
	private final SliderSetting interval =
		new SliderSetting("Interval (seconds)", 5, 1, 60, 1);
	private final CheckboxSetting addCounter =
		new CheckboxSetting("Add counter", false);

	private int timer;
	private int counter;

	public CommandSpamHack()
	{
		super("CommandSpam",
			"Spams a command at a fixed interval. Set the command in"
				+ " the field (click it, type, Enter). A leading / is"
				+ " added automatically.");
		setCategory(Category.CHAT);
		addSettings(command, interval, addCounter);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.getConnection() == null)
			return;

		if(--timer > 0)
			return;
		timer = interval.getValueI() * 20;

		String text = command.getValue().trim();
		if(text.isEmpty())
			return;
		if(!text.startsWith("/"))
			text = "/" + text;
		if(addCounter.isChecked())
			text = text + " " + (++counter);

		p.sendChatMessage(text);
	}

	@Override
	protected void onEnable()
	{
		timer = 0;
		counter = 0;
	}

	@Override
	protected void onDisable()
	{
		counter = 0;
	}
}
