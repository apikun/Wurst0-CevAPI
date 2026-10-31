/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI GameStatsHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.wurst0.Category;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Small performance HUD: FPS, ping to the server, and the server
 * brand. Lite port of the original FPS/TPS/MSPT panel (TPS/MSPT are
 * not available client-side on 1.12.2 remote servers).
 */
public final class GameStatsHack extends Hack implements ScreenRenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final CheckboxSetting showFps =
		new CheckboxSetting("Show FPS", true);
	private final CheckboxSetting showPing =
		new CheckboxSetting("Show ping", true);
	private final CheckboxSetting showBrand =
		new CheckboxSetting("Show brand", false);
	private final SliderSetting fontScale =
		new SliderSetting("Font size", 1.0, 0.5, 2, 0.05);

	public GameStatsHack()
	{
		super("GameStats",
			"Shows FPS and ping (and optionally the server brand)"
				+ " on screen.");
		setCategory(Category.TOOLS);
		addSettings(showFps, showPing, showBrand, fontScale);
	}

	@Override
	public void onScreenRender(float partialTicks)
	{
		if(mc.player == null || mc.isGamePaused())
			return;

		ScaledResolution sr = new ScaledResolution(mc);
		int y = 2;

		if(showFps.isChecked())
			y = line(sr, y, "FPS: " + Minecraft.getDebugFPS());
		if(showPing.isChecked())
		{
			Integer ping = getPing();
			y = line(sr, y,
				"Ping: " + (ping == null ? "?" : ping + " ms"));
		}
		if(showBrand.isChecked())
		{
			String brand = mc.player.getServerBrand();
			y = line(sr, y,
				"Brand: " + (brand == null ? "?" : brand));
		}
	}

	private int line(ScaledResolution sr, int y, String text)
	{
		float s = fontScale.getValueF();
		int w = (int)(mc.fontRenderer.getStringWidth(text) * s);
		int x = sr.getScaledWidth() / 2 - w / 2;
		Gui.drawRect(x - 1, y - 1, x + w + 1, y + (int)(10 * s) - 1,
			0x80000000);
		mc.fontRenderer.drawStringWithShadow(text, x, y, 0xFFFFFF);
		return y + (int)(10 * s);
	}

	private Integer getPing()
	{
		if(mc.getConnection() == null)
			return null;
		NetworkPlayerInfo info =
			mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
		return info == null ? null : Integer.valueOf(
			info.getResponseTime());
	}
}
