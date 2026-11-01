/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NbtFilterHack (GPL-3.0-or-later)
 * Lite adaptation: the original inspects every inbound packet for
 * oversized NBT (chunk data, tile entities, item stacks, bundles).
 * In 1.12.2 chunk data carries no readable NBT, so the filter drops
 * oversized tile-entity updates and slot/window item stacks via the
 * fork's PacketTap inbound filter - the same anti chunk-ban /
 * book-ban intent.
 */
package net.wurst0.hacks;

import java.io.ByteArrayOutputStream;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.network.play.server.SPacketWindowItems;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.PacketTap;

/**
 * Drops incoming packets whose NBT payload (tile entities, item
 * stacks) exceeds the configured size limits, protecting against
 * chunk-ban / book-ban style griefing.
 */
public final class NbtFilterHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting blockEntityLimit =
                new SliderSetting("Block entity limit (KB)", 4, 1, 1024, 1);
        private final SliderSetting itemStackLimit =
                new SliderSetting("Item stack limit (KB)", 64, 1, 8192, 1);
        private final CheckboxSetting failClosed =
                new CheckboxSetting("Block packets on errors", true);
        private final CheckboxSetting reportOnDisable =
                new CheckboxSetting("Report when disabled", true);

        private final PacketTap.InboundFilter filter =
                new PacketTap.InboundFilter()
                {
                        @Override
                        public int onInbound(Object packet)
                        {
                                return inspect(packet);
                        }
                };

        private int blockedBlockEntities;
        private int blockedItemStacks;
        private int statusTimer;

        public NbtFilterHack()
        {
                super("NBTFilter",
                        "Drops incoming packets whose NBT payload"
                                + " (tile entities, item stacks) exceeds"
                                + " the configured size limits, protecting"
                                + " you from chunk-ban and book-ban"
                                + " attacks.");
                setCategory(Category.OTHER);
                addSettings(blockEntityLimit, itemStackLimit, failClosed,
                        reportOnDisable);
        }

        @Override
        protected void onEnable()
        {
                blockedBlockEntities = 0;
                blockedItemStacks = 0;
                statusTimer = 0;
        }

        @Override
        protected void onDisable()
        {
                PacketTap.setInboundFilter(null);
                if(reportOnDisable.isChecked())
                        report("Total");
        }

        @Override
        public void onUpdate()
        {
                PacketTap.ensureInstalled();
                PacketTap.setInboundFilter(filter);

                statusTimer++;
                if(statusTimer >= 600)
                {
                        statusTimer = 0;
                        report("So far");
                }
        }

        private int inspect(Object packet)
        {
                try
                {
                        if(packet instanceof SPacketUpdateTileEntity)
                        {
                                NBTTagCompound nbt =
                                        ((SPacketUpdateTileEntity)packet)
                                                .getNbtCompound();
                                if(nbt != null && nbtSize(nbt)
                                        > blockEntityLimit.getValueI()
                                                * 1024)
                                {
                                        blockedBlockEntities++;
                                        return PacketTap.INBOUND_DROP;
                                }
                        }else if(packet instanceof SPacketSetSlot)
                        {
                                if(hasOversizedNbt(
                                        ((SPacketSetSlot)packet)
                                                .getStack()))
                                {
                                        blockedItemStacks++;
                                        return PacketTap.INBOUND_DROP;
                                }
                        }else if(packet instanceof SPacketWindowItems)
                        {
                                List<ItemStack> stacks =
                                        ((SPacketWindowItems)packet)
                                                .getItemStacks();
                                for(ItemStack stack : stacks)
                                {
                                        if(hasOversizedNbt(stack))
                                        {
                                                blockedItemStacks++;
                                                return PacketTap
                                                        .INBOUND_DROP;
                                        }
                                }
                        }
                        return PacketTap.INBOUND_PASS;
                }catch(Throwable t)
                {
                        return failClosed.isChecked()
                                ? PacketTap.INBOUND_DROP
                                : PacketTap.INBOUND_PASS;
                }
        }

        private boolean hasOversizedNbt(ItemStack stack)
                throws Exception
        {
                if(stack == null || stack.isEmpty())
                        return false;
                NBTTagCompound nbt = stack.getTagCompound();
                if(nbt == null)
                        return false;
                return nbtSize(nbt) > itemStackLimit.getValueI() * 1024;
        }

        private static int nbtSize(NBTTagCompound nbt) throws Exception
        {
                ByteArrayOutputStream buffer =
                        new ByteArrayOutputStream();
                CompressedStreamTools.writeCompressed(nbt, buffer);
                return buffer.size();
        }

        private void report(String prefix)
        {
                EntityPlayerSP p = mc.player;
                if(p == null)
                        return;
                if(blockedBlockEntities == 0 && blockedItemStacks == 0)
                        return;
                p.sendMessage(new TextComponentString("[NBTFilter] "
                        + prefix + ": dropped " + blockedBlockEntities
                        + " tile entity packet(s) and "
                        + blockedItemStacks + " item slot packet(s)."));
        }
}
