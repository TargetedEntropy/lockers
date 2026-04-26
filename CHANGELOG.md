# Changelog

All notable changes to Lockers will be documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0-alpha.2] — 2026-04-26

### Fixed

- Fixed a packaging bug that caused the v0.2.0-alpha.1 file to be rejected
  on CurseForge. The mod itself is unchanged — if you already have alpha.1
  installed locally, you don't need to update.

## [0.2.0-alpha.1] — 2026-04-26

Adds **Minecraft 26.1.2** support. The 1.21.1 and 1.21.4 builds from v0.1.0
continue to be supported and ship at parity.

### Heads up — alpha quality on 26.1.2

Mojang reset Minecraft's version numbering with 26.1.2 (no more `1.` prefix),
and most of the modding ecosystem is still catching up. In this release on
the 26.1.2 line:

- **The Locker GUI is a placeholder.** The window opens blank — Mojang
  rewrote the rendering pipeline and a follow-up release will catch up. You
  can still place a Locker, and saved loadouts persist correctly across
  world reloads, but you can't currently interact with them through the UI.
- **Accessories support is disabled** — Wisp Forest's library doesn't have
  a 26.1.2 build yet. Curios works.
- **For the fully-polished experience, stay on Minecraft 1.21.1 or 1.21.4.**
  Those builds are at v0.1.0 parity with full GUI, Curios, and Accessories.

### What's working on all three MC lines (1.21.1 / 1.21.4 / 26.1.2)

- Place a Locker, owner stamping, persistence across world reload
- The block carries its saved loadouts when broken and replaced (just like in v0.1.0)

## [0.1.0] — 2026-04-26

Initial public release. Inspired by the Locker block in the game **Rust**:
place a Locker, stash your kit, swap between up to 6 saved loadouts.

### Added

- **Locker block.** A 3-block-tall multiblock with custom front, side, and
  top textures by Calmingstorm. Crafts from 8 iron ingots + 1 diamond.
- **6 named loadout slots per Locker.** Type a name into each row in the
  GUI; defaults to "Loadout *N*" if you skip it.
- **Save** physically takes your armor and offhand off and stashes them in
  the selected slot. Overwriting a populated slot ejects the old contents
  to your inventory and requires a "Confirm?" click so you don't lose gear
  by accident.
- **Load** equips the saved gear; whatever you were wearing in those slots
  returns to your inventory (or drops on the floor if it's full — never
  silently deleted).
- **Delete** clears a slot. Two-click confirm so misclicks don't wipe a kit.
- **Curios + Accessories integration.** Saves and restores every accessory
  slot the same way it does armor and offhand. Auto-detects which library
  is installed; if both are, picks Curios by default. Server config option
  `preferred_accessory_impl` overrides.
- **Owner-only by default.** Other players see "This locker belongs to
  someone else." in the action bar and can't open the GUI. Owners can
  toggle a Locker public from the top-right of the GUI for shared bases on
  multiplayer servers.
- **Loadouts survive when the Locker is broken.** Pick the dropped item up,
  place it again somewhere else — your saved loadouts come along. Whoever
  places it becomes the new owner.

### Supported

- Minecraft **1.21.1** on NeoForge 21.1.228+
- Minecraft **1.21.4** on NeoForge 21.4.157+
- Curios (optional)
- Wisp Forest Accessories (optional)
- Java 21+

[0.2.0-alpha.2]: https://github.com/targetedentropy/lockers/releases/tag/v0.2.0-alpha.2
[0.2.0-alpha.1]: https://github.com/targetedentropy/lockers/releases/tag/v0.2.0-alpha.1
[0.1.0]: https://github.com/targetedentropy/lockers/releases/tag/v0.1.0
