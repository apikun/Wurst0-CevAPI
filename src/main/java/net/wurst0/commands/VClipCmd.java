/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI VClipCmd (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;

public final class VClipCmd extends Command
{
	public VClipCmd()
	{
		super("vclip",
			"Teleports you vertically. Usage: .vclip <blocks>"
				+ " (negative = down)");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 1)
			throw new CmdException("Usage: .vclip <blocks>");

		double blocks;
		try
		{
			blocks = Double.parseDouble(args[0]);
		}catch(NumberFormatException e)
		{
			throw new CmdException(
				"\"" + args[0] + "\" is not a number.");
		}

		if(Math.abs(blocks) > 256)
			throw new CmdException("Maximum distance is 256 blocks.");

		EntityPlayerSP p = Minecraft.getMinecraft().player;
		if(p == null)
			throw new CmdException("Not in a world.");

		p.setPositionAndUpdate(p.posX, p.posY + blocks, p.posZ);
	}
}
