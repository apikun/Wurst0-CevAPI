---

## ClickGUI fully stable between sessions (v0.9.3)

Fix for "the UI keeps changing even though I didn't do anything". Three
root causes found in the v0.9.2 persistence:

1. **`bringToFront()` mutated the layout list.** Every click on a window
   (toggling a hack, moving a slider) moved it to the end of
   `ClickGuiManager.windows`, and `setScreenSize()` fills its auto columns
   in list order - so the next GUI open laid the windows out in a different
   order. Now `bringToFront` only reorders a separate `zOrder` list used
   for rendering/click picking; layout always iterates the stable
   category-order list.
2. **Only dragged windows were persisted.** Windows the user merely
   clicked (not dragged) kept re-flowing on every open. `saveLayout()`
   now saves **every** window's position to `clickgui.json`, so the auto
   column layout only ever runs on the very first launch (no JSON yet).
3. **Expanded/collapsed state was session-only.** Panel heights changed
   after every restart, shifting the auto layout again. The expanded map
   is now saved per category inside `clickgui.json` (backward compatible
   with the v0.9.2 format) and restored on startup
   (`FileManager.loadClickGuiExpanded()`).

Result: after one close of the ClickGUI, the layout - window positions,
z-stacking and open sliders - is byte-stable across restarts. Windows are
still clamped into view on resize/GUI-scale changes.

## Keybinds GUI + persistent layout (v0.9.2)

- **KeybindsScreen** (`clickgui/KeybindsScreen.java`): GUI keybind editor
  in the dark/crimson theme. Lists every hack + current key, click a row
  then press a key to bind (Escape clears), saves instantly to
  keybinds.json. Opened via the **Keybinds button** (top-left of the
  ClickGUI) or **`.binds gui`**.
- **ClickGUI layout persistence**: dragged window positions are saved to
  `config/wurst0/clickgui.json` when the GUI closes and restored on every
  open and game restart. The auto column layout now only positions
  windows the user never moved - the GUI no longer re-arranges itself.
- `.binds gui` subcommand opens the editor directly.

## Grim bypass + Bedwars-safe aimbot (v0.9.1)

Built from a source audit of GrimAC's open-source check suite
(common/checks/impl: aim, combat, timer, packetorder, misc, badpackets,
multiactions). The fork-specific findings and what was implemented:

**Grim check audit -> implementation mapping:**

| Grim check (source) | What it catches | v0.9.1 countermeasure |
|---|---|---|
| TransactionOrder / floating transactions | held transaction answers | PacketTap **vital passthrough**: CPacketConfirmTransaction + CPacketKeepAlive are never held/delayed, even while Blink/FakeLag hold everything else |
| Post ("sent packets out of the expected movement tick order") | ATTACK / USE_ITEM / DIG / CLICK_WINDOW / ABILITIES / HELD_ITEM / ENTITY_ACTION sent between a flying packet and the next transaction response | PacketTap **post-safe ordering**: post-suspect packets are queued and released right after a transaction response (or right before the next flying packet) - exactly the vanilla slot |
| BadPacketsR ("stopped sending position updates while still responding to transactions") | long blinks with passing transactions | PacketTap **hold cap**: cyclic auto-flush every N ms (default 600) while holding |
| Reach ("attacked an entity from too far away", cancels impossible hits) | attacks beyond 3.0 to the interpolated hitbox | **Reach shield**: GrimBypass.capAttackRange() clamps combat range to 2.95; Killaura measures eye -> closest hitbox point, the same surface distance Grim measures |
| PacketOrderB ("did not swing for attack") | 1.12 clients must swing (1.9 order: attack then animation, but animation must exist or the hit is cancelled) | Killaura/KillauraLegit force the swing when GrimBypass is active |
| AimModulo360 ("sent a large yaw snap") | yaw delta > 320 degrees | rotation smoothing clamp (max degrees/tick) |
| BadPacketsD ("invalid rotation pitch") | pitch outside [-90, 90] | pitch clamped in GrimBypass + all rotation code |
| AimDuplicateLook | duplicate rotation packets | vanilla 1.12 only sends look packets when the rotation changes; killaura converges in-place |
| Timer ("flags 1.01 on low ping extremely quickly") | timer imbalance vs real time | existing TimerHack grimCap retained, default on |

