/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AirPlaceHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * Places blocks in mid-air: right-clicking while enabled sends the
 * placement packet at the position you are looking at, even if it
 * has no adjacent block face. Vanilla servers accept it as long as
 * the position is within reach.
 */
public final class AirPlaceHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 4.5, 2, 6, 0.5);

        public AirPlaceHack()
        {
                super("AirPlace",
                        "Lets you place blocks in mid-air without clicking"
                                + " on a block face.");
                setCategory(Category.BLOCKS);
                addSetting(range);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(!mc.gameSettings.keyBindUseItem.isKeyDown())
                        return;

                ItemStack held = p.getHeldItemMainhand();
                if(held.isEmpty() || !(held.getItem() instanceof ItemBlock))
                        return;

                Vec3d eyes = p.getPositionVector()
                        .add(0, p.getEyeHeight(), 0);
                Vec3d look = p.getLookVec();
                Vec3d hit = eyes.add(
                        look.x * range.getValueF(),
                        look.y * range.getValueF(),
                        look.z * range.getValueF());

                BlockPos pos = new BlockPos(hit);
                if(!mc.world.isAirBlock(pos))
                        return;

                mc.playerController.processRightClickBlock(p, mc.world, pos, EnumFacing.UP, hit, EnumHand.MAIN_HAND);
                p.swingArm(EnumHand.MAIN_HAND);
        }
}
