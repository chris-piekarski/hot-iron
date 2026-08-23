# Java 21 + FlatLaf UI modernization

**Status:** done 2026-08-18. Java 21 + FlatDarkLaf. `make test` 119/119, `make test-hw` 7/7.  
**Choice:** Java 21 LTS compile/runtime floor, keep Swing, FlatLaf dark so controls match the chart.

## Goal

- Compile and run as **Java 21** (`--release 21`).
- Bump UI/native-bridge libraries to current patch/minor releases.
- Install **FlatLaf 3.7.2** dark before any Swing component is created.
- Drop the **OpenJDK 8 / JNAerator** bridge-generation path.
- Keep `make test` radio-free and green; re-run `make test-hw` on the attached One.

## Non-goals

- JavaFX rewrite.
- Mass `var` / records rewrite of existing Java 8 style.
- Bundled JRE / jlink image.
- JUnit 6 (stay on Jupiter 5.13.x).
- Changing HackRF SDK pin (already v2026.01.3).

## Checklist

- [x] `pom.xml`: `--release 21`, plugin and library bumps, FlatLaf 3.7.2
- [x] `AnalyzerLookAndFeel.install()` before any widget; tests get it via constructors
- [x] Strip per-widget `Color.BLACK` / `Color.WHITE` on standard controls
- [x] Leave chart / waterfall / persistent palettes as custom-painted
- [x] Hand-maintain `HackrfSweepLibrary`; remove the obsolete `jnabridge` target
- [x] Linux and Windows launchers require Java 21 and a headful AWT
- [x] `make deps` installs `openjdk-21-jdk`
- [x] Docs / AGENTS.md / CHANGELOG
- [x] `make test` 119/119, `make test-hw` 7/7, live FlatLaf UI on JDK 21

## Pins

| Piece | After |
|---|---|
| Compiler | `--release 21` |
| maven-compiler-plugin | 3.15.0 |
| maven-surefire-plugin | 3.5.3 |
| JaCoCo | 0.8.15 |
| JFreeChart | 1.5.6 |
| MigLayout | 11.4.3 |
| JNA | 5.19.1 |
| JUnit | 5.13.4 |
| Look and feel | FlatDarkLaf 3.7.2 |
