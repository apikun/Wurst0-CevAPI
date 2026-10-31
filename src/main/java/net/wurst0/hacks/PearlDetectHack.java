/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI PearlDetectHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Warns you in chat when somebody else's ender pearl is flying
 * towards you, so you have time to react before they land.
 */
public final class PearlDetectHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 24, 8, 64, 4);

        private int cooldown;

        public PearlDetectHack()
        {
                super("PearlDetect",
                        "Warns you when an ender pearl is thrown towards you.");
                setCategory(Category.OTHER);
                addSetting(range);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(cooldown > 0)
                {
                        cooldown--;
                        return;
                }

                double rangeSq = range.getValue() * range.getValue();
                List<Entity> loaded =
                        new ArrayList<Entity>(mc.world.loadedEntityList);

                for(Entity e : loaded)
                {
                        if(!(e instanceof EntityEnderPearl))
                                continue;

                        EntityEnderPearl pearl = (EntityEnderPearl)e;
                        if(pearl.getThrower() == p)
                                continue;

                        double distSq =
                                p.getDistanceSq(pearl.posX, pearl.posY, pearl.posZ);
                        if(distSq > rangeSq)
                                continue;

                        // heading towards the player?
                        Vec3d toPlayer = new Vec3d(p.posX - pearl.posX,
                                (p.posY + 1D) - pearl.posY, p.posZ - pearl.posZ);
                        Vec3d vel = new Vec3d(pearl.motionX, pearl.motionY,
                                pearl.motionZ);
                        double speed = Math.sqrt(vel.x * vel.x + vel.y * vel.y
                                + vel.z * vel.z);
                        if(speed < 0.05)
                                continue;

                        double toPlayerLen = Math.sqrt(toPlayer.x * toPlayer.x
                                + toPlayer.y * toPlayer.y + toPlayer.z * toPlayer.z);
                        if(toPlayerLen < 1.0E-4)
                                continue;

                        double dot = (toPlayer.x * vel.x + toPlayer.y * vel.y
                                + toPlayer.z * vel.z) / (speed * toPlayerLen);
                        if(dot < 0.85)
                                continue;

                        EntityLivingBase thrower = pearl.getThrower();
                        String who = thrower != null ? thrower.getName() : "Someone";
                        p.sendMessage(new TextComponentString(
                                "\u00a7c[Guard] " + who + "'s ender pearl is incoming!"));
                        cooldown = 60;
                        return;
                }
        }

        @Override
        protected void onEnable()
        {
                cooldown = 0;
        }
}
