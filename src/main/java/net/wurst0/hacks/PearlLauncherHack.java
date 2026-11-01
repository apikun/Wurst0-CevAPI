/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PearlLauncherHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.ReflectCalls;

/**
 * While sneaking on the ground, automatically throws an ender
 * pearl from your hotbar in your look direction, then switches
 * back to your previous slot.
 */
public final class PearlLauncherHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting cooldown =
                new SliderSetting("Cooldown (s)", 5, 1, 30, 1);
        private final CheckboxSetting onlyOnGround =
                new CheckboxSetting("Only on ground", true);

        private int timer;
        private int restoreSlot = -1;
        private int restoreTimer;

        public PearlLauncherHack()
        {
                super("PearlLauncher",
                        "Sneak to automatically throw an ender pearl from"
                                + " your hotbar.");
                setCategory(Category.COMBAT);
                addSettings(cooldown, onlyOnGround);
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

                if(!p.isSneaking())
                        return;
                if(onlyOnGround.isChecked() && !p.onGround)
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
