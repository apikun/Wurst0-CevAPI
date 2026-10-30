/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FastBreakHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;
import net.wurst0.util.ReflectUtils;

public final class FastBreakHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting speed =
                new SliderSetting("Speed", 0.3, 0.05, 1, 0.05);

        private Field curBlockDamageMP;
        private Field blockHitDelay;

        public FastBreakHack()
        {
                super("FastBreak",
                        "Speeds up block breaking by adding extra mining progress"
                                + " every tick and removing the vanilla break delay.");
                setCategory(Category.BLOCKS);
                addSetting(speed);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                PlayerControllerMP pc = mc.playerController;
                if(p == null || mc.world == null || pc == null)
                        return;
                if(mc.objectMouseOver == null
                        || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK)
                        return;
                if(!pc.getIsHittingBlock())
                        return;

                if(curBlockDamageMP == null)
                        curBlockDamageMP = ReflectUtils.field(PlayerControllerMP.class,
                                "curBlockDamageMP", "field_78770_f");
                if(blockHitDelay == null)
                        blockHitDelay = ReflectUtils.field(PlayerControllerMP.class,
                                "blockHitDelay", "field_78781_i");
                if(curBlockDamageMP == null)
                        return;

                try
                {
                        if(blockHitDelay != null)
                                blockHitDelay.setInt(pc, 0);
                        float progress = curBlockDamageMP.getFloat(pc);
                        curBlockDamageMP.setFloat(pc,
                                Math.min(1F, progress + speed.getValueF()));
                }catch(Throwable t)
                {
                        // reflection failed - silently keep vanilla speed
                }
        }
}
