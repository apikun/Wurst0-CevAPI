/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI MileyCyrusHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumParticleTypes;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * You came in like a wrecking baaall~ Spawns heart, note and happy
 * villager particles all around you (client-side only).
 */
public final class MileyCyrusHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();
        private final Random rand = new Random();
        private int timer;

        public MileyCyrusHack()
        {
                super("MileyCyrus",
                        "Surrounds you with hearts, music notes and sparkles.");
                setCategory(Category.FUN);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;

                if(--timer > 0)
                        return;
                timer = 2;

                for(int i = 0; i < 4; i++)
                {
                        EnumParticleTypes type;
                        int pick = rand.nextInt(10);
                        if(pick < 4)
                                type = EnumParticleTypes.HEART;
                        else if(pick < 8)
                                type = EnumParticleTypes.NOTE;
                        else
                                type = EnumParticleTypes.VILLAGER_HAPPY;

                        double angle = rand.nextDouble() * Math.PI * 2;
                        double radius = 1.2 + rand.nextDouble() * 1.2;
                        double x = p.posX + Math.cos(angle) * radius;
                        double z = p.posZ + Math.sin(angle) * radius;
                        double y = p.posY + 0.5 + rand.nextDouble() * 1.2;

                        mc.world.spawnParticle(type, x, y, z, 0, 0.05, 0);
                }
        }
}
