/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AttributeSwapHack (GPL-3.0-or-later)
 * Lite adaptation: the original is a smart hotbar swapper (not NBT
 * attribute editing). Mace/spear logic has no 1.12.2 equivalent, so
 * Smart mode swaps between swords/axes and (with Item saver) trash
 * items instead.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.WurstClient;

/**
 * Swaps to the best hotbar item when you attack and (optionally)
 * swaps back after a short delay. Can also reserve the target slot
 * so you never swing with it by accident.
 */
public final class AttributeSwapHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting mode = new ModeSetting("Mode",
                new String[]{"Simple", "Smart"}, 0);
        private final SliderSetting targetSlot =
                new SliderSetting("Target slot", 1, 1, 9, 1);
        private final CheckboxSetting reserveTargetSlot =
                new CheckboxSetting("Reserve target slot", false);
        private final CheckboxSetting swapBack =
                new CheckboxSetting("Swap back", true);
        private final SliderSetting swapBackDelay =
                new SliderSetting("Swap-back delay (ticks)", 2, 0, 20, 1);
        private final CheckboxSetting shieldBreaker =
                new CheckboxSetting("Shield breaker (Smart)", true);
        private final CheckboxSetting itemSaver =
                new CheckboxSetting("Item saver (Smart)", true);
        private final CheckboxSetting onlyAgainstPlayers =
                new CheckboxSetting("Only against players", false);
        private final CheckboxSetting onlyAgainstMobs =
                new CheckboxSetting("Only against mobs", false);
        private final CheckboxSetting onlyWithKillaura =
                new CheckboxSetting("Only with Killaura", false);

        private boolean attackKeyWasDown;
        private int backTimer;
        private boolean awaitingBack;
        private int originalSlot = -1;
        private int lastSafeSlot = -1;
        private int protectedSlotBypassTicks;

        public AttributeSwapHack()
        {
                super("AttributeSwap",
                        "Automatically swaps to the best hotbar item when"
                                + " you attack (axe against blockers, spare"
                                + " weapon or trash item with Item saver)"
                                + " and swaps back after a short delay.");
                setCategory(Category.COMBAT);
                addSettings(mode, targetSlot, reserveTargetSlot, swapBack,
                        swapBackDelay, shieldBreaker, itemSaver,
                        onlyAgainstPlayers, onlyAgainstMobs,
                        onlyWithKillaura);
        }

        @Override
        protected void onEnable()
        {
                backTimer = 0;
                awaitingBack = false;
                originalSlot = -1;
                lastSafeSlot = -1;
                protectedSlotBypassTicks = 0;
                attackKeyWasDown = false;
        }

        @Override
        protected void onDisable()
        {
                if(awaitingBack && mc.player != null)
                {
                        doSwapTo(mc.player, originalSlot);
                        awaitingBack = false;
                }
                backTimer = 0;
                originalSlot = -1;
                lastSafeSlot = -1;
                protectedSlotBypassTicks = 0;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null
                        || mc.playerController == null)
                        return;

                if(protectedSlotBypassTicks > 0)
                        protectedSlotBypassTicks--;

                if(awaitingBack && backTimer-- <= 0)
                {
                        doSwapTo(p, originalSlot);
                        awaitingBack = false;
                        originalSlot = -1;
                }

                enforceReservedTargetSlot(p);

                boolean attackDown =
                        mc.gameSettings.keyBindAttack.isKeyDown();
                boolean pressed = attackDown && !attackKeyWasDown;
                attackKeyWasDown = attackDown;

                if(!pressed || mc.objectMouseOver == null
                        || mc.objectMouseOver.typeOfHit
                                != RayTraceResult.Type.ENTITY
                        || mc.objectMouseOver.entityHit == null)
                        return;

                Entity target = mc.objectMouseOver.entityHit;
                if(!shouldSwapForTarget(target))
                        return;

                int slot = mode.getSelected().equals("Simple")
                        ? targetSlot.getValueI() - 1
                        : getSmartSlot(p, target);
                doSwap(p, slot);
        }

        private void doSwap(EntityPlayerSP p, int slotIndex)
        {
                if(slotIndex < 0 || slotIndex > 8)
                        return;
                if(slotIndex == p.inventory.currentItem)
                        return;

                if(originalSlot == -1)
                        originalSlot = p.inventory.currentItem;
                doSwapTo(p, slotIndex);

                if(swapBack.isChecked())
                {
                        awaitingBack = true;
                        backTimer = swapBackDelay.getValueI();
                }
        }

        private void doSwapTo(EntityPlayerSP p, int slotIndex)
        {
                if(slotIndex < 0 || slotIndex > 8)
                        return;
                protectedSlotBypassTicks =
                        Math.max(protectedSlotBypassTicks, 2);
                p.inventory.currentItem = slotIndex;
                mc.playerController.updateController();
        }

        private void enforceReservedTargetSlot(EntityPlayerSP p)
        {
                if(!reserveTargetSlot.isChecked())
                        return;

                int protectedSlot = targetSlot.getValueI() - 1;
                if(p.inventory.currentItem != protectedSlot)
                {
                        lastSafeSlot = p.inventory.currentItem;
                        return;
                }

                if(protectedSlotBypassTicks > 0)
                        return;

                int fallback =
                        getFallbackSlot(protectedSlot, p.inventory.currentItem);
                if(fallback != -1 && fallback != p.inventory.currentItem)
                {
                        doSwapTo(p, fallback);
                        lastSafeSlot = fallback;
                }
        }

        private int getFallbackSlot(int protectedSlot, int current)
        {
                if(lastSafeSlot >= 0 && lastSafeSlot <= 8
                        && lastSafeSlot != protectedSlot)
                        return lastSafeSlot;

                for(int offset = 1; offset < 9; offset++)
                {
                        int right = (current + offset) % 9;
                        if(right != protectedSlot)
                                return right;

                        int left = (current - offset + 9) % 9;
                        if(left != protectedSlot)
                                return left;
                }
                return -1;
        }

        private int getSmartSlot(EntityPlayerSP p, Entity target)
        {
                if(shieldBreaker.isChecked()
                        && target instanceof EntityLivingBase
                        && ((EntityLivingBase)target)
                                .isActiveItemStackBlocking())
                {
                        int axeSlot =
                                indexOfItem(p, ItemAxe.class, 9);
                        if(axeSlot != -1
                                && axeSlot != p.inventory.currentItem)
                                return axeSlot;
                }

                int bestSlot = -1;
                int bestScore = getDurabilityScore(
                        p.getHeldItemMainhand());
                for(int slot = 0; slot < 9; slot++)
                {
                        if(slot == p.inventory.currentItem)
                                continue;
                        ItemStack stack =
                                p.inventory.mainInventory.get(slot);
                        if(stack.isEmpty())
                                continue;
                        int score = getDurabilityScore(stack);
                        if(score > bestScore)
                        {
                                bestScore = score;
                                bestSlot = slot;
                        }
                }
                return bestSlot;
        }

        private boolean shouldSwapForTarget(Entity target)
        {
                if(onlyWithKillaura.isChecked())
                {
                        Hack killaura = WurstClient.INSTANCE.getHacks()
                                .getByName("Killaura");
                        if(killaura == null || !killaura.isEnabled())
                                return false;
                }

                boolean restrictPlayers =
                        onlyAgainstPlayers.isChecked();
                boolean restrictMobs = onlyAgainstMobs.isChecked();
                if(!restrictPlayers && !restrictMobs)
                        return true;
                if(!(target instanceof EntityLivingBase))
                        return false;
                if(target instanceof EntityPlayer)
                        return restrictPlayers;
                return restrictMobs;
        }

        private int getDurabilityScore(ItemStack stack)
        {
                if(!itemSaver.isChecked())
                        return 0;
                if(!stack.isItemStackDamageable())
                        return 4;
                return 0;
        }

        private static int indexOfItem(EntityPlayerSP p,
                Class<?> itemClass, int count)
        {
                for(int i = 0; i < count; i++)
                {
                        ItemStack stack =
                                p.inventory.mainInventory.get(i);
                        if(!stack.isEmpty()
                                && itemClass.isInstance(stack.getItem()))
                                return i;
                }
                return -1;
        }
}
