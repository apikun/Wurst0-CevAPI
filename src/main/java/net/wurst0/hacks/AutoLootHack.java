/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AutoLootHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.MathHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Walks to the closest dropped item and picks it up. Lite port of the
 * original: only the walk mode (no container/looting logic).
 */
public final class AutoLootHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 16, 4, 64, 1);
        private final CheckboxSetting jump =
                new CheckboxSetting("Jump when blocked", true);

        public AutoLootHack()
        {
                super("AutoLoot",
                        "Automatically walks to the closest dropped item and"
                                + " picks it up.");
                setCategory(Category.ITEMS);
                addSettings(range, jump);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null)
                        return;
                // don't fight the user while a GUI is open
                if(mc.currentScreen != null)
                        return;

                EntityItem best = null;
                double bestDist = range.getValue() * range.getValue();

                List<Entity> items =
                        new ArrayList<Entity>(mc.world.loadedEntityList);
                for(Entity e : items)
                {
                        if(!(e instanceof EntityItem) || !e.isEntityAlive())
                                continue;
                        double dist = e.getDistanceSq(p.posX, p.posY, p.posZ);
                        if(dist < bestDist)
                        {
                                bestDist = dist;
                                best = (EntityItem)e;
                        }
                }

                if(best == null)
                {
                        releaseInput(p);
                        return;
                }

                double dx = best.posX - p.posX;
                double dz = best.posZ - p.posZ;
                double dy = best.posY - p.posY;

                // rotate towards the item
                float targetYaw = (float)(Math.toDegrees(
                        Math.atan2(dz, dx)) - 90D);
                p.rotationYaw = interpolateAngle(p.rotationYaw, targetYaw);

                if(dx * dx + dz * dz > 1.2)
                {
                        if(p.movementInput != null)
                        {
                                p.movementInput.moveForward = 1;
                                p.movementInput.moveStrafe = 0;
                        }
                        if(jump.isChecked() && p.collidedHorizontally && p.onGround)
                                p.motionY = 0.42F; // vanilla jump velocity (jump() is protected)
                }else if(dy > 0.6)
                {
                        // item is above - jump towards it
                        if(p.movementInput != null)
                        {
                                p.movementInput.moveForward = 1;
                                p.movementInput.moveStrafe = 0;
                        }
                        if(jump.isChecked() && p.onGround)
                                p.motionY = 0.42F;
                }else
                        releaseInput(p);
        }

        @Override
        protected void onDisable()
        {
                if(mc.player != null)
                        releaseInput(mc.player);
        }

        private void releaseInput(EntityPlayerSP p)
        {
                if(p.movementInput != null)
                {
                        p.movementInput.moveForward = 0;
                        p.movementInput.moveStrafe = 0;
                }
        }

        private static float interpolateAngle(float from, float to)
        {
                float delta = MathHelper.wrapDegrees(to - from);
                // gentle turn speed
                return from + MathHelper.clamp(delta, -30F, 30F);
        }
}
