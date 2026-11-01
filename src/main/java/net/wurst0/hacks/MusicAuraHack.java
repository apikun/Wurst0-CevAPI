/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MusicAuraHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.block.BlockNote;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Plays the note blocks around you by right-clicking one of them at
 * random every few ticks.
 */
public final class MusicAuraHack extends Hack implements UpdateListener
{
        private static final int RANGE = 6;

        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 4, 1, 20, 1);
        private final CheckboxSetting checkLOS =
                new CheckboxSetting("Check line of sight", true);

        private final List<BlockPos> noteBlocks =
                new ArrayList<BlockPos>();
        private int cooldown;
        private int scanTimer;

        public MusicAuraHack()
        {
                super("MusicAura",
                        "Right-clicks random note blocks around you so"
                                + " they play their notes.");
                setCategory(Category.BLOCKS);
                addSettings(delay, checkLOS);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                {
                        noteBlocks.clear();
                        return;
                }

                if(cooldown > 0)
                {
                        cooldown--;
                        return;
                }

                if(--scanTimer <= 0)
                {
                        scanTimer = 10;
                        scanNoteBlocks(p);
                }

                if(noteBlocks.isEmpty())
                        return;

                BlockPos target = noteBlocks.get(ThreadLocalRandom.current()
                        .nextInt(noteBlocks.size()));

                if(checkLOS.isChecked()
                        && !canSee(p, target))
                        return;

                mc.playerController.processRightClickBlock(p, mc.world,
                        target, EnumFacing.UP, Vec3d.ZERO,
                        EnumHand.MAIN_HAND);
                p.swingArm(EnumHand.MAIN_HAND);
                cooldown = delay.getValueI();
        }

        private void scanNoteBlocks(EntityPlayerSP p)
        {
                noteBlocks.clear();
                BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
                double rangeSq = RANGE * RANGE;

                for(BlockPos pos : BlockPos.getAllInBox(
                        center.add(-RANGE, -RANGE, -RANGE),
                        center.add(RANGE, RANGE, RANGE)))
                {
                        IBlockState state = mc.world.getBlockState(pos);
                        if(!(state.getBlock() instanceof BlockNote))
                                continue;
                        if(p.getDistanceSq(pos.getX() + 0.5,
                                pos.getY() + 0.5, pos.getZ() + 0.5) > rangeSq)
                                continue;
                        noteBlocks.add(pos);
                }
        }

        private boolean canSee(EntityPlayerSP p, BlockPos pos)
        {
                Vec3d eyes = new Vec3d(p.posX,
                        p.posY + p.getEyeHeight(), p.posZ);
                RayTraceResult trace = mc.world.rayTraceBlocks(eyes,
                        new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5,
                                pos.getZ() + 0.5));
                return trace == null || trace.typeOfHit
                        != RayTraceResult.Type.BLOCK
                        || trace.getBlockPos().equals(pos);
        }

        @Override
        protected void onEnable()
        {
                cooldown = 0;
                scanTimer = 0;
                noteBlocks.clear();
        }

        @Override
        protected void onDisable()
        {
                noteBlocks.clear();
        }
}
