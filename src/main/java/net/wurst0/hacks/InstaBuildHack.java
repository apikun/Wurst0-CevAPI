/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI InstaBuildHack (GPL-3.0-or-later)
 * Lite adaptation: builds simple templates instantly using
 * creative-mode block setting (needs creative or /give access).
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Places a whole template around you at once (one block per tick,
 * starting from the bottom). Templates are face-relative.
 */
public final class InstaBuildHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting template = new ModeSetting("Template",
                new String[]{"Floor 9x9", "Walls 9x9", "Pillar 5"}, 0);
        private final SliderSetting perTick =
                new SliderSetting("Blocks per tick", 4, 1, 16, 1);

        private final List<BlockPos> queue = new ArrayList<BlockPos>();

        public InstaBuildHack()
        {
                super("InstaBuild",
                        "Builds a template (floor / walls / pillar) around"
                                + " you, a few blocks per tick.");
                setCategory(Category.BLOCKS);
                addSettings(template, perTick);
        }

        private void buildQueue(EntityPlayerSP p)
        {
                queue.clear();
                BlockPos base = new BlockPos(p.posX, p.posY, p.posZ);
                String t = template.getSelected();

                if(t.equals("Floor 9x9"))
                {
                        for(int dx = -4; dx <= 4; dx++)
                                for(int dz = -4; dz <= 4; dz++)
                                        queue.add(base.add(dx, -1, dz));
                }
                else if(t.equals("Walls 9x9"))
                {
                        for(int dx = -4; dx <= 4; dx++)
                                for(int dy = 0; dy < 3; dy++)
                                {
                                        queue.add(base.add(dx, dy, -4));
                                        queue.add(base.add(dx, dy, 4));
                                }
                        for(int dz = -3; dz <= 3; dz++)
                                for(int dy = 0; dy < 3; dy++)
                                {
                                        queue.add(base.add(-4, dy, dz));
                                        queue.add(base.add(4, dy, dz));
                                }
                }
                else
                {
                        for(int dy = 0; dy < 5; dy++)
                                queue.add(base.add(2, dy, 0));
                }
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(queue.isEmpty())
                        buildQueue(p);

                int slot = findBlockSlot(p);
                if(slot == -1)
                        return;

                int placed = 0;
                while(placed < perTick.getValueI() && !queue.isEmpty())
                {
                        BlockPos pos = queue.get(0);
                        IBlockState state = mc.world.getBlockState(pos);
                        if(!state.getMaterial().isReplaceable()
                                && state.getMaterial()
                                        != net.minecraft.block.material.Material.AIR)
                        {
                                queue.remove(0);
                                continue;
                        }

                        if(p.inventory.currentItem != slot)
                        {
                                p.inventory.currentItem = slot;
                                mc.playerController.updateController();
                                return;
                        }

                        mc.playerController.processRightClickBlock(p, mc.world, pos, EnumFacing.UP, Vec3d.ZERO, EnumHand.MAIN_HAND);
                        queue.remove(0);
                        placed++;
                }
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
