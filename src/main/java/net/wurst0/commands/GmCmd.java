/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI GmCmd (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import net.minecraft.client.Minecraft;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;

public final class GmCmd extends Command
{
	public GmCmd()
	{
		super("gm",
			"Switches your gamemode (server-side). Usage: .gm <0-3|s|c|a|sp>");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 1)
			throw new CmdException("Usage: .gm <0-3|s|c|a|sp>");

		String mode;
		switch(args[0].toLowerCase())
		{
			case "0":
			case "s":
				mode = "survival";
				break;
			case "1":
			case "c":
				mode = "creative";
				break;
			case "2":
			case "a":
				mode = "adventure";
				break;
			case "3":
			case "sp":
				mode = "spectator";
				break;
			default:
				throw new CmdException("Unknown gamemode \"" + args[0]
					+ "\".");
		}

		Minecraft mc = Minecraft.getMinecraft();
		if(mc.player == null)
			throw new CmdException("Not connected to a server.");

		mc.player.sendChatMessage("/gamemode " + mode);
	}
}
