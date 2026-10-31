/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ClutchFallHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Auto water-bucket clutch (MLG): when you fall far enough, it swaps
 * to a water bucket and places the water on the ground below you.
 * Approximate port: uses an artificial straight-down raytrace instead
 * of the original aim logic.
 */
public final class ClutchFallHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting minFallDistance =
                new SliderSetting("Minimum fall distance", 3, 2, 10, 0.5);
        private final CheckboxSetting hotbarOnly =
                new CheckboxSetting("Hotbar only", true);

        private int bucketSlot = -1;
        private int previousSlot = -1;
        private boolean clutching;

        public ClutchFallHack()
        {
                super("ClutchFall",
                        "Automatically places water under you when you fall, like"
                                + " a manual MLG clutch.");
                setCategory(Category.MOVEMENT);
                addSettings(minFallDistance, hotbarOnly);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(!clutching)
                {
                        if(p.onGround || p.fallDistance < minFallDistance.getValue())
                                return;
                        if(p.motionY > 0)
                                return;
                        // already in liquid - no clutch needed
                        if(p.isInWater())
                                return;

                        int slot = findBucketSlot(p);
                        if(slot == -1)
                                return;

                        previousSlot = p.inventory.currentItem;
                        bucketSlot = slot;
                        p.inventory.currentItem = slot;
                        mc.playerController.updateController();
                        clutching = true;
                        return;
                }

                // clutch active: swap back once we are safe again
                if(p.onGround || p.fallDistance == 0 || p.isInWater())
                {
                        restoreSlot(p);
                        return;
                }

                // place water on the first solid block below the feet
                BlockPos target = findGroundBelow(p, 5);
                if(target == null)
                        return;

                Vec3d hitVec = new Vec3d(target.getX() + 0.5D,
                        target.getY() + 1D, target.getZ() + 0.5D);
                mc.playerController.processRightClickBlock(p, mc.world, target,
                        EnumFacing.UP, hitVec, EnumHand.MAIN_HAND);
                p.swingArm(EnumHand.MAIN_HAND);
        }

        @Override
        protected void onDisable()
        {
                if(mc.player != null)
                        restoreSlot(mc.player);
        }

        private void restoreSlot(EntityPlayerSP p)
        {
                if(previousSlot != -1 && clutching)
                {
                        p.inventory.currentItem = previousSlot;
                        mc.playerController.updateController();
                }
                clutching = false;
                bucketSlot = -1;
                previousSlot = -1;
        }

        private int findBucketSlot(EntityPlayerSP p)
        {
                int slots = hotbarOnly.isChecked() ? 9 : p.inventory.mainInventory.size();
                for(int i = 0; i < slots; i++)
                {
                        ItemStack stack = p.inventory.mainInventory.get(i);
                        if(!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET)
                                return i;
                }
                return -1;
        }

        private BlockPos findGroundBelow(EntityPlayerSP p, int maxDepth)
        {
                int x = MathHelper.floor(p.posX);
                int z = MathHelper.floor(p.posZ);
                int startY = MathHelper.floor(p.posY);
                for(int y = startY - 1; y >= startY - maxDepth; y--)
                {
                        BlockPos pos = new BlockPos(x, y, z);
                        IBlockState state = mc.world.getBlockState(pos);
                        Material m = state.getMaterial();
                        if(m != Material.AIR && !m.isLiquid())
                                return pos;
                }
                return null;
        }
}
