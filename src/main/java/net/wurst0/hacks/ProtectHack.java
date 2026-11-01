/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ProtectHack (GPL-3.0-or-later)
 * Lite adaptation: protects a sphere around the position you enable
 * it at from the Wurst0 mass-diggers (Nuker, AreaNuker, AirMiner,
 * SpeedNuker, Excavator, SuperInstaMine).
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.wurst0.Category;
import net.wurst0.event.events.RenderEvent;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

public final class ProtectHack extends Hack implements RenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting radius =
                new SliderSetting("Radius", 4, 1, 16, 1);
        private final CheckboxSetting showZone =
                new CheckboxSetting("Show zone", true);

        private double centerX;
        private double centerY;
        private double centerZ;

        public ProtectHack()
        {
                super("Protect",
                        "Stops the mass-digging hacks from breaking blocks"
                                + " around the spot you enable it at.");
                setCategory(Category.OTHER);
                addSettings(radius, showZone);
        }

        /**
         * Consulted by the digging hacks before they break a block.
         */
        public static boolean isProtected(BlockPos pos)
        {
            Hack self = WurstClientHacks();
            if(self == null || !self.isEnabled())
                    return false;
            ProtectHack protect = (ProtectHack)self;
            double dx = pos.getX() + 0.5 - protect.centerX;
            double dy = pos.getY() + 0.5 - protect.centerY;
            double dz = pos.getZ() + 0.5 - protect.centerZ;
            double r = protect.radius.getValue() + 0.5;
            return dx * dx + dy * dy + dz * dz <= r * r;
        }

        private static Hack WurstClientHacks()
        {
                try
                {
                        Object hacks = net.wurst0.WurstClient.INSTANCE
                                .getHacks();
                        return ((net.wurst0.hacks.Hacks)hacks)
                                .getByName("Protect");
                }catch(Throwable t)
                {
                        return null;
                }
        }

        @Override
        public void onRender(float partialTicks)
        {
                if(!showZone.isChecked() || mc.player == null)
                        return;

                double r = radius.getValue();
                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                RenderUtils.drawBox(new AxisAlignedBB(
                        centerX - r, centerY - r, centerZ - r,
                        centerX + r, centerY + r, centerZ + r),
                        0.3F, 0.5F, 1F, 0.5F);
                RenderUtils.disable3D(false);
        }

        @Override
        protected void onEnable()
        {
                EntityPlayerSP p = mc.player;
                if(p != null)
                {
                        centerX = p.posX;
                        centerY = p.posY;
                        centerZ = p.posZ;
                }
        }
}
