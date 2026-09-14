# 🔥 Blazify Roadmap

What's coming next, what's being considered, and how you can decide what gets
built. Blazify is shaped by the people who use it, so your votes and ideas
really do change the order.

---

## 🗳️ Have your say

- **Vote.** Every idea below has its own post in
  [Discussions › Ideas](https://github.com/rajendra7169/blazify/discussions/categories/ideas).
  Press the ⬆️ upvote arrow on the ones you want most. The most wanted get built first.
- **Suggest.** Something missing? Open a
  [feature request](https://github.com/rajendra7169/blazify/issues/new?template=feature_request.yml)
  or a [UI/UX suggestion](https://github.com/rajendra7169/blazify/issues/new?template=ui_ux_suggestion.yml).
- **Report.** Found something broken? A
  [bug report](https://github.com/rajendra7169/blazify/issues/new?template=bug_report.yml)
  takes a minute, and only one box is required.

---

## 🚧 Coming in the next release

Already built and being tested on real phones.

**Playback you can count on**
- Songs start faster and fail less often. Blazify keeps its own up-to-date copy
  of what YouTube's player needs, fetches a fresh link when a stream expires,
  and moves on from a song that loads but never plays.
- Where you were is saved every few seconds, and shuffle stays on after the app is closed.
- Opening a song link plays that song straight away instead of loading your old queue first.
- When a song can't play, the error says why, and a **Copy details** button makes it easy to report.

**Lyrics**
- Tighter timing: a line lights up just before it's sung and stays lit through
  short pauses, and lyrics that only have line timings light up across the whole line.
- New settings for text size, line spacing, glow, and animation style (Karaoke or Fade).

**Your music, your way**
- **Don't play this song.** Hidden songs stay out of queues, recommendations
  and Android Auto. Manage them in Settings › Content › Hidden songs.
- Choose whether new songs go to the top or the bottom of a playlist.
- Play next picks now play in the order you pick them, and swipe to queue is on by default.
- Podcasts remember their own playback speed.

**Player**
- Double-tap to seek on every player design, with your choice of how far. On
  the Record player, turn the record to seek.
- The Cassette player's keys now have labels.

**Discover**
- Charts and New releases open from the Search tab, and Charts shows chart playlists and top artists.
- Quick picks bring in more new music, and songs started from Speed Dial or the Home shelves keep the music going.
- Wrapped comes back every December and January, for the right year.

**Shortcuts and data**
- A Play/pause tile for Quick Settings, and Liked, Downloads and Shuffle shortcuts on the app icon.
- Mobile data controls in Player settings, and downloads at full quality when audio quality is Auto.

---

## 📋 Planned

Decided, and waiting their turn. Votes decide the order.

| Idea | What it means | |
| --- | --- | --- |
| **Install on Intel and AMD devices (x86)** | For Waydroid on Linux, Android emulators, and x86 tablets and TV boxes | [Vote](https://github.com/rajendra7169/blazify/discussions/15) |
| **Search inside the lyrics** | Find a word or line in the lyrics and jump straight to it | [Vote](https://github.com/rajendra7169/blazify/discussions/14) |
| **Import playlists from Spotify** | Bring your playlists over instead of rebuilding them by hand | [Vote](https://github.com/rajendra7169/blazify/discussions/13) |
| **Skip the talking in music videos (SponsorBlock)** | Jump past intros, sponsor messages and non-music parts | [Vote](https://github.com/rajendra7169/blazify/discussions/12) |
| **More than one Google account** | Switch between accounts without signing out | [Vote](https://github.com/rajendra7169/blazify/discussions/11) |
| **Scrobble to ListenBrainz** | An open alternative to Last.fm for your listening history | [Vote](https://github.com/rajendra7169/blazify/discussions/10) |
| **New release alerts** | A notification when an artist you follow releases something new | [Vote](https://github.com/rajendra7169/blazify/discussions/9) |

---

## 💡 Considering

Not decided yet. Enough votes move an idea up to Planned.

| Idea | What it means | |
| --- | --- | --- |
| **Animated album art and a music video switch** | Moving artwork in the player, and the video for songs that have one | [Vote](https://github.com/rajendra7169/blazify/discussions/8) |
| **Android TV layout** | Big-screen, remote-friendly Blazify | [Vote](https://github.com/rajendra7169/blazify/discussions/7) |
| **Your own music server** | Play from Jellyfin, Navidrome or any Subsonic server | [Vote](https://github.com/rajendra7169/blazify/discussions/6) |

---

## ✅ Recently shipped

### Local music (Android) in 9.13.2

**Requested in [#1](https://github.com/rajendra7169/blazify/issues/1) by @hancyking.**

The music already on your phone now plays alongside everything streamed, in
the same library, queue and player. MP3, FLAC, Ogg, Opus and WAV all play;
covers are read from the files; songs can be browsed by folder; the shortest
and smallest files can be filtered out; and a file whose tags describe it
badly can be corrected from inside the app.

### Windows installer in desktop 1.0.5

Blazify for Windows now comes as a normal installer, published with the Linux
builds on the [desktop releases page](https://github.com/rajendra7169/blazify-desktop/releases/latest).

### First iPhone builds, 0.1.0 and 0.2.0

The [iPhone app](https://github.com/rajendra7169/blazify-ios/releases/latest)
is out as an IPA to sideload. It is still early.

---

## Also in progress

- **F-Droid listing.** Submitted and in review; the reproducible-build check
  passes. The `izzy` build variant exists for it: no updater, and no permission
  to install packages. (An earlier IzzyOnDroid request was declined.)

---

## Not planned

- **Google Play release.** Apps that stream audio from YouTube are removed from
  it. This is a rule about distribution, not something that can be worked around.
- **Advertising, analytics or any tracking library.** Not now and not later.
- **A paid tier.** There is nothing to sell and nothing planned to sell.
