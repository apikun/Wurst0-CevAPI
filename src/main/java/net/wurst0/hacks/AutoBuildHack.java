/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoBuildHack (GPL-3.0-or-later)
 * Lite adaptation: face-relative templates placed one block per
 * tick with support-face detection (InstantBunker engine).
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Iterator;
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
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

public final class AutoBuildHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting template = new ModeSetting("Template",
                new String[]{"Box 5x5", "Wall 9x3", "Tower 3x3",
                        "Peninsula"}, 0);
        private final SliderSetting perTick =
                new SliderSetting("Blocks per tick", 1, 1, 4, 1);
        private final CheckboxSetting checkRotation =
                new CheckboxSetting("Rotate with player", true);

        private final List<BlockPos> queue = new ArrayList<BlockPos>();
        private int rotIndex;

        public AutoBuildHack()
        {
                super("AutoBuild",
                        "Builds a small template around you, one block per"
                                + " tick, with auto tool/block selection.");
                setCategory(Category.BLOCKS);
                addSettings(template, perTick, checkRotation);
        }

        private void enqueue(EntityPlayerSP p)
        {
                queue.clear();
                BlockPos base = new BlockPos(MathHelper.floor(p.posX),
                        MathHelper.floor(p.posY), MathHelper.floor(p.posZ));

                String t = template.getSelected();
                if(t.equals("Box 5x5"))
                {
                        for(int dy = 0; dy < 2; dy++)
                        {
                                for(int dx = -2; dx <= 2; dx++)
                                {
                                        queue.add(base.add(dx, dy, -2));
                                        queue.add(base.add(dx, dy, 2));
                                }
                                for(int dz = -1; dz <= 1; dz++)
                                {
                                        queue.add(base.add(-2, dy, dz));
                                        queue.add(base.add(2, dy, dz));
                                }
                        }
                }
                else if(t.equals("Wall 9x3"))
                {
                        for(int dy = 0; dy < 3; dy++)
                                for(int dx = -4; dx <= 4; dx++)
                                        queue.add(base.add(dx, dy, -3));
                }
                else if(t.equals("Tower 3x3"))
                {
                        for(int dy = 0; dy < 8; dy++)
                        {
                                for(int dx = -1; dx <= 1; dx++)
                                {
                                        queue.add(base.add(dx, dy, -1));
                                        queue.add(base.add(dx, dy, 1));
                                }
                                queue.add(base.add(-1, dy, 0));
                                queue.add(base.add(1, dy, 0));
                        }
                }
                else
                {
                        // Peninsula: floor strip with walls on one side
                        for(int dx = 0; dx < 6; dx++)
                        {
                                queue.add(base.add(dx, -1, 0));
                                if(dx > 0 && dx < 5)
                                {
                                        queue.add(base.add(dx, 0, -1));
                                        queue.add(base.add(dx, 1, -1));
                                }
                        }
                }

                if(checkRotation.isChecked())
                {
                        // rotate 90° steps to match where the player looks
                        float yaw = p.rotationYaw;
                        int steps = Math.round(yaw / 90F) & 3;
                        rotIndex = steps;
                        if(steps != 0)
                        {
                                List<BlockPos> rotated =
                                        new ArrayList<BlockPos>();
                                for(BlockPos pos : queue)
                                        rotated.add(rotateAround(pos, base,
                                                steps));
                                queue.clear();
                                queue.addAll(rotated);
                        }
                }
        }

        private static BlockPos rotateAround(BlockPos pos, BlockPos origin,
                int steps)
        {
                int dx = pos.getX() - origin.getX();
                int dz = pos.getZ() - origin.getZ();
                for(int i = 0; i < steps; i++)
                {
                        int tmp = dx;
                        dx = -dz;
                        dz = tmp;
                }
                return origin.add(dx, pos.getY() - origin.getY(), dz);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(queue.isEmpty())
                        enqueue(p);

                int max = perTick.getValueI();
                int placed = 0;

                Iterator<BlockPos> itr = queue.iterator();
                while(itr.hasNext() && placed < max)
                {
                        BlockPos pos = itr.next();
                        IBlockState state = mc.world.getBlockState(pos);
                        if(state.getMaterial() != Material.AIR
                                && !state.getMaterial().isReplaceable())
                        {
                                itr.remove();
                                continue;
                        }

                        int slot = findBlockSlot(p);
                        if(slot == -1)
                                return;

                        if(p.inventory.currentItem != slot)
                        {
                                p.inventory.currentItem = slot;
                                mc.playerController.updateController();
                                return;
                        }

                        EnumFacing side = findSupportSide(p, pos);
                        if(side == null)
                                continue; // no support yet, try later

                        mc.playerController.processRightClickBlock(p, mc.world, pos.offset(side), side.getOpposite(), Vec3d.ZERO, EnumHand.MAIN_HAND);
                        p.swingArm(EnumHand.MAIN_HAND);
                        itr.remove();
                        placed++;
                }
        }

        private EnumFacing findSupportSide(EntityPlayerSP p, BlockPos pos)
        {
            for(EnumFacing side : EnumFacing.VALUES)
            {
                IBlockState neighbor =
                        mc.world.getBlockState(pos.offset(side));
                if(neighbor.isSideSolid(mc.world, pos.offset(side),
                        side.getOpposite()))
                        return side;
            }
            return null;
        }

        private int findBlockSlot(EntityPlayerSP p)
        {
                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(!stack.isEmpty()
                                && stack.getItem() instanceof ItemBlock)
                                return i;
                }
                return -1;
        }

        @Override
        protected void onEnable()
        {
                queue.clear();
        }

        @Override
        protected void onDisable()
        {
                queue.clear();
        }
}