**GrimBypass v2 settings:** Transaction passthrough, Post-safe ordering,
Hold cap + ms, Reach shield, Smooth rotations + max rotation/tick, Cap
Timer speed, Idle packets, Alert on flag, Auto-disable on kick.

**Killaura v2 (the Bedwars-safe aimbot):**
- **Team check** (default on): never attacks players on your scoreboard
  team - Hypixel Bedwars teams are scoreboard teams. Fallback for
  teamless servers compares colored nametags (default white/gray ignored).
  Shared logic in `util/TeamUtils.java`.
- **Smooth aim** (default on) + Smoothness slider: eased proportional
  stepping (0.45 factor, capped per tick), attack only when converged
  within 30 degrees - looks human, no more instant snapping.
- **Smart priority** (new default): weighted distance 0.4 + view angle 0.4
  + missing health 0.2.
- **Hitbox-aware range**: eye -> closest hitbox point instead of center
  distance; aims at the closest hitbox point inside 3.0 (body center
  beyond).
- KillauraLegit: team check + reach shield + forced swing.
- AimAssist: fixed a port bug where it skipped players entirely (useless
  on Bedwars) - now targets players, with an "Ignore teammates" setting.

**Honest limits:** Speed/Flight/HighJump still get flagged - Grim
re-simulates movement from the packets, so no client-side trick defeats
the prediction engine. The bypass targets the *packet* surface (order,
transactions, reach, rotation, timer), which is what a 1.12.2 client can
actually control.

## Implemented hacks (v0.9, +72)

Registry corrections this round (important for counts):
- Removed 7 bogus registrations (upstream support classes the stub
  generator had wrongly added as hacks): AutoChatModelOverrideManager,
  AutoChatPromptManager, CustomTotemItemModel, CustomTotemModel,
  CustomTotemSkinFetcher, CustomTotemSpecialRenderer,
  ShulkerInventoryScreen. Their files are gone; CustomTotemHack carries
  its own implementation now.
- Added the 21 upstream ophack/ features that had never been stubbed
  (AirstrikePlus ... VoiderPlus). Registry now **332 = full upstream
  coverage + GrimBypassHack (new, fork-specific)**.
- Stubs dropped 84 -> 27. **305 of 332 hacks are functional.**

### New shared infrastructure in v0.9
- **PacketTap** (`util/PacketTap.java`): netty pipeline tap injected
  before "packet_handler"; holds/delays C2S packets (Blink, FakeLag,
  PacketDelay, PacketRate), drops inbound S2C packets (Backtrack,
  NbtFilter). Pure netty, no mixins. Wired into the client tick.
- **AltGuiScreen / NavigatorScreen** (`clickgui/`): standalone screens.
- **ClickGUI restyle**: dark/crimson "rusher" theme per the user's
  reference image - near-black panels, dark-red header bars, [count]
  badges, 1px crimson border, red enabled text / gray disabled text,
  red slider/checkbox accents, red scrollbar. All ScaledResolution-aware.
- **TimerHack fix**: was resolving SRG field_71467_ac (rightClickDelayTimer,
  an int) as the Timer instance in production builds -> would crash; now
  field_71428_T ("timer"). Also gained a Grim-safe speed cap.

### The +72 (all were stubs; notes = 1.12.2 adaptation)

Render/Fun: ProphuntEsp (pulsing red box on invisible entities), RemoteView
(client camera swap via setRenderViewEntity), Viewmodel (re-renders hands
with offset/rotation sliders via cancellable RenderSpecificHandEvent), Lsd
(hue-cycling overlay + FOV pulse), SkinDerp (random model-part toggles,
restored on disable), Tired (vignette overlay), CustomTotem (runtime atlas
swap: totem sprite replaced by local player face, reload-safe).

Packets: PacketDelay (C2S delay slider), PacketRate (inbound pps + held HUD),
FakeLag (hold with enemy-range + max-hold auto-flush), Blink (hold all C2S,
flush on disable, Limit auto-restart), Backtrack (drops SPacketPlayerPosLook
for N ticks after being hurt), NbtFilter (drops oversized S2C tile/slot
packets).

