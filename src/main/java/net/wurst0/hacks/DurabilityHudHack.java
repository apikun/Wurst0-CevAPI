/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI DurabilityHudHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.wurst0.Category;
import net.wurst0.event.events.ScreenRenderEvent;
import net.wurst0.event.listeners.ScreenRenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class DurabilityHudHack extends Hack
	implements ScreenRenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final CheckboxSetting showHand =
		new CheckboxSetting("Show hand items", true);

	public DurabilityHudHack()
	{
		super("DurabilityHUD",
			"Shows how much durability your armor and held items have"
				+ " left.");
		setCategory(Category.RENDER);
		addSetting(showHand);
	}

	private void addLine(FontRenderer fr, int x, int y, String name,
		ItemStack stack)
	{
		if(stack.isEmpty() || !stack.isItemStackDamageable())
			return;

		int max = stack.getMaxDamage();
		int left = max - stack.getItemDamage();
		int percent = left * 100 / max;

		int color;
		if(percent > 50)
			color = 0x40FF40;
		else if(percent > 20)
			color = 0xFFFF40;
		else
			color = 0xFF4040;

		fr.drawStringWithShadow(name + ": " + left + "/" + max, x, y,
			color);
	}

	@Override
	public void onScreenRender(float partialTicks)
	{
		if(mc.player == null || mc.gameSettings.showDebugInfo)
			return;

		FontRenderer fr = mc.fontRenderer;
		int x = 2;
		int y = 2;

		NonNullList<ItemStack> armor = mc.player.inventory.armorInventory;
		// helmet(3) down to boots(0)
		for(int i = 3; i >= 0; i--)
		{
			addLine(fr, x, y, armorName(i), armor.get(i));
			y += fr.FONT_HEIGHT + 1;
		}

		if(showHand.isChecked())
		{
			addLine(fr, x, y, "Main", mc.player.getHeldItemMainhand());
			y += fr.FONT_HEIGHT + 1;
			addLine(fr, x, y, "Off", mc.player.getHeldItemOffhand());
			y += fr.FONT_HEIGHT + 1;
		}
	}

	private static String armorName(int index)
	{
		switch(index)
		{
			case 3:
				return "Head";
			case 2:
				return "Body";
			case 1:
				return "Legs";
			default:
				return "Feet";
		}
	}
}
