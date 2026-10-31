/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SetCheckboxCmd (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import net.wurst0.WurstClient;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;
import net.wurst0.feature.Feature;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.Setting;

public final class SetCheckboxCmd extends Command
{
	public SetCheckboxCmd()
	{
		super("setcheckbox",
			"Changes a checkbox setting. Usage: .setcheckbox <hack> <checkbox> <on|off>");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 3)
			throw new CmdException(
				"Usage: .setcheckbox <hack> <checkbox> <on|off>");

		Feature feature = SetModeCmd.findFeature(args[0]);
		Setting setting = SetModeCmd.findSetting(feature, args[1]);

		if(!(setting instanceof CheckboxSetting))
			throw new CmdException("\"" + args[1]
				+ "\" is not a checkbox setting.");

		if(args[2].equalsIgnoreCase("on"))
			((CheckboxSetting)setting).setChecked(true);
		else if(args[2].equalsIgnoreCase("off"))
			((CheckboxSetting)setting).setChecked(false);
		else
			throw new CmdException(
				"Expected on or off, got \"" + args[2] + "\".");

		WurstClient.INSTANCE.getCmdProcessor().chat(
			args[0] + " " + args[1] + " is now "
				+ (((CheckboxSetting)setting).isChecked() ? "on"
					: "off")
				+ ".");
	}
}
