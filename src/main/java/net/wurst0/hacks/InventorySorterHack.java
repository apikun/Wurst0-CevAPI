/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI InventorySorterHack (GPL-3.0-or-later)
 * Lite adaptation: sorts your inventory (stacks + hotbar kept at
 * the end) one swap per tick via the normal windowClick path.
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class InventorySorterHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting startDelay =
                new SliderSetting("Start delay (ticks)", 6, 0, 40, 1);

        private final List<Integer> order = new ArrayList<Integer>();
        private int swapA = -1;
        private int swapB = -1;
        private int openTimer;
        private boolean wasGuiOpen;

        public InventorySorterHack()
        {
                super("InventorySorter",
                        "Sorts your inventory whenever you open it (stacks"
                                + " first, hotbar keeps its order).");
                setCategory(Category.ITEMS);
                addSetting(startDelay);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.playerController == null)
                        return;

                boolean guiOpen = mc.currentScreen instanceof GuiContainer
                        && p.openContainer == p.inventoryContainer;

                if(!guiOpen)
                {
                        wasGuiOpen = false;
                        swapA = swapB = -1;
                        return;
                }

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

                // execute a pending swap first
                if(swapA >= 0 && swapB >= 0)
                {
                        doSwapClicks(p, swapA, swapB);
                        swapA = swapB = -1;
                        return;
                }

                planNextSwap(p);
        }

        private void planNextSwap(EntityPlayerSP p)
        {
                List<Integer> sortable = new ArrayList<Integer>();
                for(int i = 9; i < 36; i++)
                        sortable.add(i);
                // slots 9-35 = main inventory; hotbar (36-44) keeps its
                // order so your weapon/tools stay where they are

                String prevKey = null;
                for(int slotIdx : sortable)
                {
                        Slot slot = p.inventoryContainer.getSlot(slotIdx);
                        ItemStack stack = slot.getStack();
                        String key = stack.isEmpty() ? "\uffff"
                                : stack.getItem().getRegistryName() + "#"
                                        + stack.getItemDamage();

                        if(prevKey != null && key.compareTo(prevKey) < 0)
                        {
                                // out of order -> swap with the previous slot
                                int prevSlot = slotIdx - 1;
                                Slot s2 =
                                        p.inventoryContainer.getSlot(prevSlot);
                                if(!s2.getStack().isEmpty())
                                {
                                        swapA = prevSlot;
                                        swapB = slotIdx;
                                        return;
                                }
                        }
                        prevKey = key;
                }
        }

        private void doSwapClicks(EntityPlayerSP p, int a, int b)
        {
                mc.playerController.windowClick(
                        p.openContainer.windowId, a, 0, ClickType.PICKUP, p);
                mc.playerController.windowClick(
                        p.openContainer.windowId, b, 0, ClickType.PICKUP, p);
                mc.playerController.windowClick(
                        p.openContainer.windowId, a, 0, ClickType.PICKUP, p);
        }
}
