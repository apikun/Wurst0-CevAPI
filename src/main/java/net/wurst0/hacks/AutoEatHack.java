/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoEatHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.util.ReflectUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class AutoEatHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting foodLevel =
                new SliderSetting("Food level", 15, 1, 19, 1);
        private final CheckboxSetting avoidBadFoods =
                new CheckboxSetting("Avoid bad foods", true);

        private int previousSlot = -1;

        public AutoEatHack()
        {
                super("AutoEat",
                        "Automatically eats food from your hotbar when you are"
                                + " hungry, then switches back to what you were holding.");
                setCategory(Category.ITEMS);
                addSettings(foodLevel, avoidBadFoods);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;
                if(p.openContainer != p.inventoryContainer)
                        return;

                int food = p.getFoodStats().getFoodLevel();

                if(previousSlot != -1)
                {
                        ItemStack held = p.inventory.getCurrentItem();
                        if(food >= 20 || held.isEmpty()
                                || !(held.getItem() instanceof ItemFood))
                                stopEating(p);
                        else
                                doRightClick();
                        return;
                }

                if(food > foodLevel.getValueI())
                        return;

                int slot = findFood(p);
                if(slot == -1)
                        return;

                previousSlot = p.inventory.currentItem;
                p.inventory.currentItem = slot;
                mc.playerController.updateController();
        }

        private void stopEating(EntityPlayerSP p)
        {
                p.inventory.currentItem = previousSlot;
                mc.playerController.updateController();
                previousSlot = -1;
        }

        private int findFood(EntityPlayerSP p)
        {
                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(stack.isEmpty() || !(stack.getItem() instanceof ItemFood))
                                continue;

                        ItemFood food = (ItemFood)stack.getItem();
                        if(food.getHealAmount(stack) <= 0)
                                continue;

                        if(avoidBadFoods.isChecked() && isBadFood(stack.getItem()))
                                continue;

                        return i;
                }
                return -1;
        }

        private boolean isBadFood(Object item)
        {
                return item == Items.ROTTEN_FLESH || item == Items.SPIDER_EYE
                        || item == Items.POISONOUS_POTATO || item == Items.CHICKEN
                        || item == Items.PORKCHOP;
        }

        @Override
        protected void onDisable()
        {
                if(previousSlot != -1 && mc.player != null)
                        stopEating(mc.player);
        }

	private static java.lang.reflect.Method rightClickMouseMethod;

	private void doRightClick()
	{
		if(rightClickMouseMethod == null)
			rightClickMouseMethod = ReflectUtils.method(
				net.minecraft.client.Minecraft.class,
				"rightClickMouse", "func_71007_j");
		if(rightClickMouseMethod == null)
			return;
		try
		{
			rightClickMouseMethod.invoke(mc);
		}catch(Throwable t)
		{
			// reflection failed
		}
	}
}