Movement/World: Fish (swim assist: +0.005 motionY in water), AutoFly
(altitude lock + vertical control), PearlDrop (pearl at fall threshold),
ChorusFruit (auto-eat on damage), BedrockEscape (air-pocket scan + Position
teleport), BedrockStash (budgeted chunk scan ESP for air pockets),
JoinDrop (drop shulkers/books/filter items on world join), RemoteEnderChest
(ec GUI without close packet, LWJGL hotkey), BeaconExploit (MC|Beacon
payload spoof), CrashChest (oversized-NBT boots via creative packet),
HideModMenu (hides Forge main-menu Mods button), KickForensics (disconnect
reasons -> config/wurst0/kicks.txt).

Combat: AttributeSwap (smart hotbar swap on attack), PearlIntercept
(vanilla physics solve + own pearl throw), ShieldSwing (swing while
blocking + auto offhand equip), Untouchable (teleport out of armed-player
reach), Towny (auto /t toggle pvp with confirm matching).

Automation: AutoLibrarian (best-book trade scan + optional auto-buy),
LootRunner (chest scan -> steer -> open -> QUICK_MOVE loot), AutoSwitch
(hotbar cycle per tick), LootSearch (all lockable container types, ESP +
HUD), SourceFill (fluid source fill from whitelist), AutoSpawnProofer
(spawnable-tile highlight + torch fill), TemplateTool (.t save/place/clear
block templates), MusicAura (note-block right-click cycling), SignFramePT
(right-click pass-through to block behind).

Intel/Chat: CheatDetector-style OppStats (per-server dossier JSON + HUD),
MiningEvidence (dropped-item trail detection + tracers), SimulationSonar
(chunk activity scoring + hotspot ESP), LivestreamDetector (per-player
YouTube/Twitch/TikTok/Kick live checks), ChatTranslator (google translate
endpoint via HttpURLConnection, needs internet), AutoComplete
(.complete <prefix> name/history matcher), ClientChatOverlay (fading
overlay panel), EnchantmentHandler (enchant summary HUD), UiUtils (slot
number overlay + GUI debug HUD).

OP/Creative (new batch, upstream ophack): AirstrikePlus (fireball
strikes), ArmorStandImages (reduced: single player-head stand),
AutoCommand (command queue loop), AutoDisplays + AutoTexts (adapted to
invisible marker armor stands with CustomNameVisible), AutoNames
(/scoreboard teams color/prefix), AutoScoreboard (sidebar objective),
AutoTitles (/title 1.12 JSON), BoomPlus (explosive overhead), ExplosionAura
(powered creepers), ForceOPBook + ForceOPSign (clickEvent run_command
books/signs; servers patched), ForceTP (owner-bound pearl summon),
HandOfGod (/fill lava + lightning), MultiverseAnnihilator (/mv delete +
timed /mv confirm), NBTEditor (raw SNBT -> creative packet), 
OPServerKillModule (admin command sequence), OPplayerTPmodule (tab-list
cycling /tp), SpawnRadius (/gamerule + spawner HUD), UUIDBan (template
bans from tab list), VoiderPlus (layer-per-tick /fill air sweeps).

New: GrimBypassHack (OTHER) - idle C03 packets, rotation snap clamp,
TimerHack cap, chat flag detection ("grim"/"anti-cheat"), optional
auto-disable of fast movement hacks, HUD flag timer. Best-effort; no
guarantee against current Grim versions.

### Still stubbed (27, with reasons)

- Version-gated MC content absent in 1.12.2: AnchorAura (respawn anchors
  1.16), AntiGeyser (26.2 geysers), AutoDisenchant (grindstone 1.14),
  AutoMace / MaceDmg / WindChargeKey (1.21 mace set), LocatorTriangulation
  (1.21.6 locator bar), MicDetect (Simple Voice Chat packets),
  SnowShoe (1.17 powder snow), SoulChalice (26.x custom item),
  StasisDetector (1.13 bubble columns), SusNoMore (1.20 archaeology),
  TrialSpawnerEsp (1.21), TridentEsp (1.13 trident), VaultRoll (1.21
  vault), VillagerDupe (1.20.3 shearing), VillagerRoll (1.20.2 re-roll),
  WardenEsp (1.19).
