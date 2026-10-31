/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI FreecamHack (GPL-3.0-or-later)
 */
package net.wurst0.hacks;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovementInput;
import net.wurst0.Category;
import net.wurst0.event.listeners.UpdateListener;
import net.wurst0.feature.Hack;
import net.wurst0.settings.SliderSetting;

/**
 * Detaches the camera from the player. A fake client-side player body
 * is left behind while the camera flies freely on WASD + jump/sneak.
 * The real player is frozen in place (zeroed motion + a no-op
 * MovementInput), so no movement packets are sent.
 */
public final class FreecamHack extends Hack implements UpdateListener
{
	private final Minecraft mc = Minecraft.getMinecraft();

	private final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.1, 10, 0.1);

	private EntityOtherPlayerMP fake;
	private MovementInput savedInput;

	public FreecamHack()
	{
		super("Freecam",
			"Allows you to fly out of your body and see through walls,"
				+ " like a spectator. Your real body stays where it was.");
		setCategory(Category.RENDER);
		addSetting(speed);
	}

	@Override
	public void onUpdate()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
			return;

		if(fake == null || fake.world != mc.world)
		{
			// world changed while enabled - bail out
			setEnabled(false);
			return;
		}

		// freeze the real player
		p.motionX = p.motionY = p.motionZ = 0;
		p.setSprinting(false);
		p.fallDistance = 0;

		// mouse look (deltas accumulate over all frames since last tick)
		float sens = 0.25F;
		fake.rotationYaw += Mouse.getDX() * sens;
		fake.rotationPitch -= Mouse.getDY() * sens;
		fake.rotationPitch =
			net.minecraft.util.math.MathHelper
				.clamp(fake.rotationPitch, -90F, 90F);

		// keyboard fly movement relative to fake's yaw
		double fw = 0, st = 0;
		if(mc.gameSettings.keyBindForward.isKeyDown())
			fw += 1;
		if(mc.gameSettings.keyBindBack.isKeyDown())
			fw -= 1;
		if(mc.gameSettings.keyBindLeft.isKeyDown())
			st += 1;
		if(mc.gameSettings.keyBindRight.isKeyDown())
			st -= 1;
		double up = 0;
		if(mc.gameSettings.keyBindJump.isKeyDown())
			up += 1;
		if(mc.gameSettings.keyBindSneak.isKeyDown())
			up -= 1;

		double yawRad = Math.toRadians(fake.rotationYaw);
		double sinY = Math.sin(yawRad), cosY = Math.cos(yawRad);
		double vx = st * cosY - fw * sinY;
		double vz = -st * sinY - fw * cosY;
		double len = Math.sqrt(vx * vx + vz * vz);
		if(len > 1e-4)
		{
			vx /= len;
			vz /= len;
		}
		double sp = speed.getValue();

		// keep prev coords in sync for smooth interpolation
		fake.prevPosX = fake.posX;
		fake.prevPosY = fake.posY;
		fake.prevPosZ = fake.posZ;
		fake.prevRotationYaw = fake.rotationYaw;
		fake.prevRotationPitch = fake.rotationPitch;
		fake.setPosition(fake.posX + vx * sp, fake.posY + up * sp,
			fake.posZ + vz * sp);
		fake.rotationYawHead = fake.rotationYaw;
	}

	@Override
	protected void onEnable()
	{
		EntityPlayerSP p = mc.player;
		if(p == null || mc.world == null)
		{
			setEnabled(false);
			return;
		}

		fake = new EntityOtherPlayerMP(mc.world,
			mc.getSession().getProfile());
		fake.setPositionAndRotation(p.posX, p.posY, p.posZ, p.rotationYaw,
			p.rotationPitch);
		fake.rotationYawHead = p.rotationYawHead;
		fake.prevPosX = fake.posX;
		fake.prevPosY = fake.posY;
		fake.prevPosZ = fake.posZ;
		fake.prevRotationYaw = fake.rotationYaw;
		fake.prevRotationPitch = fake.rotationPitch;

		// no-op movement input keeps the real body still
		savedInput = p.movementInput;
		p.movementInput = new MovementInput()
		{
			@Override
			public void updatePlayerMoveState()
			{}
		};

		mc.setRenderViewEntity(fake);
	}

	@Override
	protected void onDisable()
	{
		EntityPlayerSP p = mc.player;
		if(p != null && savedInput != null)
			p.movementInput = savedInput;
		savedInput = null;

		if(p != null)
			mc.setRenderViewEntity(p);
		fake = null;
	}
}
