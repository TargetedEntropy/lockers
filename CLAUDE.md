# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project summary

**Lockers** is a NeoForge Minecraft mod. Players place a Locker block and save up to 6 named loadouts (4 armor slots + offhand + all accessory slots), then swap between them instantly. Inspired by Rust's locker.

The repo supports **multiple Minecraft versions in parallel**: each version lives in its own Gradle subproject (`neoforge-1.21.1`, `neoforge-1.21.4`, …). Adding a new MC version = copy the most recent `neoforge-*` module, patch API diffs, add it to `settings.gradle.kts`.

## Architecture

### `common` module — **no Minecraft code allowed**

`common/` is plain `java-library`. It holds every piece of logic that is version-independent:

- `model/` — `SlotId`, `Loadout`, `LockerData` records
- `serialize/` — `DataTag` sealed-interface facade (our MC-free NBT substitute) and `LockerDataCodec`
- `compat/` — generic `AccessoryBridge<PLAYER, STACK>` interface + `BridgeRegistry` selection logic
- `access/` — `AccessControl` enum and `canAccess(...)` pure function
- `config/` — `CommonConfig` DTO

`common` is where the **90% JaCoCo coverage gate** is enforced. Unit tests here must not import `net.minecraft.*` — doing so would pull Mojang mappings into `common` and defeat the whole architecture. If you need a MC type in a test, add a factory interface to `common` instead and mock it.

### Per-version modules

Each `neoforge-<mcver>/` module provides the Minecraft-facing shell:

- `LockersMod` — mod entrypoint, wires `BridgeRegistry` via `ModList.get().isLoaded(...)` checks
- `registry/` — `DeferredRegister` for blocks, items, block entities, menus, data components
- `block/LockerBlock`, `block/LockerBlockEntity` — owner UUID + loadouts persist via `DataTag` bridge
- `menu/LockerMenu`, `client/screen/LockerScreen` — virtual-slot menu; 6-tab GUI with armor avatar
- `network/` — payload packets (save/load/rename/delete/sync)
- `nbt/DataTagBridge` — the **only** adapter between `CompoundTag` and `common`'s `DataTag`
- `compat/curios/*Bridge`, `compat/accessories/*Bridge` — per-version Curios/Accessories impls

### Compat layer contract (important)

`AccessoryBridge` is generic on `<PLAYER, STACK>` so Minecraft types never leak into `common`. The bound `McAccessoryBridge = AccessoryBridge<ServerPlayer, ItemStack>` lives in each per-version module. At mod construction, `BridgeRegistry.select(modList, config)` picks one of:

- `Curios*Bridge` if Curios is loaded (preferred when both present, per default config)
- `Accessories*Bridge` if Accessories is loaded
- `NoopAccessoryBridge` (from `common`) otherwise — armor + offhand still save, just no accessory slots

The `preferred_accessory_impl` config (AUTO | CURIOS | ACCESSORIES | NONE) overrides. Do **not** reach for `ServiceLoader` — `ModList` is authoritative.

### Version divergence is real

API deltas confirmed between 1.21.1 and 1.21.4 during this scaffold:

- `DirectionProperty` is removed in 1.21.4 — use `EnumProperty<Direction>` (both modules keep the same field name `FACING`)
- `BlockEntityType.Builder` is removed in 1.21.4 — use the public constructor `new BlockEntityType<>(factory, block)`
- **Curios 9 → 10 API break** — `CuriosApi.getCuriosHelper()` removed, replaced by `CuriosApi.getCuriosInventory(Player)`; capability-style registration replaces direct events. The `Curios9Bridge` (1.21.1) and `Curios10Bridge` (1.21.4) exist specifically to isolate this break.
- `BlockEntity.saveAdditional` / `loadAdditional` take `HolderLookup.Provider` in both 1.21.1 and 1.21.4 (the addition actually landed before 1.21.1 — do not assume this is a 1.21.4-only change).

If a change seems to apply to both modules identically, the logic likely belongs in `common` instead.

## Common commands

### Build and test