- Need mixins/hooks the Forge-event layer can't provide: CameraDistance
  + CameraNoClip (EntityRenderer const-inlined third-person distance),
  NoBackground (no pre-background hook), NoFireOverlay (no fire-overlay
  hook in 1.12.2 RenderGameOverlayEvent), TextureRotator (block-model
  internals), PotionSaver (needs per-packet outbound DROP; PacketTap
  only holds/delays).
- Upstream is a no-op / alt-relog tool: PortalGui (original is empty),
  OfflineSettings (needs AltManager/authlib infra).

---

## Implemented hacks (v0.8, +50)

All 50 were stubs before this round; the registry is unchanged
(317 hacks), stub count dropped 134 -> 84.

| Hack | Category | Notes / limitations |
|---|---|---|
| Overlay | RENDER | highlights the current dig target + progress bar on HUD; progress mirrors PlayerControllerMP.curBlockDamageMP via reflection (field_78770_f) with a hardness-based fallback |
| ChestSearch | RENDER | scans loaded TileEntityChest/ShulkerBox contents (server-synced) for a TextFieldSetting match, cyan boxes |
| PotEsp | RENDER | boxes around entities with active potion effects: red debuff, green buff, orange mixed |
| OpenWaterEsp | RENDER | highlights your fishing bobber + water source blocks around it (1.13 open-water concept adapted) |
| XpGui | RENDER | HUD line with XP level, progress % and total points |
| RenderAdjust | RENDER | render distance 1-64 and FOV 30-180 beyond the vanilla slider caps (direct GameSettings fields), restore-on-disable |
| TunnelHoleStairEsp | RENDER | red boxes for 3+ deep drop holes, green for 2-step stair patterns; scan every 30 ticks |
| NewerNewChunks | RENDER | red = chunks streamed in after enabling, green = pre-loaded (5s grace period); ChunkLoadListener driven |
| Mapa | RENDER | draws the held filled map on the HUD (MapData.colors -> MapColor quads, 2x downsample), size/opacity sliders |
| FunCreepers | FUN | colored smoke/angry/lava particles around creepers (client-side) |
| NecoMode | FUN | stepped cat ears on the HUD corners, scales with ScaledResolution |
| Flicker | FUN | gamma strobe (mostly dark, occasional flash); restores original gamma on disable |
| MileyCyrus | FUN | heart/note/happy-villager particles in a ring around the player |
| ElytraPitch | MOVEMENT | Lock or Clamp your pitch while elytra flying |
| ElytraDive | MOVEMENT | auto pitch-down + thrust along look vector with max-speed cap |
| ElytraBounce | MOVEMENT | pre-bounce via downward raytrace before the glide state ends, restores motionY |
| ElytraWalk | MOVEMENT | low-altitude hover (downward raytrace) + movement-key walking while gliding |
| EntityControl | MOVEMENT | steer any vehicle with WASD, jump/sneak for up/down, vehicle faces player yaw (client-side) |
| BoatPhase | MOVEMENT | boat.noClip = true + drive through walls; rubber-bands on strict servers |
| Teleport | MOVEMENT | periodic step teleports (Forward/Backward/Up/Down), step + delay sliders; "moved too quickly" caveat |
| SafeTp | MOVEMENT | Teleport that verifies the destination (solid ground, replaceable feet, air head, no lava/fire/cactus in 3x3) and warns locally instead |
| PearlLauncher | COMBAT | sneak-triggered ender pearl throw from hotbar with cooldown + slot restore (rightClickMouse reflection) |
| InfiniteReach | COMBAT | teleport-strike: position -> next to target -> attackEntity -> position back; range up to 96 |
| Outreach | BLOCKS | break/place beyond vanilla reach via custom raytrace driving clickBlock/onPlayerDamageBlock/processRightClickBlock |
| SpearAssist | COMBAT | auto-releases the bow at a target charge % (getItemInUseCount) |
| TargetPlace | BLOCKS | places a block under the crosshair entity (trap) |
| SuperInstaMine | BLOCKS | digs N blocks/tick in a cube around the crosshair block; consults Protect |
| AirPlace | BLOCKS | sends the placement packet at the look position without a support face |
| BonemealAura | BLOCKS | bone-meals every IGrowable in range, optional line-of-sight check, auto-switch |
| SilkOnly | BLOCKS | resetBlockRemoving() when hitting ores/glass/ice without silk touch |
| StairMaker | BLOCKS | places a block under your feet while jumping forward (sprint staircase) |
| PlaceNBreak | BLOCKS | place/break alternation while holding use or attack |
| AutoBuild | BLOCKS | 4 face-relative templates (Box/Wall/Tower/Peninsula), support-face detection, rotates with player yaw, N blocks/tick |
| InstaBuild | BLOCKS | instant floor/walls/pillar templates, N per tick |
| TreeBot | OTHER | lite state machine: nearest-log scan -> walk (angle interpolation + collision jump) -> chop trunk top-down |
| Protect | OTHER | protection sphere set at enable position; Nuker, AreaNuker, SpeedNuker, Excavator, AirMiner and SuperInstaMine all consult ProtectHack.isProtected() |
| CheatDetector | INTEL | per-player speed/fly motion heuristics, cooldown chat reports, optional red world boxes |
| AntiCheatDetect | INTEL | regex watch for anti-cheat kick/warn chat patterns, alert line, optional hide |
| Telemetrics | INTEL | HUD: server IP + ping, entity/player counts, coordinates |
| CoordLogger | INTEL | regex-scans chat for x/y/z triples, appends to config/wurst0/coords.txt, HUD counter |
| NoGoZone | INTEL | center set at enable position, radius slider, HUD banner + chat warning inside the zone |
| SignHistory | INTEL | scans TileEntitySign texts via reflection (signText / field_145915_a) -> config/wurst0/signs.txt |
| RespawnBedStore | INTEL | records beds you click on into config/wurst0/beds.txt, HUD shows the last one |
| InventorySorter | ITEMS | sorts the main inventory (9-35) with PICKUP click swaps, one swap per tick, hotbar untouched |
| FastFill | ITEMS | shift-clicks matching (or all) stacks into the open chest, N per tick |
| LootSorter | ITEMS | sorts the opened chest with PICKUP swaps, one per tick |
| XCarry | ITEMS | lite: stashes craft-grid items into the inventory while the GUI is open and puts them back on reopen (true packet XCarry stays blocked on the mixin phase) |
| QuickShulker | ITEMS | hold sneak ~0.75s with a shulker box in hand to open it (rightClickMouse reflection) |
| AutoTrader | ITEMS | cycles villager trades via sendEnchantPacket (C11) and QUICK_MOVE-buys from the result slot |
| BookBot | ITEMS | writes config/wurst0/bookbot.txt into the held writable book via the vanilla MC|BEdit / MC|BSign custom payloads (no GUI automation needed), optional auto-sign with title |

