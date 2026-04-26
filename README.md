# Lockers

A NeoForge Minecraft mod that mirrors the Locker feature from the game **Rust**:
place a Locker block, save up to **6 loadouts** of armor + offhand, and
instantly swap between them.

- **Owner-only** access by default; server configurable to public or ops-bypass.
- Loadouts persist per-block across chunk unloads and server restarts.
- Full **Curios** and **Accessories** API support — auto-detected; server
  config picks one if both are installed (`AUTO` / `CURIOS` / `ACCESSORIES` /
  `NONE`). Slots are saved and restored alongside armor + offhand.

## Supported versions

| Minecraft | NeoForge       | Java | Module            | Status                     |
| --------- | -------------- | ---- | ----------------- | -------------------------- |
| 1.21.1    | 21.1.228+      | 21   | `neoforge-1.21.1` | stable (v0.1.0+)           |
| 1.21.4    | 21.4.157+      | 21   | `neoforge-1.21.4` | stable (v0.1.0+)           |
| 26.1.2    | 26.1.2.29-beta | 25   | `neoforge-26.1.2` | **alpha** — see CHANGELOG  |

Additional versions are added as submodules; logic that does not touch
Minecraft classes lives in the version-agnostic `common` module.

## Build

```
./gradlew build
./gradlew :common:check                       # unit tests + 90% JaCoCo gate
./gradlew :neoforge-1.21.1:build              # 1.21.1 jar
./gradlew :neoforge-1.21.4:build              # 1.21.4 jar
./gradlew :neoforge-26.1.2:build              # 26.1.2 jar (Java 25)
./gradlew :neoforge-1.21.1:runClient          # launch dev client
./gradlew :neoforge-1.21.1:runGameTestServer  # in-world tests
```

## License

MIT — see [LICENSE](LICENSE).
