# Wurst0 - 1.12.2 Port Status

Port of [Wurst7-CevAPI](https://github.com/cev-api/Wurst7-CevAPI)
(MC 26.3 / Fabric / Java 25) to **Minecraft 1.12.2 / Forge / Java 8**.

- **Branch:** `mc1.12.2` on [apikun/Wurst0-CevAPI](https://github.com/apikun/Wurst0-CevAPI)
- **Mod id:** `wurst0`  **Package:** `net.wurst0`  **Version:** 0.1.0
- **License:** GPL-3.0-or-later (unchanged - this is a derivative of Wurst7)
- **Target:** Forge 1.12.2-14.23.5.2859, MCP `stable_39` mappings, RetroFuturaGradle 2.0.2 + Gradle 9.8.2

---

## Current state (updated 2026-10-30, v0.1.0 BUILD PASSING)

**The mod now compiles end-to-end**: `gradle build` produces
`build/libs/Wurst0-0.1.0.jar` (reobfuscated, drop into `.minecraft/mods`
with Forge 1.12.2) plus a `-dev` jar for development.

| Item | Count | Status |
|---|---|---|
| Hacks registered | 317 | all appear in ClickGUI |
| Hacks fully implemented | 15 | working logic |
| Hacks stubbed | 302 | compile + register, no logic yet (`[not ported yet]` in tooltip) |
| Commands | 5 | `.help` `.t` `.bind` `.binds` `.say` |
| ClickGUI | done | RShift, draggable windows, checkboxes/sliders/modes |
| HUD | done | watermark + enabled-hack arraylist |
| Config saving | done | `config/wurst0/*.json` (settings + keybinds) |
| Keybinds | done | `.bind <hack> <key>`, ClickGUI on Right Shift |

### Implemented hacks (v0.1)

| Hack | Category | Notes / limitations |
|---|---|---|
| Flight | MOVEMENT | Simple mode (velocity flight); no PacketFly yet |
| SpeedHack | MOVEMENT | Simple ground-multiply mode; server movement checks apply |
| AutoSprint | MOVEMENT | full |
| Step | MOVEMENT | height slider |
| NoFall | MOVEMENT | client-side fallDistance reset only; packet version planned |
| Timer | TOOLS | via `mc.timer.tickLength` |
| Fullbright | RENDER | Gamma + NightVision methods |
| PlayerESP | RENDER | box style only for now |
| MobESP | RENDER | box style only for now |
| ChestESP | RENDER | chests, ender chests, shulker boxes |
| NameTags | RENDER | name + health billboard |
| X-Ray | RENDER | **ESP mode** (highlights ores); true block-hiding needs a Block mixin, planned |
| Nuker | BLOCKS | closest-block mode via `onPlayerDamageBlock` |
| FastPlace | BLOCKS | via `blockHitDelay = 0` |
| AutoTool | BLOCKS | hotbar swap by block efficiency |

## Roadmap (priority order chosen by apikun)

1. **Hooks/mixins phase** - add SpongeMixin (Mixin 0.7.x) to the build:
   - `C03PacketPlayer` / `PlayerNetworkHook` for real NoFall, Speed (stride), Scaffold
   - `Block.isOpaqueCube` for true X-Ray block hiding
   - `EntityPlayerSP` / `PlayerControllerMP` hooks for Freecam, reach, KillAura
2. **Render priority hacks** - BarrierESP, BedESP, ItemESP, SpawnerESP, PortalESP,
   Search-style block ESP, SkyBuildESP, TunnelHoleStairESP, WardenESP (mostly
   tile-entity/block scans reusing RenderUtils)
3. **World/Player priority** - AreaNuker, NukerLegit, SpeedNuker, Excavator,
   AutoFarm, FastBreak, AutoEat, AutoMine, BuildRandom, FastPlace upgrades
4. **Combat** - KillAura, Velocity, Criticals, AimAssist, AutoSoup/AutoWeapon,
   FightBot, TriggerBot
5. **Movement** - Flight modes (PacketFly/Vanilla), Jesus, Scaffold, LongJump,
   NoSlowdown, ElytraFlight (rewrite for 1.12.2 gliding)
6. **Chat/Intel/Items/Fun/Other** - rest of the 302 stubs
7. Port `other_features` (Wurst-level features like TooManyHax, toomanyhax-style
   protections) after hacks

## Build

**Toolchain that is proven to work (2026-10-30):**

| Piece | Version | Why |
|---|---|---|
| Build system | RetroFuturaGradle 2.0.2 | FG 2.3 is dead: its `userdev` artifact was deleted from the new Forge maven |
| Gradle | **9.8.2** | RFG 2.0.2 ships Java 25 bytecode; Gradle 9.x can load it |
| Gradle runtime JVM | Temurin **25** | RFG 2.0.2 classes are Java 25 (class file 69) |
| Compile toolchain | Temurin **8** | 1.12.2 mods must be Java 8 |
| MCP mappings | stable_39 | classic 1.12.2 names |

```bash
export JAVA_HOME=/path/to/jdk-25        # runs Gradle
gradle build                            # compiles with the Java 8 toolchain
gradle runClient                        # dev client
```

`gradle.properties` points `org.gradle.java.installations.paths` at the local
JDK 8 - adjust for your machine.

**Sandbox-specific notes (not needed on a normal machine):**
- `settings.gradle` replicates RFG's repositories with PREFER_SETTINGS and
  replaces Maven Central with mirrors (`maven-central.storage-download.googleapis.com`,
  Aliyun) because shared CI egress IPs get HTTP 429 from Central. Delete the
  `dependencyResolutionManagement` block if you don't need it.
- `devtools/patch_rfg_heap.py` republishes RFG 2.0.2 with the FernFlower
  decompiler worker heap reduced 3072M -> 2048M so a 4GB CI sandbox can run
  the (one-time) decompile. On a machine with >= 8GB RAM you can skip it and
  use the stock RFG from the GTNH maven.
- A full clean build takes ~10 min (one-time decompile/recompile of Minecraft
  itself is cached afterwards in `~/.gradle` or the `-g` cache dir).

## Sandbox / environment notes

- Network to `maven.minecraftforge.net`, `launchermeta.mojang.com`,
  `services.gradle.org`, `repo1.maven.org` confirmed reachable (2026-10-30).
- Workspace resets wipe `/home/z/my-project/modport` - if both repos are gone,
  re-clone from GitHub and re-checkout `mc1.12.2` (this file is also pushed to
  the branch, so the branch history is the source of truth).

## Porting conventions

- Keep everything **Java 8** (no `var`, no records, no switch expressions).
- Forge events + reflection first; mixins only when unavoidable.
- Wurst7-CevAPI class names are preserved (e.g. `SpeedHackHack`), so the
  stub-to-port mapping is 1:1 with `net/wurstclient/hacks/*.java`.
- Hack metadata (name/category) is parsed from the original by
  `scripts/generate_stubs.py`; rerun it after upstream changes.