### Infrastructure notes from the v0.8 round

- **Sandbox rebuild after wipe**: repo1.maven.org 429s every request; the
  scala/lzma LocalFiller artifacts were recovered from **maven.minecraftforge.net**
  (jars) + hand-written minimal POMs (they reference scala-library 2.11.1,
  which the Forge maven also carries). lzma-0.0.1 is also available from
  libraries.minecraft.net. `scripts/fetch_fillers.sh` (workspace) does the
  retry dance if the Forge maven ever drops them.
- The RFG patched-jar POM gotcha hit again: `retrofuturagradle-2.0.2.pom`
  must sit beside the patched jar in the local maven or Gradle silently
  resolves the unpatched 3072M-heap jar and the decompile task OOMs (exit 137).
- 1.12.2 API corrections learned: `Vec3d` has `add(...)`, not `addVector`;
  fields are `x/y/z`; `processRightClickBlock(player, world, pos, side, vec, hand)`;
  `MapColor` lives in `net.minecraft.block.material`; `GuiMerchant` in
  `net.minecraft.client.gui`; `ContainerMerchant` exposes `getMerchantInventory()`
  only (get the IMerchant from `GuiMerchant.getMerchant()`);
  `IChunkProvider` has no chunk-count accessor.

## Implemented hacks (v0.7, +31)

