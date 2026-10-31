/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.util;

import net.minecraft.client.Minecraft;

/**
 * Shared reflection calls into Minecraft's private mouse handling.
 * Works in both dev (MCP names) and the reobfuscated jar (SRG names).
 */
public final class ReflectCalls
{
	private static java.lang.reflect.Method leftClick;
	private static java.lang.reflect.Method rightClick;

	private ReflectCalls()
	{}

	public static void leftClickMouse()
	{
		if(leftClick == null)
			leftClick = ReflectUtils.method(Minecraft.class,
				"leftClickMouse", "func_147116_af");
		if(leftClick == null)
			return;
		try
		{
			leftClick.invoke(Minecraft.getMinecraft());
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void rightClickMouse()
	{
		if(rightClick == null)
			rightClick = ReflectUtils.method(Minecraft.class,
				"rightClickMouse", "func_147121_ag");
		if(rightClick == null)
			return;
		try
		{
			rightClick.invoke(Minecraft.getMinecraft());
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
