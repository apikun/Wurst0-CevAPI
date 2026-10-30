/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SpawnerEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.AxisAlignedBB;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

public final class SpawnerEspHack extends Hack implements RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 64, 8, 128, 8);

	public SpawnerEspHack()
	{
		super("SpawnerESP",
			"Draws a box around every mob spawner so you can see them"
				+ " through walls.");
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
			if(!(te instanceof TileEntityMobSpawner))
				continue;
			if(te.getDistanceSq(mc.player.posX, mc.player.posY,
				mc.player.posZ) > rangeSq)
				continue;

			AxisAlignedBB box = te.getRenderBoundingBox();
			if(box == null)
				continue;

			RenderUtils.drawBox(box, 1F, 0.3F, 0.3F, 0.5F);
		}

		RenderUtils.disable3D(false);
	}
}
