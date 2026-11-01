/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI SilkOnlyHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

/**
 * Stops you from accidentally mining ores without a silk touch
 * tool by resetting the dig every tick.
 */
public final class SilkOnlyHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final CheckboxSetting includeGlass =
                new CheckboxSetting("Include glass/ice", true);

        public SilkOnlyHack()
        {
                super("SilkOnly",
                        "Prevents you from breaking ores unless you are"
                                + " holding a silk touch tool.");
                setCategory(Category.BLOCKS);
                addSetting(includeGlass);
        }

        private boolean needsSilk(Block block)
        {
                if(block instanceof BlockOre)
                        return true;
                if(block == Blocks.DIAMOND_ORE || block == Blocks.EMERALD_ORE
                        || block == Blocks.LAPIS_ORE
                        || block == Blocks.REDSTONE_ORE
                        || block == Blocks.LIT_REDSTONE_ORE)
                        return true;
                if(block == Blocks.DIAMOND_BLOCK
                        || block == Blocks.EMERALD_BLOCK
                        || block == Blocks.LAPIS_BLOCK)
                        return true;
                if(includeGlass.isChecked()
                        && (block == Blocks.GLASS || block == Blocks.ICE
                                || block == Blocks.PACKED_ICE
                                || block == Blocks.GLOWSTONE))
                        return true;
                return false;
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(!mc.gameSettings.keyBindAttack.isKeyDown())
                        return;
                if(mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit
                        != RayTraceResult.Type.BLOCK)
                        return;

                BlockPos pos = mc.objectMouseOver.getBlockPos();
                Block block = mc.world.getBlockState(pos).getBlock();
                if(!needsSilk(block))
                        return;

                ItemStack held = p.getHeldItemMainhand();
                int silk = EnchantmentHelper.getEnchantmentLevel(
                        Enchantments.SILK_TOUCH, held);
                if(silk > 0)
                        return;

                // cancel the dig by resetting the breaking state
                mc.playerController.resetBlockRemoving();
                p.swingArm(net.minecraft.util.EnumHand.OFF_HAND);
        }
}
