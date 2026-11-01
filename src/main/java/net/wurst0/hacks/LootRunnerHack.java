/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI LootRunnerHack (GPL-3.0-or-later)
 * Lite adaptation: the original pathfinds to SeedMapper targets with
 * baritone. This 1.12.2 version keeps the core loop - find nearest
 * unlooted chest, walk to it (pathless steering like TreeBot), open
 * it and shift-click everything into your inventory.
 */
package net.wurst0.hacks;

import java.util.HashSet;

import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockCrops;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
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
 * Walks to the nearest chest in range, opens it and loots it into
 * your inventory, then moves on to the next one.
 */
public final class LootRunnerHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 24, 8, 48, 4);
        private final SliderSetting openDelay =
                new SliderSetting("Open delay (ticks)", 6, 0, 40, 1);
        private final SliderSetting lootDelay =
                new SliderSetting("Loot delay (ticks)", 4, 0, 40, 1);
        private final CheckboxSetting jump =
                new CheckboxSetting("Jump on collision", true);
        private final CheckboxSetting trample =
                new CheckboxSetting("Trample crops", false);

        private final HashSet<BlockPos> doneChests =
                new HashSet<BlockPos>();
        private BlockPos targetChest;
        private boolean chestOpened;
        private int openTimer;
        private int lootTimer;
        private int graceTimer;
        private int idleCooldown;

        public LootRunnerHack()
        {
                super("LootRunner",
                        "Finds the nearest unlooted chest, walks to it,"
                                + " opens it and shift-clicks everything"
                                + " into your inventory (lite, pathless"
                                + " version).");
                setCategory(Category.OTHER);
                addSettings(range, openDelay, lootDelay, jump, trample);
        }

        @Override
        protected void onEnable()
        {
                doneChests.clear();
                targetChest = null;
                chestOpened = false;
                openTimer = 0;
                lootTimer = 0;
                graceTimer = 0;
                idleCooldown = 0;
        }

        @Override
        protected void onDisable()
        {
                doneChests.clear();
                targetChest = null;
                chestOpened = false;
                if(mc.currentScreen instanceof GuiChest)
                        mc.displayGuiScreen(null);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null
                        || mc.playerController == null)
                        return;

                if(idleCooldown > 0)
                {
                        idleCooldown--;
                        return;
                }

                // looting a chest we just opened
                if(chestOpened)
                {
                        if(mc.currentScreen instanceof GuiChest
                                && p.openContainer
                                        instanceof ContainerChest)
                        {
                                lootOpenChest(p);
                                return;
                        }
                        if(graceTimer > 0)
                        {
                                graceTimer--;
                                return;
                        }
                        // GUI never opened (out of reach, protection,
                        // lag) - give up on this chest
                        finishChest(p, false);
                        return;
                }

                // walk to the current target
                if(targetChest != null)
                {
                        walkToChest(p);
                        return;
                }

                targetChest = findNearestChest(p);
                if(targetChest == null)
                {
                        // all known chests handled; reset memory after a
                        // pause so the area can be re-scanned later
                        if(!doneChests.isEmpty())
                                doneChests.clear();
                        idleCooldown = 40;
                        return;
                }
                openTimer = openDelay.getValueI();
        }

        private void walkToChest(EntityPlayerSP p)
        {
                if(!isChest(targetChest))
                {
                        targetChest = null;
                        return;
                }

                double dx = targetChest.getX() + 0.5 - p.posX;
                double dz = targetChest.getZ() + 0.5 - p.posZ;
                double dy = targetChest.getY() + 0.5
                        - (p.posY + p.getEyeHeight());
                float yaw = (float)(MathHelper.atan2(dz, dx)
                        * 180.0 / Math.PI) - 90F;
                float pitch = (float)(MathHelper.atan2(dy,
                        MathHelper.sqrt(dx * dx + dz * dz))
                        * 180.0 / Math.PI);
                p.rotationYaw = interpolateAngle(p.rotationYaw, yaw);
                p.rotationPitch = MathHelper.clamp(pitch, -60F, 60F);

                double distSq = dx * dx + dz * dz;
                if(distSq > 3.2 * 3.2)
                {
                        Vec3d look = p.getLookVec();
                        p.motionX = look.x * 0.14;
                        p.motionZ = look.z * 0.14;
                        if(jump.isChecked() && p.collidedHorizontally
                                && p.onGround)
                                p.jump();
                        if(!trample.isChecked() && p.onGround
                                && isStandingInCrops(p))
                                p.jump();
                        return;
                }

                if(openTimer > 0)
                {
                        openTimer--;
                        return;
                }

                mc.playerController.processRightClickBlock(p, mc.world,
                        targetChest, EnumFacing.UP,
                        new Vec3d(targetChest.getX() + 0.5,
                                targetChest.getY() + 0.5,
                                targetChest.getZ() + 0.5),
                        EnumHand.MAIN_HAND);
                p.swingArm(EnumHand.MAIN_HAND);
                chestOpened = true;
                graceTimer = 10;
                lootTimer = Math.max(lootDelay.getValueI(), 2);
        }

        private void lootOpenChest(EntityPlayerSP p)
        {
                if(lootTimer > 0)
                {
                        lootTimer--;
                        return;
                }

                ContainerChest chest =
                        (ContainerChest)p.openContainer;
                IInventory lower = chest.getLowerChestInventory();
                int size = lower.getSizeInventory();

                if(p.inventory.getFirstEmptyStack() == -1)
                {
                        // inventory full: stop looting this chest
                        finishChest(p, true);
                        return;
                }

                for(int i = 0; i < size; i++)
                {
                        ItemStack stack = lower.getStackInSlot(i);
                        if(stack.isEmpty())
                                continue;
                        mc.playerController.windowClick(
                                p.openContainer.windowId, i, 0,
                                ClickType.QUICK_MOVE, p);
                        lootTimer = lootDelay.getValueI();
                        return;
                }

                // chest is empty: move on
                finishChest(p, true);
        }

        private void finishChest(EntityPlayerSP p, boolean closeGui)
        {
                if(closeGui && mc.currentScreen instanceof GuiChest)
                        mc.displayGuiScreen(null);
                if(targetChest != null)
                        doneChests.add(targetChest);
                targetChest = null;
                chestOpened = false;
                idleCooldown = 10;
        }

        private BlockPos findNearestChest(EntityPlayerSP p)
        {
                int r = Math.min(16, range.getValueI() / 2);
                double bestSq = range.getValue() * range.getValue();
                BlockPos best = null;
                BlockPos center =
                        new BlockPos(p.posX, p.posY, p.posZ);

                java.util.Iterator<BlockPos> itr = BlockPos.getAllInBox(
                        center.add(-r, -4, -r), center.add(r, 4, r))
                        .iterator();
                while(itr.hasNext())
                {
                        BlockPos pos = itr.next();
                        if(doneChests.contains(pos))
                                continue;
                        if(!isChest(pos))
                                continue;
                        double d = pos.distanceSq(center);
                        if(d < bestSq)
                        {
                                bestSq = d;
                                best = pos;
                        }
                }
                return best;
        }

        private boolean isChest(BlockPos pos)
        {
                TileEntity te = mc.world.getTileEntity(pos);
                if(!(te instanceof TileEntityChest))
                        return false;
                return mc.world.getBlockState(pos)
                        .getBlock() instanceof BlockChest;
        }

        private boolean isStandingInCrops(EntityPlayerSP p)
        {
                BlockPos below = new BlockPos(Math.floor(p.posX),
                        Math.floor(p.posY - 0.6), Math.floor(p.posZ));
                return mc.world.getBlockState(below)
                        .getBlock() instanceof BlockCrops;
        }

        private static float interpolateAngle(float from, float to)
        {
                float delta = MathHelper.wrapDegrees(to - from);
                return from + MathHelper.clamp(delta, -30F, 30F);
        }
}
