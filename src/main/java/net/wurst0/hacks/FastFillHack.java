/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FastFillHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * While a chest is open: shift-clicks matching items from your
 * inventory into it, up to N per tick.
 */
public final class FastFillHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting perTick =
                new SliderSetting("Stacks per tick", 3, 1, 9, 1);
        private final CheckboxSetting matchingOnly =
                new CheckboxSetting("Only items already in chest", true);

        public FastFillHack()
        {
                super("FastFill",
                        "While a chest is open, fills it with matching items"
                                + " from your inventory.");
                setCategory(Category.ITEMS);
                addSettings(perTick, matchingOnly);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.playerController == null)
                        return;

                if(!(mc.currentScreen instanceof GuiChest))
                        return;
                if(!(p.openContainer instanceof ContainerChest))
                        return;

                ContainerChest chest = (ContainerChest)p.openContainer;
                IInventory lower = chest.getLowerChestInventory();
                int chestSize = lower.getSizeInventory();

                Set<String> chestItems = new HashSet<String>();
                if(matchingOnly.isChecked())
                {
                        for(int i = 0; i < chestSize; i++)
                        {
                                ItemStack s = lower.getStackInSlot(i);
                                if(!s.isEmpty())
                                        chestItems.add(String.valueOf(
                                                s.getItem()
                                                        .getRegistryName()));
                        }
                }

                int moved = 0;
                for(int i = chestSize; i < chestSize + 36 && moved
                        < perTick.getValueI(); i++)
                {
                        ItemStack stack = p.openContainer.getSlot(i)
                                .getStack();
                        if(stack.isEmpty())
                                continue;
                        if(matchingOnly.isChecked()
                                && !chestItems.contains(String.valueOf(
                                        stack.getItem().getRegistryName())))
                                continue;

                        mc.playerController.windowClick(
                                p.openContainer.windowId, i, 0,
                                ClickType.QUICK_MOVE, p);
                        moved++;
                }

                if(moved == perTick.getValueI())
                {
                        // still more to move on the next tick
                }
        }
}
