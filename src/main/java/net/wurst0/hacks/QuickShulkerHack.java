/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI QuickShulkerHack (GPL-3.0-or-later)
 * Lite adaptation: hold sneak while holding a shulker box in your
 * hand to open it right away.
 */
package net.wurst0.hacks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.util.ReflectCalls;

public final class QuickShulkerHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private int sneakTimer;

        public QuickShulkerHack()
        {
                super("QuickShulker",
                        "Hold sneak for a moment while carrying a shulker"
                                + " box to open it instantly.");
                setCategory(Category.ITEMS);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.playerController == null)
                        return;

                if(p.isSneaking() && p.onGround
                        && p.openContainer == p.inventoryContainer)
                {
                        ItemStack held = p.getHeldItemMainhand();
                        if(!held.isEmpty()
                                && held.getItem() instanceof ItemBlock)
                        {
                                Block block = ((ItemBlock)held.getItem())
                                        .getBlock();
                                if(block instanceof BlockShulkerBox)
                                {
                                        if(++sneakTimer >= 15)
                                        {
                                                sneakTimer = 0;
                                                ReflectCalls.rightClickMouse();
                                        }
                                        return;
                                }
                        }
                }
                sneakTimer = 0;
        }

        @Override
        protected void onEnable()
        {
                sneakTimer = 0;
        }
}
