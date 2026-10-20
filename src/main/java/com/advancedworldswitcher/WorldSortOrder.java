package com.advancedworldswitcher;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorldSortOrder
{
	WORLD_ASC("World (Low to High)"),
	WORLD_DESC("World (High to Low)"),
	PING_ASC("Lowest Ping"),
	PING_DESC("Highest Ping"),
	PLAYERS_ASC("Lowest Population"),
	PLAYERS_DESC("Highest Population"),
	ACTIVITY("Activity Name");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