```sh
./gradlew build                                 # everything
./gradlew :common:check                         # unit tests + JaCoCo 90% gate
./gradlew :common:jacocoTestReport              # HTML at common/build/reports/jacoco/test/html/
./gradlew :neoforge-1.21.1:build                # 1.21.1 jar
./gradlew :neoforge-1.21.4:build                # 1.21.4 jar
```

### Single-test execution

```sh
./gradlew :common:test --tests "com.targetedentropy.lockers.common.access.AccessControlTest"
./gradlew :common:test --tests "*Codec*"        # glob patterns work
```

### In-world (dev client / dedicated server / GameTest)

```sh
./gradlew :neoforge-1.21.1:runClient
./gradlew :neoforge-1.21.1:runServer
./gradlew :neoforge-1.21.1:runGameTestServer    # CI runs this headless; pass/fail only, no coverage
./gradlew :neoforge-1.21.1:runData              # regenerate DataGen output into src/generated/
```

Replace `1.21.1` with `1.21.4` for the other branch. Gradle daemon persists between runs — use `--stop` if you see classloader weirdness after a refactor.

## Testing philosophy

- **Unit tests in `common`** carry the coverage budget. Hard CI gate is **90% line / 85% branch** (enforced by `:common:jacocoTestCoverageVerification`). Current coverage is ~99% line / ~96% branch — keep it there.
- **GameTest** carries in-world behavior verification: place block, save/load loadout, access control denies non-owners, Curios/Accessories capture roundtrips. GameTest is **not** a coverage source — do not collect JaCoCo from GameTest runs; the agent instrumentation conflicts with NeoForge runtime transforms.
- **Manual verification** via `runClient` is expected for GUI changes. CI cannot catch screen layout regressions.

## CI gates

`.github/workflows/ci.yml` on every PR:

1. `:common:check` (JUnit + JaCoCo verification) runs once unmatrixed.
2. Matrix over MC versions: `:neoforge-<ver>:build` + `:neoforge-<ver>:runGameTestServer`.
3. Codecov upload of the `common` JaCoCo XML report.

PRs must be green before merge. Branch protection should be enabled on `main`
after repo creation; the `ci.yml` checks for `common` and each MC version
should be marked required.

## Release

`.github/workflows/release.yml` triggers on `v*` tags. It builds all variants,
creates a GitHub Release with jars attached, then uploads to CurseForge
(required) and Modrinth (optional — skipped if `MODRINTH_TOKEN` is absent).

**Required secrets** (set in GitHub repo settings before tagging a release):

- `CURSEFORGE_TOKEN` — CurseForge API token with upload scope
- `MODRINTH_TOKEN` (optional) — Modrinth PAT

Tag format: `v<mod_version>` (e.g. `v0.1.0-alpha.1`). Jar names pattern:
`lockers-<mod_version>+<mc_version>.jar`.

## Adding a new Minecraft version

1. Copy `neoforge-1.21.4/` to `neoforge-<new-ver>/`.
2. Update the NeoForge coordinate in `gradle/libs.versions.toml`.
3. Update Curios and Accessories coordinates — check the Modrinth API for
   current versions targeting the new MC (see commands below).
4. Add the new subproject to `settings.gradle.kts`.
5. Fix compile errors. They will concentrate in `BlockEntity` NBT methods,
   `Block.useItemOn`, and payload registration.
6. Add the new version to the CI matrix in `.github/workflows/ci.yml`.

```sh
# Look up current Curios version for MC 1.21.x
curl -s "https://api.modrinth.com/v2/project/curios/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.x%22%5D" | jq '.[0].version_number'
```

## Things to avoid

- **Don't import `net.minecraft.*` in `common/`.** It defeats the architecture and breaks unit tests.
- **Don't put Curios/Accessories logic in `common/`.** Only the generic interface goes there.
- **Don't collect JaCoCo from GameTest runs.** The agent conflicts with NeoForge transforms.
- **Don't use `ServiceLoader` for bridge selection.** `ModList.get().isLoaded(...)` is authoritative.
- **Don't use `--no-verify` on commits.** If a hook fails, fix it.

## What's implemented vs. stubbed (v0.1.0-alpha.1)

