/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI InstantCommandHack (GPL-3.0-or-later)
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
 * Sends a command immediately after joining a server (after a short
 * delay so the server is ready). Leading slash is optional.
 */
public final class InstantCommandHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final TextFieldSetting command =
		new TextFieldSetting("Command", "spawn", 256);
	private final SliderSetting delay =
		new SliderSetting("Delay (s)", 5, 0, 60, 0.5);
	private final CheckboxSetting oncePerJoin =
		new CheckboxSetting("Once per join", true);

	private boolean wasInGame;
	private int ticksSinceJoin;
	private boolean sentThisJoin;

	public InstantCommandHack()
	{
		super("InstantCommand",
			"Runs a command automatically right after joining a"
				+ " server.");
		setCategory(Category.CHAT);
		addSettings(command, delay, oncePerJoin);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		boolean inGame = p != null && mc.world != null;

		if(inGame && !wasInGame)
		{
			// just joined
			ticksSinceJoin = 0;
			sentThisJoin = false;
		}

		if(inGame && !sentThisJoin)
		{
			if(ticksSinceJoin >= delay.getValue() * 20.0)
			{
				String cmd = command.getValue().trim();
				if(!cmd.isEmpty())
				{
					if(!cmd.startsWith("/"))
						cmd = "/" + cmd;
					p.sendChatMessage(cmd);
				}
				sentThisJoin = true;
				if(oncePerJoin.isChecked())
				{
					// stay armed for the next join
				}
			}else
				ticksSinceJoin++;
		}

		wasInGame = inGame;
	}

	@Override
	protected void onEnable()
	{
		ticksSinceJoin = 0;
		sentThisJoin = false;
		wasInGame = mc.player != null;
		if(wasInGame)
			sentThisJoin = true; // don't fire mid-session on first enable
	}
}
