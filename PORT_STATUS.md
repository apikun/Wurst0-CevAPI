# Wurst0 - 1.12.2 Port Status

Port of [Wurst7-CevAPI](https://github.com/cev-api/Wurst7-CevAPI)
(MC 26.3 / Fabric / Java 25) to **Minecraft 1.12.2 / Forge / Java 8**.

- **Branch:** `mc1.12.2` on [apikun/Wurst0-CevAPI](https://github.com/apikun/Wurst0-CevAPI)
- **Mod id:** `wurst0`  **Package:** `net.wurst0`  **Version:** 0.2.0
- **License:** GPL-3.0-or-later (unchanged - this is a derivative of Wurst7)
- **Target:** Forge 1.12.2-14.23.5.2859, MCP `stable_39` mappings, RetroFuturaGradle 2.0.2 + Gradle 9.8.2

---

## Current state (updated 2026-10-30, v0.2.0 BUILD PASSING + RELEASED)

`gradle build` produces `build/libs/Wurst0-0.2.0.jar` (reobfuscated, drop into
`.minecraft/mods` with Forge 1.12.2) plus a `-dev` jar for development.
**v0.2.0 is released on GitHub** with the jar attached (tag `v0.2.0`).

| Item | Count | Status |
|---|---|---|
| Hacks registered | 317 | all appear in ClickGUI |
| Hacks fully implemented | 52 | working logic |
| Hacks stubbed | 265 | compile + register, no logic yet (`[not ported yet]` in tooltip) |
| Commands | 5 | `.help` `.t` `.bind` `.binds` `.say` |
| ClickGUI | done | RShift, draggable windows, checkboxes/sliders/modes |
| HUD | done | watermark + enabled-hack arraylist |
| Config saving | done | `config/wurst0/*.json` (settings + keybinds) |
| Keybinds | done | `.bind <hack> <key>`, ClickGUI on Right Shift |

### Implemented hacks (v0.1, 15)

| Hack | Category | Notes / limitations |
|---|---|---|
| Flight | MOVEMENT | Simple mode (velocity flight); no PacketFly yet |
| SpeedHack | MOVEMENT | Simple ground-multiply mode; server movement checks apply |
| AutoSprint | MOVEMENT | full |
| Step | MOVEMENT | height slider |
| NoFall | MOVEMENT | see v0.2 - now has Packet mode |
| Timer | TOOLS | via `mc.timer.tickLength` |
| Fullbright | RENDER | Gamma + NightVision methods |
| PlayerESP | RENDER | see v0.2 - now has tracers styles |
| MobESP | RENDER | see v0.2 - now has tracers styles |
| ChestESP | RENDER | chests, ender chests, shulker boxes |
| NameTags | RENDER | name + health billboard |
| X-Ray | RENDER | **ESP mode** (highlights ores); true block-hiding needs a Block mixin, planned |
| Nuker | BLOCKS | closest-block mode via `onPlayerDamageBlock` |
| FastPlace | BLOCKS | via `blockHitDelay = 0` |
| AutoTool | BLOCKS | hotbar swap by block efficiency |

### Implemented hacks (v0.2, +37)

| Hack | Category | Notes / limitations |
|---|---|---|
| Killaura | COMBAT | range/speed/priority(Distance,Angle,Health)/FOV, rotate, swing, player+passive filters, LOS check, target box render |
| KillauraLegit | COMBAT | view-angle limited, smooth rotation, LOS enforced |
| Criticals | COMBAT | Packet (C03 faked jump) / MiniJump / FullJump; triggers on attack-key edge with entity in crosshair |
| AntiKnockback | COMBAT | (= Velocity) hurtTime==9 detection; Normal cancel + Reduced % mode; no packet hooks yet |
| AimAssist | COMBAT | smooth rotation to nearest valid target, only-while-attacking option |
| AutoSoup | COMBAT | health threshold, hotbar stew, private `rightClickMouse` via reflection |
| TriggerBot | COMBAT | crosshair entity, attack-speed cooldown |
| AutoSword | COMBAT | attack-damage attribute compare across hotbar |
| NoFall (v2) | MOVEMENT | **Packet mode** (sends C03 onGround=true shortly before impact) + Client mode |
| Jesus | MOVEMENT | surface walking via motionY control |
| ScaffoldWalk | BLOCKS | places block under feet, auto-switch, needs support block below |
| BunnyHop | MOVEMENT | auto jump w/ sprint boost |
| HighJump | MOVEMENT | jump height slider |
| AntiVoid | MOVEMENT | teleports back to last ground pos below Trigger Y, sends C04 |
| FastLadder | MOVEMENT | ladder/vine climb speed |
| AirWalk | MOVEMENT | mid-air jumps (limited count), Wurst7-style AirWalk needs a mixin |
| FastBreak | BLOCKS | adds mining progress + clears blockHitDelay via dual-name reflection (MCP in dev, SRG in prod) |
| AutoEat | ITEMS | food-level threshold, bad-food filter, restores previous slot |
| AutoFarm | BLOCKS | wheat/carrots/potatoes/beetroots (age 7) + nether wart (age 3) |
| AutoWalk | MOVEMENT | movementInput override, pauses in GUIs |
| AutoMine | BLOCKS | mines crosshair block continuously |
| BuildRandom | BLOCKS | random placement w/ support-face detection, auto-switch |
| AreaNuker | BLOCKS | instant-breaks up to N blocks/tick via reflection (packet-level sequential, not simultaneous) |
| NukerLegit | BLOCKS | LOS-verified closest block only |
| SpeedNuker | BLOCKS | Nuker + speed slider (extra mining progress) |
| AntiAfk | OTHER | interval-based rotation nudge + swing |
| AutoRespawn | COMBAT | respawns on GuiGameOver |
| ItemESP | RENDER | dropped items |
| SpawnerESP | RENDER | mob spawner tile entities |
| PortalESP | RENDER | nether + end portal blocks (cached scan every 40 ticks) |
| BedESP | RENDER | beds (cached block scan) |
| BarrierESP | RENDER | barrier blocks (cached block scan) |
| Breadcrumbs | INTEL | line-strip trail, capped length |
| Trajectories | RENDER | arrow/pearl/egg/snowball/potion/xp-bottle arcs + landing marker |
| NoWeather | RENDER | rain/thunder strength zeroed per tick |
| AntiFov | RENDER | pins fovSetting |
| PlayerESP/MobESP (v2) | RENDER | new Style setting: Box / Lines / Both (tracers) |

### New infrastructure in v0.2

- `net.wurst0.util.ReflectUtils` - field AND method lookup by **MCP name or SRG
  name** (works in both dev and the reobfuscated jar), cached.
- Build now survives shared-IP Maven Central 429s:
  `settings.gradle` adds `file:///home/z/my-project/tools/localmaven` as
  `LocalFiller` (first repo) holding hand-built `scala-xml_2.11`,
  `scala-parser-combinators_2.11`, `scala-swing_2.11`, `lzma` modules
  (jars from the Forge maven + minimal POMs).
- RFG 2.0.2 is republished locally with the FernFlower heap at **1536M**
  (see below) - a complete POM (`retrofuturagradle-2.0.2.pom` next to the jar
  in localmaven) is REQUIRED, otherwise Gradle silently falls back to the
  unpatched GTNH jar and the decompile worker OOMs on 4GB sandboxes.

## Roadmap (priority order chosen by apikun)

1. **Hooks/mixins phase** - add SpongeMixin (Mixin 0.7.x) to the build:
   - `C03PacketPlayer` / `PlayerNetworkHook` for packet-level Velocity, real
     Speed (stride), Scaffold, Freecam
   - `Block.isOpaqueCube` for true X-Ray block hiding
   - `EntityPlayerSP` / `PlayerControllerMP` hooks for reach, simultaneous
     multi-block breaking (real AreaNuker)
2. **Combat leftovers** - BowAimbot, CrystalAura-lite (no crystals in 1.12.2
   PvP - skip or adapt), FightBot, TriggerBot upgrades, TpAura
3. **Render priority leftovers** - Search/MobSearch block scans, SkyBuildESP,
   TunnelHoleStairESP, WardenESP-style, RadarHack, NewChunks
4. **World/Player leftovers** - Excavator, AutoFish, AutoArmor, AutoSteal,
   InventorySorter, Tunneller, TreeBot-lite
5. **Movement leftovers** - Flight PacketFly mode, LongJump, NoSlowdown,
   ElytraFlight (rewrite for 1.12.2 gliding), SafeWalk (needs mixin or
   clipAtLedge reflection)
6. **Chat/Intel/Items/Fun/Other** - rest of the 265 stubs (chat hacks need a
   ClientChatReceivedEvent bridge in ForgeEvents first)
7. Port `other_features` (Wurst-level features like TooManyHax protections)

## Build

**Toolchain that is proven to work (2026-10-30, v0.2.0):**

| Piece | Version | Why |
|---|---|---|
| Build system | RetroFuturaGradle 2.0.2 (patched jar in tools/localmaven) | FG 2.3 is dead: its `userdev` artifact was deleted from the new Forge maven |
| Gradle | **9.8.2** | RFG 2.0.2 ships Java 25 bytecode; Gradle 9.x can load it |
| Gradle runtime JVM | Temurin **25** (`JAVA_HOME` for the build) | RFG 2.0.2 classes are Java 25 (class file 69) |
| Compile toolchain | Temurin **8** | 1.12.2 mods must be Java 8 |
| MCP mappings | stable_39 | classic 1.12.2 names |

```bash
export JAVA_HOME=/path/to/jdk-25        # runs Gradle
gradle build                            # compiles with the Java 8 toolchain
gradle runClient                        # dev client
```

`gradle.properties` points `org.gradle.java.installations.paths` at the local
JDK 8 and caps the Gradle JVM at `-Xmx512M` (4GB sandbox).

**Sandbox-specific notes (not needed on a normal machine):**
- `settings.gradle` replicates RFG's repositories with PREFER_SETTINGS,
  prepends the `LocalFiller` file repo (scala+lzma artifacts for Central-429
  workarounds) and mirrors Central (Google storage mirror + Aliyun). Delete
  the `dependencyResolutionManagement` block if you don't need it.
- `devtools/patch_rfg_heap.py` republishes RFG 2.0.2 with the FernFlower
  decompiler worker heap reduced **3072M -> 1536M** (4GB sandbox: 2048M still
  OOMs when combined with the 512M Gradle JVM; 1536M works). The local maven
  needs the real `retrofuturagradle-2.0.2.pom` beside the patched jar or
  Gradle silently uses the unpatched GTNH jar. On a machine with >= 8GB RAM
  skip all of this and use stock RFG.
- A full clean build takes ~10 min (one-time decompile/recompile of Minecraft
  is cached in the `-g` gradle home; on a reset sandbox it re-runs).

## Release process (used for v0.2.0)

1. Bump `version` in `build.gradle` (and `mcmod.info` is expanded from it).
2. `gradle build` -> verify `build/libs/Wurst0-<v>.jar`.
3. Commit + push `mc1.12.2`, tag `v<v>`, push tag.
4. Create the GitHub release via API (token from the origin remote URL -
   never store it in files) and upload the jar via the uploads API.
   `scripts/release_v020.sh` in the sandbox holds the exact commands.

## Sandbox / environment notes

- `repo1.maven.org` 429s aggressively from shared CI egress IPs; even the
  Google/Aliyun mirrors can miss artifacts (404 for scala/lzma). The Forge
  maven and Sponge maven carry the needed jars.
- Workspace resets wipe `/home/z/my-project/modport` AND the toolchain in
  `tools/`. Re-clone both repos, re-download Temurin 8 + Temurin 25 +
  Gradle 9.8.2, re-run `devtools/patch_rfg_heap.py`, re-create the
  LocalFiller artifacts (see `scripts/setup_tools.sh` +
  `scripts/fetch_central.sh` history). The branch history is the source of
  truth for everything else.

## Porting conventions

- Keep everything **Java 8** (no `var`, no records, no switch expressions).
- Forge events + reflection first; mixins only when unavoidable.
- Wurst7-CevAPI class names are preserved (e.g. `SpeedHackHack`), so the
  stub-to-port mapping is 1:1 with `net/wurstclient/hacks/*.java`.
- **MCP stable_39 gotchas learned in v0.2:**
  - `CPacketPlayer` (not C03PacketPlayer); inner classes are `Position`,
    `Rotation`, `PositionRotation`.
  - `Vec3d` fields are `x/y/z` (not xCoord...); no `lengthVector()` - use
    `Math.sqrt`.
  - `EnumFacing.getXOffset/getYOffset/getZOffset` (not getFrontOffsetX...).
  - `EntityLivingBase.isHandActive()` (not isItemInUse()).
  - `Minecraft.rightClickMouse()` is PRIVATE - call via ReflectUtils.method
    (MCP `rightClickMouse` / SRG `func_71007_j`).
  - `Hacks` categories: there is NO `PLAYER` category - AutoEat is ITEMS,
    AutoRespawn stays COMBAT (upstream mapping).
- Hack metadata (name/category) is parsed from the original by
  `scripts/generate_stubs.py`; rerun it after upstream changes.