| Hack | Category | Notes / limitations |
|---|---|---|
| SafeWalk | MOVEMENT | zeroes horizontal motion when the predicted next position has no support; optional sneak-at-edge flag (original patches the sneaking collision via mixin) |
| AntiEntityPush | MOVEMENT | cancels small horizontal kicks while standing still next to a living entity (collision push is re-applied client-side by vanilla) |
| ClutchFall | MOVEMENT | auto water-bucket MLG: swaps to hotbar water bucket at fall threshold, artificial raytrace places on first solid block below, swaps back after landing |
| AutoLoot | ITEMS | walks to the closest dropped item (rotate + movement input), jumps on horizontal collision / item above; pauses in GUIs |
| AntiBreak | ITEMS | aborts dig + warns when the held tool is within N uses of breaking (threshold slider) |
| KillPotion | ITEMS | creative-only: replaces held slot with Instant Damage (amp 124) + optional Wither potion via CPacketCreativeInventoryAction; drinkable/splash/lingering |
| TrollPotion | ITEMS | creative-only: 13-effect mixed potion (Speed/Haste/Strength/Jump/Regen/Resistance/FireRes/WaterBreathing/NightVision/HealthBoost/Absorption/Glowing/Luck) |
| Liquids | BLOCKS | re-raytraces with liquids on RightClickEmpty and sends the right-click on the water/lava block (source blocks are replaceable for placement) |
| InstantBunker | BLOCKS | 2x2 wall ring (2 high) + optional roof, one block per tick, support-face detection, auto-switch, restores slot |
| AirMiner | OTHER | Nuker-style dig of the closest breakable block in a small sphere around the player, N blocks/tick, no aiming needed |
| CrystalAura | COMBAT | attacks every EntityEnderCrystal in range on cooldown, LOS check, red-magenta target box render; no auto-placement |
| AntiProjectile | COMBAT | punches fireballs + shulker bullets that fly towards you (motion dot-product check) on cooldown |
| ForceOp | TOOLS | tries comma-separated password list on an interval (Login / Register / Say modes), stop-after-list option |
| AutoChat | CHAT | rotates (or randomizes) "|" separated messages on an interval |
| InstantCommand | CHAT | sends a command N seconds after every server join (leading slash optional) |
| StaffMonitor | CHAT | polls the tab list every second, chat+sound alert when a joining name matches a comma-separated pattern list (or every join) |
| DamageESP | RENDER | orange box (through walls) around entities with hurtTime > 0 |
| DamageDetect | OTHER | chat lines when nearby entities take damage (health delta) or die; 3s per-entity cooldown |
| PlayerSonar | INTEL | periodic chat ping listing players in range with distance + compass direction |
| MobOwners | RENDER | "Owner: <name>" billboard above tamed animals; resolves via getOwner() or tab-list UUID lookup, short-UUID fallback |
| MeasurementESP | RENDER | distance player-locked-point -> crosshair block/entity; 3D line, screen-space readout under crosshair, marker box at A |
| ArrowDMG | COMBAT | multiplies own arrows' motion right after bow release (reflection into EntityArrow.shootingEntity, velocityChanged=true); strict servers may reject |
| SkyBuildESP | RENDER | column heightmap scan above Y threshold (sky bases), 40-tick cache, optional chat alerts |
| SurfaceXray | RENDER | X-Ray variant: only ores with canSeeSky on the block above (exposed ores) |
| NoPumpkin | RENDER | cancels the HELMET overlay only when a carved pumpkin is worn (NoOverlay cancels it always) |
| ServerIntel | INTEL | top-center HUD: server brand, IP, dimension, difficulty |
| GameStats | TOOLS | top-center HUD: FPS (Minecraft.getDebugFPS), own ping (NetworkPlayerInfo.getResponseTime), optional brand; font scale |
| SoundMute | RENDER | PlaySoundEvent bridge (ForgeEvents) mutes sounds whose ResourceLocation path contains a comma-separated pattern |
| WebhookAlert | OTHER | Discord webhook (background thread, JSON-escaped): chat mentions (alias list) + players entering range |
| TooManyHax | OTHER | hides comma-listed hacks from the ClickGUI; "Hide only" keeps them in the HUD arraylist (filters in ClickGuiManager + Hud) |
| Waypoints | RENDER | tracers + labels + boxes to waypoints; persisted as a TextFieldSetting ("name x y z;..."); new `.wp add/del/list` command |

### New infrastructure in v0.7

