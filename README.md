# Advanced World Switcher

A RuneLite plugin providing multi-order world sorting, activity categorization, fast concurrent ping measurement with session caching, and quick-hopping.

## Features

- **Multi-Order World Sorting:**
  - Define both a **Primary Sort** and a **Secondary Sort** (e.g. Primary: *Lowest Ping*, Secondary: *Lowest Population*).
  - Supported criteria: Ping (Asc/Desc), Population (Asc/Desc), World ID (Asc/Desc), Activity Name.
- **Ping Threshold:**
  - Configurable tolerance (default `15ms`) where worlds with near-identical pings defer to the secondary sort (e.g. lowest player count).
- **Fast Concurrent Pinging & Persistence:**
  - Pings all worlds concurrently in parallel on launch (~1.5s total).
  - Pings are automatically persisted to your RuneLite profile so the switcher displays ping times immediately on startup.
- **Activity Grouping & Sections:**
  - Optionally group worlds into collapsible headers:
    - **Skill Total** (500, 750, 1250, 1500, 1750, 2000, 2200)
    - **Minigames** (Pest Control, Barbarian Assault, Wintertodt, Tempoross, GOTR, Soul Wars, Castle Wars, etc.)
    - **Bossing & Raids** (CoX, ToB, ToA, Nex, Corp, Yama, etc.)
    - **PvP & High Risk** (Bounty Hunter, PvP, High Risk, Target Worlds)
    - **Skilling & Training**
    - **Social & Trading** (Trade, Role-playing)
    - **Quest Speedrunning**
    - **General Worlds**
- **Favorites:**
  - Click the star (★) next to any world to pin favorite worlds to the top section.
- **Live Search:**
  - Filter worlds by number, activity, or region in real time as you type.
- **Filter Toggles:**
  - Instant checkbox filters to hide PvP / High Risk, Skill Total worlds, Fresh Start worlds, and Free-to-play worlds.
- **Quick Hop:**
  - Click any row to hop to that world immediately (supported both in-game and from the login screen).

## Configuration

- **Primary Sort:** Primary criterion to sort the world list.
- **Secondary Sort:** Secondary criterion used when primary values are close or tied.
- **Ping Threshold:** Differences within this threshold (in ms) are considered equal for tie-breaking.
- **Group by Activity:** Toggles section headers grouping worlds by activity.
- **Hide PvP / High Risk:** Hides PvP, High Risk, Bounty Hunter, and Deadman worlds.
- **Hide Skill Total:** Hides worlds with total level requirements.
- **Hide Fresh Start:** Hides Fresh Start worlds.
- **Hide Free-to-play:** Hides Free-to-play worlds.

## Running in Development

Run with the included development launcher:
```powershell
.\run-dev.bat
```
Or with Gradle:
```powershell
.\gradlew.bat run
```

## License

This project is licensed under the BSD 2-Clause License - see the [LICENSE](LICENSE) file for details.
