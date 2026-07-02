# Strummer — Project Brief

> Global rules: **personal/hobby project** → apply global rules lightly (clean
> architecture, concise, recommend-with-reason, useful CLAUDE.md). Skip heavy
> ceremony. Package is personal namespace (`geo.*`), NOT employer.

## Mission
The most playable, best-sounding virtual guitar on Android — genuinely fun to
strum, with realistic chord-rake timing and clean UX (the anti-Real Guitar).
App #4 in the suite (piano → drums → tuner → guitar). Built strictly **phase by
phase**; stop and report against each phase's acceptance criteria before the next.

## Stack
- Kotlin · Jetpack Compose (Material 3) · MVVM + Clean Architecture · Hilt
- Native audio: **Oboe** (LowLatency + Exclusive + Float) + lock-free SPSC queue
- Synth: **Karplus-Strong** physically-modeled plucked strings (Phase 0); SF2 via
  **TinySoundFont** planned for Phase 1+ (header already bundled in cpp/)
- minSdk 26 · targetSdk/compileSdk 36 · NDK 27.1.12297006 · CMake 3.22.1
- Oboe 1.9.3 as prefab AAR; `buildFeatures.prefab = true` + `-DANDROID_STL=c++_shared`

## Build & run
```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.15/libexec/openjdk.jdk/Contents/Home
cd ~/AndroidStudioProjects/Strummer
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```
- **Audio MUST be tested on a real device** — emulators lie about latency.

## Architecture
- `presentation/` — Compose + ViewModels (theme, Phase 0 test screen)
- `domain/audio/` — `GuitarEngine` interface + `AudioDiagnostics` (pure Kotlin)
- `domain/guitar/model/` — `GuitarString` enum (EADGBE standard tuning)
- `data/audio/` — `NativeGuitarEngine` (impl) + `GuitarEngineBridge` (JNI)
- `di/` — `AudioModule` binds interface → native impl
- `cpp/audio/` — the real-time engine:
  - `GuitarEngine.*` — owns Oboe stream, renders 6 KS strings, stereo panning
  - `KarplusStrong.h` — plucked-string synthesis (delay-line + lowpass feedback)
  - `RingBuffer.h` — lock-free SPSC queue (UI thread → audio thread)
- `cpp/native-lib.cpp` — JNI entry points (`GuitarEngineBridge`)
- `cpp/tsf.h` — TinySoundFont header (gitignored, downloaded separately; Phase 1)

### The one rule that matters most
**Never allocate, lock, log, or call into the JVM inside the Oboe callback.**
UI pushes commands into the ring buffer; callback drains them per render block.

## Key design decisions
- **Karplus-Strong over SF2 for Phase 0**: KS is the standard algorithm for
  plucked-string synthesis — it naturally models guitar timbre without needing
  external sample files. SF2 will augment/replace it in Phase 1 for richer tones.
- **Per-string monophony**: each of the 6 strings has its own KS delay line.
  Re-plucking a string cuts the previous note (just like a real string).
- **Stereo panning**: strings are subtly panned low-E left → high-E right.

## Resume here (current status — 2026-07-02)

**Phase 1 — Chord system + strum area — CODE COMPLETE, builds.**

Added on top of Phase 0's KS engine:
- **ChordLibrary**: hand-authored open shapes (C, D, E, G, A major/minor/7th/sus)
  + moveable barre templates (E-shape, A-shape) that generate chords across all
  12 keys for 7 chord types. Prefers open shapes over barre equivalents.
- **StrumEngine**: converts voicing + direction + swipe speed into timed
  per-string pluck events with correct rake timing (3–16ms per string, speed-
  dependent). Down-strum = low→high, up-strum = high→low. Muted strings
  contribute to rake timing but don't sound.
- **StrumScreen**: chord palette (8 campfire chords in a 4×2 grid, tap to select)
  + strum area (swipe vertically to strum, tap to pick individual strings).
  Shows string names, fret numbers, muted indicators. Diagnostics bar at bottom.
- **StrumViewModel**: wires chord selection → strum events → timed engine calls
  via coroutine delay (adequate for rake timing; sample-accurate scheduling
  deferred to Phase 2 patterns).

### Pending before Phase 2 starts
1. **On-device verification** — strum feel is the make-or-break: does the rake
   timing sound like a real strum, not six simultaneous notes? Is switching
   chords + strumming responsive and musical?
2. **Source a guitar SF2** — still needed for richer tones in Phase 3.
3. GP review → sign off Phase 1.

## Phase roadmap
- **Phase 0** — Scaffold + KS engine + 6 tappable strings. ✓ Done.
- **Phase 1 (CURRENT)** — Chord system + strum area with rake timing. ✓ Code complete.
- **Phase 2** — Auto-strum & fingerpicking patterns (scheduler-driven).
- **Phase 3** — Tones (SF2 acoustic/nylon/electric), fretboard/solo mode, capo, palm mute.
- **Phase 4** — Chord palette builder, recording & export.
- **Phase 5** — Onboarding, monetization, OEM hardening, release prep.

## Content & licensing
- TinySoundFont: MIT license (bundled as header; gitignored, re-download if needed)
- Guitar SF2: TBD — must be license-clean; document in CREDITS.md + in-app
- Chord fingerings: factual/computable data, not copyrighted
- No copyrighted song tabs/chord sheets
