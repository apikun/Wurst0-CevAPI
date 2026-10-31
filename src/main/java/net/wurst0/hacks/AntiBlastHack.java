/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiBlastHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.event.world.ExplosionEvent;
import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Bridges through Forge's ExplosionEvent.Detonate (see ForgeEvents):
 * removing the player from the explosion's affected entity list
 * prevents both the damage and the knockback for that blast.
 */
public final class AntiBlastHack extends Hack
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 6, 2, 16, 1);

        public AntiBlastHack()
        {
                super("AntiBlast",
                        "Protects you from explosions by removing you from their"
                                + " affected entities.");
                setCategory(Category.OTHER);
                addSetting(range);
        }

        /**
         * Called from ForgeEvents.onExplosionDetonate(); removes the local
         * player from the blast's affected entities if in range.
         */
        public void handleDetonate(ExplosionEvent.Detonate event)
        {
                List<Entity> affected = event.getAffectedEntities();
                if(affected == null)
                        return;

                net.minecraft.util.math.Vec3d pos =
                        event.getExplosion().getPosition();
                if(pos == null || mc.player == null)
                        return;

                double rangeSq = range.getValue() * range.getValue();
                if(mc.player.getDistanceSq(pos.x, pos.y, pos.z) <= rangeSq)
                        affected.remove(mc.player);
        }
}
