package com.advancedworldswitcher;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AdvancedWorldSwitcherPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AdvancedWorldSwitcherPlugin.class);
		RuneLite.main(args);
	}
}
