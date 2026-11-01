/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ForceTPHack (GPL-3.0-or-later)
 * Adapted to 1.12.2: the creative spawn-egg entity-data API does not
 * exist, so owner-bound ender pearls are spawned with /summon
 * ender_pearl instead. In 1.12.2 a pearl's ownerName NBT resolves
 * either a player name or a raw UUID string (e.g. the entity you are
 * looking at). The unused "Range" slider of the original was dropped.
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.math.RayTraceResult;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;
import net.wurst0.settings.TextFieldSetting;

public final class ForceTPHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final ModeSetting targetMode = new ModeSetting("Target mode",
                new String[]{"OnTarget", "ToVoid", "ToPlayer"}, 0);
        private final TextFieldSetting targetPlayer =
                new TextFieldSetting("Target player", "");
        private final SliderSetting velocity =
                new SliderSetting("Velocity", 5, 0, 10, 1);
        private final CheckboxSetting automatic =
                new CheckboxSetting("FULLAUTO", true);
        private final SliderSetting delay =
                new SliderSetting("Tick delay", 3, 3, 20, 1);

        private int ticks;

        public ForceTPHack()
        {
                super("ForceTP",
                        "Summons an ender pearl that teleports its owner (you,"
                                + " the entity you look at, or a named player)"
                                + " to the point of impact on impact.");
                setCategory(Category.CREATIVE_OP);
                addSettings(targetMode, targetPlayer, velocity, automatic,
                        delay);
        }

        @Override
        protected void onEnable()
        {
                ticks = delay.getValueI();
                spawnPearl();
        }

        @Override
        protected void onDisable()
        {
                ticks = 0;
        }

        @Override
        public void onUpdate()
        {
                if(!automatic.isChecked())
                        return;
                if(ticks++ >= delay.getValueI())
                {
                        ticks = 0;
                        spawnPearl();
                }
        }

        private void spawnPearl()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.getConnection() == null)
                        return;

                // 1.12.2 world bottom is Y 0.
                double x = p.posX;
                double y = 0;
                double z = p.posZ;
                String mode = targetMode.getSelected();
                if(!mode.equals("ToVoid"))
                {
                        RayTraceResult hit = mc.objectMouseOver;
                        if(hit != null
                                && hit.typeOfHit != RayTraceResult.Type.MISS
                                && hit.hitVec != null)
                        {
                                x = hit.hitVec.x;
                                y = hit.hitVec.y;
                                z = hit.hitVec.z;
                        }
                }

                String owner = resolveOwner(p, mode);
                if(owner == null)
                        return;

                String command = "summon ender_pearl " + fmt(x) + " "
                        + fmt(y) + " " + fmt(z) + " {Motion:[0.0,-"
                        + velocity.getValueI() + ".0,0.0],ownerName:\""
                        + nbtEscape(owner) + "\"}";

                // 1.12.2 servers reject chat packets longer than 256 chars.
                if(command.length() + 1 <= 256)
                        p.sendChatMessage("/" + command);
        }

        private String resolveOwner(EntityPlayerSP p, String mode)
        {
                // entity under the crosshair wins, like the original
                RayTraceResult hit = mc.objectMouseOver;
                if(hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY
                        && hit.entityHit != null
                        && !mode.equals("ToVoid"))
                        return hit.entityHit.getUniqueID().toString();

                if(mode.equals("ToPlayer"))
                {
                        String name = targetPlayer.getValue().trim();
                        if(!name.isEmpty() && isOnline(name))
                                return name;
                }

                return p.getGameProfile().getName();
        }

        private boolean isOnline(String name)
        {
                if(mc.getConnection() == null)
                        return false;
                for(NetworkPlayerInfo info : mc.getConnection()
                        .getPlayerInfoMap())
                        if(info.getGameProfile().getName()
                                .equalsIgnoreCase(name))
                                return true;
                return false;
        }

        private static String fmt(double v)
        {
                return String.valueOf(Math.round(v * 100.0) / 100.0);
        }

        private static String nbtEscape(String s)
        {
                if(s == null)
                        return "";
                return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
}
