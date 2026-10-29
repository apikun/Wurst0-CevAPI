/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ChestEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.math.AxisAlignedBB;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

public final class ChestEspHack extends Hack implements RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 64, 8, 128, 8);

	public ChestEspHack()
	{
		super("ChestESP",
			"Draws a box around every chest, ender chest and shulker"
				+ " box so you can see them through walls.");
		setCategory(Category.RENDER);
		addSetting(range);
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null)
			return;

		double rangeSq = range.getValue() * range.getValue();

		RenderUtils.enable3D(1.5F, false);
		RenderUtils.applyCameraTransform(partialTicks);

		List<TileEntity> tiles =
			new ArrayList<TileEntity>(mc.world.loadedTileEntityList);
		for(TileEntity te : tiles)
		{
			float r;
			float g;
			float b;

			if(te instanceof TileEntityChest)
			{
				r = 0.9F;
				g = 0.6F;
				b = 0.1F;
			}else if(te instanceof TileEntityEnderChest)
			{
				r = 0.4F;
				g = 0.1F;
				b = 0.9F;
			}else if(te instanceof TileEntityShulkerBox)
			{
				r = 0.6F;
				g = 0.3F;
				b = 0.8F;
			}else
				continue;

			if(te.getDistanceSq(mc.player.posX, mc.player.posY,
				mc.player.posZ) > rangeSq)
				continue;

			AxisAlignedBB box = te.getRenderBoundingBox();
			if(box == null)
				continue;

			RenderUtils.drawBox(box, r, g, b, 0.5F);
		}

		RenderUtils.disable3D(false);
	}
}
