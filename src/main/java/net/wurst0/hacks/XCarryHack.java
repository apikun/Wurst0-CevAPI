/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI XCarryHack (GPL-3.0-or-later)
 * Lite adaptation: true XCarry needs packet hooks, so this port
 * keeps the crafting grid empty while the GUI is open and puts
 * everything back where it was when you close it (nothing can be
 * lost that way).
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class XCarryHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting refillOnOpen =
                new CheckboxSetting("Refill on reopen", true);

        private ItemStack[] stashed = new ItemStack[4];

        public XCarryHack()
        {
                super("XCarry",
                        "Keeps items safe in your crafting grid: stashes"
                                + " them into your inventory before the grid"
                                + " can drop them, and puts them back when"
                                + " you reopen the inventory.");
                setCategory(Category.ITEMS);
                addSetting(refillOnOpen);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.playerController == null)
                        return;

                if(!(mc.currentScreen instanceof GuiInventory)
                        || !(p.openContainer instanceof ContainerPlayer))
                        return;

                ContainerPlayer container = (ContainerPlayer)p.openContainer;

                // stash craft grid items (container slots 1-4)
                for(int i = 1; i <= 4; i++)
                {
                        Slot slot = container.getSlot(i);
                        ItemStack stack = slot.getStack();
                        int gridIdx = i - 1;

                        if(!stack.isEmpty())
                        {
                                if(stashed[gridIdx] == null
                                        || stashed[gridIdx].isEmpty())
                                        stashed[gridIdx] = stack.copy();
                                mc.playerController.windowClick(
                                        container.windowId, i, 0,
                                        ClickType.QUICK_MOVE, p);
                        }
                        else if(refillOnOpen.isChecked()
                                && stashed[gridIdx] != null
                                && !stashed[gridIdx].isEmpty())
                        {
                                // put the stashed item back into the grid:
                                // pick it up from the inventory, then place
                                // it into the craft slot
                                int found = findItem(p, stashed[gridIdx]);
                                if(found >= 0)
                                {
                                        mc.playerController.windowClick(
                                                container.windowId, found, 0,
                                                ClickType.PICKUP, p);
                                        mc.playerController.windowClick(
                                                container.windowId, i, 0,
                                                ClickType.PICKUP, p);
                                        stashed[gridIdx] = ItemStack.EMPTY;
                                }
                        }
                }
        }

        private int findItem(EntityPlayerSP p, ItemStack want)
        {
                for(int i = 9; i < 45; i++)
                {
                        Slot slot = p.inventoryContainer.getSlot(i);
                        ItemStack stack = slot.getStack();
                        if(!stack.isEmpty()
                                && ItemStack.areItemsEqual(stack, want))
                                return i;
                }
                return -1;
        }

        @Override
        protected void onDisable()
        {
                stashed = new ItemStack[4];
        }
}
