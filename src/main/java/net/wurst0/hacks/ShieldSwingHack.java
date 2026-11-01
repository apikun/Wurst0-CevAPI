/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ShieldSwingHack (GPL-3.0-or-later)
 * Lite adaptation: 1.12.2 already sends the attack while blocking,
 * but vanilla suppresses the swing animation while isHandActive(),
 * so we force swingArm() each tick while the shield is up and
 * perform the attack + swing on attack presses. Auto-hold uses
 * KeyBinding.setKeyBindState and auto-equip uses container clicks
 * (offhand is slot 45 in ContainerPlayer).
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Lets you attack with a visible swing while blocking with a
 * shield, and can auto-hold and auto-equip the shield for you.
 */
public final class ShieldSwingHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting autoHoldShield =
                new CheckboxSetting("Auto hold shield", false);
        private final CheckboxSetting autoEquipShield =
                new CheckboxSetting("Auto equip shield", false);

        private boolean attackKeyWasDown;
        private boolean forcingUseKey;

        public ShieldSwingHack()
        {
                super("ShieldSwing",
                        "Lets you attack with a visible swing while"
                                + " blocking with a shield, and can"
                                + " auto-hold and auto-equip it.");
                setCategory(Category.COMBAT);
                addSettings(autoHoldShield, autoEquipShield);
        }

        @Override
        protected void onEnable()
        {
                attackKeyWasDown = false;
                forcingUseKey = false;
        }

        @Override
        protected void onDisable()
        {
                releaseUseKey();
                attackKeyWasDown = false;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.playerController == null)
                {
                        releaseUseKey();
                        attackKeyWasDown = false;
                        return;
                }

                tryAutoEquipShield(p);
                tryAutoHoldShield(p);

                if(mc.currentScreen != null)
                {
                        attackKeyWasDown = false;
                        return;
                }

                boolean attackDown =
                        mc.gameSettings.keyBindAttack.isKeyDown();
                boolean pressed = attackDown && !attackKeyWasDown;
                attackKeyWasDown = attackDown;

                boolean blocking = p.isHandActive()
                        && isShield(p.getActiveItemStack());

                if(blocking)
                {
                        // vanilla suppresses the swing while
                        // isHandActive(); force it so the swing
                        // shows while blocking
                        if(attackDown)
                                p.swingArm(EnumHand.MAIN_HAND);

                        if(pressed
                                && mc.objectMouseOver != null
                                && mc.objectMouseOver.typeOfHit
                                        == RayTraceResult.Type.ENTITY
                                && mc.objectMouseOver
                                        .entityHit instanceof EntityLivingBase)
                        {
                                mc.playerController.attackEntity(p,
                                        mc.objectMouseOver.entityHit);
                                p.swingArm(EnumHand.MAIN_HAND);
                        }
                }
        }

        private void tryAutoHoldShield(EntityPlayerSP p)
        {
                boolean shouldForce = autoHoldShield.isChecked()
                        && mc.currentScreen == null
                        && isShield(p.getHeldItemOffhand());

                if(shouldForce)
                {
                        KeyBinding.setKeyBindState(
                                mc.gameSettings.keyBindUseItem
                                        .getKeyCode(),
                                true);
                        forcingUseKey = true;
                        return;
                }

                releaseUseKey();
        }

        private void tryAutoEquipShield(EntityPlayerSP p)
        {
                if(!autoEquipShield.isChecked())
                        return;
                if(!p.getHeldItemOffhand().isEmpty())
                        return;

                int shieldSlot = -1;
                for(int i = 0; i < p.inventory.mainInventory.size(); i++)
                {
                        if(isShield(p.inventory.mainInventory.get(i)))
                        {
                                shieldSlot = i;
                                break;
                        }
                }
                if(shieldSlot == -1)
                        return;

                // mainInventory 0-8 is hotbar -> container slots 36-44,
                // 9-35 maps to container slots 9-35; offhand is slot 45
                int containerSlot =
                        shieldSlot < 9 ? 36 + shieldSlot : shieldSlot;
                int windowId = p.inventoryContainer.windowId;
                mc.playerController.windowClick(windowId, containerSlot,
                        0, ClickType.PICKUP, p);
                mc.playerController.windowClick(windowId, 45, 0,
                        ClickType.PICKUP, p);
        }

        private void releaseUseKey()
        {
                if(!forcingUseKey)
                        return;
                KeyBinding.setKeyBindState(
                        mc.gameSettings.keyBindUseItem.getKeyCode(),
                        false);
                forcingUseKey = false;
        }

        private static boolean isShield(ItemStack stack)
        {
                return stack != null && !stack.isEmpty()
                        && stack.getItem() instanceof ItemShield;
        }
}
