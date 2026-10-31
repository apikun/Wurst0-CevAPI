/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.WurstClient;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;
import net.wurst0.hacks.WaypointsHack;
import net.wurst0.hacks.WaypointsHack.Waypoint;

/**
 * Manages WaypointsHack's waypoint list: .wp add <name>, .wp del
 * <name>, .wp list.
 */
public final class WaypointsCmd extends Command
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public WaypointsCmd()
	{
		super("wp",
			"Manages waypoints: .wp add <name>, .wp del <name>,"
				+ " .wp list.");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		WaypointsHack hack = (WaypointsHack)WurstClient.INSTANCE
			.getHacks().getByName("Waypoints");
		if(hack == null)
			throw new CmdException("Waypoints hack not found");

		if(args.length < 1)
			throw new CmdException(
				"Usage: .wp add <name> / .wp del <name> / .wp list");

		String sub = args[0].toLowerCase();
		if(sub.equals("list"))
		{
			List<Waypoint> list = hack.parseWaypoints();
			if(list.isEmpty())
			{
				say("\u00a77No waypoints set.");
				return;
			}
			for(Waypoint wp : list)
				say("\u00a75" + wp.name + "\u00a7r " + wp.pos.getX()
					+ " " + wp.pos.getY() + " " + wp.pos.getZ());
			return;
		}

		if(sub.equals("add"))
		{
			if(args.length < 2)
				throw new CmdException("Usage: .wp add <name>");
			StringBuilder name = new StringBuilder();
			for(int i = 1; i < args.length; i++)
			{
				if(name.length() > 0)
					name.append('_');
				name.append(args[i]);
			}

			BlockPos pos;
			if(mc.player != null)
				pos = new BlockPos(mc.player.posX, mc.player.posY,
					mc.player.posZ);
			else
				pos = new BlockPos(0, 0, 0);

			List<Waypoint> list =
				new ArrayList<Waypoint>(hack.parseWaypoints());
			list.add(new Waypoint(name.toString(), pos));
			hack.setWaypoints(list);
			say("\u00a7aWaypoint \u00a75" + name + "\u00a7a set at "
				+ pos.getX() + " " + pos.getY() + " " + pos.getZ());
			return;
		}

		if(sub.equals("del") || sub.equals("remove"))
		{
			if(args.length < 2)
				throw new CmdException("Usage: .wp del <name>");
			String target = args[1];
			List<Waypoint> list =
				new ArrayList<Waypoint>(hack.parseWaypoints());
			boolean removed = false;
			Iterator<Waypoint> itr = list.iterator();
			while(itr.hasNext())
			{
				if(itr.next().name.equalsIgnoreCase(target))
				{
					itr.remove();
					removed = true;
				}
			}
			if(!removed)
				throw new CmdException(
					"No waypoint named \"" + target + "\"");
			hack.setWaypoints(list);
			say("\u00a7aWaypoint \u00a75" + target + "\u00a7a removed.");
			return;
		}

		throw new CmdException(
			"Unknown subcommand. Use add / del / list.");
	}

	private static void say(String text)
	{
		if(Minecraft.getMinecraft().player != null)
			Minecraft.getMinecraft().player.sendMessage(
				new TextComponentString(text));
	}
}
