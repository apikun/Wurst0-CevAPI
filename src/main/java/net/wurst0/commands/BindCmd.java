/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import org.lwjgl.input.Keyboard;

import net.wurst0.WurstClient;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;
import net.wurst0.feature.Hack;

public final class BindCmd extends Command
{
	public BindCmd()
	{
		super("bind",
			"Binds a hack to a key. Usage: .bind <hack> <key|none>");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdException("Usage: .bind <hack> <key|none>");

		Hack hack = WurstClient.INSTANCE.getHacks().getByName(args[0]);
		if(hack == null)
			throw new CmdException("No hack named \"" + args[0] + "\" exists.");

		if(args[1].equalsIgnoreCase("none"))
		{
			hack.setKeybind(-1);
			save();
			WurstClient.INSTANCE.getCmdProcessor()
				.chat(hack.getName() + " is no longer bound to any key.");
			return;
		}

		int keyCode = Keyboard.getKeyIndex(args[1].toUpperCase());
		if(keyCode == Keyboard.KEY_NONE)
			throw new CmdException(
				"Unknown key: " + args[1] + ". Try names like R, G, LSHIFT.");

		hack.setKeybind(keyCode);
		save();
		WurstClient.INSTANCE.getCmdProcessor().chat(
			hack.getName() + " is now bound to "
				+ Keyboard.getKeyName(keyCode) + ".");
	}

	private void save()
	{
		WurstClient.INSTANCE.getFileManager()
			.saveKeybinds(WurstClient.INSTANCE.getHacks());
	}
}
