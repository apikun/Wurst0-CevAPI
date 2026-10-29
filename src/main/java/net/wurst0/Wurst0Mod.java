/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 * https://github.com/apikun/Wurst0-CevAPI
 */
package net.wurst0;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "wurst0", name = "Wurst0", version = Wurst0Mod.VERSION,
	clientSideOnly = true, acceptedMinecraftVersions = "[1.12.2]")
public final class Wurst0Mod
{
	public static final String VERSION = "0.1.0";

	@Mod.EventHandler
	public void init(FMLInitializationEvent event)
	{
		WurstClient.INSTANCE.init();
	}
}
