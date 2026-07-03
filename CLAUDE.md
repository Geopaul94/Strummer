# Strummer — Project Brief

> Global rules: **personal/hobby project** → apply global rules lightly (clean
> architecture, concise, recommend-with-reason, useful CLAUDE.md). Skip heavy
> ceremony. Package is personal namespace (`geo.*`), NOT employer.

## Mission
The most playable, best-sounding virtual guitar on Android — genuinely fun to
strum, with realistic chord-rake timing and clean UX (the anti-Real Guitar).
App #4 in the suite (piano → drums → tuner → guitar).

## Stack
- Kotlin · Jetpack Compose (Material 3) · MVVM + Clean Architecture · Hilt
- Native audio: **Oboe** (LowLatency + Exclusive + Float) + lock-free SPSC queue
- Synth: **Karplus-Strong** physically-modeled plucked strings (no SF2 bundled;
  TinySoundFont header is in cpp/ for a future sampled-tone upgrade)
- Persistence: **Room** (saved palettes) + **DataStore** (onboarding flag)
- minSdk 26 · targetSdk/compileSdk 36 · NDK 27.1.12297006 · CMake 3.22.1
- Oboe 1.9.3 prefab AAR; `buildFeatures.prefab = true` + `-DANDROID_STL=c++_shared`

## Build & run
```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.15/libexec/openjdk.jdk/Contents/Home
cd ~/AndroidStudioProjects/Strummer
./gradlew :app:assembleDebug     # debug APK
./gradlew :app:assembleRelease   # R8 + shrink, per-ABI APKs (~2.2-2.8 MB each)
./gradlew :app:bundleRelease     # AAB for Play (6.1 MB, splits per-device)
./gradlew :app:installDebug      # install to a connected device
```
- **Audio MUST be tested on a real device** — emulators lie about latency.

## Architecture (domain has no Android imports)
- `presentation/`
  - `shell/` — `StrummerShell` (bottom-nav: Strum/Patterns/Solo/Record/Settings) +
    `ShellViewModel` (owns engine lifecycle: start on create, shutdown on finish)
  - `strum/` — hero: chord palette + swipe-to-strum area (`StrumScreen`/VM)
  - `patterns/` — auto-strum + fingerpicking selector, BPM, play/stop
  - `fretboard/` — solo/lead single-note fretboard (6×13, tap to play)
  - `recording/` — record WAV, playback (MediaPlayer), share (FileProvider)
  - `settings/` — tone/capo/palm-mute + entry to palette builder + credits
  - `palette/` — chord picker → build/save/load custom palettes (Room)
  - `onboarding/` — one-time intro gate (DataStore-persisted)
  - `common/` — shared `ChordPaletteGrid`
  - `theme/` — Material 3 warm/woody palette
- `player/GuitarSession` — **the coordinator**: single @Singleton that owns the
  engine + all shared state (palette, tone, capo, palm mute, pattern, bpm,
  recording), pattern playback (coroutine), strum rake scheduling, audio focus.
  Every ViewModel is thin and delegates here.
- `domain/audio/` — `GuitarEngine` interface + `AudioDiagnostics`
- `domain/guitar/` — `ChordLibrary`, `StrumEngine`, `PatternLibrary` + models
  (`Chord`, `Voicing`, `ChordType`, `NoteName`, `Tone`, `StrumPattern`, …)
- `domain/billing/` — `EntitlementRepository` interface (Pro gating, no ads)
- `data/audio/` — `NativeGuitarEngine` + `GuitarEngineBridge` (JNI) + `WavFileWriter`
- `data/palette/` — Room entity/DAO/DB + `PaletteRepository`
- `data/prefs/` — `OnboardingPrefs` (DataStore)
- `data/billing/` — `StubEntitlementRepository` (free tier, swap for real billing)
- `di/` — `AudioModule` (engine + entitlement binds), `DataModule` (Room)
- `cpp/audio/` — real-time engine:
  - `GuitarEngine.*` — Oboe stream, 6 KS strings, stereo pan, tone/palm-mute,
    lock-free recording FIFO drained to Kotlin
  - `KarplusStrong.h` — plucked-string synth (delay-line + lowpass; brightness +
    palm-mute output envelope)
  - `RingBuffer.h` — lock-free SPSC queue
- `cpp/native-lib.cpp` — JNI entry points (`GuitarEngineBridge`); `cpp/tsf.h`
  (TinySoundFont header, gitignored)

### The one rule that matters most
**Never allocate, lock, log, or call into the JVM inside the Oboe callback.**
UI pushes commands into the ring buffer; the callback drains per render block.
Recording pushes output samples into a second lock-free FIFO; Kotlin drains it.

## Key design decisions
- **Karplus-Strong, not SF2**: KS is the standard plucked-string algorithm and
  needs no external sample files (all attempts to fetch a license-clean guitar
  SF2 failed). Tones are KS brightness presets (nylon/acoustic/electric). A real
  multisampled SF2 can slot in later behind the same `Tone` enum.
- **One session owns the engine**: avoids multiple screen ViewModels fighting
  over the singleton engine's start/stop/release.
- **Strum rake** (the signature feel): `StrumEngine` spreads a chord's strings
  by 3–16 ms each (swipe-speed dependent); down = low→high, up = high→low.
  Scheduled via coroutine delay (fine at these tiny offsets).
- **Recording** captures the mixed stereo output into a native FIFO, drained by a
  coroutine into a 16-bit PCM WAV (`WavFileWriter`).

## Status — ALL PHASES CODE-COMPLETE (2026-07-02), builds debug + release + AAB
- **Phase 0** — KS engine, 6 strings, per-string monophony. ✓
- **Phase 1** — Chord library (open + barre, all keys) + strum area w/ rake. ✓
- **Phase 2** — Auto-strum + fingerpicking patterns (folk/pop/ballad/rock,
  Travis/arpeggio), BPM, live chord change. ✓
- **Phase 3** — Tones (acoustic/nylon/electric), capo, palm mute, fretboard solo. ✓
- **Phase 4** — Chord palette builder (Room save/load) + WAV recording/share. ✓
- **Phase 5** — Onboarding, `EntitlementRepository` stub (no ad walls), audio-focus
  handling, R8 + resource shrink, AAB, `CREDITS.md`, `PRIVACY.md`. ✓

### Pending (needs a real device — all untested on hardware)
1. **On-device verification of feel** — especially strum rake realism, latency,
   xruns under load, tone differences, recording round-trip, audio-focus on calls.
2. **Guitar SF2** — optional quality upgrade for Phase 3 tones if KS isn't enough.
3. **Real billing/ads** — wire Play Billing + AdMob behind the existing interfaces.
4. Signing config for a real release (currently unsigned release artifacts).

### Legacy / dead code
- `presentation/strum/Phase0Screen.kt` + `Phase0ViewModel.kt` — the throwaway
  Phase 0 test screen, no longer wired into the shell. Safe to delete (kept only
  because of the "ask before deleting" rule).

## Content & licensing
- No SF2 bundled → no sample-license exposure. Oboe = Apache-2.0. TinySoundFont
  header = MIT. Chord fingerings = factual data. Documented in `CREDITS.md` +
  in-app Credits screen. If an SF2 is added later it MUST be license-clean.

## Gotchas
- Pin JDK 17 for Gradle (Homebrew java is too new).
- Manual `splits { abi }` conflicts with `bundleRelease` ("Sequence contains more
  than one matching element") — omitted; the AAB splits per-ABI automatically.
- Material icons beyond the core set aren't available without
  `material-icons-extended`; the nav bar uses text glyphs instead.
