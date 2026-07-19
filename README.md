<div align="center">

<img src="docs/assets/logo.png" alt="Blazify" width="120" />

# Blazify 🔥

**A modern music streaming player for Android — Kotlin, Jetpack Compose, Material 3.**

*Stream it. Feel it. Blaze it.*

[![Release](https://img.shields.io/github/v/release/rajendra7169/blazify?color=FFA726&label=release)](https://github.com/rajendra7169/blazify/releases/latest)
[![License](https://img.shields.io/badge/license-GPL--3.0-FFA726.svg)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-FFA726)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-FFA726)

</div>

---

<div align="center">
<img src="docs/assets/home.png" width="200" />
<img src="docs/assets/player.png" width="200" />
<img src="docs/assets/look-and-feel.png" width="200" />
<img src="docs/assets/splash.png" width="200" />
</div>

---

## What it does

Blazify streams from a vast online catalogue, plays gapless with Media3/ExoPlayer,
shows word-by-word synced lyrics, and lets you restyle almost every surface —
with a live preview while you do it. No ads, no tracking.

## Features

### 🎵 Streaming & playback
- Search and stream millions of songs, albums, artists, playlists and podcasts
- Gapless playback with configurable audio quality
- Radio and autoplay queues that keep going after the song ends
- Offline downloads with a persistent cache
- Full queue management — drag to reorder, swipe to remove, shuffle, repeat
- Audio normalisation, tempo and pitch control, skip-silence
- Sleep timer with a live countdown, plus an end-of-song mode
- Song recognition — identify what's playing around you
- Android Auto and home-screen widgets

### 🎚️ Sound
- **Ten-band equalizer** with a live frequency-response curve
- **13 built-in presets** — Rock, Pop, Jazz, Classical, Hip-hop, Electronic,
  Acoustic, Vocal, Bass boost, Treble boost, Loudness, Podcast, Flat
- Preamp control, with each preset pre-compensated so boosts never clip
- **Bass boost, surround and reverb** layered on top of the parametric EQ
- Import AutoEQ profiles, or run the headphone wizard to fetch one
- **Audio output switching** — speaker, wired, USB or Bluetooth, from the player

### 🎨 Look & Feel
- A dedicated hub with a **live phone-frame preview** across five tabs
- **Dynamic theming** that follows your album art, or pick any accent —
  including a full custom colour picker (saturation/value field, hue rail, hex)
- Pure-black dark mode for OLED panels
- **Five player layouts** — Classic, Ring, Full art, Record, Cassette
- **Four seek-bar styles** — Capsule, Wavy, Slim, Squiggly
- Mini-player designs, four navigation-bar styles, configurable home header

### 📖 Lyrics
- Word-by-word synced lyrics from multiple providers, with priority ordering
- Translation and romanization for singing along in any language
- Adjustable size, spacing, alignment, glow and animation style

### 👥 Social & library
- **Listen Together** — share a room code and play in sync with friends
- Playlist import, last.fm scrobbling, library sync

---

## Install

Download the latest APK from **[Releases](https://github.com/rajendra7169/blazify/releases/latest)**.

Requires **Android 8.0 (API 26)** or newer.

> Signed with the project's own release key. If you have an earlier build that
> came from a different key, uninstall it before installing this one.

## Build from source

```bash
git clone https://github.com/rajendra7169/blazify.git
cd blazify
./gradlew :app:assembleFossRelease
```

Requires **JDK 21**. Output lands in `app/build/outputs/apk/foss/release/`.

Three flavours are available: `foss` (default), `gms` (adds Google Cast) and `izzy`.

## Tech

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Playback | Media3 / ExoPlayer |
| DI | Hilt |
| Database | Room |
| Images | Coil |
| Async | Coroutines + Flow |

---

## License

Blazify is released under the **[GNU General Public License v3.0](LICENSE)**.

The user interface, theming, branding and feature work are original. The
streaming core derives from [Metrolist](https://github.com/MetrolistGroup/Metrolist),
itself derived from InnerTune — both GPL-3.0. See [NOTICE](NOTICE) for details.

If you distribute this app or a build of it, GPL-3.0 asks you to pass on the
same freedoms: keep the licence, keep the notices, and make the source available.
