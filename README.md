# Blazify 🔥

**Blazify** is a modern, feature-rich music streaming player for Android, built
entirely with Kotlin and Jetpack Compose. It combines a vast online music
catalog with a fast, elegant interface, offline listening, synced lyrics, and
deep personalization.

> Stream it. Feel it. Blaze it.

---

## ✨ Features

### 🎵 Streaming & Playback
- **Massive online catalog** — search and stream millions of songs, albums,
  artists, playlists, and podcasts
- **Gapless, high-quality audio** powered by Media3/ExoPlayer with configurable
  audio quality
- **Radio & autoplay queues** — start a song and Blazify keeps the music going
  with related tracks
- **Full queue management** — reorder by drag, swipe to remove, shuffle,
  repeat-one/all, and a persistent queue that survives restarts
- **Audio normalization, tempo and pitch controls, skip-silence** for a tailored
  listening experience
- **Android Auto** support for in-car playback

### 🔍 Discovery
- **Personalized home** — quick picks, mood chips (Energize, Relax, Feel good,
  Workout), fresh releases, charts, and listening-history-based suggestions
- **Powerful search** with suggestions, filters (songs / albums / artists /
  playlists), and both online and local-library results
- **Song recognition** — a built-in recognizer (tap the mic in the search bar)
  identifies music playing around you and takes you straight to it

### 📚 Library & Offline
- **Full library management** — like songs, follow artists, save albums and
  playlists, all synced with your account
- **Offline downloads** — save any song, album, or playlist for playback
  without a connection
- **Local playlist tools** — create, rename, reorder, import/export (M3U/CSV)
- **Listening history and detailed stats** — most played songs, artists,
  albums, and a yearly recap

### 🎤 Lyrics
- **Synced lyrics** with word-by-word or line-by-line highlighting from
  multiple providers, with automatic fallback
- **Lyrics offset adjustment, romanization** (CJK and Cyrillic), and a
  full-screen immersive lyrics view
- **Shareable lyric cards** — turn a lyric into a stylized image

### 🎨 Design & Personalization
- **Signature Blazify look** — amber-to-orange gradient identity, greeting
  card home header, pure-black dark theme for OLED displays
- **Dynamic theming** — the interface recolors itself from the current song's
  album artwork in real time
- **Player styles** — gradient or blurred artwork backgrounds, multiple slider
  styles, configurable player buttons
- **Home-screen widgets** — multiple sizes and styles, plus a recognizer widget
- **60+ interface languages**

### ⏰ Smart Extras
- **Sleep timer** — preset chips (15/30/45/60 min), custom duration, live
  countdown, and an *end-of-song* mode that stops after the current track
- **Listen Together** — real-time shared listening sessions with friends
- **Scrobbling** — Last.fm integration with love/unlove sync
- **Discord Rich Presence** — show friends what you're playing
- **Backup & restore** — take your whole library and settings to a new device

---

## 📱 Screenshots

| Home | Player | Lyrics |
|---|---|---|
| _coming soon_ | _coming soon_ | _coming soon_ |

---

## 📦 Download

Grab the latest APK from the [Releases](../../releases) page.
Each release ships with proper release notes describing what changed.

> Blazify is distributed as a direct APK. It is not available on Google Play.

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin (JVM 21) |
| UI | Jetpack Compose + Material 3 (expressive) |
| Playback | AndroidX Media3 / ExoPlayer |
| DI | Hilt |
| Persistence | Room (38 schema versions), DataStore preferences |
| Networking | Ktor client |
| Images | Coil |
| Realtime | Protocol Buffers over WebSocket (Listen Together) |

## 🏗 Architecture

```
blazify/
├── app/            # Application: UI (Compose screens/components), playback
│   │               # service, database, DI, widgets, viewmodels
│   ├── ui/         #   screens, player, components, menus, theme
│   ├── playback/   #   MusicService (Media3), PlayerConnection, queues
│   ├── db/         #   Room database, DAOs, entities
│   └── di/         #   Hilt modules
├── innertube/      # Online music catalog client (search, browse, streams)
├── betterlyrics/   # Lyrics provider
├── lrclib/         # Synced-lyrics provider
├── kugou/          # Lyrics provider
├── lastfm/         # Scrobbling client
├── shazamkit/      # Song recognition
├── paxsenix/       # Auxiliary API provider
└── proto/          # Protobuf definitions (Listen Together)
```

The UI layer observes state exposed by Hilt viewmodels and a `PlayerConnection`
bridge to the Media3 service; all remote access flows through the `innertube`
module. Stream resolution is handled by
[BlazifyExtractor](https://github.com/rajendra7169/BlazifyExtractor).

## 🔨 Building from Source

Requirements: **JDK 21**, Android SDK 37.

```bash
git clone https://github.com/rajendra7169/blazify.git
cd blazify
./gradlew :app:assembleFossRelease
```

The APK is produced at `app/build/outputs/apk/foss/release/`.

Build flavors: `foss` (default), `gms` (Google Cast support), `izzy`.

## 🗺 Roadmap

- [ ] Release signing & first public release APK
- [ ] Screenshot gallery
- [ ] More home personalization (rails, moods)
- [ ] Equalizer presets

---

*Made with ❤️ by Rajendra Pandey*
