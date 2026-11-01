/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TemplateToolHack (GPL-3.0-or-later)
 * Lite adaptation: the 26.x state-machine select-box tool becomes a
 * chat-command driven recorder (".t save" / ".t place" / ".t clear")
 * that snapshots the blocks around you and rebuilds them relative to
 * your current position.
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.events.ChatOutputEvent;
import net.wurst0.event.listeners.ChatOutputListener;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Records the blocks in a cube around you as a template and rebuilds
 * it relative to a new anchor. Type ".t save" while enabled to record,
 * ".t place" to rebuild, ".t clear" to reset.
 */
public final class TemplateToolHack extends Hack
        implements UpdateListener, RenderListener, ChatOutputListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting size =
                new SliderSetting("Template size", 4, 1, 16, 1);
        private final SliderSetting perTick =
                new SliderSetting("Blocks per tick", 2, 1, 8, 1);
        private final CheckboxSetting skipOccupied =
                new CheckboxSetting("Skip occupied tiles", true);

        /** offsets relative to the anchor, in record order */
        private final LinkedHashMap<BlockPos, IBlockState> template =
                new LinkedHashMap<BlockPos, IBlockState>();
        private final List<BlockPos> queue = new ArrayList<BlockPos>();
        private BlockPos anchor;
        private boolean placing;
        private int placed;
        private int skipped;
        private int stallTicks;

        public TemplateToolHack()
        {
                super("TemplateTool",
                        "Saves a cube of blocks around you as a template"
                                + " (\".t save\") and rebuilds it at your"
                                + " current spot (\".t place\").");
                setCategory(Category.BLOCKS);
                addSettings(size, perTick, skipOccupied);
        }

        @Override
        public void onSentMessage(ChatOutputEvent event)
        {
                String msg = event.getText() == null ? ""
                        : event.getText().trim();
                if(msg.equals(".t save"))
                {
                        event.setCancelled(true);
                        record();
                }else if(msg.equals(".t place"))
                {
                        event.setCancelled(true);
                        startPlacing();
                }else if(msg.equals(".t clear"))
                {
                        event.setCancelled(true);
                        template.clear();
                        queue.clear();
                        placing = false;
                        report("\u00a7e[TemplateTool] Template cleared.");
                }
        }

        private void record()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                BlockPos origin = new BlockPos(
                        Math.floor(p.posX), Math.floor(p.posY),
                        Math.floor(p.posZ));
                int r = size.getValueI();

                template.clear();
                for(BlockPos pos : BlockPos.getAllInBox(
                        origin.add(-r, -r, -r), origin.add(r, r, r)))
                {
                        IBlockState state = mc.world.getBlockState(pos);
                        if(state.getMaterial() == Material.AIR)
                                continue;
                        template.put(pos.subtract(origin), state);
                        if(template.size() >= 4096)
                                break;
                }

                if(template.isEmpty())
                        report("\u00a7e[TemplateTool] Nothing recorded -"
                                + " the area is empty.");
                else
                        report("\u00a7a[TemplateTool] Saved "
                                + template.size() + " blocks (size " + r
                                + "). Type .t place to rebuild.");
        }

        private void startPlacing()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(template.isEmpty())
                {
                        report("\u00a7e[TemplateTool] No template yet."
                                + " Type .t save first.");
                        return;
                }

                anchor = new BlockPos(Math.floor(p.posX),
                        Math.floor(p.posY), Math.floor(p.posZ));
                queue.clear();
                queue.addAll(template.keySet());
                // build bottom-up, near-to-far for stable placement
                queue.sort(new Comparator<BlockPos>()
                {
                        @Override
                        public int compare(BlockPos a, BlockPos b)
                        {
                                if(a.getY() != b.getY())
                                        return Integer.compare(a.getY(),
                                                b.getY());
                                return Integer.compare(
                                        a.getX() * a.getX() + a.getZ()
                                                * a.getZ(),
                                        b.getX() * b.getX() + b.getZ()
                                                * b.getZ());
                        }
                });

                placing = true;
                placed = 0;
                skipped = 0;
                stallTicks = 0;
                report("\u00a7a[TemplateTool] Placing "
                        + queue.size() + " blocks here.");
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null || !placing || anchor == null)
                        return;

                int max = perTick.getValueI();
                int done = 0;

                Iterator<BlockPos> itr = queue.iterator();
                while(itr.hasNext() && done < max)
                {
                        BlockPos offset = itr.next();
                        BlockPos target = anchor.add(offset);
                        IBlockState wanted = template.get(offset);
                        if(wanted == null)
                        {
                                itr.remove();
                                continue;
                        }

                        IBlockState current =
                                mc.world.getBlockState(target);
                        if(current.getMaterial() != Material.AIR
                                && !current.getMaterial().isReplaceable())
                        {
                                if(skipOccupied.isChecked()
                                        && current.getBlock() == wanted
                                                .getBlock())
                                {
                                        itr.remove();
                                        continue;
                                }
                                if(skipOccupied.isChecked())
                                {
                                        skipped++;
                                        itr.remove();
                                        continue;
                                }
                        }

                        int slot = findSlotForBlock(p,
                                wanted.getBlock());
                        if(slot == -1)
                        {
                                skipped++;
                                itr.remove();
                                continue;
                        }

                        if(p.inventory.currentItem != slot)
                        {
                                p.inventory.currentItem = slot;
                                mc.playerController.updateController();
                                return;
                        }

                        EnumFacing side = findSupportSide(target);
                        if(side == null)
                                continue; // no support yet, retry later

                        mc.playerController.processRightClickBlock(p,
                                mc.world, target.offset(side),
                                side.getOpposite(), Vec3d.ZERO,
                                EnumHand.MAIN_HAND);
                        p.swingArm(EnumHand.MAIN_HAND);
                        itr.remove();
                        placed++;
                        done++;
                }

                if(queue.isEmpty())
                {
                        placing = false;
                        report("\u00a7a[TemplateTool] Done. placed="
                                + placed + " skipped=" + skipped + ".");
                }else if(done == 0)
                {
                        // nothing placeable this tick (missing support or
                        // items) - abort after a while to avoid hanging
                        if(++stallTicks > 200)
                        {
                                placing = false;
                                report("\u00a7e[TemplateTool] Stalled"
                                        + " (missing items or support?)."
                                        + " placed=" + placed + " left="
                                        + queue.size() + ".");
                        }
                }else
                        stallTicks = 0;
        }

        private int findSlotForBlock(EntityPlayerSP p,
                net.minecraft.block.Block block)
        {
                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(stack.isEmpty()
                                || !(stack.getItem() instanceof ItemBlock))
                                continue;
                        if(((ItemBlock)stack.getItem()).getBlock() == block)
                                return i;
                }
                return -1;
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

        @Override
        public void onRender(float partialTicks)
        {
                if(mc.player == null || anchor == null || !placing)
                        return;

                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);

                // anchor origin marker (green)
                RenderUtils.drawBox(
                        new AxisAlignedBB(anchor).shrink(0.0625),
                        0.15F, 1F, 0.3F, 0.5F);

                // template bounds outline (black-ish) while building
                if(!queue.isEmpty() && !template.isEmpty())
                {
                        BlockPos min = null;
                        BlockPos max = null;
                        for(BlockPos offset : template.keySet())
                        {
                                BlockPos world = anchor.add(offset);
                                if(min == null)
                                {
                                        min = world;
                                        max = world;
                                }else
                                {
                                        min = new BlockPos(
                                                Math.min(min.getX(),
                                                        world.getX()),
                                                Math.min(min.getY(),
                                                        world.getY()),
                                                Math.min(min.getZ(),
                                                        world.getZ()));
                                        max = new BlockPos(
                                                Math.max(max.getX(),
                                                        world.getX()),
                                                Math.max(max.getY(),
                                                        world.getY()),
                                                Math.max(max.getZ(),
                                                        world.getZ()));
                                }
                        }
                        if(min != null)
                        {
                                AxisAlignedBB bounds = new AxisAlignedBB(min,
                                        max.add(1, 1, 1)).shrink(0.03);
                                RenderUtils.drawBox(bounds, 0F, 0F, 0F, 0.8F);
                        }
                }

                RenderUtils.disable3D(false);
        }

        private void report(String text)
        {
                if(mc.player != null)
                        mc.player.sendMessage(new TextComponentString(text));
        }

        @Override
        protected void onEnable()
        {
                placing = false;
                queue.clear();
                anchor = null;
        }

        @Override
        protected void onDisable()
        {
                placing = false;
                queue.clear();
                anchor = null;
        }
}
