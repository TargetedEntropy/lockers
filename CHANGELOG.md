# Changelog

All notable changes to Lockers will be documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[0.1.0]: https://github.com/targetedentropy/lockers/releases/tag/v0.1.0
