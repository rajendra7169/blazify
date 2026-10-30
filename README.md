<div align="center">

<img src="docs/assets/logo-tight.png" width="120" alt="Blazify logo: a flame with a play button inside headphones" />

# Blazify 🎧🔥

**A free, open-source music player and YouTube Music client. No ads of its own, no tracking, no account needed.**

*Stream it. Feel it. Blaze it.*

**Download it now:** one click, straight from the newest release.

<a href="https://github.com/rajendra7169/blazify/releases/latest"><img src="docs/assets/buttons/get-android.png" width="130" alt="Download for Android" /></a> <a href="https://github.com/rajendra7169/blazify-ios/releases/latest"><img src="docs/assets/buttons/get-iphone.png" width="130" alt="Download for iPhone" /></a> <a href="https://github.com/rajendra7169/blazify-desktop/releases/latest"><img src="docs/assets/buttons/get-windows.png" width="130" alt="Download for Windows" /></a> <a href="https://github.com/rajendra7169/blazify-desktop/releases/latest"><img src="docs/assets/buttons/get-linux.png" width="130" alt="Download for Linux" /></a>

On a phone without Google services (de-Googled, LineageOS, a Huawei), take the
[latest release](https://github.com/rajendra7169/blazify/releases/latest) and take
`Blazify.apk` instead of `Blazify-gms.apk`. It is the same app without casting
to a television. Or get it from
[F-Droid](https://f-droid.org/packages/com.blazify.music/).

**Official website: [rajendra7169.github.io/blazify](https://rajendra7169.github.io/blazify/)** · screenshots, features and downloads for every platform

[![Release](https://img.shields.io/github/v/release/rajendra7169/blazify?color=FFA726&label=release)](https://github.com/rajendra7169/blazify/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/rajendra7169/blazify/total?color=FFA726&label=downloads)](https://github.com/rajendra7169/blazify/releases)
[![Website](https://img.shields.io/badge/website-blazify-FFA726)](https://rajendra7169.github.io/blazify/)
[![WhatsApp](https://img.shields.io/badge/WhatsApp-channel-FFA726?logo=whatsapp&logoColor=1A0E00)](https://whatsapp.com/channel/0029VbDTBZy8F2pEkp0ERZ2Y)
[![Discussions](https://img.shields.io/badge/discussions-ask%20anything-FFA726?logo=github&logoColor=1A0E00)](https://github.com/rajendra7169/blazify/discussions)
[![License](https://img.shields.io/badge/license-GPL--3.0-FFA726.svg)](LICENSE)
![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-FFA726)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-FFA726)

</div>

---

<div align="center">

<a href="docs/assets/screens/screens-1.png"><img src="docs/assets/screens/screens-1.png" width="412" alt="Blazify Home with For you and Quick picks, Search with recent searches, and word-by-word synced lyrics" /></a> <a href="docs/assets/screens/screens-2.png"><img src="docs/assets/screens/screens-2.png" width="412" alt="Yours with Recently Played and Recommended, the Look and Feel hub with a live preview, and Settings" /></a>

<a href="docs/assets/screens/players-1.png"><img src="docs/assets/screens/players-1.png" width="412" alt="Classic, Ring and Full Art player designs" /></a> <a href="docs/assets/screens/players-2.png"><img src="docs/assets/screens/players-2.png" width="412" alt="Record, Cassette and Video Art player designs" /></a>

<sub>Six player designs, each taking its colours from the song that is playing.<br />Tap any screen to see it full size.</sub>

<a href="https://www.youtube.com/watch?v=Q7ooU5S2Wck"><img src="docs/assets/buttons/watch-youtube.png" width="130" alt="Watch the Blazify video on YouTube" /></a>

</div>

---

## Overview

**Blazify is a free, open-source YouTube Music client and music player for Android**,
with versions for iPhone, Windows and Linux. It has no ads of its own, no tracking,
and needs no account. It is an alternative to Spotify and the official YouTube Music
app that plays any song you choose rather than shuffling, saves songs for offline
listening, and keeps playing with the screen off.

It streams from a vast online catalogue, shows word-by-word synced lyrics, has a
ten-band equalizer, and lets you restyle almost every surface of the app with a
live preview while you do it. Signing in with Google is optional and brings your
own YouTube Music library and playlists.

Available for **Android, Windows, Linux and iPhone**. Built with Kotlin and
Jetpack Compose, gapless through Media3/ExoPlayer.

**Looking for something like it?** Blazify fits if you want a Spotify alternative,
a YouTube Music alternative for Android, an open-source music streaming app, or a free
music player with synced lyrics and an equalizer. If you used ViMusic, it will feel
familiar, and if you used InnerTune or Metrolist, Blazify grew from them (see
[Thank you](#thank-you-)).

---

## Blazify everywhere else

The same player, built natively for each platform. The buttons at the top of
this page download any of them. These are the repos:

**Android:** [rajendra7169/blazify](https://github.com/rajendra7169/blazify) *(you are here)*<br>
**Windows & Linux:** [rajendra7169/blazify-desktop](https://github.com/rajendra7169/blazify-desktop)<br>
**iPhone:** [rajendra7169/blazify-ios](https://github.com/rajendra7169/blazify-ios)

Screenshots, install guides and everything else: **[blazify website](https://rajendra7169.github.io/blazify/)**

---

## Features

### 🎵 Playback

- **Streaming**: Millions of songs, albums, artists, playlists and podcasts
- **Gapless audio**: Media3/ExoPlayer with selectable audio quality
- **Radio & autoplay**: Keeps the music going with related tracks after the queue ends
- **Offline downloads**: Save anything for listening without a connection
- **Your own music**: Plays the music already on your phone beside everything streamed, browse by folder, fix badly tagged files
- **Queue control**: Drag to reorder, swipe to remove, shuffle, repeat one/all
- **Don't play this song**: Hide a song from queues, radio, recommendations and Android Auto
- **Double-tap to seek**: On every player design, as far as you choose
- **Podcasts**: Each show remembers its own playback speed
- **Mobile data controls**: Save data on mobile networks, download only on Wi-Fi
- **Quick controls**: Play/pause tile in Quick Settings, plus Liked, Downloads and Shuffle shortcuts
- **Charts & new releases**: Right from the Search tab
- **Audio tools**: Volume normalisation, tempo and pitch control, skip-silence
- **Sleep timer**: Live countdown, plus an end-of-song mode
- **Song recognition**: Identify whatever is playing around you

### 🎚️ Sound

- **10-band equalizer**: Drag each band, with a live frequency-response curve above
- **13 presets**: Rock · Pop · Jazz · Classical · Hip-hop · Electronic · Acoustic · Vocal · Bass boost · Treble boost · Loudness · Podcast · Flat
- **Preamp**: Each preset pre-compensated so boosts never clip
- **Extra effects**: Bass boost, surround and reverb layered on the parametric EQ
- **AutoEQ**: Import a profile, or run the wizard to fetch one for your headphones
- **Output switching**: Move audio to speaker, wired, USB or Bluetooth from the player

### 🎨 Look & Feel

- **Live preview hub**: Five tabs (Theme, Player, Mini-player, Lyrics, Home) previewed in a phone frame
- **Dynamic theming**: Colours follow your album art, automatically
- **Custom colours**: Or pick your own: saturation/value field, hue rail, hex entry
- **Pure black**: True black dark mode for OLED panels
- **6 player designs**: Classic · Ring · Full Art · Record · Cassette · Video Art
- **3 seek-bar styles**: Capsule · Wavy · Slim
- **Navigation styles**: Four nav-bar looks, plus a configurable home header

### 📖 Lyrics

- **Synced lyrics**: Word-by-word highlighting from multiple providers
- **Provider priority**: Choose which sources are tried, and in what order
- **Translation**: Read along in your own language
- **Romanization**: Sing along to any script
- **Styling**: Size, spacing, alignment, glow and animation

### 👥 Social & library

- **Listen Together**: Share a room code and play in sync with friends
- **Playlist import**: Bring playlists in from a file or a link
- **last.fm**: Scrobble what you play
- **Android Auto**: Full in-car playback
- **Widgets**: Home-screen controls
- **Backup & restore**: Your whole library in one file: songs, playlists, history, sign-in
- **In-app updates**: Tells you when a new version is out, downloads it, and installs it. Want new things early? Turn on beta updates

---

## Roadmap

What is planned, what has been asked for, and what is deliberately not being done: **[ROADMAP.md](ROADMAP.md)**. Vote on what comes next in [Ideas](https://github.com/rajendra7169/blazify/discussions/categories/ideas), or pick one in the [poll](https://github.com/rajendra7169/blazify/discussions/17).

---

## Install

Open the **[latest release](https://github.com/rajendra7169/blazify/releases/latest)** and take `Blazify-gms.apk`, the everyday build, with casting to a television included.
On a phone with no Google services, take `Blazify.apk` from that same release instead.
Every build lives on the **[Releases](https://github.com/rajendra7169/blazify/releases/latest)** page.

Requires **Android 8.0 (API 26)** or newer. New install? Just open the APK, nothing else to do.
It runs on ARM phones and tablets, and on Intel and AMD devices too, such as Waydroid and Android emulators.

### Automatic updates with Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) watches this repository and
installs each new release for you.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.blazify.music%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Frajendra7169%2Fblazify%22%2C%22author%22%3A%22rajendra7169%22%2C%22name%22%3A%22Blazify%22%7D"><img src="docs/assets/buttons/store-obtainium.png" width="130" alt="Get it on Obtainium" /></a>

Tap that on your phone, or add `https://github.com/rajendra7169/blazify` in
Obtainium by hand. Blazify updates itself too; this is for people who prefer
one app managing everything they sideload.

### F-Droid

Blazify is on [F-Droid](https://f-droid.org/packages/com.blazify.music/), built
from this source code and checked against the published release. New versions
reach it a few days after they appear here. The F-Droid build has no casting to
a television, and F-Droid updates it for you.

<a href="https://f-droid.org/packages/com.blazify.music/"><img src="docs/assets/buttons/store-fdroid.png" width="130" alt="Get it on F-Droid" /></a>

### Also on app stores

GitHub Releases above is the official source and always has the newest
version first. The same app is also listed here:

<a href="https://apkpure.com/blazify/com.blazify.music"><img src="docs/assets/buttons/store-apkpure.png" width="130" alt="Get it on APKPure" /></a> <a href="https://appteka.store/app/b16r317210"><img src="docs/assets/buttons/store-appteka.png" width="130" alt="Get it on Appteka" /></a> <a href="https://blazify.apk.com/"><img src="docs/assets/buttons/store-apkcom.png" width="130" alt="Also at blazify.apk.com" /></a>

---

### Coming from 9.12.3 or earlier

9.12.4 is signed with a new key, so Android will not install it over an
older build. You have to back up, uninstall, then reinstall. Two minutes,
steps here: **[MIGRATION.md](MIGRATION.md)**.

---

## Thank you 🙏

Blazify stands on the foundation of remarkable open-source projects and the talented people behind them.
Their contributions inspire us to build with the same spirit of collaboration, innovation, and openness.

**Main inspirations**

| Project | Authors |
|---|---|
| **InnerTune** | [Zion Huang](https://github.com/z-huang) · [Malopieds](https://github.com/Malopieds) |
| **OuterTune** | [Davide Garberi](https://github.com/DD3Boh) · [Michael Zh](https://github.com/mikooomich) |
| **Metrolist** | [Mo Agamy](https://github.com/mostafaalagamy) · [Nyx](https://github.com/nyxiereal) |

## Libraries & Integrations

<details>
<summary><b>Projects Blazify builds on</b></summary>

| Project | Contribution |
|---|---|
| **Better Lyrics** | Time-synced lyrics with word-by-word highlighting & YouTube Music integration |
| **LRCLIB · KuGou** | Additional synced-lyrics sources |
| **metroserver** | Listen-together real-time backend |
| **MusicRecognizer** | Music recognition feature & Shazam API integration |
| **BlazifyExtractor** | YouTube cipher deobfuscation and PoToken generation |
| **last.fm** | Scrobbling and listening history |

</details>

## Tech

<details>
<summary><b>Built with</b></summary>

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Playback | Media3 / ExoPlayer |
| DI | Hilt |
| Database | Room |
| Images | Coil |
| Async | Coroutines + Flow |

</details>

---

## Community

**[Join the Blazify channel on WhatsApp](https://whatsapp.com/channel/0029VbDTBZy8F2pEkp0ERZ2Y)**.
Every release is announced there first, with the download link. It is one-way,
so there is no group chat to mute, and nobody's phone number is shown to anyone.

<div align="center">

<img src="docs/assets/whatsapp-channel-qr.png" width="200" alt="QR code to follow the Blazify channel on WhatsApp" />

**Scan to follow the channel**

</div>

**[Discussions](https://github.com/rajendra7169/blazify/discussions)**: ask how
something works, or say what you would like built next. Local music support, the
tag editor and folder browsing were all added because someone asked for them.
Every planned idea has its own post in
[Ideas](https://github.com/rajendra7169/blazify/discussions/categories/ideas): upvote the
ones you want most.

**[Issues](https://github.com/rajendra7169/blazify/issues)**: for bugs, feature ideas
and UI/UX suggestions, each with a short form. Or use **Settings → Report a problem**
inside the app: it fills in your app version and phone for you, which is usually the
difference between a bug that gets fixed and one that cannot be reproduced.

---

## Disclaimer

This project is not affiliated with, funded, authorized, endorsed by, or in any
way associated with YouTube, Google LLC, or any of their affiliates and
subsidiaries.

All trademarks, service marks, and intellectual property rights referenced in
this project belong to their respective owners.

---

## Support ☕

<details>
<summary><b>Buy me a coffee</b></summary>

If Blazify made your day a little better, you can buy me a coffee.

<div align="center">

<img src="docs/assets/coffee-qr.png" width="200" alt="QR code to support Blazify" />

**Scan to support Blazify**

</div>

</details>

---

## License

Released under the **[GNU General Public License v3.0](LICENSE)**. See [NOTICE](NOTICE).

If you distribute this app or a build of it, GPL-3.0 asks you to pass on the
same freedoms: keep the licence, keep the notices, and make the source available.

---

<div align="center">

Made with ❤️ by **Rajendra Pandey**

</div>
