/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 * TODO: port logic from net/wurstclient/hacks/CameraDistanceHack.java
 * in the original repo (MC 26.3 / Fabric). This stub only registers
 * the hack so it appears in the GUI; it currently does nothing when
 * enabled.
 *
 * BLOCKER (found 2026-10-31 against the decompiled 1.12.2 sources):
 * EntityRenderer.thirdPersonDistance is a `private final` compile-time
 * constant in 1.12.2, so the compiler inlined 4.0F at every use site
 * (orientCamera interpolates towards the literal 4.0F), and
 * updateRenderer() resets thirdPersonDistancePrev to 4.0F every
 * frame. There is no Forge event between the reset and orientCamera,
 * so reflection cannot change the third-person distance - this needs
 * a mixin/ASM hook.
 */
package net.wurst0.hacks;

import net.wurst0.Category;
import net.wurst0.feature.Hack;

public final class CameraDistanceHack extends Hack
{
	public CameraDistanceHack()
	{
		super("CameraDistance", "[not ported yet]");
		setCategory(Category.RENDER);
	}
}
