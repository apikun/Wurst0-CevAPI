/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SpearAssistHack (GPL-3.0-or-later)
 * Lite adaptation: auto-releases your bow at a set charge level
 * (the "spear" equivalent in 1.12.2).
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

public final class SpearAssistHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting power =
                new SliderSetting("Power", 95, 10, 100, 5);

        public SpearAssistHack()
        {
                super("SpearAssist",
                        "Automatically releases your bow at the configured"
                                + " charge power for consistent shots.");
                setCategory(Category.COMBAT);
                addSetting(power);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || !p.isHandActive())
                        return;

                ItemStack active = p.getActiveItemStack();
                if(active.isEmpty() || active.getItem() != Items.BOW)
                        return;

                int charge = 72000 - p.getItemInUseCount();
                int target = (int)(power.getValue() / 100.0 * 20.0);

                if(charge >= target && charge < 20)
                        p.stopActiveHand();
        }
}
