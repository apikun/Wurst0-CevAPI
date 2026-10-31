/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ShearAuraHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Automatically shears the closest sheep or mooshroom that still has
 * wool. Switches to shears from the hotbar if you are not holding
 * them. The shearing itself goes through the vanilla
 * interact-with-entity path (ItemShears -> IShearable.onSheared).
 */
public final class ShearAuraHack extends Hack implements UpdateListener
{
        private final Minecraft mc = Minecraft.getMinecraft();

        private final SliderSetting range =
                new SliderSetting("Range", 4, 1, 6, 0.5);
        private final SliderSetting delay =
                new SliderSetting("Delay (ticks)", 10, 0, 40, 1);

        private int timer;

        public ShearAuraHack()
        {
                super("ShearAura",
                        "Automatically shears the closest sheep or mooshroom around"
                                + " you, switching to shears from the hotbar.");
                setCategory(Category.OTHER);
                addSettings(range, delay);
        }

        @Override
        public void onUpdate()
        {
                EntityPlayerSP p = mc.player;
                if(p == null || mc.world == null || mc.playerController == null)
                        return;

                if(timer-- > 0)
                        return;
                timer = delay.getValueI();

                Entity target = findTarget(p);
                if(target == null)
                        return;

                ItemStack held = p.getHeldItemMainhand();
                if(held.isEmpty() || held.getItem() != Items.SHEARS)
                {
                        int slot = findShears(p);
                        if(slot == -1)
                                return;
                        p.inventory.currentItem = slot;
                }

                mc.playerController.interactWithEntity(p, target,
                        EnumHand.MAIN_HAND);
        }

        private Entity findTarget(EntityPlayerSP p)
        {
                double rangeSq = range.getValue() * range.getValue();
                Entity best = null;
                double bestDist = Double.MAX_VALUE;

                List<Entity> entities =
                        new ArrayList<Entity>(mc.world.loadedEntityList);
                for(Entity e : entities)
                {
                        boolean shearable;
                        if(e instanceof EntitySheep)
                                shearable = !((EntitySheep)e).getSheared();
                        else if(e instanceof EntityMooshroom)
                                shearable = true;
                        else
                                continue;

                        if(!e.isEntityAlive() || ((EntityAgeable)e).isChild()
                                || !shearable)
                                continue;

                        double dist = e.getDistanceSq(p.posX, p.posY, p.posZ);
                        if(dist > rangeSq || dist >= bestDist)
                                continue;

                        bestDist = dist;
                        best = e;
                }
                return best;
        }

        private int findShears(EntityPlayerSP p)
        {
                for(int i = 0; i < 9; i++)
                {
                        ItemStack stack = p.inventory.getStackInSlot(i);
                        if(!stack.isEmpty() && stack.getItem() == Items.SHEARS)
                                return i;
                }
                return -1;
        }

        @Override
        protected void onEnable()
        {
                timer = 0;
        }
}
