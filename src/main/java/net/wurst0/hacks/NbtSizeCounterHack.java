/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NbtSizeCounterHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.wurst0.Category;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;

/**
 * Shows the size of the held item's NBT data in the bottom-left
 * corner. Useful to spot books/chests that are close to crashing a
 * server (or your client).
 */
public final class NbtSizeCounterHack extends Hack
	implements ScreenRenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public NbtSizeCounterHack()
	{
		super("NbtSizeCounter",
			"Shows how much NBT data the item you are holding contains.");
		setCategory(Category.TOOLS);
	}

	@Override
	public void onScreenRender(float partialTicks)
	{
		if(mc.player == null || mc.gameSettings.showDebugInfo)
			return;

		ItemStack stack = mc.player.getHeldItemMainhand();
		if(stack.isEmpty())
			return;

		NBTTagCompound tag = stack.getTagCompound();
		if(tag == null)
			return;

		int bytes = tag.toString().length();
		String text = "NBT: " + format(bytes);

		FontRenderer fr = mc.fontRenderer;
		ScaledResolution sr = new ScaledResolution(mc);
		int x = 2;
		int y = sr.getScaledHeight() - fr.FONT_HEIGHT - 2;
		fr.drawStringWithShadow(text, x, y, 0x8080FF);
	}

	private static String format(int bytes)
	{
		if(bytes >= 1048576)
			return String.format("%.2f MB", bytes / 1048576D);
		if(bytes >= 1024)
			return String.format("%.2f KB", bytes / 1024D);
		return bytes + " B";
	}
}
