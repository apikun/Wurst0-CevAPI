# Wurst0

**Wurst0** is a port of [Wurst7-CevAPI](https://github.com/cev-api/Wurst7-CevAPI)
to **Minecraft 1.12.2** (Forge, Java 8). It is a fork of Wurst7 by
Alexander01998, licensed **GPL-3.0-or-later**.

See [PORT_STATUS.md](PORT_STATUS.md) for the detailed port roadmap and
per-hack status.

## Status

- Core framework (hack/command system, event manager, settings, config
  saving, ClickGUI, HUD, keybinds) - **ported**
- 15 hacks fully implemented, 302 registered as no-op stubs to be ported
  incrementally
- Commands: `.help`, `.t <hack> [on|off]`, `.bind <hack> <key>`,
  `.binds`, `.say`
- ClickGUI opens with **Right Shift**

## Building

Requires **JDK 8** and **Gradle 4.10.3** (ForgeGradle 2.3 does not run on
modern Java):

```bash
export JAVA_HOME=/path/to/jdk8
gradle build
```

The jar is written to `build/libs/Wurst0-0.1.0.jar` - put it in your
`.minecraft/mods` folder with Forge 1.12.2-14.23.5.2859.

## License

This project is free software under the GNU General Public License
version 3 or later, same as Wurst7. Wurst7 is copyright
Wurst-Imperium and contributors.
