/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoTraderHack (GPL-3.0-or-later)
 * Lite adaptation: cycles through all villager trades and buys the
 * selected amount from each one.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.entity.IMerchant;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class AutoTraderHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting buysPerTrade =
                new SliderSetting("Buys per trade", 1, 1, 8, 1);
        private final CheckboxSetting skipOutOfStock =
                new CheckboxSetting("Skip empty trades", true);

        private int timer;
        private int currentTrade;
        private int buysLeft;

        public AutoTraderHack()
        {
                super("AutoTrader",
                        "While a villager GUI is open, automatically buys"
                                + " from every trade in order.");
                setCategory(Category.ITEMS);
                addSettings(buysPerTrade, skipOutOfStock);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.playerController == null)
                        return;

                if(!(mc.currentScreen instanceof GuiMerchant))
                {
                        currentTrade = 0;
                        buysLeft = 0;
                        return;
                }
                if(!(p.openContainer instanceof ContainerMerchant))
                        return;

                ContainerMerchant container =
                        (ContainerMerchant)p.openContainer;
                IMerchant merchant =
                        ((GuiMerchant)mc.currentScreen).getMerchant();
                MerchantRecipeList trades = merchant.getRecipes(p);
                if(trades == null || trades.isEmpty())
                        return;

                if(timer > 0)
                {
                        timer--;
                        return;
                }
                timer = 6;

                if(buysLeft > 0)
                {
                        // result slot is index 2 in ContainerMerchant
                        mc.playerController.windowClick(
                                container.windowId, 2, 0,
                                ClickType.QUICK_MOVE, p);
                        buysLeft--;
                        return;
                }

                if(currentTrade >= trades.size())
                        return;

                MerchantRecipe recipe = trades.get(currentTrade);
                if(recipe != null && !(skipOutOfStock.isChecked()
                        && recipe.isRecipeDisabled()))
                {
                        // select the trade (C11 enchant packet)
                        mc.playerController.sendEnchantPacket(
                                container.windowId, currentTrade);
                        buysLeft = buysPerTrade.getValueI();
                        currentTrade++;
                }
                else
                        currentTrade++;
        }

        @Override
        protected void onEnable()
        {
                currentTrade = 0;
                buysLeft = 0;
                timer = 0;
        }
}
