# Credits & Licenses — Strummer

Strummer bundles only license-clean components. This file documents every
third-party dependency and the origin of the app's audio content.

## Audio

- **Oboe** (Google) — low-latency audio I/O. Apache License 2.0.
- **Guitar sound** — generated on-device using **Karplus-Strong** physical
  modeling of a plucked string. No sampled audio and no third-party soundfont is
  bundled in this build, so there is no sample-licensing exposure.
- **TinySoundFont** (© Bernhard Schelling) — MIT license. The single-header SF2
  synth is bundled (source header only) for a planned future sampled-tone
  upgrade. Not active in the current build.

## Chord data

Guitar chord fingerings (open shapes + moveable E/A-shape barre forms) are
standard, factual musical data authored for this app. They are not derived from,
or copied out of, any copyrighted chord database.

## Frameworks

- Kotlin, Jetpack Compose (Material 3), AndroidX Lifecycle — Apache License 2.0.
- Dagger Hilt — Apache License 2.0.
- AndroidX Room, DataStore — Apache License 2.0.

## Note for a future sampled-tone (SF2) upgrade

If/when a multisampled guitar SF2 is added for richer tones, it MUST be
license-clean (CC0 / CC-BY / public domain or explicitly permissive), and this
file plus the in-app Credits screen must be updated with its name, author, and
license before shipping.
