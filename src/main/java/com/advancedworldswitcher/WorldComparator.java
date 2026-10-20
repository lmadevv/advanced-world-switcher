package com.advancedworldswitcher;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import net.runelite.http.api.worlds.World;

public class WorldComparator implements Comparator<World>
{
	private final WorldSortOrder primary;
	private final WorldSortOrder secondary;
	private final Map<Integer, Integer> pings;
	private final int pingThreshold;

	public WorldComparator(WorldSortOrder primary, WorldSortOrder secondary, Map<Integer, Integer> pings, int pingThreshold)
	{
		this.primary = primary;
		this.secondary = secondary;
		this.pings = pings;
		this.pingThreshold = Math.max(0, pingThreshold);
	}

	@Override
	public int compare(World a, World b)
	{
		int cmp = compareBy(a, b, primary, pingThreshold);
		if (cmp != 0)
		{
			return cmp;
		}

		if (secondary != null && secondary != primary)
		{
			int secCmp = compareBy(a, b, secondary, 0);
			if (secCmp != 0)
			{
				return secCmp;
			}
		}

		return Integer.compare(a.getId(), b.getId());
	}

	private int compareBy(World a, World b, WorldSortOrder order, int threshold)
	{
		if (order == null)
		{
			return 0;
		}

		switch (order)
		{
			case WORLD_ASC:
				return Integer.compare(a.getId(), b.getId());
			case WORLD_DESC:
				return Integer.compare(b.getId(), a.getId());
			case PLAYERS_ASC:
				return Integer.compare(normalizePlayers(a.getPlayers()), normalizePlayers(b.getPlayers()));
			case PLAYERS_DESC:
				return Integer.compare(normalizePlayers(b.getPlayers()), normalizePlayers(a.getPlayers()));
			case PING_ASC:
				return comparePings(a, b, threshold, false);
			case PING_DESC:
				return comparePings(a, b, threshold, true);
			case ACTIVITY:
				String actA = a.getActivity() == null ? "" : a.getActivity().trim().toLowerCase(Locale.ROOT);
				String actB = b.getActivity() == null ? "" : b.getActivity().trim().toLowerCase(Locale.ROOT);
				return actA.compareTo(actB);
			default:
				return 0;
		}
	}

	private int normalizePlayers(int p)
	{
		return p < 0 ? Integer.MAX_VALUE : p;
	}

	private int comparePings(World a, World b, int threshold, boolean desc)
	{
		Integer pingA = pings.get(a.getId());
		Integer pingB = pings.get(b.getId());

		int valA = (pingA == null || pingA <= 0) ? Integer.MAX_VALUE : pingA;
		int valB = (pingB == null || pingB <= 0) ? Integer.MAX_VALUE : pingB;

		if (threshold > 0 && valA != Integer.MAX_VALUE && valB != Integer.MAX_VALUE)
		{
			if (Math.abs(valA - valB) <= threshold)
			{
				return 0; // Close enough: defer to secondary sort!
			}
		}

		return desc ? Integer.compare(valB, valA) : Integer.compare(valA, valB);
	}
}
