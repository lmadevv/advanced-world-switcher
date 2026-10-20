package com.advancedworldswitcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;

public class AdvancedWorldSwitcherPanel extends PluginPanel
{
	private final AdvancedWorldSwitcherPlugin plugin;
	private final AdvancedWorldSwitcherConfig config;

	private final IconTextField searchBar = new IconTextField();
	private final JComboBox<WorldSortOrder> primarySortBox = new JComboBox<>(WorldSortOrder.values());
	private final JComboBox<WorldSortOrder> secondarySortBox = new JComboBox<>(WorldSortOrder.values());
	private final JCheckBox groupByActivityCheck = new JCheckBox("Group by Activity");
	private final JCheckBox hidePvpCheck = new JCheckBox("Hide PvP / Risk");
	private final JCheckBox hideSkillTotalCheck = new JCheckBox("Hide Skill Total");
	private final JCheckBox hideFreshStartCheck = new JCheckBox("Hide Fresh Start");
	private final JCheckBox hideF2pCheck = new JCheckBox("Hide Free-to-play");

	private final JPanel worldContainer = new JPanel();
	private final JLabel statusLabel = new JLabel("Loading worlds...", SwingConstants.CENTER);
	private final List<WorldTableRow> rows = new ArrayList<>();

	private final Set<ActivityCategory> collapsedCategories = new HashSet<>();

	public void updatePing(int worldId, int ping)
	{
		SwingUtilities.invokeLater(() ->
		{
			for (WorldTableRow row : rows)
			{
				if (row.getWorld().getId() == worldId)
				{
					row.setPing(ping, worldId == plugin.getCurrentWorld());
					break;
				}
			}
		});
	}

