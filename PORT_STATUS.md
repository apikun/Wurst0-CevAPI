# Wurst0 - 1.12.2 Port Status

Port of [Wurst7-CevAPI](https://github.com/cev-api/Wurst7-CevAPI)
(MC 26.3 / Fabric / Java 25) to **Minecraft 1.12.2 / Forge / Java 8**.

- **Branch:** `mc1.12.2` on [apikun/Wurst0-CevAPI](https://github.com/apikun/Wurst0-CevAPI)
- **Mod id:** `wurst0`  **Package:** `net.wurst0`  **Version:** 0.5.0
- **License:** GPL-3.0-or-later (unchanged - this is a derivative of Wurst7)
- **Target:** Forge 1.12.2-14.23.5.2859, MCP `stable_39` mappings, RetroFuturaGradle 2.0.2 + Gradle 9.8.2

---

## Current state (updated 2026-10-31, v0.5.0 BUILD PASSING + RELEASED)

`gradle build` produces `build/libs/Wurst0-0.5.0.jar` (reobfuscated, drop into
`.minecraft/mods` with Forge 1.12.2) plus a `-dev` jar for development.
**v0.5.0 is released on GitHub** with the jar attached (tag `v0.5.0`).

| Item | Count | Status |
|---|---|---|
| Hacks registered | 317 | all appear in ClickGUI |
| Hacks fully implemented | 123 | working logic |
| Hacks stubbed | 194 | compile + register, no logic yet (`[not ported yet]` in tooltip) |
| Commands | 12 | `.help` `.t` `.bind` `.binds` `.say` `.vclip` `.jump` `.gm` `.setmode` `.setslider` `.setcheckbox` `.features` |
| ClickGUI | done | small-screen adaptive: column layout, wheel scrolling, drag clamp, click-to-front, text fields |
| HUD | done | watermark + arraylist (capped on small screens) + rainbow option |
| Config saving | done | `config/wurst0/*.json` (settings incl. text fields + keybinds) |
| Keybinds | done | `.bind <hack> <key>`, ClickGUI on Right Shift |

### UI small-screen overhaul (v0.5)

- **Column layout**: windows no longer cascade diagonally off-screen; on
  every GUI open / resize they are distributed into columns that fit the
  screen. Windows the user dragged keep their position (clamped into view).
- **Window scrolling**: content taller than the screen is scrollable with
  the mouse wheel (scissor-clipped, with a scrollbar). Previously the
  overflow of big categories (Movement has ~30 hacks) was unreachable.
- **Adaptive window width** on very small screens; drag is clamped so a
  window can never be lost off-screen.
- **Click-to-front** + only the top-most window under the mouse receives a
  click (overlapping windows no longer double-toggle).
- **Fixed latent bugs**: clicking a settings row toggled the hack instead
  of the setting; the slider drag hit-test started from the wrong origin
  (only the first feature's sliders could be dragged).
- **HUD**: arraylist is capped to the screen height (`+N more`), so many
  enabled hacks can't push it off-screen.

### New infrastructure in v0.5

- **Settings phase started**: new `TextFieldSetting` + editable text field
  widget in the ClickGUI (click row, type, Enter/Esc; Esc while editing
  doesn't close the GUI). Persisted in settings.json. Unblocks ChatSpam
  (now free-text) and future CommandSpam/AutoChat.
- `RainbowUi` hooks: ClickGUI title bars + HUD watermark shift hue while
  it is enabled.

### Implemented hacks (v0.5, +12)

| Hack | Category | Notes / limitations |
|---|---|---|
| ShearAura | OTHER | sheep + mooshrooms, auto-switches to hotbar shears; right-click via vanilla IShearable path |
| BonemealAura | BLOCKS | hold bonemeal; IGrowable scan + artificial raytrace (same as Tillaura); no MultiMeal option yet |
| Follow | MOVEMENT | nearest player, keep-distance slider, rotate+walk, jumps when blocked; pauses in GUIs |
| MobHealth | RENDER | crosshair entity `Name: HP / Max` under the crosshair; hostile-only filter |
| SignEsp | RENDER | cached scan (40 ticks) of standing + wall signs, like PortalESP |
| LogoutSpots | INTEL | boxes + tracers where players left (max 20/session); no name labels yet (no world-space text) |
| Kaboom | BLOCKS | START/ABORT dig-packet bursts; Power 1-32; effects toggle gates the effect-triggering packet |
| NoWeb | MOVEMENT | approximate: undoes the web's 0.25 motion multiplier (capped); server checks apply |
| Derp | FUN | random head rotation each tick |
| AntiWobble | RENDER | removes nausea client-side each tick (same mechanism as AntiBlind) |
| InfiniChat | CHAT | raises the chat input limit to 2000 via reflection (`inputField`/`field_146415_a` verified) |
| RainbowUi | FUN | rainbow ClickGUI title bars + HUD watermark |
| ChatSpam (upgrade) | CHAT | preset messages replaced by a free-text TextFieldSetting |

### Implemented hacks (v0.4, +34)

| Hack | Category | Notes / limitations |
|---|---|---|
| Freecam | RENDER | fake EntityOtherPlayerMP + setRenderViewEntity; real player frozen via no-op MovementInput swap; WASD+jump/sneak flight, mouse look |
| NoHurtcam | RENDER | zeroes hurtTime/maxHurtTime each tick (hurtCameraEffect skips when maxHurtTime==0) |
| NoVignette | RENDER | cancels ElementType.VIGNETTE via new OverlayEvent bridge |
| NoOverlay | RENDER | pumpkin (HELMET) + portal (PORTAL) overlays; fire/water not separate element types in 1.12.2 |
| NoFog | RENDER | FogDensity bridge sets density 0; removes liquid fog only (linear distance fog needs mixin) |
| NoPlayerChat | CHAT | cancels all incoming chat; "Allow own messages" heuristically matches last sent text |
| NameProtect | CHAT | case-insensitive replace of own name in received chat with "Player" |
| ProjectileESP | RENDER | boxes around arrows/pearls/potions/snowballs/eggs/xp bottles/fish hooks, per-type colors |
| PearlESP | RENDER | ender pearl boxes |
| TrueSight | RENDER | partial: invisible living entities as cyan boxes (barrier vision needs mixin) |
| NoShieldOverlay | RENDER | cancels RenderSpecificHandEvent when a shield is in either hand |
| RedstoneESP | RENDER | cached scan of 18 redstone component block types |
| DurabilityHUD | RENDER | top-left armor + hand durability with color coding (green/yellow/red) |
| AutoTotem | COMBAT | 3-click offhand swap (slot 45) below health threshold, works GUI-closed |
| AutoClicker | OTHER | left/right click via shared ReflectCalls (MCP+SRG dual-name reflection) |
| AntiBlast | OTHER | removes player from ExplosionEvent.Detonate affected entities (damage+knockback immunity in range) |
| AntiHunger | MOVEMENT | approximate: extra C03 onGround=true packet when airborne; sprint-packet suppression needs mixin |
| NoLevitation | MOVEMENT | removes client copy of levitation effect each tick (motion is client-simulated) |
| Dolphin | MOVEMENT | water motion boost |
| AntiWaterPush | MOVEMENT | approximate: zeroes flow push while standing still in water; optional surface-keep |
| BoatFly | MOVEMENT | jump/sneak vertical control of ridden boat, hover by default |
| ElytraFlight | MOVEMENT | Normal/Hover modes; look-direction flight, fall distance cleared (client-simulated physics in 1.12.2) |
| AntiDrop | ITEMS | ForgeEvents cancels ItemTossEvent (also suppresses the C07 packet) |
| Restock | ITEMS | refills low hotbar stacks via QUICK_MOVE from main inventory |
| DuraSwap | ITEMS | swaps nearly-broken tools for a better hotbar twin; self-disables when nothing left |
| Throw | FUN | drops held stack repeatedly (dropItem(true)) |
| AutoLeave | OTHER | disconnects below health threshold, back to main menu |
| FeedAura | OTHER | right-clicks nearest feedable non-love-mode animal with its breeding item (isBreedingItem check) |
| Tillaura | BLOCKS | hoes dirt/grass via processRightClickBlock with artificial raytrace (no aiming needed) |
| FancyChat | CHAT | fullwidth unicode rewrite of outgoing chat via ChatOutputEvent |
| Panic | OTHER | disables every enabled hack, then itself |
| GlobalToggle | OTHER | keybind-driven Panic (`.bind GlobalToggle <key>`) |
| HideWurst | OTHER | HUD watermark + arraylist hidden while enabled |
| ClickGUI | OTHER | opens the ClickGUI (bindable key, Right Shift still hardcoded) |

### New infrastructure in v0.4

- Two new Wurst events: `OverlayEvent` (RenderGameOverlayEvent.Pre bridge,
  carries the ElementType name) and `FogEvent` (EntityViewRenderEvent.FogDensity
  bridge, cancelled => custom density).
- ForgeEvents also bridges `ItemTossEvent` (AntiDrop gate) and
  `ExplosionEvent.Detonate` (AntiBlast) directly, since both need the Forge
  event object itself and don't warrant a Wurst event class.
- `net.wurst0.util.ReflectCalls` - shared cached leftClickMouse/rightClickMouse
  reflection (MCP `leftClickMouse`/SRG `func_147116_af`,
  MCP `rightClickMouse`/SRG `func_147121_ag`); AutoClicker uses it.
- Seven new commands; `.setmode`/`.setslider`/`.setcheckbox` share the
  feature/setting lookup helpers in SetModeCmd and work on hacks AND commands.
- 1.12.2 MCP gotchas found this round: `Blocks.NOTEBLOCK` (one word),
  repeaters/comparators only exist as UNPOWERED_/POWERED_ pairs,
  EntityEgg/EntityPotion/EntitySnowball live in `entity.projectile` (not item),
  `Explosion.getPosition()` (no getExploder in MCP 1.12.2),
  `Util.milliTime()` does not exist (use System.currentTimeMillis),
  MovementInput is a plain non-abstract class (substitutable for input
  freezing).

### Implemented hacks (v0.3, +27)

| Hack | Category | Notes / limitations |
|---|---|---|
| BowAimbot | COMBAT | aims while bow drawn, target leading + drop compensation (approximate) |
| FightBot | COMBAT | rotates, walks to and attacks nearest target (Killaura filters) |
| TpAura | COMBAT | packet-level teleport around target via CPacketPlayer.Position + CPacketUseEntity(ATTACK) |
| MultiAura | COMBAT | up to N targets per cooldown, same filters as Killaura |
| AutoPotion | COMBAT | splash healing, hotbar or SWAP from inventory, rightClickMouse reflection |
| AutoArmor | COMBAT | best-armor scan (material + prot enchant), 3-click swap state machine |
| AntiCactus | BLOCKS | cancels DamageSource.CACTUS via new DamageEvent (LivingHurtEvent bridge) |
| HandNoClip | BLOCKS | hides hand when eye is inside an opaque block (RenderSpecificHandEvent bridge) |
| Excavator | BLOCKS | AreaNuker mechanism, digs top-down, per-tick limit |
| Search | RENDER | cached scan, 8 ore filters, per-ore colors |
| MobSearch | RENDER | mob spawners + silverfish blocks |
| Radar | RENDER | screen-space minimap (new ScreenRenderEvent), yaw-rotated dots, category colors |
| AntiBlind | RENDER | removes blindness/nausea/mining fatigue client-side (server can re-apply) |
| NewChunks | INTEL | marks chunks first loaded this session via ChunkEvent.Load bridge |
| NoSlowdown | MOVEMENT | approximate: compensates item-use slowdown with motion boost, capped at sprint speed |
| Glide | MOVEMENT | fall speed slider |
| Spider | MOVEMENT | climbs walls on horizontal collision |
| Jetpack | MOVEMENT | hold jump to fly up |
| AutoSwim | MOVEMENT | swims up in water |
| Parkour | MOVEMENT | jumps at block edges (motionY = 0.42, jump() is protected) |
| AutoFish | OTHER | bite detection via caughtEntity reflection (SRG field_146043_c verified) |
| AutoReconnect | OTHER | rejoins last server after configurable delay via GuiConnecting |
| AutoSteal | ITEMS | QUICK_MOVE loop on GuiChest, closes when empty |
| AutoDrop | ITEMS | junk filter (6 toggles), THROW via windowClick |
| ChatSpam | CHAT | preset messages + interval + counter (free-text setting still missing) |
| MassTpa | CHAT | queues /tpa (or /tpahere) for every tab-list player, auto-disables |
| AntiSpam | CHAT | collapses consecutive repeats into `(xN)` via ChatInputEvent |

### New infrastructure in v0.3

- Six new Wurst events bridged from Forge: `ChatInputEvent`
  (ClientChatReceivedEvent, mutable/cancellable), `ChatOutputEvent`
  (ClientChatEvent), `DamageEvent` (LivingHurtEvent), `ScreenRenderEvent`
  (after HUD), `ChunkLoadEvent` (ChunkEvent.Load), `HandRenderEvent`
  (RenderSpecificHandEvent). Listener interfaces + EventManager overloads
  follow the existing hand-rolled dispatcher pattern.
- **Bugfix:** `rightClickMouse` reflection used a wrong SRG name
  (`func_71007_j` does not exist in 1.12.2; correct is `func_147121_ag`),
  so AutoSoup/AutoEat silently did nothing in production jars. All four
  call sites fixed; verified against `mcp_stable-39` methods.csv.
- MCP names verified against the actual decompiled sources when in doubt
  (`build/rfg/srg_patched_minecraft-sources.jar` + fields.csv reverse
  lookup). Gotchas found this way: `PotionTypes` is in `net.minecraft.init`,
  `EntityEquipmentSlot` in `net.minecraft.inventory`, `GuiConnecting` in
  `net.minecraft.client.multiplayer`, `DamageSource.CACTUS` (upper case),
  `Chunk.x/z` (not xPosition), `EntityLivingBase.jump()` is protected.

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

1. **Hooks/mixins phase** - add SpongeMixin (Mixin 0.7.x) to the build
   (STILL not started - highest priority for next session):
   - `C03PacketPlayer` / `PlayerNetworkHook` for packet-level Velocity, real
     Speed (stride), packet-cancel for AntiHunger (sprint suppression),
     Flight PacketFly
   - `Block.isOpaqueCube` for true X-Ray block hiding
   - `EntityPlayerSP` / `PlayerControllerMP` hooks for reach, simultaneous
     multi-block breaking (real AreaNuker), Sneak without slowdown, NoWeb
     (real), XCarry, fire/water overlay removal (NoOverlay completeness),
     true NoFog (linear fog)
   - `GuiScreen.drawDefaultBackground` for NoBackground (1.12.2 Forge's
     `BackgroundDrawnEvent` fires AFTER the background is drawn, so it
     cannot be cancelled - confirmed this session)
2. **Settings phase (continued)** - FileSetting (unblocks ChatSpam
   use-file + CommandSpam), ButtonSetting; rest of the ClickGUI polish
3. **World/Player leftovers** - InventorySorter, Tunneller, TreeBot-lite,
   AutoSwitch, Waypoints (needs commands + persistence),
   Excavator polish
4. **Render leftovers** - BaseFinder, MobSpawnESP, HealthTags,
   MobOwners, CameraDistance, world-space text (unblocks SignESP labels,
   LogoutSpots names, Waypoints)
5. **Combat leftovers** - CrystalAura-lite (no crystals in 1.12.2 PvP - skip
   or adapt), TriggerBot upgrades, AntiProjectile
6. **Chat/Intel/Items/Fun/Other** - rest of the ~194 stubs
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

## Release process (used for v0.2.0-v0.5.0)

1. Bump `version` in `build.gradle` (and `mcmod.info` is expanded from it).
2. `gradle build` -> verify `build/libs/Wurst0-<v>.jar`.
3. Commit + push `mc1.12.2`, tag `v<v>`, push tag.
4. Create the GitHub release via API (token from the environment -
   never store it in files) and upload the jar via the uploads API.
   See `devtools/release_v050.sh`.

## Blockers

- None. (The v0.4.0 push/release blocker from 2026-10-31 was resolved the
  same day with a fresh PAT; v0.4.0 and v0.5.0 are both released.)
  Reminder: every PAT that appeared in chat must be revoked and
  regenerated at https://github.com/settings/tokens after use.

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
