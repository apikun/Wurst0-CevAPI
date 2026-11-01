/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI LootSorterHack (GPL-3.0-or-later)
 * Lite adaptation: sorts the chest you open (one swap per tick).
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class LootSorterHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting startDelay =
                new SliderSetting("Start delay (ticks)", 8, 0, 40, 1);

        private int openTimer;
        private boolean wasGuiOpen;
        private int pendingA = -1;
        private int pendingB = -1;

        public LootSorterHack()
        {
                super("LootSorter",
                        "Automatically sorts the contents of chests you"
                                + " open.");
                setCategory(Category.ITEMS);
                addSetting(startDelay);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.playerController == null)
                        return;

                if(!(mc.currentScreen instanceof GuiChest))
                {
                        wasGuiOpen = false;
                        pendingA = pendingB = -1;
                        return;
                }
                if(!(p.openContainer instanceof ContainerChest))
                        return;

                if(!wasGuiOpen)
                {
                        wasGuiOpen = true;
                        openTimer = startDelay.getValueI();
                        return;
                }

                if(openTimer > 0)
                {
                        openTimer--;
                        return;
                }

                if(pendingA >= 0 && pendingB >= 0)
                {
                        doSwap(p, pendingA, pendingB);
                        pendingA = pendingB = -1;
                        return;
                }

                planNextSwap(p);
        }

        private void planNextSwap(EntityPlayerSP p)
        {
                ContainerChest chest = (ContainerChest)p.openContainer;
                IInventory lower = chest.getLowerChestInventory();
                int size = lower.getSizeInventory();

                String prevKey = null;
                int prevSlot = -1;
                for(int i = 0; i < size; i++)
                {
                        Slot slot = p.openContainer.getSlot(i);
                        ItemStack stack = slot.getStack();
                        String key = stack.isEmpty() ? "\uffff"
                                : stack.getItem().getRegistryName() + "#"
                                        + stack.getItemDamage();

                        if(prevKey != null && key.compareTo(prevKey) < 0
                                && !stack.isEmpty())
                        {
                                pendingA = prevSlot;
                                pendingB = i;
                                return;
                        }
                        if(!stack.isEmpty() || prevSlot == -1
                                || lower.getStackInSlot(prevSlot)
                                        .isEmpty())
                        {
                                // track last non-empty for strict compare
                        }
                        prevKey = key;
                        prevSlot = i;
                }
        }

        private void doSwap(EntityPlayerSP p, int a, int b)
        {
                mc.playerController.windowClick(
                        p.openContainer.windowId, a, 0, ClickType.PICKUP, p);
                mc.playerController.windowClick(
                        p.openContainer.windowId, b, 0, ClickType.PICKUP, p);
                mc.playerController.windowClick(
                        p.openContainer.windowId, a, 0, ClickType.PICKUP, p);
        }
}
