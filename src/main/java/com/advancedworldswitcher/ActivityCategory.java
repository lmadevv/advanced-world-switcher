package com.advancedworldswitcher;

import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;

@Getter
@RequiredArgsConstructor
public enum ActivityCategory
{
	FAVORITES("Favorites", 0),
	SKILL_TOTAL("Skill Total", 1),
	PVP_WILDERNESS("PvP & Wilderness", 2),
	BOSSING_RAIDS("Bossing & Raids", 3),
	MINIGAMES("Minigames", 4),
	SKILLING("Skilling & Training", 5),
	SOCIAL_COMMUNITY("Social & Trading", 6),
	QUEST_SPEEDRUNNING("Quest Speedrunning", 7),
	SPECIALTY("Other Activities", 8),
	GENERAL("General Worlds", 9);

	private final String displayName;
	private final int order;

	public static ActivityCategory categorize(World world, boolean isFavorite)
	{
		if (isFavorite)
		{
			return FAVORITES;
		}

		if (world.getTypes().contains(WorldType.SKILL_TOTAL))
		{
			return SKILL_TOTAL;
		}

		if (world.getTypes().contains(WorldType.PVP)
			|| world.getTypes().contains(WorldType.HIGH_RISK)
			|| world.getTypes().contains(WorldType.BOUNTY)
			|| world.getTypes().contains(WorldType.DEADMAN)
			|| world.getTypes().contains(WorldType.PVP_ARENA)
			|| world.getTypes().contains(WorldType.LAST_MAN_STANDING))
		{
			return PVP_WILDERNESS;
		}

		if (world.getTypes().contains(WorldType.QUEST_SPEEDRUNNING))
		{
			return QUEST_SPEEDRUNNING;
		}

		String act = world.getActivity();
		if (act == null || act.trim().isEmpty() || "-".equals(act.trim()))
		{
			return GENERAL;
		}

		String lower = act.toLowerCase(Locale.ROOT);

		// Skill total text in activity string
		if (lower.contains("skill total"))
		{
			return SKILL_TOTAL;
		}

		// PvP & Wilderness activities
		if (lower.contains("pvp")
			|| lower.contains("wilderness pk")
			|| lower.contains("bounty hunter")
			|| lower.contains("deadman")
			|| lower.contains("high risk")
			|| lower.contains("lms"))
		{
			return PVP_WILDERNESS;
		}

		// Bosses & Raids (including Yama, Nex, Colosseum, Varlamore PvM, Titans, etc.)
		if (lower.contains("yama")
			|| lower.contains("theatre of blood")
			|| lower.contains("tombs of amascut")
			|| lower.contains("toa")
			|| lower.contains("chambers of xeric")
			|| lower.contains("cox")
			|| lower.contains("nex")
			|| lower.contains("nightmare")
			|| lower.contains("royal titans")
			|| lower.contains("colosseum")
			|| lower.contains("varlamore pvm")
			|| lower.contains("group pvm")
			|| lower.contains("corporeal beast")
			|| lower.contains("god wars")
			|| lower.contains("dagannoth kings")
			|| lower.contains("kalphite queen")
			|| lower.contains("zalcano")
			|| lower.contains("tzhaar fight pit"))
		{
			return BOSSING_RAIDS;
		}

		// Minigames
		if (lower.contains("pest control")
			|| lower.contains("barbarian assault")
			|| lower.contains("wintertodt")
			|| lower.contains("tempoross")
			|| lower.contains("guardians of the rift")
			|| lower.contains("blast furnace")
			|| lower.contains("volcanic mine")
			|| lower.contains("fishing trawler")
			|| lower.contains("castle wars")
			|| lower.contains("soul wars")
			|| lower.contains("trouble brewing")
			|| lower.contains("clan wars")
			|| lower.contains("burthorpe games")
			|| lower.contains("mort'ton temple")
			|| lower.contains("rat pits")
			|| lower.contains("pyramid plunder")
			|| lower.contains("sorceress's garden")
			|| lower.contains("tithe farm"))
		{
			return MINIGAMES;
		}

		// Skilling & Training
		if (lower.contains("forestry")
			|| lower.contains("group skilling")
			|| lower.contains("agility")
			|| lower.contains("brimhaven agility")
			|| lower.contains("mastering mixology")
			|| lower.contains("ourania altar")
			|| lower.contains("zeah runecrafting")
			|| lower.contains("sulliuscep")
			|| lower.contains("salvaging"))
		{
			return SKILLING;
		}

		// Social & Trading
		if (lower.contains("trade")
			|| lower.contains("house party")
			|| lower.contains("gilded altar")
			|| lower.contains("clan recruitment")
			|| lower.contains("falador party room")
			|| lower.contains("role-playing")
			|| lower.contains("dog walking"))
		{
			return SOCIAL_COMMUNITY;
		}

		if (lower.contains("speedrunning"))
		{
			return QUEST_SPEEDRUNNING;
		}

		return SPECIALTY;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
