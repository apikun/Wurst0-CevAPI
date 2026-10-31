/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI BaseFinderHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Highlights man-made blocks (planks, glass, wool, chests, torches
 * ...) to reveal hidden bases. Uses the same cached scan pattern as
 * Search; "Underground only" filters out surface builds.
 */
public final class BaseFinderHack extends Hack
        implements UpdateListener, RenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 16, 8, 32, 4);
        private final CheckboxSetting undergroundOnly =
                new CheckboxSetting("Underground only", true);

        private static final Set<Block> BASE_BLOCKS = new HashSet<Block>(
                Arrays.asList(
                        Blocks.PLANKS, Blocks.STONEBRICK, Blocks.COBBLESTONE,
                        Blocks.MOSSY_COBBLESTONE, Blocks.GLASS, Blocks.STAINED_GLASS,
                        Blocks.GLASS_PANE, Blocks.STAINED_GLASS_PANE, Blocks.TORCH,
                        Blocks.FURNACE, Blocks.LIT_FURNACE, Blocks.CRAFTING_TABLE,
                        Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST,
                        Blocks.BOOKSHELF, Blocks.BRICK_BLOCK, Blocks.QUARTZ_BLOCK,
                        Blocks.SANDSTONE, Blocks.ANVIL, Blocks.ENCHANTING_TABLE,
                        Blocks.BREWING_STAND, Blocks.BED, Blocks.LADDER,
                        Blocks.IRON_BARS, Blocks.GLOWSTONE, Blocks.SEA_LANTERN,
                        Blocks.LIT_PUMPKIN, Blocks.WOOL, Blocks.CARPET,
                        Blocks.HARDENED_CLAY, Blocks.STAINED_HARDENED_CLAY,
                        Blocks.NOTEBLOCK, Blocks.JUKEBOX, Blocks.DISPENSER,
                        Blocks.HOPPER, Blocks.OBSERVER, Blocks.GRASS_PATH));

        private final List<BlockPos> found = new ArrayList<BlockPos>();
        private int scanTimer;
        private net.minecraft.world.World lastWorld;

        public BaseFinderHack()
        {
                super("BaseFinder",
                        "Highlights man-made blocks to help you find hidden"
                                + " bases.");
                setCategory(Category.INTEL);
                addSettings(range, undergroundOnly);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(mc.world != lastWorld)
                {
                        lastWorld = mc.world;
                        scanTimer = 0;
                }

                if(--scanTimer > 0)
                        return;
                scanTimer = 60;

                found.clear();
                int r = range.getValueI();
                BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
                Iterator<BlockPos> itr = BlockPos.getAllInBox(
                        center.add(-r, -r, -r), center.add(r, r, r)).iterator();

                while(itr.hasNext())
                {
                        BlockPos pos = itr.next();
                        if(undergroundOnly.isChecked() && mc.world.canSeeSky(pos))
                                continue;

                        IBlockState state = mc.world.getBlockState(pos);
                        if(BASE_BLOCKS.contains(state.getBlock()))
                                found.add(pos);
                }
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(mc.player == null || mc.world == null || found.isEmpty())
                        return;

                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);

                for(BlockPos pos : found)
                        RenderUtils.drawBox(new net.minecraft.util.math.AxisAlignedBB(
                                pos.getX(), pos.getY(), pos.getZ(),
                                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
                                1F, 0.6F, 0.2F, 0.4F);

                RenderUtils.disable3D(false);
        }

        @Override
        protected void onEnable()
        {
                scanTimer = 0;
                found.clear();
        }

        @Override
        protected void onDisable()
        {
                found.clear();
        }
}
