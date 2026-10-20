package com.advancedworldswitcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldType;

public class WorldTableRow extends JPanel
{
	private static final Color CURRENT_WORLD_COLOR = new Color(50, 80, 50);
	private static final Color CURRENT_WORLD_HOVER = new Color(60, 100, 60);
	private static final Color PING_EXCELLENT = new Color(60, 200, 80);
	private static final Color PING_FAIR = new Color(240, 200, 50);
	private static final Color PING_POOR = new Color(230, 80, 80);
	private static final Color MEMBERS_COLOR = new Color(240, 190, 80);
	private static final Color F2P_COLOR = ColorScheme.LIGHT_GRAY_COLOR;

	private final World world;
	private final JLabel favoriteLabel = new JLabel();
	private final JLabel pingLabel = new JLabel("-", SwingConstants.RIGHT);

	public World getWorld()
	{
		return world;
	}

	public void setPing(int ping, boolean isCurrent)
	{
		String pingText = ping <= 0 ? "-" : ping + "ms";
		pingLabel.setText(pingText);
		pingLabel.setForeground(getPingColor(ping));
		setToolTipText(buildTooltip(world, ping, isCurrent));
	}

	public WorldTableRow(World world, Integer ping, boolean isCurrent, boolean isFavorite,
						Consumer<World> onHop, Consumer<World> onToggleFavorite)
	{
		this.world = world;

		setLayout(new BorderLayout(4, 0));
		setBorder(new CompoundBorder(
			new MatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(4, 6, 4, 6)
		));

		Color bg = isCurrent ? CURRENT_WORLD_COLOR : ColorScheme.DARKER_GRAY_COLOR;
		Color hoverBg = isCurrent ? CURRENT_WORLD_HOVER : ColorScheme.DARK_GRAY_HOVER_COLOR;
		setBackground(bg);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setPreferredSize(new Dimension(0, 26));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

		// Left: Favorite star + World Number + Region Flag
		JPanel left = new JPanel(new BorderLayout(4, 0));
		left.setOpaque(false);

		favoriteLabel.setText(isFavorite ? "★" : "☆");
		favoriteLabel.setForeground(isFavorite ? Color.YELLOW : Color.GRAY);
		favoriteLabel.setFont(FontManager.getRunescapeSmallFont());
		favoriteLabel.setToolTipText(isFavorite ? "Remove favorite" : "Add favorite");
		favoriteLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				e.consume();
				if (onToggleFavorite != null)
				{
					onToggleFavorite.accept(world);
				}
			}
		});
		left.add(favoriteLabel, BorderLayout.WEST);

		JLabel worldNum = new JLabel("W" + world.getId());
		boolean isMembers = world.getTypes().contains(WorldType.MEMBERS);
		worldNum.setForeground(isMembers ? MEMBERS_COLOR : F2P_COLOR);
		worldNum.setFont(FontManager.getRunescapeBoldFont());
		worldNum.setPreferredSize(new Dimension(46, 16));
		left.add(worldNum, BorderLayout.CENTER);

		String flagText = getRegionFlag(world.getRegion());
		JLabel flagLabel = new JLabel(flagText);
		flagLabel.setFont(FontManager.getRunescapeSmallFont());
		flagLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		left.add(flagLabel, BorderLayout.EAST);

		// Center: Activity Name
		String activity = world.getActivity();
		if (activity == null || activity.trim().isEmpty() || "-".equals(activity.trim()))
		{
			activity = isMembers ? "Members" : "Free-to-play";
		}
		JLabel activityLabel = new JLabel(activity);
		activityLabel.setFont(FontManager.getRunescapeSmallFont());
		activityLabel.setForeground(Color.WHITE);
		activityLabel.setBorder(new EmptyBorder(0, 6, 0, 4));

		// Right: Population + Ping
		JPanel right = new JPanel(new GridLayout(1, 2, 4, 0));
		right.setOpaque(false);

		int players = world.getPlayers();
		String popText = players < 0 ? "OFF" : String.valueOf(players);
		JLabel popLabel = new JLabel(popText, SwingConstants.RIGHT);
		popLabel.setFont(FontManager.getRunescapeSmallFont());
		popLabel.setForeground(players >= 1950 ? PING_POOR : ColorScheme.LIGHT_GRAY_COLOR);
		popLabel.setPreferredSize(new Dimension(36, 16));

		String pingText = (ping == null || ping <= 0) ? "-" : ping + "ms";
		pingLabel.setText(pingText);
		pingLabel.setFont(FontManager.getRunescapeSmallFont());
		pingLabel.setForeground(getPingColor(ping));
		pingLabel.setPreferredSize(new Dimension(42, 16));

		right.add(popLabel);
		right.add(pingLabel);

		add(left, BorderLayout.WEST);
		add(activityLabel, BorderLayout.CENTER);
		add(right, BorderLayout.EAST);

		// Tooltip on the row and all sub-components so hovering anywhere works reliably
		String tooltip = buildTooltip(world, ping, isCurrent);
		setToolTipText(tooltip);
		activityLabel.setToolTipText(tooltip);
		worldNum.setToolTipText(tooltip);
		flagLabel.setToolTipText(tooltip);
		popLabel.setToolTipText(tooltip);
		pingLabel.setToolTipText(tooltip);
		left.setToolTipText(tooltip);
		right.setToolTipText(tooltip);

		// Hover and click actions
		MouseAdapter clickAdapter = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				setBackground(hoverBg);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setBackground(bg);
			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getSource() != favoriteLabel && onHop != null)
				{
					onHop.accept(world);
				}
			}
		};

		addMouseListener(clickAdapter);
		activityLabel.addMouseListener(clickAdapter);
		worldNum.addMouseListener(clickAdapter);
		flagLabel.addMouseListener(clickAdapter);
		left.addMouseListener(clickAdapter);
		right.addMouseListener(clickAdapter);
		popLabel.addMouseListener(clickAdapter);
		pingLabel.addMouseListener(clickAdapter);
	}

	private static Color getPingColor(Integer ping)
	{
		if (ping == null || ping <= 0)
		{
			return ColorScheme.LIGHT_GRAY_COLOR;
		}
		if (ping < 65)
		{
			return PING_EXCELLENT;
		}
		if (ping < 120)
		{
			return PING_FAIR;
		}
		return PING_POOR;
	}

	private static String getRegionFlag(WorldRegion region)
	{
		if (region == null)
		{
			return "";
		}
		switch (region)
		{
			case UNITED_STATES_OF_AMERICA:
				return "US";
			case UNITED_KINGDOM:
				return "UK";
			case AUSTRALIA:
				return "AU";
			case GERMANY:
				return "DE";
			default:
				return region.name();
		}
	}

	private static String buildTooltip(World world, Integer ping, boolean isCurrent)
	{
		StringBuilder sb = new StringBuilder("<html>");
		sb.append("<b>World ").append(world.getId()).append("</b>");
		if (isCurrent)
		{
			sb.append(" <font color='#50c878'>(Current)</font>");
		}
		sb.append("<br>");
		sb.append("Type: ").append(world.getTypes().contains(WorldType.MEMBERS) ? "Members" : "Free-to-play");
		if (world.getActivity() != null && !world.getActivity().isEmpty())
		{
			sb.append("<br>Activity: ").append(world.getActivity());
		}
		sb.append("<br>Region: ").append(world.getRegion() != null ? world.getRegion().name() : "Unknown");
		sb.append("<br>Population: ").append(world.getPlayers() < 0 ? "Offline" : world.getPlayers());
		if (ping != null && ping > 0)
		{
			sb.append("<br>Ping: ").append(ping).append(" ms");
		}
		sb.append("<br><i>Click to hop</i>");
		sb.append("</html>");
		return sb.toString();
	}
}
