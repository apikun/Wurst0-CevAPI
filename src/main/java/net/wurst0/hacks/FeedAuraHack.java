/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FeedAuraHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Feeds nearby hungry animals by right-clicking them with the
 * breeding item you are holding. Only feeds animals that are not
 * already in love and not babies, and only with the correct food for
 * that animal (EntityAnimal.isBreedingItem).
 */
public final class FeedAuraHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting range =
		new SliderSetting("Range", 4, 1, 6, 0.5);
	private final SliderSetting delay =
		new SliderSetting("Delay (ticks)", 10, 0, 40, 1);

	private int timer;

	public FeedAuraHack()
	{
		super("FeedAura",
			"Automatically feeds animals with the breeding item you"
				+ " are holding, so they enter love mode.");
		setCategory(Category.OTHER);
		addSettings(range, delay);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		ItemStack held = p.getHeldItemMainhand();
		if(held.isEmpty())
			return;

		if(timer > 0)
		{
			timer--;
			return;
		}
		timer = delay.getValueI();

		double rangeSq = range.getValue() * range.getValue();
		EntityAnimal best = null;
		double bestDist = Double.MAX_VALUE;

		List<Entity> entities =
			new ArrayList<Entity>(mc.world.loadedEntityList);
		for(Entity e : entities)
		{
			if(!(e instanceof EntityAnimal))
				continue;

			EntityAnimal animal = (EntityAnimal)e;
			if(animal.isChild() || animal.isInLove())
				continue;
			if(!animal.isBreedingItem(held))
				continue;

			double dist = e.getDistanceSq(p.posX, p.posY, p.posZ);
			if(dist > rangeSq || dist >= bestDist)
				continue;

			bestDist = dist;
			best = animal;
		}

		if(best != null)
			mc.playerController.interactWithEntity(p, best,
				EnumHand.MAIN_HAND);
	}
}
