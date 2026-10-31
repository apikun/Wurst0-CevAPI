/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI InstantBunkerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Builds a small 2x2 emergency bunker around you out of hotbar
 * blocks: a ring of walls two blocks up plus a roof. One block is
 * placed per tick (the original places the whole shape per click with
 * a packet burst that vanilla 1.12.2 servers reject).
 */
public final class InstantBunkerHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final CheckboxSetting roof =
		new CheckboxSetting("Roof", true);
	private final CheckboxSetting autoSwitch =
		new CheckboxSetting("Auto-switch", true);

	private final List<BlockPos> queue =
		new ArrayList<BlockPos>();
	private int originalSlot = -1;

	public InstantBunkerHack()
	{
		super("InstantBunker",
			"Builds a small emergency bunker around you out of your"
				+ " hotbar blocks.");
		setCategory(Category.BLOCKS);
		addSettings(roof, autoSwitch);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(queue.isEmpty())
		{
			planBunker(p);
			if(queue.isEmpty())
			{
				// nothing placeable at all
				finish(p);
				return;
			}
			originalSlot = p.inventory.currentItem;
		}

		while(!queue.isEmpty())
		{
			BlockPos pos = queue.get(0);
			// target spot must be replaceable/air by now
			IBlockState state = mc.world.getBlockState(pos);
			if(state.getMaterial() != Material.AIR
				&& !state.getMaterial().isReplaceable())
			{
				queue.remove(0);
				continue;
			}

			int slot = findBlockSlot(p);
			if(slot == -1)
			{
				finish(p);
				return;
			}

			if(p.inventory.currentItem != slot && autoSwitch.isChecked())
			{
				p.inventory.currentItem = slot;
				mc.playerController.updateController();
			}

			// click on the nearest solid neighbour as support
			BlockPos support = null;
			EnumFacing face = EnumFacing.UP;
			for(EnumFacing f : EnumFacing.values())
			{
				BlockPos n = pos.offset(f);
				IBlockState ns = mc.world.getBlockState(n);
				if(ns.getMaterial() != Material.AIR
					&& !ns.getMaterial().isLiquid()
					&& !ns.getMaterial().isReplaceable())
				{
					support = n;
					face = f.getOpposite();
					break;
				}
			}
			if(support == null)
			{
				queue.remove(0);
				continue;
			}

			Vec3d hitVec = new Vec3d(
				support.getX() + 0.5 + face.getXOffset() * 0.5,
				support.getY() + 0.5 + face.getYOffset() * 0.5,
				support.getZ() + 0.5 + face.getZOffset() * 0.5);
			mc.playerController.processRightClickBlock(p, mc.world,
				support, face, hitVec, EnumHand.MAIN_HAND);
			p.swingArm(EnumHand.MAIN_HAND);
			queue.remove(0);
			return; // one block per tick
		}

		if(queue.isEmpty())
			finish(p);
	}

	private void planBunker(EntityPlayerSP p)
	{
		int x = MathHelper.floor(p.posX);
		int y = MathHelper.floor(p.posY);
		int z = MathHelper.floor(p.posZ);

		// wall ring: two blocks tall, one block out from the player
		int[][] ring = new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2},
			{2, 2}, {2, -2}, {-2, 2}, {-2, -2}};
		for(int[] c : ring)
		{
			queue.add(new BlockPos(x + c[0], y, z + c[1]));
			queue.add(new BlockPos(x + c[0], y + 1, z + c[1]));
		}

		if(roof.isChecked())
		{
			queue.add(new BlockPos(x + 1, y + 2, z + 1));
			queue.add(new BlockPos(x + 1, y + 2, z - 1));
			queue.add(new BlockPos(x - 1, y + 2, z + 1));
			queue.add(new BlockPos(x - 1, y + 2, z - 1));
		}
	}

	@Override
	protected void onDisable()
	{
		finish(mc.player);
	}

	private void finish(EntityPlayerSP p)
	{
		queue.clear();
		if(p != null && originalSlot != -1
			&& autoSwitch.isChecked())
		{
			p.inventory.currentItem = originalSlot;
			mc.playerController.updateController();
		}
		originalSlot = -1;
		setEnabled(false);
	}

	private int findBlockSlot(EntityPlayerSP p)
	{
		ItemStack held = p.inventory.getCurrentItem();
		if(!held.isEmpty() && held.getItem() instanceof ItemBlock
			&& held.getCount() > 0)
			return p.inventory.currentItem;

		for(int i = 0; i < 9; i++)
		{
			ItemStack stack = p.inventory.mainInventory.get(i);
			if(!stack.isEmpty() && stack.getItem() instanceof ItemBlock)
				return i;
		}
		return -1;
	}
}
