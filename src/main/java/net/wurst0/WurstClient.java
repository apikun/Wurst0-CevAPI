/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0;

import net.minecraftforge.common.MinecraftForge;
import net.wurst0.clickgui.ClickGuiManager;
import net.wurst0.command.CmdProcessor;
import net.wurst0.commands.CmdList;
import net.wurst0.config.FileManager;
import net.wurst0.event.EventManager;
import net.wurst0.hacks.Hacks;
import net.wurst0.hud.Hud;
import net.wurst0.keybinds.KeybindManager;

public final class WurstClient
{
	public static final WurstClient INSTANCE = new WurstClient();

	public static final String VERSION = Wurst0Mod.VERSION;
	public static final String MC_VERSION = "1.12.2";

	private EventManager eventManager;
	private Hacks hacks;
	private CmdList cmds;
	private CmdProcessor cmdProcessor;
	private KeybindManager keybindManager;
	private FileManager fileManager;
	private ClickGuiManager clickGuiManager;
	private Hud hud;

	private WurstClient()
	{}

	public void init()
	{
		eventManager = new EventManager();
		fileManager = new FileManager();
		hacks = new Hacks();
		cmds = new CmdList();
		cmdProcessor = new CmdProcessor(cmds);
		keybindManager = new KeybindManager();
		clickGuiManager = new ClickGuiManager(hacks);
		hud = new Hud();
		MinecraftForge.EVENT_BUS.register(new ForgeEvents());

		fileManager.load(hacks);
	}

	public EventManager getEventManager()
	{
		return eventManager;
	}

	public Hacks getHacks()
	{
		return hacks;
	}

	public CmdList getCmds()
	{
		return cmds;
	}

	public CmdProcessor getCmdProcessor()
	{
		return cmdProcessor;
	}

	public KeybindManager getKeybindManager()
	{
		return keybindManager;
	}

	public FileManager getFileManager()
	{
		return fileManager;
	}

	public ClickGuiManager getClickGuiManager()
	{
		return clickGuiManager;
	}

	public Hud getHud()
	{
		return hud;
	}
}
