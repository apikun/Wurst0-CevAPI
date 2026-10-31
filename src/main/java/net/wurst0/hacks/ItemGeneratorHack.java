/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ItemGeneratorHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Creative mode only: keeps the held hotbar slot topped up with a
 * full stack of whatever you are holding, sending the new stack
 * through CPacketCreativeInventoryAction (sendSlotPacket) - the
 * same way vanilla syncs creative inventory edits.
 */
public final class ItemGeneratorHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting interval =
                new SliderSetting("Interval (ticks)", 5, 1, 40, 1);
        private final CheckboxSetting onlyEmptyHand =
                new CheckboxSetting("Refill below full stack", true);

        private int timer;

        public ItemGeneratorHack()
        {
                super("ItemGenerator",
                        "Generates a full stack of the held item in your hotbar"
                                + " slot. Only works in creative mode.");
                setCategory(Category.ITEMS);
                addSettings(interval, onlyEmptyHand);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.playerController == null)
                        return;
                if(!p.isCreative())
                        return;

                if(--timer > 0)
                        return;
                timer = interval.getValueI();

                ItemStack held = p.getHeldItemMainhand();
                if(held.isEmpty())
                        return;
                if(onlyEmptyHand.isChecked()
                        && held.getCount() >= held.getMaxStackSize())
                        return;

                ItemStack full = held.copy();
                full.setCount(full.getMaxStackSize());

                int slot = p.inventory.currentItem;
                p.inventory.setInventorySlotContents(slot, full);
                // sync the edit to the server (creative inventory action)
                mc.playerController.sendSlotPacket(full, slot + 36);
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
        }
}
