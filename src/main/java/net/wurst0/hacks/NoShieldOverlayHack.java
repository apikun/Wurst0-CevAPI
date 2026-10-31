/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoShieldOverlayHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemShield;
import net.wurst0.Category;
import net.wurst0.event.events.HandRenderEvent;
import net.wurst0.event.listeners.HandRenderListener;
import net.wurst0.feature.Hack;

/**
 * Hides the shield when it is held in either hand, by cancelling the
 * first-person hand render (bridged from RenderSpecificHandEvent).
 */
public final class NoShieldOverlayHack extends Hack
        implements HandRenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        public NoShieldOverlayHack()
        {
                super("NoShieldOverlay",
                        "Hides the shield in first person so it doesn't block your"
                                + " view.");
                setCategory(Category.RENDER);
        }

        @Override
        public void onHandRender(HandRenderEvent event)
        {
                EntityPlayer p = mc.player;
                if(p == null)
                        return;

                if(p.getHeldItemMainhand().getItem() instanceof ItemShield
                        || p.getHeldItemOffhand().getItem() instanceof ItemShield)
                        event.setCancelled(true);
        }
}
