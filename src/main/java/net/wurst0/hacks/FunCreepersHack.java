/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FunCreepersHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.util.EnumParticleTypes;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * Purely cosmetic: spawns colorful smoke particles around every
 * creeper near you, so they are easier to spot and slightly less
 * boring to look at.
 */
public final class FunCreepersHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();
        private final Random rand = new Random();
        private int timer;

        public FunCreepersHack()
        {
                super("FunCreepers",
                        "Makes creepers emit colored smoke particles"
                                + " (client-side only).");
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
                timer = 3;

                for(EntityCreeper creeper : mc.world.getEntities(
                        EntityCreeper.class, e -> true))
                {
                        if(creeper.isDead)
                                continue;
                        if(creeper.getDistanceSq(p) > 16 * 16)
                                continue;

                        EnumParticleTypes type;
                        int pick = rand.nextInt(3);
                        if(pick == 0)
                                type = EnumParticleTypes.SMOKE_NORMAL;
                        else if(pick == 1)
                                type = EnumParticleTypes.VILLAGER_ANGRY;
                        else
                                type = EnumParticleTypes.LAVA;

                        for(int i = 0; i < 3; i++)
                        {
                                double x = creeper.posX
                                        + (rand.nextDouble() - 0.5) * 0.8;
                                double y = creeper.posY
                                        + rand.nextDouble() * 1.6;
                                double z = creeper.posZ
                                        + (rand.nextDouble() - 0.5) * 0.8;
                                mc.world.spawnParticle(type, x, y, z,
                                        0, 0.02, 0);
                        }
                }
        }
}
