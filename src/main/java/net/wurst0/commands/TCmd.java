/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import net.wurst0.WurstClient;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;
import net.wurst0.feature.Hack;

public final class TCmd extends Command
{
	public TCmd()
	{
		super("t",
			"Toggles a hack. Usage: .t <hack> [on|off]");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1)
			throw new CmdException("Usage: .t <hack> [on|off]");

		Hack hack = WurstClient.INSTANCE.getHacks().getByName(args[0]);
		if(hack == null)
			throw new CmdException("No hack named \"" + args[0] + "\" exists.");

		if(args.length == 2)
		{
			if(args[1].equalsIgnoreCase("on"))
				hack.setEnabled(true);
			else if(args[1].equalsIgnoreCase("off"))
				hack.setEnabled(false);
			else
				throw new CmdException(
					"Expected on or off, got \"" + args[1] + "\".");
		}else
			hack.toggle();

		WurstClient.INSTANCE.getCmdProcessor().chat(hack.getName()
			+ (hack.isEnabled() ? " is now on." : " is now off."));
	}
}
