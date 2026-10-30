/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiKnockbackHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.ModeSetting;
import net.wurst0.settings.SliderSetting;

public final class AntiKnockbackHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final ModeSetting mode = new ModeSetting("Mode",
		new String[]{"Normal", "Reduced"}, 0);
	private final SliderSetting percent =
		new SliderSetting("Knockback kept", 25, 0, 100, 5);

	public AntiKnockbackHack()
	{
		super("AntiKnockback",
			"Protects you from knockback when taking damage. Normal mode"
				+ " cancels it completely, Reduced mode keeps a percentage"
				+ " of it (less obvious on servers with anti-cheat).");
		setCategory(Category.COMBAT);
		addSettings(mode, percent);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		// hurtTime == 9 is the first tick after taking damage, which is
		// when the server-set knockback motion is applied client-side.
		if(p.hurtTime != 9)
			return;

		if("Reduced".equals(mode.getSelected()))
		{
			double keep = percent.getValue() / 100D;
			p.motionX *= keep;
			p.motionZ *= keep;
			if(p.motionY > 0)
				p.motionY *= keep;
		}else
		{
			p.motionX = 0;
			p.motionZ = 0;
			if(p.motionY > 0)
				p.motionY = 0;
		}
	}
}
