# jEvents

`jEvents` is a dual-loader event administration mod with shared logic for Forge and Fabric.

## Current Build Target

- Minecraft: `1.16.5`
- Loaders: `Forge` and `Fabric`

## Implemented Systems

- Runtime-toggle hardcore rules:
  - Death -> spectator mode, or
  - Death -> event ban + disconnect
- SG spawn point set/clear + teleport-all in a ring formation
- Custom event ban list
- Chat mute with trusted chat whitelist
- Lifesteal hearts on PvP kill
- Last Breath protection list (death prevention)
- Mod Mode:
  - invisibility
  - flight + custom flight speed
- Inventory snapshot on death + restore command
- Live mob spawn rate controls:
  - global multiplier
  - per-player multiplier
  - dynamic scaling by online player count
- Held item renamer

## Command Root

All features are under:

- `/jevents ...`

Use `/jevents status` for live state output.

## Dual-Loader Architecture

- `common/` -> shared gameplay logic, commands, data
- `forge/` -> Forge bootstrap + metadata
- `fabric/` -> Fabric bootstrap + metadata

This repo is intentionally structured so feature logic stays in `common/`, and loader-specific code stays minimal.

## Version Range Strategy (1.16.5 -> Latest)

A single jar cannot reliably run every MC version from `1.16.5` to latest because Mojang/Fabric/Forge APIs change between versions.

Use this repository as the `1.16.5` baseline, then maintain one branch per supported MC line, for example:

- `mc/1.16.5`
- `mc/1.20.1`
- `mc/latest`

Keep command/data/business logic aligned across branches by porting only API-facing differences (imports, event signatures, metadata, and dependency versions).
