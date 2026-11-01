/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TunnelHoleStairEspHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;

/**
 * Highlights holes you could fall into and staircase shapes in the
 * terrain around you. Useful when caving without a torch supply.
 */
public final class TunnelHoleStairEspHack extends Hack
        implements UpdateListener, RenderListener
{
        private static final class Entry
        {
                final BlockPos pos;
                final boolean hole;

                Entry(BlockPos pos, boolean hole)
                {
                        this.pos = pos;
                        this.hole = hole;
                }
        }

        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 10, 4, 16, 1);

        private final List<Entry> entries = new ArrayList<Entry>();
        private int scanTimer;

        public TunnelHoleStairEspHack()
        {
                super("TunnelHoleStairEsp",
                        "Highlights dangerous holes (red) and staircases"
                                + " (green) near you.");
                setCategory(Category.RENDER);
                addSetting(range);
        }

        private boolean isPassable(BlockPos pos)
        {
                return mc.world.getBlockState(pos).getMaterial()
                        == Material.AIR;
        }

        private boolean isSolid(BlockPos pos)
        {
                IBlockState state = mc.world.getBlockState(pos);
                return state.getMaterial() != Material.AIR
                        && !state.getMaterial().isLiquid()
                        && state.isFullBlock();
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(--scanTimer > 0)
                        return;
                scanTimer = 30;

                entries.clear();
                int r = range.getValueI();
                BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
                int holes = 0;
                int stairs = 0;

                for(BlockPos pos : BlockPos.getAllInBox(
                        center.add(-r, -2, -r), center.add(r, 2, r)))
                {
                        if(!isPassable(pos) || !isPassable(pos.up()))
                                continue;
                        if(!isSolid(pos.down()))
                                continue;

                        // hole: 3+ deep air column below the surface
                        if(holes < 150 && isPassable(pos.down())
                                && isPassable(pos.down(2))
                                && isPassable(pos.down(3)))
                        {
                                entries.add(new Entry(pos, true));
                                holes++;
                                continue;
                        }

                        // staircase: two consecutive step-ups in one direction
                        if(stairs < 150)
                        {
                                for(EnumFacing dir : EnumFacing.HORIZONTALS)
                                {
                                        BlockPos step1 = pos.offset(dir).up();
                                        BlockPos step2 =
                                                pos.offset(dir, 2).up(2);
                                        if(isSolid(step1.down())
                                                && isPassable(step1)
                                                && isPassable(step1.up())
                                                && isSolid(step2.down())
                                                && isPassable(step2)
                                                && isPassable(step2.up()))
                                        {
                                                entries.add(
                                                        new Entry(pos, false));
                                                stairs++;
                                                break;
                                        }
                                }
                        }
                }
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(entries.isEmpty() || mc.player == null)
                        return;

                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                for(Entry entry : entries)
                {
                        if(entry.hole)
                                RenderUtils.drawBox(new AxisAlignedBB(entry.pos),
                                        1F, 0.15F, 0.15F, 0.55F);
                        else
                                RenderUtils.drawBox(new AxisAlignedBB(entry.pos),
                                        0.2F, 1F, 0.3F, 0.55F);
                }
                RenderUtils.disable3D(false);
        }

        @Override
        protected void onEnable()
        {
                scanTimer = 0;
                entries.clear();
        }

        @Override
        protected void onDisable()
        {
                entries.clear();
        }
}
