/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI WaypointsHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.RenderListener;
import net.wurst0.feature.Hack;
import net.wurst0.render.RenderUtils;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

/**
 * Renders tracers and labels for your waypoints. The waypoint list is
 * a text setting ("name x y z" entries separated by ';') so it
 * persists in settings.json automatically; the .wp command edits it.
 */
public final class WaypointsHack extends Hack implements RenderListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final TextFieldSetting waypoints = new TextFieldSetting(
                "Waypoints", "", 1024);
        private final SliderSetting renderDistance =
                new SliderSetting("Render distance", 512, 64, 2048, 64);
        private final SliderSetting labelScale =
                new SliderSetting("Label scale", 1, 0.5, 2, 0.05);

        public WaypointsHack()
        {
                super("Waypoints",
                        "Shows tracers and labels to your waypoints. Use .wp add"
                                + " <name> / .wp del <name> / .wp list to manage"
                                + " them.");
                setCategory(Category.RENDER);
                addSettings(waypoints, renderDistance, labelScale);
        }

        public static final class Waypoint
        {
                public final String name;
                public final BlockPos pos;

                public Waypoint(String name, BlockPos pos)
                {
                        this.name = name;
                        this.pos = pos;
                }
        }

        public List<Waypoint> parseWaypoints()
        {
                List<Waypoint> list = new ArrayList<Waypoint>();
                String raw = waypoints.getValue();
                if(raw == null || raw.trim().isEmpty())
                        return list;
                for(String entry : raw.split(";"))
                {
                        String[] parts = entry.trim().split("\\s+");
                        if(parts.length != 4)
                                continue;
                        try
                        {
                                int x = Integer.parseInt(parts[1]);
                                int y = Integer.parseInt(parts[2]);
                                int z = Integer.parseInt(parts[3]);
                                list.add(new Waypoint(parts[0],
                                        new BlockPos(x, y, z)));
                        }catch(NumberFormatException ignored)
                        {}
                }
                return list;
        }

        public void setWaypoints(List<Waypoint> list)
        {
                StringBuilder sb = new StringBuilder();
                for(Waypoint wp : list)
                {
                        if(sb.length() > 0)
                                sb.append(';');
                        sb.append(wp.name).append(' ').append(wp.pos.getX())
                                .append(' ').append(wp.pos.getY()).append(' ')
                                .append(wp.pos.getZ());
                }
                waypoints.setValue(sb.toString());
                net.wurst0.WurstClient.INSTANCE.getFileManager().markDirty();
        }

        @Override
        public void onRender(float partialTicks)
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                double maxDistSq =
                        renderDistance.getValue() * renderDistance.getValue();
                List<Waypoint> list = parseWaypoints();
                if(list.isEmpty())
                        return;

                RenderUtils.enable3D(1.5F, false);
                RenderUtils.applyCameraTransform(partialTicks);
                RenderManager rm = mc.getRenderManager();

                for(Waypoint wp : list)
                {
                        double wx = wp.pos.getX() + 0.5;
                        double wy = wp.pos.getY() + 0.5;
                        double wz = wp.pos.getZ() + 0.5;

                        double dx = wx - p.posX;
                        double dy = wy - p.posY;
                        double dz = wz - p.posZ;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if(distSq > maxDistSq)
                                continue;
                        double dist = Math.sqrt(distSq);

                        // tracer line
                        RenderUtils.drawLine(
                                p.lastTickPosX + (p.posX - p.lastTickPosX)
                                        * partialTicks,
                                p.lastTickPosY + (p.posY - p.lastTickPosY)
                                        * partialTicks + p.getEyeHeight(),
                                p.lastTickPosZ + (p.posZ - p.lastTickPosZ)
                                        * partialTicks,
                                wx, wy, wz, 0.75F, 0.3F, 1F, 0.9F);

                        // small box
                        AxisAlignedBB box = new AxisAlignedBB(wp.pos);
                        RenderUtils.drawBox(box, 0.75F, 0.3F, 1F, 0.8F);

                        // label billboard
                        GlStateManager.pushMatrix();
                        GlStateManager.translate(wx, wy + 1.0, wz);
                        GlStateManager.rotate(-rm.playerViewY, 0F, 1F, 0F);
                        GlStateManager.rotate(rm.playerViewX, 1F, 0F, 0F);
                        float s = 0.03F * labelScale.getValueF();
                        GlStateManager.scale(-s, -s, s);
                        GlStateManager.disableLighting();

                        String text = wp.name + " (" + String.format("%.0f", dist)
                                + "m)";
                        int w = mc.fontRenderer.getStringWidth(text);
                        mc.fontRenderer.drawStringWithShadow(text, -w / 2F, 0F,
                                0xFFBF5FFF);

                        GlStateManager.enableLighting();
                        GlStateManager.popMatrix();
                }

                RenderUtils.disable3D(false);
        }
}
