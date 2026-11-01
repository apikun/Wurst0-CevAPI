/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ChorusFruitHack (GPL-3.0-or-later)
 *
 * 1.12.2 adaptation: chorus fruit exists in 1.12.2, but the CevAPI
 * custom tiers (golden chorus fruit, chorus carrot) do not, so only
 * vanilla chorus fruit is supported. Triggers on damage (health drop)
 * while your health is at or below the configured threshold; the
 * "on player enter" trigger needs the range-alert manager and is not
 * ported. Eating is driven through the vanilla use keybind state.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class ChorusFruitHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting health = new SliderSetting("Health", 4,
                0.5, 9.5, 0.5);
        private final CheckboxSetting onDamage =
                new CheckboxSetting("On damage", true);
        private final CheckboxSetting disableOnActivation =
                new CheckboxSetting("Turn off on activation", false);

        private float lastHealth = -1F;
        private boolean consuming;
        private int oldSlot = -1;
        private int useStartTick = -1;

        public ChorusFruitHack()
        {
                super("ChorusFruit",
                        "Automatically eats a chorus fruit when you take"
                                + " damage at low health, teleporting you to"
                                + " a random nearby position to escape.");
                setCategory(Category.OTHER);
                addSettings(health, onDamage, disableOnActivation);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null)
                {
                        resetState();
                        lastHealth = -1F;
                        return;
                }

                if(consuming)
                {
                        keepConsuming(p);
                        lastHealth = p.getHealth();
                        return;
                }

                handleDamageTrigger(p);
                lastHealth = p.getHealth();
        }

        private void handleDamageTrigger(EntityPlayerSP p)
        {
                if(!onDamage.isChecked())
                        return;

                float current = p.getHealth();
                if(lastHealth < 0F || current >= lastHealth)
                        return;

                tryConsume(p);
        }

        private void tryConsume(EntityPlayerSP p)
        {
                if(consuming)
                        return;

                // like the original: only act at or below the threshold
                if(p.getHealth() > health.getValueF() * 2F)
                        return;

                if(p.getCooldownTracker().hasCooldown(Items.CHORUS_FRUIT))
                        return;

                int slot = findFruitSlot(p);
                if(slot == -1)
                        return;

                oldSlot = p.inventory.currentItem;
                p.inventory.currentItem = slot;
                if(mc.playerController != null)
                        mc.playerController.updateController();

                consuming = true;
                useStartTick = p.ticksExisted;
                setUseKey(true);
                pressUseKey();
        }

        private void keepConsuming(EntityPlayerSP p)
        {
                setUseKey(true);

                // keep chewing until the use action finishes
                if(p.isHandActive() || p.ticksExisted <= useStartTick + 1)
                        return;

                // the press did not start eating (e.g. blocked by a block
                // interaction) - queue another key press and retry
                if(p.ticksExisted <= useStartTick + 8)
                {
                        pressUseKey();
                        useStartTick = p.ticksExisted;
                        return;
                }

                stopConsuming();
                mc.player.sendMessage(new TextComponentString(
                        "\u00a7a[ChorusFruit] Chorus fruit eaten."));
                if(disableOnActivation.isChecked())
                        setEnabled(false);
        }

        private void stopConsuming()
        {
                if(!consuming)
                        return;

                consuming = false;
                setUseKey(false);

                if(oldSlot != -1)
                {
                        EntityPlayerSP p = mc.player;
                        if(p != null)
                        {
                                p.inventory.currentItem = oldSlot;
                                if(mc.playerController != null)
                                        mc.playerController.updateController();
                        }
                        oldSlot = -1;
                }
        }

        private void resetState()
        {
                stopConsuming();
        }

        private void setUseKey(boolean down)
        {
                KeyBinding.setKeyBindState(
                        mc.gameSettings.keyBindUseItem.getKeyCode(), down);
        }

        /** registers one key press so processKeyBinds runs rightClickMouse */
        private void pressUseKey()
        {
                KeyBinding.onTick(
                        mc.gameSettings.keyBindUseItem.getKeyCode());
        }

        private int findFruitSlot(EntityPlayerSP p)
        {
                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(!stack.isEmpty()
                                && stack.getItem() == Items.CHORUS_FRUIT)
                                return i;
                }
                return -1;
        }

        @Override
        protected void onEnable()
        {
                lastHealth = -1F;
                consuming = false;
                oldSlot = -1;
                useStartTick = -1;
        }

        @Override
        protected void onDisable()
        {
                stopConsuming();
        }
}
