/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import net.wurst0.WurstClient;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;
import net.wurst0.feature.Feature;

public final class HelpCmd extends Command
{
	public HelpCmd()
	{
		super("help", "Shows the list of Wurst0 commands.");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length == 0)
			list();
		else
		{
			Command cmd =
				WurstClient.INSTANCE.getCmds().getByName(args[0]);
			if(cmd == null)
				throw new CmdException("No command named \"" + args[0]
					+ "\" exists.");
			help(cmd);
		}
	}

	private void list()
	{
		for(Command cmd : WurstClient.INSTANCE.getCmds().getList())
		{
			String help = ".help " + cmd.getName().toLowerCase()
				+ " for more info.";
			WurstClient.INSTANCE.getCmdProcessor().chat(
				"." + cmd.getName().toLowerCase() + " - " + cmd.getDescription()
					+ " (" + help + ")");
		}
	}

	private void help(Command cmd)
	{
		WurstClient.INSTANCE.getCmdProcessor().chat(
			"." + cmd.getName().toLowerCase() + " - " + cmd.getDescription());
	}
}
