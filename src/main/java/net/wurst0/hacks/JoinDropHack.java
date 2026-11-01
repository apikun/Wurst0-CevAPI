/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI JoinDropHack (GPL-3.0-or-later)
 *
 * 1.12.2 adaptation: on the first tick in a world it drops the
 * configured item types for a short window. Hotbar stacks are dropped
 * with CPacketPlayerDigging(DROP_ALL_ITEMS) after CPacketHeldItemChange;
 * main-inventory stacks are dropped via windowClick THROW. There is no
 * offhand in 1.12.2 and bundles don't exist yet, so those original
 * options are not ported; a text filter matches item registry names.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.inventory.ClickType;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.TextFieldSetting;

public final class JoinDropHack extends Hack implements UpdateListener
{
        private static final int RUN_TICKS = 200;
        private static final int PASSES_PER_TICK = 3;

        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting itemFilter =
                new TextFieldSetting("Items", "");
        private final CheckboxSetting shulkers =
                new CheckboxSetting("Shulkers", true);
        private final CheckboxSetting books =
                new CheckboxSetting("Books", true);
        private final CheckboxSetting all = new CheckboxSetting("All", false);
        private final CheckboxSetting disableAfterComplete =
                new CheckboxSetting("Disable after complete", false);

        private int ticksLeft;
        private boolean running;
        private boolean wasInWorld;

        public JoinDropHack()
        {
                super("JoinDrop",
                        "Drops configured item types (shulkers, books or a"
                                + " custom filter) immediately after joining"
                                + " a server.");
                setCategory(Category.OTHER);
                addSettings(itemFilter, shulkers, books, all,
                        disableAfterComplete);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                {
                        wasInWorld = false;
                        stopRun();
                        return;
                }

                if(!wasInWorld)
                {
                        wasInWorld = true;
                        if(isEnabled())
                        {
                                ticksLeft = RUN_TICKS;
                                running = true;
                        }
                        return;
                }

                if(!running)
                        return;

                boolean droppedAny = dropMatches(p);
                ticksLeft--;
                if(!droppedAny || ticksLeft <= 0)
                {
                        stopRun();
                        if(disableAfterComplete.isChecked())
                                setEnabled(false);
                }
        }

        private boolean dropMatches(EntityPlayerSP p)
        {
                boolean droppedAny = false;

                for(int pass = 0; pass < PASSES_PER_TICK; pass++)
                {
                        for(int slot = 0; slot < 9; slot++)
                        {
                                ItemStack stack =
                                        p.inventory.mainInventory.get(slot);
                                if(!shouldDrop(stack))
                                        continue;
                                droppedAny = true;
                                dropHotbarStack(p, slot);
                        }
                }

                for(int slot = 9; slot < 36; slot++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(slot);
                        if(!shouldDrop(stack))
                                continue;
                        droppedAny = true;
                        mc.playerController.windowClick(
                                p.inventoryContainer.windowId, slot, 1,
                                ClickType.THROW, p);
                }

                return droppedAny;
        }

        private void dropHotbarStack(EntityPlayerSP p, int slot)
        {
                int oldSlot = p.inventory.currentItem;
                if(oldSlot != slot)
                        mc.getConnection().sendPacket(
                                new CPacketHeldItemChange(slot));

                mc.getConnection().sendPacket(new CPacketPlayerDigging(
                        CPacketPlayerDigging.Action.DROP_ALL_ITEMS,
                        BlockPos.ORIGIN, EnumFacing.DOWN));

                if(oldSlot != slot)
                        mc.getConnection().sendPacket(
                                new CPacketHeldItemChange(oldSlot));
        }

        private boolean shouldDrop(ItemStack stack)
        {
                if(stack == null || stack.isEmpty())
                        return false;

                if(all.isChecked())
                        return true;

                if(shulkers.isChecked()
                        && stack.getItem() instanceof ItemShulkerBox)
                        return true;

                if(books.isChecked() && (stack.getItem() == Items.WRITABLE_BOOK
                        || stack.getItem() == Items.WRITTEN_BOOK))
                        return true;

                String filter = itemFilter.getValue();
                if(filter == null || filter.trim().isEmpty())
                        return false;

                String name = String.valueOf(stack.getItem().getRegistryName());
                name = name.toLowerCase(java.util.Locale.ROOT);
                String[] fragments = filter.toLowerCase(java.util.Locale.ROOT)
                        .split(",");
                for(int i = 0; i < fragments.length; i++)
                {
                        String fragment = fragments[i].trim();
                        if(!fragment.isEmpty() && name.contains(fragment))
                                return true;
                }
                return false;
        }

        private void stopRun()
        {
                running = false;
                ticksLeft = 0;
        }

        @Override
        protected void onEnable()
        {
                // if we are already in a world, start dropping right away
                if(mc.player != null)
                {
                        wasInWorld = true;
                        ticksLeft = RUN_TICKS;
                        running = true;
                }
        }

        @Override
        protected void onDisable()
        {
                stopRun();
        }
}
