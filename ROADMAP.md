# Roadmap

What is planned, what has been asked for, and what is deliberately not being
done. Requests come from the [issue tracker](https://github.com/rajendra7169/blazify/issues);
if something you want is missing, open one.

---

## Recently shipped

### Local music (Android) — 9.13.2

**Requested in [#1](https://github.com/rajendra7169/blazify/issues/1) by @hancyking.**

The music already on your phone now plays alongside everything streamed, in
the same library, queue and player. MP3, FLAC, Ogg, Opus and WAV all play;
covers are read from the files; songs can be browsed by folder; the shortest
and smallest files can be filtered out; and a file whose tags describe it
badly can be corrected from inside the app.

---

## Also wanted

- **Windows installer** published alongside the Linux desktop builds
- **iOS release.** The build pipeline is proven; the app itself is still being built
- **F-Droid listing.** Submitted and in review; the reproducible-build check
  passes. The `izzy` build variant exists for it: no updater, and no permission
  to install packages. (An earlier IzzyOnDroid request was declined.)

---

## Not planned

- **Google Play release.** Apps that stream audio from YouTube are removed from
  it. This is a rule about distribution, not something that can be worked around.
- **Advertising, analytics or any tracking library.** Not now and not later.
- **A paid tier.** There is nothing to sell and nothing planned to sell.
