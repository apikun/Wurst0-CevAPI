/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoSpawnProoferHack (GPL-3.0-or-later)
 * Lite adaptation: 1.12.2 spawn rules via WorldEntitySpawner plus
 * block-light scanning instead of the 26.x SpawnPlacements API.
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Highlights tiles where hostile mobs can currently spawn (spawn
 * placement + light rules of 1.12.2) and optionally places light
 * sources from the hotbar on them.
 */
public final class AutoSpawnProoferHack extends Hack
        implements UpdateListener, RenderListener
{
        private static final Block[] LIGHT_BLOCKS =
                {Blocks.TORCH, Blocks.GLOWSTONE, Blocks.LIT_PUMPKIN,
                        Blocks.SEA_LANTERN, Blocks.END_ROD};

        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 5, 1, 7, 1);
        private final SliderSetting lightLevel =
                new SliderSetting("Light level", 7, 0, 7, 1);
        private final CheckboxSetting checkSky =
                new CheckboxSetting("Check sky light", true);
        private final CheckboxSetting autoPlace =
                new CheckboxSetting("Auto place light", true);

        private final List<BlockPos> targets =
                new ArrayList<BlockPos>();
        private int scanTimer;

        public AutoSpawnProoferHack()
        {
                super("AutoSpawnProofer",
                        "Highlights tiles where hostile mobs can spawn"
                                + " and can automatically place torches on"
                                + " them to proof the area.");
                setCategory(Category.BLOCKS);
                addSettings(range, lightLevel, checkSky, autoPlace);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                {
                        targets.clear();
                        return;
                }

                if(--scanTimer > 0)
                {
                        if(autoPlace.isChecked())
                                placeLight(p);
                        return;
                }
                scanTimer = 4;

                targets.clear();
                int r = range.getValueI();
                BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);

                for(BlockPos pos : BlockPos.getAllInBox(
                        center.add(-r, -r, -r), center.add(r, r, r)))
                {
                        if(!isSpawnable(pos))
                                continue;
                        targets.add(pos);
                        if(targets.size() >= 400)
                                break;
                }

                if(autoPlace.isChecked())
                        placeLight(p);
        }

        private boolean isSpawnable(BlockPos pos)
        {
                IBlockState state = mc.world.getBlockState(pos);
                if(state.getMaterial() != net.minecraft.block.material.Material.AIR
                        && !state.getMaterial().isReplaceable())
                        return false;

                // 1.12.2 hostile spawn rule: solid spawn ground, passable
                // tile, passable tile above, not bedrock/barrier.
                if(!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(
                        EntityLiving.SpawnPlacementType.ON_GROUND, mc.world,
                        pos))
                        return false;

                int blockLight = mc.world.getLightFromNeighborsFor(
                        EnumSkyBlock.BLOCK, pos);
                if(blockLight > lightLevel.getValueI())
                        return false;

                if(checkSky.isChecked())
                {
                        // mirrors EntityMob.isValidLightLevel(): skylight
                        // minus the weather/sleep penalty must stay dark
                        int sky = mc.world.getLightFromNeighborsFor(
                                EnumSkyBlock.SKY, pos);
                        if(sky - mc.world.getSkylightSubtracted() >= 8)
                                return false;
                }
                return true;
        }

        private void placeLight(EntityPlayerSP p)
        {
                if(targets.isEmpty())
                        return;

                for(BlockPos pos : targets)
                {
                        EnumFacing side = findSupportSide(pos);
                        if(side == null)
                                continue;

                        int slot = findLightSlot(p);
                        if(slot == -1)
                                return;

                        if(p.inventory.currentItem != slot)
                        {
                                p.inventory.currentItem = slot;
                                mc.playerController.updateController();
                                return;
                        }

                        mc.playerController.processRightClickBlock(p,
                                mc.world, pos.offset(side),
                                side.getOpposite(),
                                net.minecraft.util.math.Vec3d.ZERO,
                                EnumHand.MAIN_HAND);
                        p.swingArm(EnumHand.MAIN_HAND);
                        try
                        {
                                // mirror vanilla: brief right-click cooldown
                                ReflectionHelper.setPrivateValue(
                                        Minecraft.class, mc, 4,
                                        "rightClickDelayTimer",
                                        "field_71467_ac");
                        }catch(Throwable t)
                        {
                                // without the field we still only place once
                                // per scanned target per tick
                        }
                        return;
                }
        }

        private EnumFacing findSupportSide(BlockPos pos)
        {
                for(EnumFacing side : EnumFacing.VALUES)
                {
                        BlockPos neighbor = pos.offset(side);
                        IBlockState state = mc.world.getBlockState(neighbor);
                        if(state.isSideSolid(mc.world, neighbor,
                                side.getOpposite()))
                                return side;
                }
                return null;
        }

        private int findLightSlot(EntityPlayerSP p)
        {
                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(stack.isEmpty()
                                || !(stack.getItem() instanceof ItemBlock))
                                continue;
                        Block block =
                                ((ItemBlock)stack.getItem()).getBlock();
                        for(Block light : LIGHT_BLOCKS)
                                if(block == light)
                                        return i;
                }
                return -1;
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(targets.isEmpty() || mc.player == null)
                        return;

                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                for(BlockPos pos : targets)
                        RenderUtils.drawBox(new AxisAlignedBB(pos),
                                1F, 0.15F, 0.15F, 0.5F);
                RenderUtils.disable3D(false);
        }

        @Override
        protected void onEnable()
        {
                scanTimer = 0;
                targets.clear();
        }

        @Override
        protected void onDisable()
        {
                targets.clear();
        }
}
