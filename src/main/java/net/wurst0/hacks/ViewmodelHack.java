/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI ViewmodelHack (GPL-3.0-or-later)
 * Lite adaptation: 1.12.2's hand render has no matrix mixin hook, so
 * each hand's vanilla render is cancelled via Forge's cancellable
 * RenderSpecificHandEvent and re-rendered immediately through the
 * public ItemRenderer.renderItemInFirstPerson() overload with an
 * extra translation/rotation applied (opacity/tint/glint dropped).
 */
package net.wurst0.hacks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.wurst0.Category;
import net.wurst0.feature.Hack;
import net.wurst0.settings.CheckboxSetting;
import net.wurst0.settings.SliderSetting;

/**
 * Moves and rotates your first-person hands independently, or hides
 * them entirely. Works with any held item and preserves the vanilla
 * swing/equip animation.
 */
public final class ViewmodelHack extends Hack
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting rightX =
		new SliderSetting("Right X", 0, -2, 2, 0.01);
	private final SliderSetting rightY =
		new SliderSetting("Right Y", 0, -2, 2, 0.01);
	private final SliderSetting rightZ =
		new SliderSetting("Right Z", 0, -2, 2, 0.01);
	private final SliderSetting rightPitch =
		new SliderSetting("Right pitch", 0, -180, 180, 1);
	private final SliderSetting rightYaw =
		new SliderSetting("Right yaw", 0, -180, 180, 1);
	private final SliderSetting rightRoll =
		new SliderSetting("Right roll", 0, -180, 180, 1);
	private final CheckboxSetting rightHidden =
		new CheckboxSetting("Right hidden", false);

	private final SliderSetting leftX =
		new SliderSetting("Left X", 0, -2, 2, 0.01);
	private final SliderSetting leftY =
		new SliderSetting("Left Y", 0, -2, 2, 0.01);
	private final SliderSetting leftZ =
		new SliderSetting("Left Z", 0, -2, 2, 0.01);
	private final SliderSetting leftPitch =
		new SliderSetting("Left pitch", 0, -180, 180, 1);
	private final SliderSetting leftYaw =
		new SliderSetting("Left yaw", 0, -180, 180, 1);
	private final SliderSetting leftRoll =
		new SliderSetting("Left roll", 0, -180, 180, 1);
	private final CheckboxSetting leftHidden =
		new CheckboxSetting("Left hidden", false);

	public ViewmodelHack()
	{
		super("Viewmodel",
			"Lets you change the position and rotation of your"
				+ " first-person hands, or hide them completely.");
		setCategory(Category.RENDER);
		addSettings(rightX, rightY, rightZ, rightPitch, rightYaw, rightRoll,
			rightHidden, leftX, leftY, leftZ, leftPitch, leftYaw, leftRoll,
			leftHidden);
	}

	@Override
	protected void onEnable()
	{
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	protected void onDisable()
	{
		MinecraftForge.EVENT_BUS.unregister(this);
	}

	@SubscribeEvent
	public void onRenderHand(RenderSpecificHandEvent event)
	{
		if(mc.player == null || !(mc.player instanceof AbstractClientPlayer))
			return;

		// hide the vanilla hand; we draw it again with our transform
		event.setCanceled(true);

		boolean main = event.getHand() == EnumHand.MAIN_HAND;
		if(main ? rightHidden.isChecked() : leftHidden.isChecked())
			return;

		SliderSetting x = main ? rightX : leftX;
		SliderSetting y = main ? rightY : leftY;
		SliderSetting z = main ? rightZ : leftZ;
		SliderSetting pitch = main ? rightPitch : leftPitch;
		SliderSetting yaw = main ? rightYaw : leftYaw;
		SliderSetting roll = main ? rightRoll : leftRoll;

		GlStateManager.pushMatrix();
		GlStateManager.translate(x.getValueF(), y.getValueF(),
			z.getValueF());
		GlStateManager.rotate(pitch.getValueF(), 1F, 0F, 0F);
		GlStateManager.rotate(yaw.getValueF(), 0F, 1F, 0F);
		GlStateManager.rotate(roll.getValueF(), 0F, 0F, 1F);

		mc.getItemRenderer().renderItemInFirstPerson(
			(AbstractClientPlayer)mc.player, event.getPartialTicks(),
			event.getInterpolatedPitch(), event.getHand(),
			event.getSwingProgress(), event.getItemStack(),
			event.getEquipProgress());

		GlStateManager.popMatrix();
	}
}
