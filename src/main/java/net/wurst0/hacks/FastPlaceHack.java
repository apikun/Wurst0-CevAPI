/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FastPlaceHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;

public final class FastPlaceHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private Field blockHitDelayField;
	private boolean failed;

	public FastPlaceHack()
	{
		super("FastPlace",
			"Removes the delay between placing or breaking blocks when"
				+ " holding down the mouse button.");
		setCategory(Category.BLOCKS);
	}

	@Override
	public void onUpdate()
	{
		if(failed || mc.playerController == null)
			return;

		try
		{
			if(blockHitDelayField == null)
				blockHitDelayField = ReflectionHelper.findField(
					PlayerControllerMP.class, "field_78781_i",
					"blockHitDelay");
			blockHitDelayField.setInt(mc.playerController, 0);
		}catch(Throwable t)
		{
			failed = true;
			System.err.println("[Wurst0] FastPlace: blockHitDelay not"
				+ " accessible:");
			t.printStackTrace();
		}
	}
}
