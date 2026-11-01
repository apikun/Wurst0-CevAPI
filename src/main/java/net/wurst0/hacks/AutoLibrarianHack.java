/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoLibrarianHack (GPL-3.0-or-later)
 * Lite adaptation: 1.12.2 has no lecterns or profession re-rolling,
 * so the "train a librarian" loop is mapped to its 1.12 equivalent:
 * when trading with a villager, automatically pick the best
 * enchanted-book offer (highest level, lowest emerald price) and
 * optionally buy it once to lock the trade in (like the original's
 * "Lock in trade").
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiMerchant;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.IMerchant;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.village.MerchantRecipe;
import net.minecraft.village.MerchantRecipeList;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * While a villager GUI is open, scans its trades for enchanted
 * books and announces (and optionally auto-buys) the best offer.
 */
public final class AutoLibrarianHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting minLevel =
                new SliderSetting("Min book level", 1, 1, 5, 1);
        private final SliderSetting maxPrice =
                new SliderSetting("Max price (emeralds)", 64, 1, 64, 1);
        private final CheckboxSetting autoBuy =
                new CheckboxSetting("Auto-buy best book", true);
        private final CheckboxSetting announce =
                new CheckboxSetting("Announce in chat", true);

        private boolean handledThisGui;
        private int buyTimer;
        private int buysLeft;

        public AutoLibrarianHack()
        {
                super("AutoLibrarian",
                        "While trading with a villager, automatically"
                                + " finds the best enchanted book offer"
                                + " (highest level for the lowest price)"
                                + " and can buy it once to lock the trade"
                                + " in.");
                setCategory(Category.OTHER);
                addSettings(minLevel, maxPrice, autoBuy, announce);
        }

        @Override
        protected void onEnable()
        {
                handledThisGui = false;
                buyTimer = 0;
                buysLeft = 0;
        }

        @Override
        protected void onDisable()
        {
                handledThisGui = false;
                buyTimer = 0;
                buysLeft = 0;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.playerController == null)
                        return;

                if(!(mc.currentScreen instanceof GuiMerchant)
                        || !(p.openContainer instanceof ContainerMerchant))
                {
                        handledThisGui = false;
                        buysLeft = 0;
                        return;
                }

                if(buysLeft > 0)
                {
                        if(buyTimer > 0)
                        {
                                buyTimer--;
                                return;
                        }
                        buyTimer = 6;
                        // result slot is index 2 in ContainerMerchant
                        mc.playerController.windowClick(
                                p.openContainer.windowId, 2, 0,
                                ClickType.QUICK_MOVE, p);
                        buysLeft--;
                        return;
                }

                if(handledThisGui)
                        return;
                handledThisGui = true;

                IMerchant merchant =
                        ((GuiMerchant)mc.currentScreen).getMerchant();
                MerchantRecipeList trades = merchant.getRecipes(p);
                if(trades == null || trades.isEmpty())
                        return;

                int bestIndex = -1;
                int bestLevel = -1;
                int bestPrice = Integer.MAX_VALUE;
                for(int i = 0; i < trades.size(); i++)
                {
                        MerchantRecipe recipe = trades.get(i);
                        if(recipe == null || recipe.isRecipeDisabled())
                                continue;

                        ItemStack sell = recipe.getItemToSell();
                        NBTTagList stored =
                                getStoredEnchantments(sell);
                        if(stored == null || stored.tagCount() == 0)
                                continue;

                        NBTTagCompound first =
                                stored.getCompoundTagAt(0);
                        int level = first.getShort("lvl");
                        if(level < minLevel.getValueI())
                                continue;

                        int price = getEmeraldPrice(recipe);
                        if(price > maxPrice.getValueI())
                                continue;

                        if(level > bestLevel
                                || (level == bestLevel
                                        && price < bestPrice))
                        {
                                bestLevel = level;
                                bestPrice = price;
                                bestIndex = i;
                        }
                }

                if(bestIndex == -1)
                        return;

                MerchantRecipe best = trades.get(bestIndex);
                if(announce.isChecked())
                {
                        String name = "?";
                        NBTTagList stored =
                                getStoredEnchantments(
                                        best.getItemToSell());
                        if(stored != null && stored.tagCount() > 0)
                        {
                                Enchantment ench =
                                        Enchantment.getEnchantmentByID(
                                                stored.getCompoundTagAt(0)
                                                        .getShort("id"));
                                if(ench != null)
                                        name = ench.getTranslatedName(
                                                bestLevel);
                        }
                        p.sendMessage(new TextComponentString(
                                "[AutoLibrarian] Best offer: " + name
                                        + " for "
                                        + getEmeraldPrice(best)
                                        + " emerald(s)"));
                }

                if(autoBuy.isChecked())
                {
                        // select the trade, then shift-click the result
                        mc.playerController.sendEnchantPacket(
                                p.openContainer.windowId, bestIndex);
                        buysLeft = 1;
                        buyTimer = 6;
                }
        }

        private static NBTTagList getStoredEnchantments(
                ItemStack stack)
        {
                if(stack.isEmpty()
                        || stack.getItem() != Items.ENCHANTED_BOOK)
                        return null;
                NBTTagCompound nbt = stack.getTagCompound();
                if(nbt == null || !nbt.hasKey("StoredEnchantments", 9))
                        return null;
                return nbt.getTagList("StoredEnchantments", 9);
        }

        private static int getEmeraldPrice(MerchantRecipe recipe)
        {
                int price = 0;
                if(!recipe.getItemToBuy().isEmpty()
                        && recipe.getItemToBuy().getItem()
                                == Items.EMERALD)
                        price += recipe.getItemToBuy().getCount();
                if(recipe.hasSecondItemToBuy()
                        && !recipe.getSecondItemToBuy().isEmpty()
                        && recipe.getSecondItemToBuy().getItem()
                                == Items.EMERALD)
                        price += recipe.getSecondItemToBuy().getCount();
                return price;
        }
}
