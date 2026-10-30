/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiVoidHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

public final class AntiVoidHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting triggerY =
                new SliderSetting("Trigger Y", -8, -64, 0, 1);

        private BlockPos lastGround;

        public AntiVoidHack()
        {
                super("AntiVoid",
                        "Teleports you back up when you fall into the void, using"
                                + " the last position where you stood on solid ground.");
                setCategory(Category.MOVEMENT);
                addSetting(triggerY);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(p.onGround && p.posY > 0)
                        lastGround = new BlockPos(p.posX, p.posY, p.posZ);

                if(p.onGround || p.posY >= triggerY.getValue() || lastGround == null)
                        return;
                if(p.isRiding())
                        return;

                double x = p.posX;
                double y = lastGround.getY() + 1.1D;
                double z = p.posZ;
                p.setPosition(x, y, z);
                p.connection.sendPacket(new CPacketPlayer.Position(x,
                        y, z, true));
                p.motionY = 0;
                p.fallDistance = 0;
        }

        @Override
        protected void onEnable()
        {
                lastGround = null;
        }
}
