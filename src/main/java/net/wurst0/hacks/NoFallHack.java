/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoFallHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.CPacketPlayer;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;

public final class NoFallHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting mode = new ModeSetting("Mode",
                new String[]{"Packet", "Client"}, 0);

        private boolean sentThisFall;

        public NoFallHack()
        {
                super("NoFall",
                        "Protects you from fall damage. Packet mode tells the server"
                                + " you already landed right before you hit the ground,"
                                + " Client mode just resets your fall distance locally.");
                setCategory(Category.MOVEMENT);
                addSetting(mode);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if("Packet".equals(mode.getSelected()))
                {
                        if(p.onGround)
                        {
                                sentThisFall = false;
                                return;
                        }

                        // Tell the server we are on the ground shortly before the
                        // impact so it never computes fall damage for this fall.
                        if(!sentThisFall && p.fallDistance > 2 && p.motionY < -0.5D
                                && !p.isInWater() && !p.isOnLadder())
                        {
                                p.connection.sendPacket(new CPacketPlayer(true));
                                sentThisFall = true;
                        }
                }

                p.fallDistance = 0;
        }

        @Override
        protected void onEnable()
        {
                sentThisFall = false;
        }
}
