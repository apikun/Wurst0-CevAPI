/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.command;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.wurst0.WurstClient;
import net.wurst0.commands.CmdList;
import net.wurst0.feature.Command;

public final class CmdProcessor
{
	public static final String PREFIX = ".";

	private final CmdList cmds;
	private final Minecraft mc = Minecraft.getMinecraft();

	public CmdProcessor(CmdList cmds)
	{
		this.cmds = cmds;
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onChatMessage(ClientChatEvent event)
	{
		String message = event.getMessage().trim();
		if(!message.startsWith(PREFIX))
			return;

		event.setCanceled(true);

		String input = message.substring(PREFIX.length()).trim();
		if(input.isEmpty())
		{
			error("Unknown command. Type " + PREFIX + "help for a list.");
			return;
		}

		String[] parts = input.split("\\s+");
		Command cmd = cmds.getByName(parts[0].toLowerCase());
		if(cmd == null)
		{
			error("Unknown command: " + PREFIX + parts[0] + ". Type "
				+ PREFIX + "help for a list.");
			return;
		}

		String[] args = Arrays.copyOfRange(parts, 1, parts.length);
		try
		{
			cmd.call(args);
		}catch(CmdException e)
		{
			error(e.getMessage());
		}catch(Exception e)
		{
			error("An unexpected error occurred in " + PREFIX + cmd.getName()
				+ ".");
			e.printStackTrace();
		}
	}

	public void error(String text)
	{
		chat(TextFormatting.RED + text);
	}

	public void chat(String text)
	{
		if(mc.player == null)
			return;
		mc.player.sendMessage(new TextComponentString(
			TextFormatting.GOLD + "[" + TextFormatting.BOLD + "Wurst0"
				+ TextFormatting.RESET + TextFormatting.GOLD + "] "
				+ TextFormatting.RESET + text));
	}
}
