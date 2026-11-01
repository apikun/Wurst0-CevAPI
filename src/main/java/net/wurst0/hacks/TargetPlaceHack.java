/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI TargetPlaceHack (GPL-3.0-or-later)
 * Lite adaptation: places a block under the entity you are
 * looking at (traps it in place).
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class TargetPlaceHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 4, 0, 20, 1);

        private int timer;

        public TargetPlaceHack()
        {
                super("TargetPlace",
                        "Places a block under the entity you are looking at,"
                                + " e.g. to trap mobs or box in players.");
                setCategory(Category.BLOCKS);
                addSetting(delay);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(delay.getValueI() > 0 && --timer > 0)
                        return;

                if(mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit
                        != RayTraceResult.Type.ENTITY)
                        return;

                Entity target = mc.objectMouseOver.entityHit;
                if(target == null || target.isDead)
                        return;

                BlockPos pos = new BlockPos(target.posX,
                        Math.floor(target.posY) - 1, target.posZ);
                if(!mc.world.isAirBlock(pos.up()))
                        return;

                int slot = findBlockSlot(p);
                if(slot == -1)
                        return;

                if(p.inventory.currentItem != slot)
                {
                        p.inventory.currentItem = slot;
                        mc.playerController.updateController();
                        return;
                }

                mc.playerController.processRightClickBlock(p, mc.world, pos, EnumFacing.UP, new Vec3d(pos.getX() + 0.5, pos.getY() + 1,
                                pos.getZ() + 0.5), EnumHand.MAIN_HAND);
                p.swingArm(EnumHand.MAIN_HAND);
                timer = delay.getValueI();
        }

        private int findBlockSlot(EntityPlayerSP p)
        {
                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(!stack.isEmpty()
                                && stack.getItem() instanceof ItemBlock)
                                return i;
                }
                return -1;
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
        }
}
