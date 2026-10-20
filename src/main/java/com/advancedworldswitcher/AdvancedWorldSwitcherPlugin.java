package com.advancedworldswitcher;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldListLoad;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.WorldsFetch;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.worldhopper.ping.Ping;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

@Slf4j
@PluginDescriptor(
	name = "Advanced World Switcher",
	description = "Multi-order world sorting (ping, population), activity grouping, and quick hop",
	tags = {"world", "hop", "switcher", "ping", "activity", "sort", "population", "group"}
)
public class AdvancedWorldSwitcherPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private WorldService worldService;

	@Inject
	private AdvancedWorldSwitcherConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	private AdvancedWorldSwitcherPanel panel;
	private NavigationButton navButton;

	@Getter
	private final Map<Integer, Integer> pings = new HashMap<>();

	@Getter
	private final Set<Integer> favorites = new HashSet<>();

	private ScheduledExecutorService pingScheduler;
	private ExecutorService pingPool;
	private ScheduledFuture<?> pingTask;
	private final AtomicBoolean isPinging = new AtomicBoolean(false);
	private volatile long lastPingSweep = 0;

	private net.runelite.api.World quickHopTargetWorld;
	private int displaySwitcherAttempts;

	@Override
	protected void startUp()
	{
		pingScheduler = Executors.newSingleThreadScheduledExecutor(
			new ThreadFactoryBuilder().setNameFormat("aws-scheduler-%d").setDaemon(true).build()
		);
		pingPool = Executors.newFixedThreadPool(
			12,
			new ThreadFactoryBuilder().setNameFormat("aws-ping-%d").setDaemon(true).build()
		);

		loadFavorites();
		loadCachedPings();
		panel = new AdvancedWorldSwitcherPanel(this, config);

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "world_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Advanced World Switcher")
			.icon(icon)
			.priority(4)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		startPinging();
		panel.updateList();
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;

		if (pingTask != null)
		{
			pingTask.cancel(true);
			pingTask = null;
		}

		if (pingScheduler != null)
		{
			pingScheduler.shutdownNow();
			pingScheduler = null;
		}

		if (pingPool != null)
		{
			pingPool.shutdownNow();
			pingPool = null;
		}

		saveCachedPings();
		pings.clear();
		resetQuickHopper();
	}

	public List<World> getWorlds()
	{
		WorldResult result = worldService.getWorlds();
		return result != null ? result.getWorlds() : Collections.emptyList();
	}

	public int getCurrentWorld()
	{
		return client.getWorld();
	}

	public void hopTo(World world)
	{
		if (world == null)
		{
			return;
		}

		clientThread.invoke(() ->
		{
			net.runelite.api.World rsWorld = client.createWorld();
			rsWorld.setActivity(world.getActivity());
			rsWorld.setAddress(world.getAddress());
			rsWorld.setId(world.getId());
			rsWorld.setPlayerCount(world.getPlayers());
			if (world.getRegion() != null)
			{
				rsWorld.setLocation(world.getRegion().getId());
			}
			rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

			if (client.getGameState() == GameState.LOGIN_SCREEN)
			{
				client.changeWorld(rsWorld);
				if (panel != null)
				{
					panel.updateList();
				}
				return;
			}

			String message = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append("Quick-hopping to World ")
				.append(ChatColorType.HIGHLIGHT)
				.append(Integer.toString(world.getId()))
				.append(ChatColorType.NORMAL)
				.append("..")
				.build();

			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(message)
				.build());

			quickHopTargetWorld = rsWorld;
			displaySwitcherAttempts = 0;
		});
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (quickHopTargetWorld == null)
		{
			return;
		}

		if (client.getWidget(ComponentID.WORLD_SWITCHER_WORLD_LIST) == null)
		{
			client.openWorldHopper();
			if (++displaySwitcherAttempts >= 3)
			{
				String message = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("Failed to quick-hop after ")
					.append(ChatColorType.HIGHLIGHT)
					.append(Integer.toString(displaySwitcherAttempts))
					.append(ChatColorType.NORMAL)
					.append(" attempts.")
					.build();

				chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(message)
					.build());

				resetQuickHopper();
			}
			return;
		}

		client.hopToWorld(quickHopTargetWorld);
		resetQuickHopper();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if ("Please finish what you're doing before using the World Switcher.".equals(event.getMessage()))
		{
			resetQuickHopper();
		}
	}

	private void resetQuickHopper()
	{
		quickHopTargetWorld = null;
		displaySwitcherAttempts = 0;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (panel != null)
		{
			panel.updateList();
		}
	}

	@Subscribe
	public void onWorldsFetch(WorldsFetch event)
	{
		if (panel != null)
		{
			panel.updateList();
		}
		pingAllWorlds();
	}

	@Subscribe
	public void onWorldListLoad(WorldListLoad event)
	{
		if (panel != null)
		{
			panel.updateList();
		}
		pingAllWorlds();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!AdvancedWorldSwitcherConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("cachedPings".equals(event.getKey()) || "favorites".equals(event.getKey()))
		{
			return;
		}

		if (panel != null)
		{
			panel.updateList();
		}
	}

	public void toggleFavorite(World world)
	{
		if (world == null)
		{
			return;
		}
		int id = world.getId();
		if (favorites.contains(id))
		{
			favorites.remove(id);
		}
		else
		{
			favorites.add(id);
		}
		saveFavorites();
		if (panel != null)
		{
			panel.updateList();
		}
	}

	private void loadFavorites()
	{
		favorites.clear();
		String str = configManager.getConfiguration(AdvancedWorldSwitcherConfig.GROUP, "favorites");
		if (str != null && !str.trim().isEmpty())
		{
			for (String part : str.split(","))
			{
				try
				{
					favorites.add(Integer.parseInt(part.trim()));
				}
				catch (NumberFormatException ignored)
				{
				}
			}
		}
	}

	private void saveFavorites()
	{
		String str = favorites.stream()
			.map(String::valueOf)
			.collect(Collectors.joining(","));
		configManager.setConfiguration(AdvancedWorldSwitcherConfig.GROUP, "favorites", str);
	}

	private void loadCachedPings()
	{
		pings.clear();
		String str = configManager.getConfiguration(AdvancedWorldSwitcherConfig.GROUP, "cachedPings");
		if (str != null && !str.trim().isEmpty())
		{
			for (String entry : str.split(","))
			{
				String[] kv = entry.split(":");
				if (kv.length == 2)
				{
					try
					{
						int wid = Integer.parseInt(kv[0].trim());
						int ping = Integer.parseInt(kv[1].trim());
						if (ping > 0)
						{
							pings.put(wid, ping);
						}
					}
					catch (NumberFormatException ignored)
					{
					}
				}
			}
		}
	}

	private synchronized void saveCachedPings()
	{
		if (pings.isEmpty())
		{
			return;
		}

		String str = pings.entrySet().stream()
			.map(e -> e.getKey() + ":" + e.getValue())
			.collect(Collectors.joining(","));
		configManager.setConfiguration(AdvancedWorldSwitcherConfig.GROUP, "cachedPings", str);
	}

	private void startPinging()
	{
		if (pingScheduler != null)
		{
			pingTask = pingScheduler.scheduleWithFixedDelay(this::pingAllWorlds, 0, 60, TimeUnit.SECONDS);
		}
	}

	private void pingAllWorlds()
	{
		List<World> worlds = getWorlds();
		if (worlds == null || worlds.isEmpty() || pingPool == null || pingPool.isShutdown())
		{
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastPingSweep < 10000)
		{
			return;
		}
		if (!isPinging.compareAndSet(false, true))
		{
			return;
		}
		lastPingSweep = now;

		// Prioritize favorites and local region first
		List<World> sortedWorlds = new ArrayList<>(worlds);
		sortedWorlds.sort((w1, w2) ->
		{
			boolean fav1 = favorites.contains(w1.getId());
			boolean fav2 = favorites.contains(w2.getId());
			if (fav1 != fav2)
			{
				return fav1 ? -1 : 1;
			}
			return Integer.compare(w1.getId(), w2.getId());
		});

		final int total = sortedWorlds.size();
		final AtomicInteger finished = new AtomicInteger(0);

		for (World w : sortedWorlds)
		{
			pingPool.execute(() ->
			{
				try
				{
					int ping = Ping.ping(w);
					if (ping > 0)
					{
						pings.put(w.getId(), ping);
						if (panel != null)
						{
							panel.updatePing(w.getId(), ping);
						}
					}
				}
				catch (Exception ignored)
				{
				}
				finally
				{
					if (finished.incrementAndGet() >= total)
					{
						isPinging.set(false);
						saveCachedPings();
					}
				}
			});
		}

		// Also schedule early saves so newly discovered pings persist even if closed quickly
		if (pingScheduler != null && !pingScheduler.isShutdown())
		{
			pingScheduler.schedule(this::saveCachedPings, 3, TimeUnit.SECONDS);
		}
	}

	@Provides
	AdvancedWorldSwitcherConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AdvancedWorldSwitcherConfig.class);
	}
}
