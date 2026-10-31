/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.event.events;

/**
 * Fired when the local player receives damage. Cancelling the event
 * prevents the damage (client-side interception through Forge).
 */
public final class DamageEvent
{
	private final net.minecraft.util.DamageSource source;
	private float amount;
	private boolean cancelled;

	public DamageEvent(net.minecraft.util.DamageSource source, float amount)
	{
		this.source = source;
		this.amount = amount;
	}

	public net.minecraft.util.DamageSource getSource()
	{
		return source;
	}

	public float getAmount()
	{
		return amount;
	}

	public void setAmount(float amount)
	{
		this.amount = amount;
	}

	public boolean isCancelled()
	{
		return cancelled;
	}

	public void setCancelled(boolean cancelled)
	{
		this.cancelled = cancelled;
	}
}
