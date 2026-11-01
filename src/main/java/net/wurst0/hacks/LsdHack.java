/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI LsdHack (GPL-3.0-or-later)
 * Lite adaptation: 1.12.2 has no post-effect shader pipeline hook,
 * so instead of the "wurst:lsd" post shader this cycles a translucent
 * hue wash over the whole screen (drawn after the HUD) and pulses
 * the FOV setting for a wobbly feel.
 */
package net.wurst0.hacks;

import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.wurst0.Category;
import net.wurst0.WurstClient;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Turns your screen into a rainbow acid trip. Purely visual.
 * Photosensitive players: do not use.
 */
public final class LsdHack extends Hack
	implements UpdateListener, ScreenRenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.1, 5, 0.1);
	private final SliderSetting strength =
		new SliderSetting("Strength", 0.35, 0.05, 0.75, 0.05);
	private final CheckboxSetting fovPulse =
		new CheckboxSetting("FOV pulse", true);

	private float savedFov = -1;

	public LsdHack()
	{
		super("LSD",
			"Makes the screen swim with cycling colors and a wobbling"
				+ " FOV. Do not operate heavy machinery.");
		setCategory(Category.FUN);
		addSettings(speed, strength, fovPulse);
	}

	@Override
	protected void onEnable()
	{
		// disable incompatible hacks, same as upstream
		Hack remoteView =
			WurstClient.INSTANCE.getHacks().getByName("RemoteView");
		if(remoteView != null && remoteView.isEnabled())
			remoteView.setEnabled(false);

		if(mc.player == null)
		{
			setEnabled(false);
			return;
		}

		savedFov = mc.gameSettings.fovSetting;
	}

	@Override
	protected void onDisable()
	{
		if(savedFov >= 0)
			mc.gameSettings.fovSetting = savedFov;
		savedFov = -1;
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || !fovPulse.isChecked() || savedFov < 0)
			return;

		float t = (System.currentTimeMillis() % 100000L) / 1000F
			* speed.getValueF();
		mc.gameSettings.fovSetting =
			savedFov + (float)Math.sin(t * Math.PI) * 8F;
	}

	@Override
	public void onScreenRender(float partialTicks)
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;

		ScaledResolution sr = new ScaledResolution(mc);
		int w = sr.getScaledWidth();
		int h = sr.getScaledHeight();
		float t = (System.currentTimeMillis() % 100000L) / 1000F
			* speed.getValueF();

		// two complementary hue washes, fading in and out of each other
		float a = strength.getValueF();
		float wave = 0.75F + 0.25F * (float)Math.sin(t * 2F);

		int rgb1 = Color.HSBtoRGB((t * 0.11F) % 1F, 0.9F, 1F);
		int rgb2 = Color.HSBtoRGB((t * 0.11F + 0.5F) % 1F, 0.9F, 1F);

		int alpha1 = (int)(a * wave * 255F) & 0xFF;
		int alpha2 = (int)(a * (1.5F - wave) * 0.6F * 255F) & 0xFF;

		Gui.drawRect(0, 0, w, h, (alpha1 << 24) | (rgb1 & 0xFFFFFF));
		Gui.drawRect(0, 0, w, h, (alpha2 << 24) | (rgb2 & 0xFFFFFF));
	}
}
