# ForgivingMod

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-blue)
![Fabric API](https://img.shields.io/badge/Fabric-API-blueviolet)
![License](https://img.shields.io/badge/Licence-ILRL-Blue)

---

## Overview

**ForgivingMod** is a Minecraft mod designed to give players a more forgiving death system. Players have a limited number of lives, can earn bonus lives, and gain extra health with each respawn. Upon reaching their last life, players receive a special warning. Optionally, players who exceed their maximum lives can be banned automatically.

This mod is fully configurable via a JSON configuration file and supports per-player death tracking with optional bonus lives.

---

## Features

- Track deaths per player.
- Limit maximum deaths and allow bonus lives.
- Apply extra health on respawn based on death count.
- Display a **last-life warning**.
- Automatic banning for players who exceed their allowed lives (optional).
- Admin commands for:
  - Resetting a player’s deaths and unbanning them.
  - Reloading the configuration.
  - Viewing player death stats.
- Fully configurable with clear, commented JSON config.
- Safe and asynchronous saving of player data.

---

## Installation

1. Ensure you have **Fabric Loader** installed for Minecraft.
2. Download the latest `ForgivingMod` `.jar` file.
3. Place the `.jar` in your `mods/` folder.
4. Launch Minecraft with Fabric.
5. Configuration will automatically generate at:
