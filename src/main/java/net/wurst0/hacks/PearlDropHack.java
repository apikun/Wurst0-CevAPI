/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PearlDropHack (GPL-3.0-or-later)
 *
 * NOTE: the original is a 1.13+/1.15+ "stasis pearl" system (throw a
 * pearl up through a bubble column while standing on honey blocks and
 * time the drop - bubble columns and honey blocks don't exist in
 * 1.12.2). This lite adaptation keeps the survival intent: it throws
 * an ender pearl right before you would take big fall damage, so the
 * pearl teleport resets your fall.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.ReflectCalls;

/**
 * While falling, automatically throws an ender pearl from your hotbar
 * once the fall distance reaches the configured threshold, then
 * switches back to your previous slot. The pearl teleport cancels the
 * fall damage. Cooldown prevents pearl spam while descending.
 */
public final class PearlDropHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting fallDistance =
                new SliderSetting("Fall distance", 8, 3, 30, 1);
        private final SliderSetting cooldown =
                new SliderSetting("Cooldown (s)", 3, 1, 10, 1);

        private int timer;
        private int restoreSlot = -1;
        private int restoreTimer;

        public PearlDropHack()
        {
                super("PearlDrop",
                        "Automatically throws an ender pearl when falling"
                                + " far enough, to negate the fall damage."
                                + " Lite adaptation: the original needs"
                                + " 1.13+ bubble columns / honey blocks.");
                setCategory(Category.MOVEMENT);
                addSettings(fallDistance, cooldown);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.playerController == null)
                        return;

                if(restoreTimer > 0 && --restoreTimer == 0
                        && restoreSlot >= 0)
                {
                        p.inventory.currentItem = restoreSlot;
                        mc.playerController.updateController();
                        restoreSlot = -1;
                }

                if(timer > 0)
                {
                        timer--;
                        return;
                }

                if(p.onGround || p.fallDistance < fallDistance.getValue())
                        return;
                if(p.openContainer != p.inventoryContainer)
                        return;

                int slot = findPearl(p);
                if(slot == -1)
                        return;

                if(p.inventory.currentItem != slot)
                {
                        restoreSlot = p.inventory.currentItem;
                        p.inventory.currentItem = slot;
                        mc.playerController.updateController();
                }

                ReflectCalls.rightClickMouse();
                timer = cooldown.getValueI() * 20;
                restoreTimer = 8;
        }

        private int findPearl(EntityPlayerSP p)
        {
                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(!stack.isEmpty()
                                && stack.getItem() == Items.ENDER_PEARL)
                                return i;
                }
                return -1;
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
                restoreSlot = -1;
                restoreTimer = 0;
        }

        @Override
        protected void onDisable()
        {
                if(restoreSlot >= 0 && mc.player != null)
                {
                        mc.player.inventory.currentItem = restoreSlot;
                        if(mc.playerController != null)
                                mc.playerController.updateController();
                }
                restoreSlot = -1;
        }
}