- ✅ `common` domain model + codec + access policy + bridge selector (99% line, 96% branch coverage)
- ✅ Multi-module Gradle + MDG 2.0.141 + both per-MC-version modules build clean
- ✅ Block + BlockEntity persist `LockerData` through `DataTagBridge`
- ✅ Menu + Screen open for the owner; non-owners are denied with a translatable message
- ✅ Creative-tab registration; static blockstate/model/recipe/loot table JSON
- ✅ **Network packets** — `Save`/`Load`/`Rename`/`Delete`/`SyncLocker` payloads registered via `RegisterPayloadHandlersEvent`. All C2S packets re-run `AccessPolicy.canAccess` server-side before mutating (client trust = 0).
- ✅ **Real capture/apply for vanilla slots** — `saveLoadoutFromPlayer` grabs the 4 armor slots + offhand via `player.getItemBySlot(...)` and stashes NBT blobs; `loadLoadoutToPlayer` restores them, putting the previously-equipped items back into the player's main inventory (dropping on the floor if full — never silently deleted).
- ✅ **Curios bridges (9.x and 10.x)** — full capture and apply via `CuriosApi.getCuriosInventory(Player)` → `ICuriosItemHandler.getCurios()` → per-`ICurioStacksHandler` slot iteration. Slot ids encode as `curios:<type>/<index>`. Items previously equipped during apply are returned to the main inventory or dropped (never silently deleted).
- ✅ **Accessories bridges (1.x and 2.x)** — full capture and apply via `AccessoriesCapability.getOptionally(Player)` → `AccessoriesContainer.getAccessories()` → `ExpandedSimpleContainer`. Same return-to-inventory semantics as Curios.
- ✅ **Rename UX** — `LockerScreen` renders an `EditBox` per row; Enter or focus-out fires `RenameLoadoutPacket`; auto-named "Loadout N" if blank on save.
- ✅ **GUI wired end-to-end** — `LockerScreen` holds the latest `LockerData` from `SyncLockerPacket`; per-row state (populated vs. empty) drives button enable/disable; in-progress edits aren't clobbered by sync events.
- ✅ `BlockEntity.data()` returns `Optional<LockerData>`; callers handle empty rather than risking NPE.
- ⚠️ **DataGen is not active.** Recipes, models, loot, and blockstates live as static JSON under `common-resources/`. If the set grows, wire `GatherDataEvent` in each module and delete the static copies.
- ⚠️ **GUI textures not authored.** `LockerScreen` draws a plain dark rectangle — `textures/gui/locker.png` is referenced but not shipped; the plain fill fallback is acceptable for alpha.
- ⚠️ **GameTest job remains gated** (`if: false`). The `LockerPlacementGameTest` exists and compiles, but it expects a `lockers:empty_3x3` structure NBT under `data/lockers/structures/` that has not been authored. Without the structure file, the CI job won't run successfully — leaving it gated until someone with a hands-on session can author the binary `.nbt` (or correct-format `.snbt`) and verify it loads on both 1.21.1 and 1.21.4.

## Manual verification (required before tagging a release)

Automated coverage in `common` + the CI build matrix catch logic and compile regressions, but the client-facing save/load/GUI flow cannot be exercised from the CLI. Before tagging:

```sh
./gradlew :neoforge-1.21.1:runClient
# in-game:
#   /give @s minecraft:iron_chestplate
#   /give @s minecraft:diamond_helmet
#   (equip them)
#   /give @s lockers:locker
#   place the locker, right-click it
#   click [Save] on Slot 1 — expect overlay "Saved loadout 'Loadout 1'."
#   remove armor (drop it or /clear @s)
#   right-click the locker again, click [Load] on Slot 1 — expect armor restored
#   click [X] on Slot 1 — expect slot to go back to "Empty"
#   try to access with a second player (/op second; /deop self) — expect "This locker belongs to someone else."
#   break the locker, re-place it, verify loadout data was lost (ownership resets per-block)
# repeat with :neoforge-1.21.4:runClient
```

If this flow works end-to-end on both MC versions, tag `v0.1.0-alpha` and let the release workflow publish.
