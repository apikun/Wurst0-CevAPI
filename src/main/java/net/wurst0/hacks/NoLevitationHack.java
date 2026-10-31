/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI NoLevitationHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.MobEffects;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

/**
 * The levitation motion is applied client-side in travel(), so
 * removing the (client copy of the) effect each tick disables the
 * upward drift. The server keeps its own effect state, which is what
 * we want - fall damage behaviour matches normal falling.
 */
public final class NoLevitationHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	public NoLevitationHack()
	{
		super("NoLevitation",
			"Cancels the effect of the levitation potion.");
		setCategory(Category.MOVEMENT);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null)
			return;

		if(p.isPotionActive(MobEffects.LEVITATION))
			p.removePotionEffect(MobEffects.LEVITATION);
	}
}
