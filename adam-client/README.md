# Adam Client

One Fabric client mod (Minecraft 1.21.11) that merges **Adam Blocks** and **Adam Camera**, plus a new **Wings** section.

Press **Right Shift** in a world to open the module menu.

## Sections

| Section | Module | What it does |
|---|---|---|
| **Blocks** | Adam Blocks | 3D boxes on the blocks you pick, drawn through walls, with optional tracer lines from your crosshair. Switches for Tracers (all) and Hover tooltip; each block also has its own "Tracer to this block" switch, color, quick-color presets and opacity. "Configure module" opens the block list. |
| **Cam** | Freecam | Detaches the camera so you can fly around while your real player stands still. WASD move, Space up, Ctrl down, Shift faster. |
| **Wings** | Fly | Creative-style flight on foot. Jump to take off, Space up, Shift down, Ctrl faster. |
| **Wings** | Boatfly | Fly while riding a boat. WASD moves it where you look, Space up, Ctrl down, hovers when you let go. |
| **Sword** | Kill Aura | Attacks the closest target in range. Choose players / hostile mobs / passive mobs, range, attacks per second, look-at-target, wait-for-full-charge. Skips friends and tamed pets, needs line of sight. |
| **Sword** | Reach | Longer hit reach and block reach. In singleplayer the built-in server gets the same reach. |
| **Sword** | Criticals | Your hits (and Kill Aura's) land as crits. Doesn't work while sprinting (vanilla rule). |
| **Sword** | Auto Shield | Raises your shield when a hostile mob or player gets close; needs a shield and a sword, axe, shield or empty main hand. |
| **Sword** | Hitbox Expand | Makes mobs and players easier to hit with your crosshair. |
| **Sword** | Auto Weapon | Switches to your best sword (or axe) before every hit. |
| **Protection** | Staff Protection | Lists every player within a radius of you (default 50 blocks, adjustable) in the screen corner you pick, with an optional red screen alert when someone new comes near. |
| **Protection** | Spectator Protection | Lists every player on the server who is in spectator mode, in the corner you pick, with an optional red screen alert. |
| **Sword** | Friend Protection | "Configure module" lists everyone on your server (or type a name). Friends can't be hit by you or Kill Aura. On by default. |

Every module has a rebindable key (Options > Controls > Key Binds > Adam Client):

| Module | Default key |
|---|---|
| Menu | Right Shift |
| Freecam | F4 |
| Fly | G |
| Boatfly | B |
| Kill Aura | R |
| Everything else | not bound |

Sliders (speeds, range, group distance) are in the menu's preview panel on the right. Settings are saved to `config/adam.json`.
If you used the standalone Adam Blocks mod, its block list is imported from `config/adamblocks.json` the first time.

## Notes

- Fly, Boatfly and the Sword modules are meant for **singleplayer** or servers you run. On public servers, anti-cheat will flag them and you can be banned, and using Kill Aura or Reach on other players ruins the game for them.
- Remove the old `adam-blocks` and `adam-camera` jars from your mods folder. This mod replaces both.

## Build

You need **JDK 25** (the mod itself targets Java 21).

    ./gradlew build        (Windows: gradlew.bat build)

The jar is created at `build/libs/adam-client-1.0.0.jar`. Pushing to GitHub also builds it with the included Actions workflow.

## Install

Put the jar in `.minecraft/mods` with **Fabric Loader 0.19.5+** and **Fabric API** for Minecraft 1.21.11.

## Project layout

    module/Module.java          base class for every module (toggle, settings, key)
    module/ModuleManager.java   creates modules, registers keys, runs ticks
    module/blocks/              Adam Blocks (tracker, overlay, block list screens)
    module/cam/                 Freecam (camera entity + module)
    module/wings/               Fly and Boatfly
    module/protection/          Staff Protection, Spectator Protection and their on-screen lists/alerts
    module/sword/               Kill Aura, Reach, Criticals, Auto Shield, Hitbox Expand, Auto Weapon, Friend Protection
    gui/AdamScreen.java         the module menu
    mixin/                      freecam mixins, plus the attack hook (friends, auto weapon, criticals) and hitbox hook

To add a module: extend `Module`, then add it to the list in `ModuleManager.init`.
