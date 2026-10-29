/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import net.minecraft.client.Minecraft;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;

public final class SayCmd extends Command
{
	public SayCmd()
	{
		super("say", "Sends a message as if you typed it. Usage: .say <msg>");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1)
			throw new CmdException("Usage: .say <msg>");

		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < args.length; i++)
		{
			if(i > 0)
				sb.append(" ");
			sb.append(args[i]);
		}

		if(Minecraft.getMinecraft().player == null)
			throw new CmdException("You must be in a world to use .say.");

		Minecraft.getMinecraft().player.sendChatMessage(sb.toString());
	}
}
