# Roadmap

What is planned, what has been asked for, and what is deliberately not being
done. Requests come from the [issue tracker](https://github.com/rajendra7169/blazify/issues);
if something you want is missing, open one.

---

## Next

### Local music support (Android)

**Requested in [#1](https://github.com/rajendra7169/blazify/issues/1) by @hancyking.**

Play the music already on your phone alongside everything streamed: point
Blazify at a folder, and its songs appear in the library with their tags and
artwork, in the same queue, on the same player.

The desktop build already does this, so the shape of it is settled. What it
needs on Android:

- `READ_MEDIA_AUDIO` permission (and `READ_EXTERNAL_STORAGE` below Android 13)
- A `MediaStore.Audio` scan, plus folders the user picks by hand
- Local tracks stored as ordinary songs with a marker on the id, so the queue,
  the mini player and every list treat them like anything else
- Tags and embedded artwork read from the file
- A Local section in the library, and local results in search

Nothing here is exotic. The player is Media3 and will play a file path as
readily as a URL; most of the work is the library plumbing and the permission
flow.

---

## Also wanted

- **Windows installer** published alongside the Linux desktop builds
- **iOS release.** The build pipeline is proven; the app itself is still being built
- **F-Droid listing.** An IzzyOnDroid request was declined. F-Droid proper is the
  next attempt, and the `izzy` build variant exists for it: no updater, and no
  permission to install packages

---

## Not planned

- **Google Play release.** Apps that stream audio from YouTube are removed from
  it. This is a rule about distribution, not something that can be worked around.
- **Advertising, analytics or any tracking library.** Not now and not later.
- **A paid tier.** There is nothing to sell and nothing planned to sell.
