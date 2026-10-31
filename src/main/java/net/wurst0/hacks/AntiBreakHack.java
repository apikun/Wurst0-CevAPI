/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiBreakHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemTool;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Stops you from using your tools up: when the held tool is within
 * N uses of breaking, block breaking is aborted. (The original also
 * guards other item uses; on 1.12.2 the break path is the main one
 * that silently destroys tools.)
 */
public final class AntiBreakHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting threshold =
		new SliderSetting("Threshold", 5, 1, 50, 1);

	private boolean warned;

	public AntiBreakHack()
	{
		super("AntiBreak",
			"Stops you from mining when your tool is about to break.");
		setCategory(Category.ITEMS);
		addSetting(threshold);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;

		ItemStack held = p.inventory.getCurrentItem();
		if(held.isEmpty() || !(held.getItem() instanceof ItemTool))
		{
			warned = false;
			return;
		}

		int left = held.getMaxDamage() - held.getItemDamage();
		if(left > threshold.getValueI())
		{
			warned = false;
			return;
		}

		// tool is about to break: abort any dig in progress
		if(mc.playerController.getIsHittingBlock())
		{
			mc.playerController.resetBlockRemoving();
			if(!warned)
			{
				p.sendMessage(new TextComponentString(
					"\u00a7c[AntiBreak]\u00a7r Tool has only " + left
						+ " uses left - stopped mining."));
				warned = true;
			}
		}
	}
}
