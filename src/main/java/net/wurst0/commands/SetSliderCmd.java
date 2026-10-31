/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SetSliderCmd (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import net.wurst0.WurstClient;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;
import net.wurst0.feature.Feature;
import net.wurst0.settings.Setting;
import net.wurst0.settings.SliderSetting;

public final class SetSliderCmd extends Command
{
	public SetSliderCmd()
	{
		super("setslider",
			"Changes a slider setting. Usage: .setslider <hack> <slider> <value>");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 3)
			throw new CmdException(
				"Usage: .setslider <hack> <slider> <value>");

		Feature feature = SetModeCmd.findFeature(args[0]);
		Setting setting = SetModeCmd.findSetting(feature, args[1]);

		if(!(setting instanceof SliderSetting))
			throw new CmdException("\"" + args[1]
				+ "\" is not a slider setting.");

		double value;
		try
		{
			value = Double.parseDouble(args[2]);
		}catch(NumberFormatException e)
		{
			throw new CmdException(
				"\"" + args[2] + "\" is not a number.");
		}

		SliderSetting slider = (SliderSetting)setting;
		slider.setValue(value);
		WurstClient.INSTANCE.getCmdProcessor().chat(
			args[0] + " " + args[1] + " is now " + slider.getValue()
				+ ".");
	}
}
