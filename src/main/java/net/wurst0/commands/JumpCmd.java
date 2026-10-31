/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Ported from Wurst7-CevAPI JumpCmd (GPL-3.0-or-later)
 */
package net.wurst0.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.wurst0.command.CmdException;
import net.wurst0.feature.Command;

public final class JumpCmd extends Command
{
	public JumpCmd()
	{
		super("jump", "Makes you jump. Usage: .jump");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		EntityPlayerSP p = Minecraft.getMinecraft().player;
		if(p == null)
			throw new CmdException("Not in a world.");

		// 0.42 is the vanilla jump impulse (jump() is protected)
		p.motionY = 0.42;
		if(p.isSprinting())
		{
			p.motionX *= 1.2;
			p.motionZ *= 1.2;
		}
	}
}
