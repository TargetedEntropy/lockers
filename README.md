# Lockers

A NeoForge Minecraft mod that mirrors the Locker feature from the game **Rust**:
place a Locker block, save up to **6 named loadouts** of armor + accessories,
and instantly swap between them.

- Full **Curios** and **Accessories** API support (auto-detected, with a
  server-config override if both are installed).
- **Owner-only** access by default; server configurable to public.
- Loadouts persist per-block across chunk unloads and server restarts.

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
