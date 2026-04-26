# Changelog

All notable changes to Lockers will be documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0-alpha.2] — 2026-04-26

Re-release of alpha.1 to fix the CurseForge upload pipeline.

### Fixed

- **CurseForge / Modrinth upload no longer ships the sources jar as a
  primary file.** The `lockers-neoforge-<ver>+<mc>-sources.jar` was being
  uploaded alongside the real mod jar; CurseForge correctly rejected it
  ("File is not compatible with client since it includes only `.java`
  files"). The release workflow now strips `*-sources.jar` from the
  dist directory before the `mc-publish` step. Sources jars continue to
  be attached to the GitHub Release for anyone who wants them.

No code changes since alpha.1.

## [0.2.0-alpha.1] — 2026-04-26

Adds support for **Minecraft 26.1.2** (the version where Mojang dropped the
`1.` prefix from the public version string and reset the major-version
counter). This is an **alpha** release — the underlying NeoForge, Curios,
and tooling for this MC line are themselves all in beta. The 1.21.1 and
1.21.4 lines from v0.1.0 continue to be supported and ship at parity.

### Added

- **New module `neoforge-26.1.2`** — produces `lockers-neoforge-0.2.0-alpha.1+26.1.2.jar`.
  Built against NeoForge `26.1.2.29-beta`, Java 25, Curios `15.0.0-beta.2+26.1.2`.
- The CI build matrix and Release workflow now run all three MC lines
  (1.21.1 / 1.21.4 / 26.1.2) on every PR and tag.

### Limitations on 26.1.2 (will be addressed before 0.2.0 final)

- **GUI is stubbed.** Mojang's 26.1.2 client replaced `GuiGraphics` with a
  new extract-then-render pipeline (`GuiGraphicsExtractor`), and made
  `imageWidth` / `imageHeight` final. Porting `LockerScreen` is a deeper
  rewrite than a signature swap. The Locker block opens a blank container
  screen on 26.1.2 — server-side block placement, NBT persistence, and
  Curios capture/apply still work end-to-end (test via console commands or
  by attaching to the BE programmatically).
- **Accessories integration is omitted.** Wisp Forest hasn't published a
  `+26.1.2` Accessories build yet. The mod loads fine without it; the
  bridge falls through to `NoopAccessoryBridge`.
- **Parchment mappings unavailable.** The new module compiles against
  MojMaps only — variable names in stack traces will be obfuscated until
  ParchmentMC publishes `parchment-26.1.2`.
- **No GameTest** (the framework's API moved between 1.21.4 → 26.1.2;
  smoke test deleted from the new module).

### Confirmed vanilla API breaks (for anyone porting other mods)

- `net.minecraft.resources.ResourceLocation` → `net.minecraft.resources.Identifier`
- `BlockEntity.saveAdditional(CompoundTag, HolderLookup.Provider)` →
  `saveAdditional(ValueOutput)`. Same for `loadAdditional` → `ValueInput`.
- `ItemStack.save(Provider)` / `parseOptional(Provider, CompoundTag)` removed;
  use `ItemStack.CODEC` with `RegistryOps`.
- Authlib 7.x: `GameProfile.getName()` → `name()` (record).
- `Player.hasPermissions(int)` → `Player.permissions().hasPermission(Permission)`.
  (Vanilla level 2 ≈ `Permissions.COMMANDS_GAMEMASTER`.)
- `displayClientMessage(component, true)` → `sendOverlayMessage(component)`.
- `CompoundTag.getAllKeys()` → `keySet()`. Primitive NBT classes
  (`StringTag`, `IntTag`, `LongTag`) are records; `getAsString/getAsInt/getAsLong`
  → `value()`.
- `CompoundTag.getCompound(String)` returns `Optional<CompoundTag>`.
- `DeferredRegister.Items.registerSimpleBlockItem(Holder<Block>, Item.Properties)` →
  `(Holder<Block>, Supplier<Item.Properties>)`.
- `AbstractContainerScreen` constructor now takes width + height (5-arg
  form); `imageWidth` / `imageHeight` final.
- `Screen.keyPressed(int, int, int)` → `keyPressed(KeyEvent)`.
  `mouseClicked(double, double, int)` → `mouseClicked(MouseButtonEvent)`.
  `AbstractContainerScreen.mouseClicked(MouseButtonEvent, boolean)` (extra
  consumed flag).
- `GuiGraphics` removed; replaced by `GuiGraphicsExtractor` (extract-then-render).
- `Level.isClientSide` field is private; use `Level.isClientSide()` method.

## [0.1.0] — 2026-04-26

Initial public release. Inspired by the Locker block in the game **Rust**:
place a Locker, stash your kit, swap between up to 6 saved loadouts.

### Added

- **Locker block.** A 3-block-tall multiblock structure (BOTTOM / MIDDLE / TOP)
  with custom front, side, and top textures by Calmingstorm. Crafts from
  8 iron ingots + 1 diamond.
- **6 named loadout slots per Locker.** Per-slot rename via an in-GUI text
  field; defaults to "Loadout *N*" if you don't type one.
- **Save** physically takes your armor + offhand off and stashes them in the
  selected slot. Overwriting a populated slot ejects the old contents to your
  main inventory and requires a "Confirm?" click.
- **Load** physically removes the saved gear from the slot and equips it on
  you; whatever you were wearing in those slots returns to your main
  inventory (or drops on the floor if it's full — never silently deleted).
  The slot becomes empty after Load.
- **Delete** clears a slot. Two-click confirm (X → ?) to prevent misclicks.
- **Curios API integration.** Full save / restore of every Curios accessory
  slot. Slot ids map cleanly through the Locker's storage so a saved Curios
  slot survives world reloads. Compatible with Curios 9.x on Minecraft 1.21.1
  and Curios 10.x on 1.21.4 — the 9→10 API break is handled by per-version
  bridge classes.
- **Wisp Forest Accessories integration.** Same coverage as Curios; the mod
  picks one when both are installed (default: Curios; configurable via the
  `preferred_accessory_impl` option).
- **Accessory partial-merge semantics.** Saving captures every equipped
  accessory; loading only touches the slots the saved loadout actually
  named, leaving the rest of your equipped accessories alone.
- **Owner-only access by default.** The player who places the Locker is the
  owner. Non-owners get "This locker belongs to someone else." in their
  action bar and cannot open the GUI. Owners get a `Public` / `Private`
  toggle in the GUI top-right; flipping it lets anyone use the Locker
  (handy for shared base lockers on SMP servers).
- **Loadouts survive break + replace.** Break a Locker with a pickaxe and
  the dropped item carries every saved loadout via vanilla
  `minecraft:custom_data`. Place it again and the new placer becomes the
  owner — saved gear stays.
- **Server config:** `default_access` (`OWNER_ONLY` / `PUBLIC`),
  `ops_bypass_ownership` (boolean, default true), and
  `preferred_accessory_impl` (`AUTO` / `CURIOS` / `ACCESSORIES` / `NONE`).

### Supported

- Minecraft **1.21.1** on NeoForge 21.1.228+
- Minecraft **1.21.4** on NeoForge 21.4.157+
- Curios (optional, both versions)
- Accessories (optional, both versions)
- Java 21+

### Known limitations

- Inventory icon shows the bottom slice of the locker. Stacked-icon polish
  is targeted for a follow-up.
- GUI uses a plain dark panel — no custom GUI texture is authored yet.
- GameTest CI job is wired up but disabled (`if: false`); the structure NBT
  template hasn't been verified in a runtime GameTest server pass.

[0.2.0-alpha.2]: https://github.com/targetedentropy/lockers/releases/tag/v0.2.0-alpha.2
[0.2.0-alpha.1]: https://github.com/targetedentropy/lockers/releases/tag/v0.2.0-alpha.1
[0.1.0]: https://github.com/targetedentropy/lockers/releases/tag/v0.1.0
