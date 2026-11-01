/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoSwitchHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * Cycles through your hotbar slots, one step per tick.
 */
public final class AutoSwitchHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        public AutoSwitchHack()
        {
                super("AutoSwitch",
                        "Automatically switches between hotbar slots,"
                                + " one step per tick.");
                setCategory(Category.ITEMS);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                int prevSlot = p.inventory.currentItem;
                if(prevSlot == 8)
                        p.inventory.currentItem = 0;
                else
                        p.inventory.currentItem = prevSlot + 1;
        }
}
