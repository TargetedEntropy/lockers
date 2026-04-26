# Minecraft 26.1.2 — vanilla API breaks log

Notes captured while porting the Lockers mod from Minecraft 1.21.4 to
Minecraft 26.1.2 (the version where Mojang dropped the `1.` prefix and
reset major-version numbering). Useful as a reference if you're porting
any other NeoForge mod over the same gap. Not for end users.

## Toolchain

- **Java 25** required (vanilla `version.json` `javaVersion.majorVersion=25`).
  Per-module Gradle toolchain override picks it up; the `foojay-resolver-convention`
  plugin auto-fetches Java 25 if it isn't installed locally.
- NeoForge 26.1.2.x line is **beta-only** at the time of writing
  (latest: `26.1.2.29-beta`). No stable releases yet.

## Renames

- `net.minecraft.resources.ResourceLocation` → `net.minecraft.resources.Identifier`
  (matches the name Fabric has used for years). Static factory
  `fromNamespaceAndPath` preserved.
- `Authlib 7.x`: `GameProfile` is a record. `getId()` → `id()`,
  `getName()` → `name()`, `getProperties()` → `properties()`.
- `CompoundTag.getAllKeys()` → `keySet()`.
- Primitive NBT classes (`StringTag`, `IntTag`, `LongTag`) are now records.
  `getAsString()` / `getAsInt()` / `getAsLong()` → `value()`.
- `Level.isClientSide` field → `isClientSide()` method (field is private now).

## Signature changes

- `BlockEntity.saveAdditional(CompoundTag, HolderLookup.Provider)` →
  `saveAdditional(ValueOutput)`. Same shape for `loadAdditional` →
  `ValueInput`. Whole new data-format-agnostic IO interface; use
  `output.store(name, Codec, value)` and `input.read(name, Codec)`.
  `ValueInput.lookup()` returns the `HolderLookup.Provider`; `ValueOutput`
  doesn't expose one (codecs are resolved by caller-provided context).

- `ItemStack.save(HolderLookup.Provider)` and
  `ItemStack.parseOptional(HolderLookup.Provider, CompoundTag)` removed.
  Serialization now goes through `ItemStack.CODEC`:
  ```java
  RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
  Tag tag = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow(...);
  ItemStack back = ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
  ```

- `CompoundTag.getCompound(String)` returns `Optional<CompoundTag>`.
  Use `.getCompoundOrEmpty(String)` if you want the legacy non-optional behavior.

- `Player.hasPermissions(int level)` → `Player.permissions().hasPermission(Permission)`.
  Old level 2 (operator commands) ≈ `Permissions.COMMANDS_GAMEMASTER`.

- `Player.displayClientMessage(Component, boolean overlay)` split:
  - overlay=true → `sendOverlayMessage(Component)`
  - overlay=false → `sendSystemMessage(Component)`

- `DeferredRegister.Items.registerSimpleBlockItem(Holder<Block>, Item.Properties)` →
  `(Holder<Block>, Supplier<Item.Properties>)`. Pass `Item.Properties::new`.

## Client/Screen rewrite

- `AbstractContainerScreen.imageWidth` / `imageHeight` are now `final`. Set them
  via the new 5-arg super constructor: `super(menu, inv, title, width, height)`.

- `Screen.keyPressed(int keyCode, int scanCode, int modifiers)` →
  `keyPressed(KeyEvent event)`. `KeyEvent` is a record with `key()`, `scancode()`,
  `modifiers()`.

- `Screen.mouseClicked(double x, double y, int button)` →
  `mouseClicked(MouseButtonEvent event)`. `AbstractContainerScreen` overrides as
  `mouseClicked(MouseButtonEvent event, boolean consumed)` (extra flag).

- `GuiGraphics` is **gone**, replaced by `GuiGraphicsExtractor`. The whole
  rendering pipeline shifted from imperative `gfx.fill / drawString` calls to
  an extract-then-render pattern (`extractRenderState(GuiGraphicsExtractor, ...)`).
  This is a real port, not a signature swap. The Lockers mod's `LockerScreen`
  is currently a stub on 26.1.2 because of this.

## NeoForge

- `LevelHeightAccessor.getMaxBuildHeight()` removed; use `getMaxY()`.
- `IItemHandler.getSlots()` and `IItemHandlerModifiable` deprecated for removal
  (still functional in 26.1.2, but flagged).
- `RegisterPayloadHandlersEvent` and `PayloadRegistrar.playToServer/playToClient`
  signatures unchanged from 1.21.4 — packets ported with no edits beyond the
  `ResourceLocation → Identifier` rename.

## Ecosystem (as of 2026-04-26)

| Library | Status on 26.1.2 |
| --- | --- |
| NeoForge | `26.1.2.29-beta` only — no stable |
| Curios | `15.0.0-beta.2+26.1.2` only |
| Accessories (Wisp Forest) | not yet published |
| ParchmentMC | not yet published |

The Lockers mod ships against the betas with a `0.2.0-alpha.x` tag and
falls through to `NoopAccessoryBridge` when Accessories is absent. Re-run
the version-pin checks once these libraries publish stable / 26.1.2 builds.
