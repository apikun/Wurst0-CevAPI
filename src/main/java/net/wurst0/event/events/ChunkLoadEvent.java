/*
 * Wurst0 - a 1.12.2 port of Wurst7-CevAPI
 * Based on Wurst7 by Alexander01998 (GPL-3.0-or-later)
 */
package net.wurst0.event.events;

/**
 * Fired when a chunk finishes loading on the client.
 */
public final class ChunkLoadEvent
{
	private final int chunkX;
	private final int chunkZ;

	public ChunkLoadEvent(int chunkX, int chunkZ)
	{
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}

	public int getChunkX()
	{
		return chunkX;
	}

	public int getChunkZ()
	{
		return chunkZ;
	}
}