- Two new Forge bridges in ForgeEvents: `PlaySoundEvent` (SoundMute) and
  `PlayerInteractEvent.RightClickEmpty` (Liquids), following the AntiDrop/AntiBlast
  direct-hook pattern (no new Wurst event classes needed).
- TooManyHax filters wired into both the ClickGUI window builder and the HUD arraylist.
- 13th command: `.wp` (WaypointsCmd) edits the Waypoints text field and marks config dirty.

### Environment notes discovered this round

- `devtools/patch_rfg_heap.py` DID match (`.../mcp/DecompileTask.class` endswith check is fine),
  but during setup the download of `rfg-orig.jar` had not finished, so the published jar was the
  UNPATCHED copy -> decompiler worker OOM (exit 137). Re-running the patch manually fixed it.
- The localmaven RFG directory was also missing `retrofuturagradle-2.0.2.pom`; without it Gradle
  silently falls back to the GTNH jar (see v0.2 notes). Both are now fetched in the right order.

# Wurst0 - 1.12.2 Port Status

Port of [Wurst7-CevAPI](https://github.com/cev-api/Wurst7-CevAPI)
(MC 26.3 / Fabric / Java 25) to **Minecraft 1.12.2 / Forge / Java 8**.

- **Branch:** `mc1.12.2` on [apikun/Wurst0-CevAPI](https://github.com/apikun/Wurst0-CevAPI)
- **Mod id:** `wurst0`  **Package:** `net.wurst0`  **Version:** 0.7.0
- **License:** GPL-3.0-or-later (unchanged - this is a derivative of Wurst7)
- **Target:** Forge 1.12.2-14.23.5.2859, MCP `stable_39` mappings, RetroFuturaGradle 2.0.2 + Gradle 9.8.2

---

## Current state (updated 2026-10-31, v0.7.0 BUILD PASSING + RELEASED)

`gradle build` produces `build/libs/Wurst0-0.7.0.jar` (reobfuscated, drop into
`.minecraft/mods` with Forge 1.12.2) plus a `-dev` jar for development.
**v0.7.0 is released on GitHub** with the jar attached (tag `v0.7.0`).

| Item | Count | Status |
|---|---|---|
| Hacks registered | 308 | all appear in ClickGUI |
| Hacks fully implemented | 183 | working logic (59% of registry) |
| Hacks stubbed | 125 | compile + register, no logic yet (`[not ported yet]` in tooltip) |
| Commands | 13 | `.help` `.t` `.bind` `.binds` `.say` `.vclip` `.jump` `.gm` `.setmode` `.setslider` `.setcheckbox` `.features` `.wp` |
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

### Implemented hacks (v0.6, +29, one reverted)

| Hack | Category | Notes / limitations |
|---|---|---|
| Sneak | MOVEMENT | forces isSneaking + movementInput.sneak each tick (vanilla sends C0B); approximate: no input override mixin |
| CreativeFlight | MOVEMENT | forces capabilities.allowFlying/isFlying + sendPlayerAbilities (C13); vanilla servers push back without permission |
| InvWalk | MOVEMENT | polls keybinds while a GuiContainer is open (vanilla skips input in GUIs); anvil rename screen excluded |
| NoClip | MOVEMENT | client-side: noClip + WASD flight; server rubber-bands without spectator/flight perms |
| ExtraElytra | MOVEMENT | 3-click chestplate swap (slot 6) when elytra below threshold, works GUI-closed |
| Antisocial | OTHER | inverse Follow: rotates away and walks from players within trigger radius, jumps obstacles |
| Reach | OTHER | raises the `generic.reachDistance` attribute (EntityPlayer.REACH_DISTANCE) - Forge 1.12.2 routes getBlockReachDistance + getMouseOver through it; restored to 5.0 on disable |
| PearlDetect | OTHER | warns in chat when a foreign ender pearl flies towards you (dot > 0.85, 3s cooldown) |
| ItemGenerator | ITEMS | creative only: tops up the held slot via sendSlotPacket (CPacketCreativeInventoryAction, slot+36) |
| UseItemSpam | ITEMS | rightClickMouse reflection (ReflectCalls) at an interval, optional only-with-item |
| ClickAura | COMBAT | attacks all valid entities in range per cooldown, no rotation; players/passives filters |
| BedBreakAura | BLOCKS | closest bed in range via clickBlock/onPlayerDamageBlock + target box render |
| VeinMiner | BLOCKS | BFS vein collection from the block you mine (8 ore types, 6-connected, limit slider), one dig per tick |
| Tunneller | BLOCKS | digs eye-then-feet block ahead of the horizontal facing, optional walk-forward, stops on unbreakable |
| AutoSign | BLOCKS | reflection into GuiEditSign.tileSign + TileEntitySign.signText, sends CPacketUpdateSign and closes; \n separates the 4 lines, Overwrite checkbox |
| HeadRoll | FUN | sine-wave pitch, yaw kept |
| MobView | FUN | camera attaches to nearest mob via setRenderViewEntity, restores on disable/despawn |
| CommandSpam | CHAT | ChatSpam variant sending commands (auto-prepends /), optional counter |
| Mention | CHAT | gold-highlights chat lines containing your name + XP-orb sound (ENTITY_EXPERIENCE_ORB_PICKUP) |
| PlayerMute | CHAT | cancels chat lines matching "<Name>" via TextFieldSetting |
| HealthTags | RENDER | hostile-only name+health billboards, red, through walls (NameTags machinery) |
| ElytraInfo | RENDER | bottom-right elytra durability + flight speed HUD |
| NbtSizeCounter | TOOLS | bottom-left NBT size of held item (B/KB/MB from tag.toString) |
| EntityCount | TOOLS | top-right loaded entity counts (players/hostiles/passives/items), 10-tick refresh |
| BaseFinder | INTEL | 38-type man-made block set, cached scan, "Underground only" (canSeeSky) filter |
| CaveFinder | INTEL | dark air pockets (skylight < 8, no sky access), cached scan |
| RoofEsp | RENDER | nether bedrock roof (y 125-127, dimension -1 check) |
| WorkstationEsp | RENDER | crafting table/furnace/enchant table/anvil/brewing stand/jukebox/noteblock |
| LavaWaterEsp | RENDER | lava + optional water highlight through walls |
| MobSpawnESP | RENDER | solid top + 2 air + block light <= 7 spots (client light data) |
| CameraDistance | RENDER | **reverted to stub** - see Blockers (const-inlined final field) |

### 1.12.2 findings from the v0.6 round (verified against mcp_patched_minecraft-sources.jar)

- **Reach is an attribute now**: Forge patches `PlayerControllerMP.getBlockReachDistance()` to return
  `mc.player.getEntityAttribute(EntityPlayer.REACH_DISTANCE)` (default 5.0, `setShouldWatch(true)`),
  and `EntityRenderer.getMouseOver` uses it for entity crosshair selection. No reflection needed.
- **CameraDistance is impossible via reflection** (see Blockers): `EntityRenderer.thirdPersonDistance`
  is `private final` (constant-inlined to 4.0F at every use site) and `updateRenderer()` resets
  `thirdPersonDistancePrev = 4.0F` every frame before `orientCamera` interpolates towards the
  inlined literal. Needs a mixin/ASM hook.
- MCP name gotchas: `GuiRepair` lives in `net.minecraft.client.gui` (NOT gui.inventory),
  `SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP` (ENTITY_ prefix), `Blocks.LIT_PUMPKIN` (jack
  o'lantern) and `Blocks.NOTEBLOCK` (one word, confirmed again).
- `PlayerControllerMP.getIsHittingBlock()` is public and works as the VeinMiner trigger.
- `PlayerControllerMP.sendSlotPacket(ItemStack, slot)` sends CPacketCreativeInventoryAction -
  the clean way to give items in creative.
- `pearl.getThrower()` returns `EntityLivingBase`, not `EntityPlayer`.

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

- **CameraDistance** needs a mixin/ASM hook (EntityRenderer.thirdPersonDistance is a
  const-inlined `private final float` in 1.12.2 and updateRenderer() resets
  thirdPersonDistancePrev to 4.0F every frame; no Forge event fires between the reset
  and orientCamera). Blocked until the SpongeMixin phase.
- (The v0.4.0 push/release blocker from 2026-10-31 was resolved the
  same day with a fresh PAT; v0.4.0-v0.6.0 are all released.)
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
