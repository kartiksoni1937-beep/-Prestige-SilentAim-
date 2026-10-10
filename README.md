# Prestige SilentAim

A client-side Fabric mod project for Minecraft Java Edition 1.21, with PojavLauncher compatibility as a testing goal.

## Project status

Initial repository setup. The first milestone is a clean, reproducible Java 21 + Fabric build that can be tested in a local world.

## Development scope

- Client-side Fabric mod structure and build configuration
- Configurable client UI and keybinds
- Rotation and targeting diagnostics for local testing
- Compatibility checks for Minecraft 1.21 and PojavLauncher

This project does not implement anti-cheat evasion or hidden combat automation for public servers.

## Build

Use JDK 21 and Gradle. Once the Gradle project files are added, run:

```bash
./gradlew build
```

The mod JAR will be produced in `build/libs/`.

## Testing

Test changes in a local single-player world or a server where you have permission to experiment. Keep client logs when reporting crashes.
