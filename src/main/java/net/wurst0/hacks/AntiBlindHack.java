/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI AntiBlindHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.MobEffects;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;

public final class AntiBlindHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final CheckboxSetting blindness =
		new CheckboxSetting("Blindness", true);
	private final CheckboxSetting nausea =
		new CheckboxSetting("Nausea", true);
	private final CheckboxSetting miningFatigue =
		new CheckboxSetting("Mining fatigue", false);

	public AntiBlindHack()
	{
		super("AntiBlind",
			"Prevents blindness and nausea effects from affecting"
				+ " your screen. The server can re-apply them.");
		setCategory(Category.RENDER);
		addSettings(blindness, nausea, miningFatigue);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(blindness.isChecked())
			p.removeActivePotionEffect(MobEffects.BLINDNESS);
		if(nausea.isChecked())
			p.removeActivePotionEffect(MobEffects.NAUSEA);
		if(miningFatigue.isChecked())
			p.removeActivePotionEffect(MobEffects.MINING_FATIGUE);
	}
}
