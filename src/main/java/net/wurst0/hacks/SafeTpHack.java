/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SafeTpHack (GPL-3.0-or-later)
 * Lite adaptation: Teleport that refuses to step into lava, fire,
 * cactus or the void, and warns you instead.
 */
package net.wurst0.hacks;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class SafeTpHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting step =
                new SliderSetting("Step", 4, 0.5, 32, 0.5);
        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 10, 0, 40, 1);

        private int timer;
        private int warnCooldown;

        public SafeTpHack()
        {
                super("SafeTp",
                        "Like Teleport, but refuses to teleport into lava,"
                                + " fire, cactus or the void.");
                setCategory(Category.MOVEMENT);
                addSettings(step, delay);
        }

        private boolean isDanger(BlockPos pos)
        {
                IBlockState state = mc.world.getBlockState(pos);
                return state.getMaterial() == Material.LAVA
                        || state.getBlock() == Blocks.FIRE
                        || state.getBlock() == Blocks.CACTUS
                        || state.getBlock() == Blocks.MAGMA;
        }

        private boolean isSafeDestination(BlockPos feet)
        {
                BlockPos head = feet.up();
                BlockPos ground = feet.down();

                IBlockState groundState = mc.world.getBlockState(ground);
                if(groundState.getMaterial() == Material.AIR
                        || groundState.getMaterial().isLiquid())
                        return false; // would fall / void
                if(!mc.world.getBlockState(feet).getMaterial()
                        .isReplaceable())
                        return false; // feet blocked
                if(mc.world.getBlockState(head).getMaterial()
                        != Material.AIR)
                        return false; // head blocked

                // no danger in the 3x3 area around the landing spot
                for(int dx = -1; dx <= 1; dx++)
                {
                        for(int dz = -1; dz <= 1; dz++)
                        {
                                if(isDanger(feet.add(dx, 0, dz))
                                        || isDanger(ground.add(dx, 0, dz)))
                                        return false;
                        }
                }
                return true;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(warnCooldown > 0)
                        warnCooldown--;

                if(delay.getValueI() > 0 && --timer > 0)
                        return;
                timer = delay.getValueI();

                double amount = step.getValue();
                float yaw = p.rotationYaw;
                float pitch = p.rotationPitch;
                double dx = -MathHelper.sin(yaw * 0.017453292F)
                        * MathHelper.cos(pitch * 0.017453292F);
                double dz = MathHelper.cos(yaw * 0.017453292F)
                        * MathHelper.cos(pitch * 0.017453292F);
                double dy = -MathHelper.sin(pitch * 0.017453292F);

                // snap to a safe landing: follow the look vector, then
                // drop onto the first floor that is safe
                double tx = p.posX + dx * amount;
                double ty = p.posY + dy * amount;
                double tz = p.posZ + dz * amount;

                BlockPos target = new BlockPos(
                        MathHelper.floor(tx), MathHelper.floor(ty),
                        MathHelper.floor(tz));

                int tries = 0;
                while(!isSafeDestination(target) && tries++ < 16)
                        target = target.up();

                if(!isSafeDestination(target))
                {
                        if(warnCooldown == 0)
                        {
                                warnLocally(p);
                                warnCooldown = 100;
                        }
                        return;
                }

                p.setPositionAndUpdate(target.getX() + 0.5,
                        target.getY(), target.getZ() + 0.5);
        }

        private void warnLocally(EntityPlayer p)
        {
                net.minecraft.util.text.TextComponentString msg =
                        new net.minecraft.util.text.TextComponentString(
                                "\u00a7c[SafeTp] Destination unsafe -"
                                        + " step cancelled.");
                p.sendMessage(msg);
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
                warnCooldown = 0;
        }
}
