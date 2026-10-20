package com.advancedworldswitcher;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(AdvancedWorldSwitcherConfig.GROUP)
public interface AdvancedWorldSwitcherConfig extends Config
{
	String GROUP = "advancedworldswitcher";

	@ConfigItem(
		keyName = "primarySort",
		name = "Primary Sort",
		description = "First priority sorting criterion",
		position = 1
	)
	default WorldSortOrder primarySort()
	{
		return WorldSortOrder.PING_ASC;
	}

	@ConfigItem(
		keyName = "secondarySort",
		name = "Secondary Sort",
		description = "Secondary criterion used when primary values are close or tied",
		position = 2
	)
	default WorldSortOrder secondarySort()
	{
		return WorldSortOrder.PLAYERS_ASC;
	}

	@ConfigItem(
		keyName = "groupByActivity",
		name = "Group by Activity",
		description = "Group worlds under categorized sections (Skill Total, Minigames, PvP, etc.)",
		position = 3
	)
	default boolean groupByActivity()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hidePvp",
		name = "Hide PvP / High Risk",
		description = "Hide PvP, Bounty, and High Risk dangerous worlds",
		position = 4
	)
	default boolean hidePvp()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideSkillTotal",
		name = "Hide Skill Total",
		description = "Hide worlds with skill total level requirements",
		position = 5
	)
	default boolean hideSkillTotal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideFreshStart",
		name = "Hide Fresh Start",
		description = "Hide Fresh Start worlds",
		position = 6
	)
	default boolean hideFreshStart()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideF2p",
		name = "Hide Free-to-play",
		description = "Hide Free-to-play worlds",
		position = 7
	)
	default boolean hideF2p()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pingThreshold",
		name = "Ping Difference Threshold",
		description = "Consider pings equal if within this amount of ms so secondary sort takes precedence",
		position = 6
	)
	default int pingThreshold()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "favorites",
		name = "Favorite Worlds",
		description = "List of favorite world IDs",
		hidden = true
	)
	default String favorites()
	{
		return "";
	}

	@ConfigItem(
		keyName = "cachedPings",
		name = "Cached Pings",
		description = "Persisted ping cache across restarts",
		hidden = true
	)
	default String cachedPings()
	{
		return "";
	}
}
