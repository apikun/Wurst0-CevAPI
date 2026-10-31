/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SkyBuildEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Reveals floating builds above the height threshold (sky bases).
 * Column-scan like BaseFinder (cheaper than a 3D box), with optional
 * chat alerts when new blocks appear.
 */
public final class SkyBuildEspHack extends Hack implements UpdateListener,
	RenderListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting yThreshold =
		new SliderSetting("Y threshold", 128, 64, 250, 4);
	private final SliderSetting range =
		new SliderSetting("Range", 128, 32, 256, 16);
	private final CheckboxSetting chatAlerts =
		new CheckboxSetting("Chat alerts", true);

	private final List<BlockPos> found = new ArrayList<BlockPos>();
	private int scanTimer;
	private int dimension;

	public SkyBuildEspHack()
	{
		super("SkyBuildESP",
			"Reveals sky bases: blocks floating above the Y"
				+ " threshold.");
		setCategory(Category.RENDER);
		addSettings(yThreshold, range, chatAlerts);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
		{
			found.clear();
			return;
		}

		if(p.dimension != dimension)
		{
			dimension = p.dimension;
			found.clear();
		}

		if(scanTimer++ % 40 != 0)
			return;
		scan(p);
	}

	private void scan(EntityPlayerSP p)
	{
		List<BlockPos> newFound = new ArrayList<BlockPos>();
		int yMin = yThreshold.getValueI();
		int r = range.getValueI();
		int x0 = (int)p.posX, z0 = (int)p.posZ;

		// scan a horizontal grid of columns down from their heightmap
		// top - far cheaper than a full 3D box
		int step = 6;
		for(int x = x0 - r; x <= x0 + r; x += step)
		{
			for(int z = z0 - r; z <= z0 + r; z += step)
			{
				BlockPos top =
					mc.world.getHeight(new BlockPos(x, 0, z));
				int topY = top.getY();
				if(topY < yMin)
					continue;
				for(int y = yMin; y <= topY; y++)
				{
					BlockPos pos = new BlockPos(x, y, z);
					IBlockState state = mc.world.getBlockState(pos);
					Material m = state.getMaterial();
					if(m == Material.AIR || m.isLiquid())
						continue;
					newFound.add(pos);
				}
			}
		}

		if(chatAlerts.isChecked() && !newFound.isEmpty()
			&& found.size() < newFound.size())
		{
			BlockPos first = newFound.get(0);
			p.sendMessage(new TextComponentString(
				"\u00a7c[SkyBuildESP]\u00a7r " + newFound.size()
					+ " blocks above Y" + yMin + " near \u00a7e"
					+ first.getX() + " " + first.getY() + " "
					+ first.getZ()));
		}

		found.clear();
		found.addAll(newFound);
	}

	@Override
	public void onRender(float partialTicks)
	{
		if(mc.player == null || mc.world == null || found.isEmpty())
			return;

		RenderUtils.enable3D(1, false);
		RenderUtils.applyCameraTransform(partialTicks);

		int shown = 0;
		for(BlockPos pos : found)
		{
			if(shown++ > 4096)
				break;
			AxisAlignedBB box = new AxisAlignedBB(pos);
			RenderUtils.drawBox(box, 1F, 0.25F, 0.25F, 0.55F);
		}

		RenderUtils.disable3D(false);
	}

	@Override
	protected void onDisable()
	{
		found.clear();
		scanTimer = 0;
	}
}
