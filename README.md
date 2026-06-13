# MDTanos

[![Version](https://img.shields.io/badge/version-1.0-blue.svg)]()
[![Platform](https://img.shields.io/badge/platform-Spigot%2FPaper-green.svg)]()
[![License](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)

A comprehensive Minecraft server plugin designed to bring unique boss encounters and gameplay enhancements to your server.

## Features
* **Unlimited Boss Encounters**: Create as many custom bosses as you want with unique stats and abilities.
* **Custom Boss Encounters**: Challenging and immersive boss fights.
* **Altar System**: Unique altar interactions and mechanics.
* **Custom Mechanics**: Specialized handling for death events, mining, and protection.
* **Highly Configurable**: Full control via YAML configuration files.

## Supported Versions
* **Platform**: Spigot / Paper (1.16.5+)

## Configuration
You can customize the plugin behavior using the provided configuration files.

### boss.yml
```yaml
bosses:
  thanos:
    world: pvp1
    x: -126
    y: 172
    z: 275
    display-name: "&6Thanos"
    entity-type: IRON_GOLEM
    health: 1000.0
    # ... more settings
    minions:
      types:
      - HOGLIN
      - PIGLIN_BRUTE
      amount: 3
      health: 20.0
      limit: 30
      spawn-interval-seconds: 30
      spawn-on-low-hp: true
      low-hp-threshold: 0.3
    auto-spawn:
      enabled: false
      interval-minutes: 60
```


## Dependencies
### Hard Depend
* [Citizens](https://www.spigotmc.org/resources/citizens.13811/)

### Soft Depend (Optional)
* [WorldGuard](https://worldguard.enginehub.org/)
* [DecentHolograms](https://www.spigotmc.org/resources/decentholograms.96927/)

## Installation
1. Install [Citizens](https://www.spigotmc.org/resources/citizens.13811/).
2. Put `MDTanos.jar` into your plugins folder.
3. Restart the server.
4. Configure the plugin in `plugins/MDTanos/`.      
