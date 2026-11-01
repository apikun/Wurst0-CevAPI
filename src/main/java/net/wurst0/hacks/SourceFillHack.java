/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SourceFillHack (GPL-3.0-or-later)
 * Lite adaptation: hotbar whitelist by item id, placement via
 * PlayerController (no BlockPlacer/InteractionSimulator on 1.12.2).
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;
import net.wurst0.util.ReflectUtils;

/**
 * Highlights fluid source blocks around you and can fill them with
 * blocks from the hotbar (optionally breaking the placed block again).
 */
public final class SourceFillHack extends Hack
        implements UpdateListener, RenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 5, 1, 6, 1);
        private final CheckboxSetting autoFill =
                new CheckboxSetting("Auto fill", true);
        private final CheckboxSetting autoBreak =
                new CheckboxSetting("Auto break placed block", false);
        private final ModeSetting liquidMode = new ModeSetting("Liquids",
                new String[]{"Water and lava", "Water only",
                        "Lava only"}, 0);
        private final TextFieldSetting whitelist =
                new TextFieldSetting("Block whitelist",
                        "dirt,cobblestone,netherrack");
        private final CheckboxSetting renderWater =
                new CheckboxSetting("Render water", true);
        private final CheckboxSetting renderLava =
                new CheckboxSetting("Render lava", true);

        private final List<BlockPos> waterTargets =
                new ArrayList<BlockPos>();
        private final List<BlockPos> lavaTargets =
                new ArrayList<BlockPos>();
        private final Set<BlockPos> blocksToBreak =
                new HashSet<BlockPos>();
        private int scanTimer;
        private int breakCooldown;

        private java.lang.reflect.Field curBlockDamageMP;

        public SourceFillHack()
        {
                super("SourceFill",
                        "Highlights water and lava source blocks and"
                                + " automatically fills them with blocks"
                                + " from your hotbar.");
                setCategory(Category.BLOCKS);
                addSettings(range, autoFill, autoBreak, liquidMode,
                        whitelist, renderWater, renderLava);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                {
                        waterTargets.clear();
                        lavaTargets.clear();
                        blocksToBreak.clear();
                        return;
                }

                if(--scanTimer <= 0)
                {
                        scanTimer = 4;
                        scanSources(p);
                }

                pruneBreakQueue();

                if(autoBreak.isChecked() && breakCooldown <= 0
                        && breakFilledBlock(p))
                {
                        breakCooldown = 4;
                        return;
                }
                if(breakCooldown > 0)
                        breakCooldown--;

                if(autoFill.isChecked())
                        fillSourceBlock(p);
        }

        private void scanSources(EntityPlayerSP p)
        {
                waterTargets.clear();
                lavaTargets.clear();
                int r = range.getValueI();
                BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
                double rangeSq = range.getValue() * range.getValue();

                for(BlockPos pos : BlockPos.getAllInBox(
                        center.add(-r, -r, -r), center.add(r, r, r)))
                {
                        if(p.getDistanceSq(pos.getX() + 0.5,
                                pos.getY() + 0.5, pos.getZ() + 0.5) > rangeSq)
                                continue;

                        IBlockState state = mc.world.getBlockState(pos);
                        Material material = state.getMaterial();
                        if(material != Material.WATER
                                && material != Material.LAVA)
                                continue;

                        // only actual source blocks (level 0)
                        Integer level = state.getValue(BlockLiquid.LEVEL);
                        if(level == null || level.intValue() != 0)
                                continue;

                        if(material == Material.WATER)
                                waterTargets.add(pos);
                        else if(lavaWanted())
                                lavaTargets.add(pos);

                        if(waterTargets.size() + lavaTargets.size() >= 300)
                                break;
                }
        }

        private boolean lavaWanted()
        {
                return !liquidMode.getSelected().equals("Water only");
        }

        private boolean waterWanted()
        {
                return !liquidMode.getSelected().equals("Lava only");
        }

        private void fillSourceBlock(EntityPlayerSP p)
        {
                List<BlockPos> targets = null;
                if(waterWanted() && !waterTargets.isEmpty())
                        targets = waterTargets;
                else if(lavaWanted() && !lavaTargets.isEmpty())
                        targets = lavaTargets;
                if(targets == null)
                        return;

                for(BlockPos pos : targets)
                {
                        if(!isStillSource(pos))
                                continue;

                        EnumFacing side = findSupportSide(pos);
                        if(side == null)
                                continue;

                        int slot = findWhitelistedSlot(p);
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
                                side.getOpposite(), Vec3d.ZERO,
                                EnumHand.MAIN_HAND);
                        p.swingArm(EnumHand.MAIN_HAND);
                        if(autoBreak.isChecked())
                                blocksToBreak.add(pos);
                        return;
                }
        }

        private boolean breakFilledBlock(EntityPlayerSP p)
        {
                Iterator<BlockPos> itr = blocksToBreak.iterator();
                while(itr.hasNext())
                {
                        BlockPos pos = itr.next();
                        IBlockState state = mc.world.getBlockState(pos);
                        if(state.getMaterial() == Material.AIR
                                || state.getMaterial().isLiquid())
                        {
                                itr.remove();
                                continue;
                        }

                        PlayerControllerMP pc = mc.playerController;
                        if(pc == null)
                                return false;

                        pc.clickBlock(pos, EnumFacing.UP);
                        if(curBlockDamageMP == null)
                                curBlockDamageMP = ReflectUtils.field(
                                        PlayerControllerMP.class,
                                        "curBlockDamageMP",
                                        "field_78770_f");
                        if(curBlockDamageMP != null)
                        {
                                try
                                {
                                        curBlockDamageMP.setFloat(pc, 1F);
                                }catch(Throwable t)
                                {
                                        // break slower without the field
                                }
                        }
                        pc.onPlayerDamageBlock(pos, EnumFacing.UP);
                        p.swingArm(EnumHand.MAIN_HAND);
                        return true;
                }
                return false;
        }

        private void pruneBreakQueue()
        {
                Iterator<BlockPos> itr = blocksToBreak.iterator();
                while(itr.hasNext())
                {
                        BlockPos pos = itr.next();
                        IBlockState state = mc.world.getBlockState(pos);
                        if(state.getMaterial() == Material.AIR
                                || state.getMaterial().isLiquid())
                        {
                                itr.remove();
                                continue;
                        }
                        // no auto-break support: stop if it's a source again
                        if(isStillSource(pos))
                                itr.remove();
                }
        }

        private boolean isStillSource(BlockPos pos)
        {
                IBlockState state = mc.world.getBlockState(pos);
                Material material = state.getMaterial();
                if(material != Material.WATER && material != Material.LAVA)
                        return false;
                Integer level = state.getValue(BlockLiquid.LEVEL);
                return level != null && level.intValue() == 0;
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

        private int findWhitelistedSlot(EntityPlayerSP p)
        {
                String wl = whitelist.getValue().toLowerCase();
                String[] wanted = wl.isEmpty()
                        ? new String[0] : wl.split(",");

                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(stack.isEmpty()
                                || !(stack.getItem() instanceof ItemBlock))
                                continue;

                        if(wanted.length == 0)
                                return i;

                        String id = stack.getItem()
                                .getRegistryName() == null ? ""
                                : stack.getItem().getRegistryName()
                                        .getPath();
                        for(String w : wanted)
                        {
                                String name = w.trim();
                                if(!name.isEmpty() && id.equals(name))
                                        return i;
                        }
                }
                return -1;
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(mc.player == null)
                        return;

                boolean drawWater = renderWater.isChecked()
                        && waterWanted() && !waterTargets.isEmpty();
                boolean drawLava = renderLava.isChecked()
                        && lavaWanted() && !lavaTargets.isEmpty();
                if(!drawWater && !drawLava)
                        return;

                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);

                if(drawWater)
                        for(BlockPos pos : waterTargets)
                                RenderUtils.drawBox(new AxisAlignedBB(pos),
                                        0.25F, 0.46F, 0.9F, 0.6F);
                if(drawLava)
                        for(BlockPos pos : lavaTargets)
                                RenderUtils.drawBox(new AxisAlignedBB(pos),
                                        1F, 0.5F, 0.1F, 0.6F);

                RenderUtils.disable3D(false);
        }

        @Override
        protected void onEnable()
        {
                scanTimer = 0;
                breakCooldown = 0;
                waterTargets.clear();
                lavaTargets.clear();
                blocksToBreak.clear();
        }

        @Override
        protected void onDisable()
        {
                waterTargets.clear();
                lavaTargets.clear();
                blocksToBreak.clear();
        }
}
