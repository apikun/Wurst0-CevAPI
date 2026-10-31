/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SetModeCmd (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import net.wurst0.WurstClient;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;
import net.wurst0.feature.Feature;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.Setting;

public final class SetModeCmd extends Command
{
	public SetModeCmd()
	{
		super("setmode",
			"Changes a mode setting. Usage: .setmode <hack> <mode setting> <value>");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 3)
			throw new CmdException(
				"Usage: .setmode <hack> <mode setting> <value>");

		Feature feature = findFeature(args[0]);
		Setting setting = findSetting(feature, args[1]);

		if(!(setting instanceof ModeSetting))
			throw new CmdException("\"" + args[1]
				+ "\" is not a mode setting.");

		ModeSetting mode = (ModeSetting)setting;
		mode.setSelected(args[2]);
		WurstClient.INSTANCE.getCmdProcessor().chat(
			args[0] + " " + args[1] + " is now \"" + mode.getSelected()
				+ "\".");
	}

	static Feature findFeature(String name) throws CmdException
	{
		Hack hack = WurstClient.INSTANCE.getHacks().getByName(name);
		if(hack != null)
			return hack;
		Command cmd = WurstClient.INSTANCE.getCmds().getByName(name);
		if(cmd != null)
			return cmd;
		throw new CmdException("No feature named \"" + name
			+ "\" exists.");
	}

	static Setting findSetting(Feature feature, String settingName)
		throws CmdException
	{
		for(Setting setting : feature.getSettings())
			if(setting.getName().equalsIgnoreCase(settingName))
				return setting;
		throw new CmdException("No setting named \"" + settingName
			+ "\" on " + feature.getName() + ".");
	}
}
