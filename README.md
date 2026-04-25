# Lockers

A NeoForge Minecraft mod that mirrors the Locker feature from the game **Rust**:
place a Locker block, save up to **6 loadouts** of armor + offhand, and
instantly swap between them.

- **Owner-only** access by default; server configurable to public or ops-bypass.
- Loadouts persist per-block across chunk unloads and server restarts.
- **Curios and Accessories compat layer present** — interface + version-specific
  bridges land the hooks for accessory slots. Actual capture/apply is stubbed
  in **v0.1.0-alpha** (vanilla armor + offhand only); full Curios/Accessories
  integration targeted for v0.2.

## Supported versions

| Minecraft | NeoForge    | Module            |
| --------- | ----------- | ----------------- |
| 1.21.1    | 21.1.228    | `neoforge-1.21.1` |
| 1.21.4    | 21.4.157    | `neoforge-1.21.4` |

Additional versions will be added as submodules; logic that does not touch
Minecraft classes lives in the version-agnostic `common` module.

## Build

```
./gradlew build
./gradlew :common:check                       # unit tests + 90% JaCoCo gate
./gradlew :neoforge-1.21.1:build              # 1.21.1 jar
./gradlew :neoforge-1.21.4:build              # 1.21.4 jar
./gradlew :neoforge-1.21.1:runClient          # launch dev client
./gradlew :neoforge-1.21.1:runGameTestServer  # in-world tests
```

## License

MIT — see [LICENSE](LICENSE).
