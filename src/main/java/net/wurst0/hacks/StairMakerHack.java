/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI StairMakerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * While you are jumping forward (e.g. climbing a hill), places a
 * block behind you so you can sprint-build staircases.
 */
public final class StairMakerHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 3, 0, 10, 1);

        private int timer;
        private BlockPos lastPlaced;

        public StairMakerHack()
        {
                super("StairMaker",
                        "Automatically builds a staircase under you while"
                                + " you jump forward.");
                setCategory(Category.BLOCKS);
                addSetting(delay);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(timer > 0)
                {
                        timer--;
                        return;
                }

                if(!p.onGround && p.motionY > 0
                        && mc.gameSettings.keyBindForward.isKeyDown())
                {
                        BlockPos pos = new BlockPos(MathHelper.floor(p.posX),
                                MathHelper.floor(p.posY) - 1,
                                MathHelper.floor(p.posZ));

                        if(!mc.world.isAirBlock(pos))
                                return;
                        if(pos.equals(lastPlaced))
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

                        mc.playerController.processRightClickBlock(p, mc.world, pos, EnumFacing.UP, net.minecraft.util.math.Vec3d.ZERO, EnumHand.MAIN_HAND);
                        p.swingArm(EnumHand.MAIN_HAND);
                        lastPlaced = pos;
                        timer = delay.getValueI();
                }
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
                lastPlaced = null;
        }
}