	public AdvancedWorldSwitcherPanel(AdvancedWorldSwitcherPlugin plugin, AdvancedWorldSwitcherConfig config)
	{
		super(false);
		this.plugin = plugin;
		this.config = config;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header Controls
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(new EmptyBorder(8, 8, 8, 8));

		// Search bar
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(0, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.addActionListener(e -> updateList());
		searchBar.addClearListener(this::updateList);
		searchBar.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				updateList();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				updateList();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				updateList();
			}
		});

		// Sort controls panel
		JPanel sortGrid = new JPanel(new GridLayout(2, 2, 4, 4));
		sortGrid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		sortGrid.setBorder(new EmptyBorder(6, 0, 4, 0));

		JLabel pLabel = new JLabel("Primary Sort:");
		pLabel.setFont(FontManager.getRunescapeSmallFont());
		pLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JLabel sLabel = new JLabel("Secondary Sort:");
		sLabel.setFont(FontManager.getRunescapeSmallFont());
		sLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		primarySortBox.setFont(FontManager.getRunescapeSmallFont());
		primarySortBox.setSelectedItem(config.primarySort());
		primarySortBox.addActionListener(e -> updateList());

		secondarySortBox.setFont(FontManager.getRunescapeSmallFont());
		secondarySortBox.setSelectedItem(config.secondarySort());
		secondarySortBox.addActionListener(e -> updateList());

		sortGrid.add(pLabel);
		sortGrid.add(primarySortBox);
		sortGrid.add(sLabel);
		sortGrid.add(secondarySortBox);

		// Filters checkbox row (3 rows, 2 columns)
		JPanel checkRow = new JPanel(new GridLayout(3, 2, 2, 2));
		checkRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		checkRow.setBorder(new EmptyBorder(4, 0, 4, 0));

		groupByActivityCheck.setFont(FontManager.getRunescapeSmallFont());
		groupByActivityCheck.setSelected(config.groupByActivity());
		groupByActivityCheck.addActionListener(e -> updateList());

		hidePvpCheck.setFont(FontManager.getRunescapeSmallFont());
		hidePvpCheck.setSelected(config.hidePvp());
		hidePvpCheck.addActionListener(e -> updateList());

		hideSkillTotalCheck.setFont(FontManager.getRunescapeSmallFont());
		hideSkillTotalCheck.setSelected(config.hideSkillTotal());
		hideSkillTotalCheck.addActionListener(e -> updateList());

		hideFreshStartCheck.setFont(FontManager.getRunescapeSmallFont());
		hideFreshStartCheck.setSelected(config.hideFreshStart());
		hideFreshStartCheck.addActionListener(e -> updateList());

		hideF2pCheck.setFont(FontManager.getRunescapeSmallFont());
		hideF2pCheck.setSelected(config.hideF2p());
		hideF2pCheck.addActionListener(e -> updateList());

		checkRow.add(groupByActivityCheck);
		checkRow.add(hidePvpCheck);
		checkRow.add(hideSkillTotalCheck);
		checkRow.add(hideFreshStartCheck);
		checkRow.add(hideF2pCheck);

		// Column Header Row
		JPanel columnHeaders = new JPanel(new BorderLayout(4, 0));
		columnHeaders.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		columnHeaders.setBorder(new EmptyBorder(4, 6, 4, 6));

		JLabel cWorld = new JLabel("★ World");
		cWorld.setFont(FontManager.getRunescapeBoldFont());
		cWorld.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JLabel cActivity = new JLabel("Activity", SwingConstants.LEFT);
		cActivity.setFont(FontManager.getRunescapeBoldFont());
		cActivity.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		cActivity.setBorder(new EmptyBorder(0, 16, 0, 0));

		JPanel cRight = new JPanel(new GridLayout(1, 2, 4, 0));
		cRight.setOpaque(false);
		JLabel cPop = new JLabel("Pop", SwingConstants.RIGHT);
		cPop.setFont(FontManager.getRunescapeBoldFont());
		cPop.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		JLabel cPing = new JLabel("Ping", SwingConstants.RIGHT);
		cPing.setFont(FontManager.getRunescapeBoldFont());
		cPing.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		cRight.add(cPop);
		cRight.add(cPing);

		columnHeaders.add(cWorld, BorderLayout.WEST);
		columnHeaders.add(cActivity, BorderLayout.CENTER);
		columnHeaders.add(cRight, BorderLayout.EAST);

		header.add(searchBar);
		header.add(sortGrid);
		header.add(checkRow);
		header.add(columnHeaders);

		// World list container
		worldContainer.setLayout(new BoxLayout(worldContainer, BoxLayout.Y_AXIS));
		worldContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setBorder(new EmptyBorder(16, 4, 16, 4));
		worldContainer.add(statusLabel);

		JScrollPane scrollPane = new JScrollPane(worldContainer);
		scrollPane.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(null);

		add(header, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	public void updateList()
	{
		SwingUtilities.invokeLater(() ->
		{
			List<World> allWorlds = plugin.getWorlds();
			if (allWorlds == null || allWorlds.isEmpty())
			{
				worldContainer.removeAll();
				statusLabel.setText("Fetching worlds from Jagex...");
				worldContainer.add(statusLabel);
				worldContainer.revalidate();
				worldContainer.repaint();
				return;
			}

			Map<Integer, Integer> pings = plugin.getPings();
			Set<Integer> favorites = plugin.getFavorites();
			int currentWorld = plugin.getCurrentWorld();

			WorldSortOrder pSort = (WorldSortOrder) primarySortBox.getSelectedItem();
			WorldSortOrder sSort = (WorldSortOrder) secondarySortBox.getSelectedItem();
			boolean groupActivities = groupByActivityCheck.isSelected();
			boolean hidePvp = hidePvpCheck.isSelected();
			boolean hideTotal = hideSkillTotalCheck.isSelected();
			boolean hideFreshStart = hideFreshStartCheck.isSelected();
			boolean hideF2p = hideF2pCheck.isSelected();
			String search = searchBar.getText().toLowerCase(Locale.ROOT).trim();

			// Filter worlds
			List<World> filtered = allWorlds.stream().filter(w ->
			{
				if (hidePvp && (w.getTypes().contains(WorldType.PVP) || w.getTypes().contains(WorldType.HIGH_RISK) || w.getTypes().contains(WorldType.DEADMAN)))
				{
					return false;
				}
				if (hideTotal && w.getTypes().contains(WorldType.SKILL_TOTAL))
				{
					return false;
				}
				if (hideFreshStart && (w.getTypes().contains(WorldType.FRESH_START_WORLD) || (w.getActivity() != null && w.getActivity().toLowerCase(Locale.ROOT).contains("fresh start"))))
				{
					return false;
				}
				if (hideF2p && !w.getTypes().contains(WorldType.MEMBERS))
				{
					return false;
				}
				if (!search.isEmpty())
				{
					String idStr = String.valueOf(w.getId());
					String act = w.getActivity() != null ? w.getActivity().toLowerCase(Locale.ROOT) : "";
					String loc = w.getRegion() != null ? w.getRegion().name().toLowerCase(Locale.ROOT) : "";
					if (!idStr.contains(search) && !act.contains(search) && !loc.contains(search))
					{
						return false;
					}
				}
				return true;
			}).collect(Collectors.toList());

			WorldComparator comparator = new WorldComparator(pSort, sSort, pings, config.pingThreshold());

			worldContainer.removeAll();
			rows.clear();

			if (filtered.isEmpty())
			{
				statusLabel.setText("No worlds match current filter.");
				worldContainer.add(statusLabel);
				worldContainer.revalidate();
				worldContainer.repaint();
				return;
			}

			if (groupActivities)
			{
				// Group by ActivityCategory
				Map<ActivityCategory, List<World>> grouped = filtered.stream()
					.collect(Collectors.groupingBy(w -> ActivityCategory.categorize(w, favorites.contains(w.getId()))));

				List<ActivityCategory> categories = new ArrayList<>(grouped.keySet());
				categories.sort((c1, c2) -> Integer.compare(c1.getOrder(), c2.getOrder()));

				for (ActivityCategory category : categories)
				{
					List<World> catWorlds = grouped.get(category);
					catWorlds.sort(comparator);

					worldContainer.add(createCategoryHeader(category, catWorlds.size()));

					if (!collapsedCategories.contains(category))
					{
						for (World w : catWorlds)
						{
							WorldTableRow row = new WorldTableRow(
								w,
								pings.get(w.getId()),
								w.getId() == currentWorld,
								favorites.contains(w.getId()),
								plugin::hopTo,
								plugin::toggleFavorite
							);
							rows.add(row);
							worldContainer.add(row);
						}
					}
				}
			}
			else
			{
				// Flat list with Favorites pinned to top if desired
				List<World> favs = new ArrayList<>();
				List<World> nonFavs = new ArrayList<>();
				for (World w : filtered)
				{
					if (favorites.contains(w.getId()))
					{
						favs.add(w);
					}
					else
					{
						nonFavs.add(w);
					}
				}

				favs.sort(comparator);
				nonFavs.sort(comparator);

				for (World w : favs)
				{
					WorldTableRow row = new WorldTableRow(
						w,
						pings.get(w.getId()),
						w.getId() == currentWorld,
						true,
						plugin::hopTo,
						plugin::toggleFavorite
					);
					rows.add(row);
					worldContainer.add(row);
				}
				for (World w : nonFavs)
				{
					WorldTableRow row = new WorldTableRow(
						w,
						pings.get(w.getId()),
						w.getId() == currentWorld,
						false,
						plugin::hopTo,
						plugin::toggleFavorite
					);
					rows.add(row);
					worldContainer.add(row);
				}
			}

			worldContainer.add(Box.createVerticalGlue());

			worldContainer.revalidate();
			worldContainer.repaint();
		});
	}

	private JPanel createCategoryHeader(ActivityCategory category, int count)
	{
		boolean collapsed = collapsedCategories.contains(category);
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		panel.setBorder(new CompoundBorder(
			new MatteBorder(1, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(6, 8, 6, 8)
		));
		panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		panel.setPreferredSize(new Dimension(0, 24));
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

		JLabel title = new JLabel((collapsed ? "▶ " : "▼ ") + category.getDisplayName() + " (" + count + ")");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(category == ActivityCategory.FAVORITES ? Color.YELLOW : Color.WHITE);

		panel.add(title, BorderLayout.CENTER);

		panel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (collapsedCategories.contains(category))
				{
					collapsedCategories.remove(category);
				}
				else
				{
					collapsedCategories.add(category);
				}
				updateList();
			}
		});

		return panel;
	}
}
