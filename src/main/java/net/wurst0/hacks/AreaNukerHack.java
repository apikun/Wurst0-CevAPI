/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AreaNukerHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;
import java.util.Iterator;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.ReflectUtils;

public final class AreaNukerHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 4.5, 1, 5, 0.1);
        private final SliderSetting perTick =
                new SliderSetting("Blocks per tick", 8, 1, 32, 1);
        private final CheckboxSetting swing =
                new CheckboxSetting("Swing", true);

        private Field curBlockDamageMP;

        public AreaNukerHack()
        {
                super("AreaNuker",
                        "Breaks every breakable block in range, a few blocks per"
                                + " tick. Server movement checks may slow it down.");
                setCategory(Category.BLOCKS);
                addSettings(range, perTick, swing);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                PlayerControllerMP pc = mc.playerController;
                if(p == null || mc.world == null || pc == null)
                        return;

                if(curBlockDamageMP == null)
                        curBlockDamageMP = ReflectUtils.field(PlayerControllerMP.class,
                                "curBlockDamageMP", "field_78770_f");

                double rangeSq = range.getValue() * range.getValue();
                int r = (int)Math.ceil(range.getValue());

                BlockPos center = new BlockPos(p.posX, p.posY, p.posZ);
                Iterator<BlockPos> itr = BlockPos.getAllInBox(
                        center.add(-r, -r, -r), center.add(r, r, r)).iterator();

                int broken = 0;
                while(itr.hasNext() && broken < perTick.getValueI())
                {
                        BlockPos pos = itr.next();
                        IBlockState state = mc.world.getBlockState(pos);
                        Material material = state.getMaterial();
                        if(material == Material.AIR || material.isLiquid())
                                continue;
                        if(state.getBlockHardness(mc.world, pos) < 0)
                                continue;
                        if(p.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5,
                                pos.getZ() + 0.5) > rangeSq)
                                continue;

                        pc.clickBlock(pos, EnumFacing.UP);
                        if(curBlockDamageMP != null)
                        {
                                try
                                {
                                        curBlockDamageMP.setFloat(pc, 1F);
                                }catch(Throwable t)
                                {
                                        // no instant break without the field
                                }
                        }
                        pc.onPlayerDamageBlock(pos, EnumFacing.UP);
                        if(swing.isChecked())
                                p.swingArm(EnumHand.MAIN_HAND);
                        broken++;
                }
        }
}
