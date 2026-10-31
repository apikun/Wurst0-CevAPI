/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoReconnectHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiConnecting;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class AutoReconnectHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting delay =
		new SliderSetting("Delay (seconds)", 5, 1, 60, 1);
	private final CheckboxSetting announce =
		new CheckboxSetting("Show countdown in chat", true);

	private int timer;

	public AutoReconnectHack()
	{
		super("AutoReconnect",
			"Automatically reconnects to the last server after"
				+ " being kicked.");
		setCategory(Category.OTHER);
		addSettings(delay, announce);
	}

	@Override
	public void onUpdate()
	{
		if(!(mc.currentScreen instanceof GuiDisconnected))
		{
			timer = 0;
			return;
		}

		timer++;

		int target = delay.getValueI() * 20;
		if(timer == target && announce.isChecked() && mc.player != null)
			mc.player.sendMessage(new TextComponentString(
				"[Wurst0] Reconnecting..."));

		if(timer >= target)
		{
			timer = 0;
			ServerData data = mc.getCurrentServerData();
			if(data == null)
				return;
			mc.displayGuiScreen(
				new GuiConnecting(mc.currentScreen, mc, data));
		}
	}

	@Override
	protected void onDisable()
	{
		timer = 0;
	}
}
