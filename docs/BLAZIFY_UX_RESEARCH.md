# Blazify — Settings, Personalization & Feature Research

**Date:** 2026-07-18 · **Author:** research pass for Rajendra
**Goal:** Make Blazify's settings genuinely more user-friendly, put live previews
where they matter (theme, mini-player, player, home), and identify the features
and redesigns that would push Blazify clearly ahead of other music apps.

> This is a research + proposal document. Nothing here is committed to the app
> yet — it's the plan to review together. File:line anchors point at the current
> code so we can move fast when we start building.

---

## 0. TL;DR — what I recommend, in priority order

1. **Split the 2,162-line `AppearanceSettings` mega-screen** into a clean
   **"Look & Feel" hub** with ONE shared interactive phone-frame preview at the
   top that live-reflects every visual change (theme, player, mini-player, home).
   This is the single biggest usability win and the natural home for all the
   preview work we've already built.
2. **Fix settings fragmentation** — lyrics settings currently live in *three*
   screens (Appearance, Content, AI); "app language" appears twice in Content.
   Consolidate by topic, not by history.
3. **Add a profile/identity header + quick toggles** to the settings landing so
   the most-changed switches (theme mode, dynamic color, sleep timer) are one tap
   away instead of buried.
4. **Adopt one component standard** — `Material3SettingsGroup` everywhere; retire
   the `@Deprecated` `PreferenceEntry`/`SwitchPreference` still lingering.
5. **Ship 3–5 signature features** (see §5) that competitors don't have, built on
   the strengths Blazify already has (dynamic theming, Listen Together, multi-source
   lyrics).

Detailed, code-backed breakdowns below.

---

## 1. Current state — how settings are organised today

### 1.1 Landing screen (`ui/screens/settings/SettingsScreen.kt`)
A search field + five grouped card-lists (`BlazeSettingRow`: colored chip icon,
title, subtitle, chevron). Search filters rows by title/subtitle.

| Group | Rows → route |
|---|---|
| **Personalize** | Appearance → `settings/appearance` |
| **Playback** | Player and audio → `settings/player` · Stream sources → `settings/stream_sources` |
| **Content** | Content → `settings/content` · AI lyrics translation → `settings/ai` · *(Android Auto if installed)* |
| **Privacy & Data** | Privacy · Storage · Backup & restore |
| **About** | About · Changelog · *(Updater)* · *(Default links, A12+)* |

The landing screen itself is clean and already Blazified. The problem is what's
*behind* the "Appearance" and "Content" rows.

### 1.2 Sub-screen sizes (a bloat signal)
`AppearanceSettings.kt` **2,162 lines**, `PlayerSettings` 1,108, `ContentSettings`
1,041, `ThemeScreen` 1,050, `PlayerDesignScreen` 838, `AiSettings` 663 … (full
list in the appendix). Line count ≠ complexity, but a 2,162-line settings screen
is doing far too many unrelated jobs.

### 1.3 What's inside the mega-screen (`AppearanceSettings.kt`)
One screen currently mixes **six unrelated concerns**:
- Lyrics look — text position (`:417`), animation style (`:437`)
- Player look — buttons style (`:586`), player background style (`:606`)
- Mini-player — background style (`:626`), the whole design picker (group `:1135`)
- Library layout — default open tab (`:652`), default library chips (`:676`), grid cell size (`:703`)
- Theme entry (`:1005`) → `ThemeScreen`
- Player group (`:1185`), Lyrics group (`:1424`), Misc (`:1645`), Auto-playlists (`:1794`)

**Auto-playlists and library-grid settings have nothing to do with "Appearance."**

### 1.4 Fragmentation (settings that belong together but aren't)
- **Lyrics** settings are split across **three** screens: `AppearanceSettings`
  (position/animation, group `:1424`), `ContentSettings` (lyrics group `:906`),
  and `AiSettings` (translation, romanization). A user tweaking lyrics has to
  visit three places.
- **`app_language` appears twice** in `ContentSettings.kt` (`:347` and `:834`).
- **Player** look (Appearance) vs **Player** behaviour (PlayerSettings) vs
  **Player designs** (PlayerDesignScreen) are three separate entry points.

### 1.5 Component library (what we build with)
- **Standard:** `ui/component/Material3SettingsGroup.kt` —
  `Material3SettingsGroup(title, items: List<Material3SettingsItem>, useLowContrast)`
  renders M3-Expressive connected cards (24 dp outer corners, 6 dp inner joins,
  `animateContentSize`). `Material3SettingsItem` exposes `icon`, **`leadingContent`**,
  `title`, **`description`**, **`trailingContent`**, `showBadge`, `isHighlighted`,
  `enabled`, `onClick`. The `leadingContent`/`trailingContent` slots are flexible
  enough to embed **mini live previews** and value chips — we don't need a new
  component for previews.
- **Legacy/deprecated:** `ui/component/Preference.kt` — `PreferenceEntry` and
  `SwitchPreference` are both `@Deprecated`. Any screen still using them is an
  inconsistency to sweep up.

> _Detailed, per-screen, item-by-item audit + ranked UX problems:_ see **§4**
> (populated from the settings-audit pass).

---

## 2. Proposed settings IA v2 (reorganised for humans)

Reorganise **by what the user is trying to do**, not by code history. Target
top-level:

```
Settings
├─ [profile header]  avatar · name/guest · "Manage account"      (new)
├─ ⚡ Quick toggles   Theme mode · Dynamic color · Pure black · Sleep timer  (new, inline)
│
├─ 🎨 Look & Feel                → unified hub w/ live phone preview  (NEW, absorbs Appearance)
│     Theme & color · Player design · Mini-player · Home layout · Lyrics look
├─ ▶  Playback & audio           (PlayerSettings + Stream sources)
│     Quality · Loudness/EQ · Gapless/crossfade · Sleep timer · Queue
├─ 📚 Library & content           (library layout + ContentSettings, minus lyrics)
│     Default tab/chips · Grid size · Auto-playlists · Region · Explicit · Discover
├─ 🗣 Lyrics                       (NEW single home; pulls lyrics out of 3 screens)
│     Sources & priority · Sync · Translation · Romanization · Look (position/anim)
├─ 👥 Social & sharing            Listen Together · Last.fm · Discord · Share cards
├─ 🔌 Integrations                Android Auto · Widgets · Quick tiles · Notifications
├─ 🔒 Privacy & data              Privacy · Storage · Backup & restore
└─ ℹ  About                       About · Changelog · Updater
```

### 2.1 The "Look & Feel" hub (headline change)
A single destination that opens with **one interactive phone-frame preview**
(reusing `ThemePhoneFrame`) pinned at top, and below it a set of
`Material3SettingsGroup` rows. As the user edits any visual setting, the preview
updates live. Sub-sections:
- **Theme & color** — mode (auto/light/dark/pure-black), dynamic album-art
  theming toggle, seed-color palette (today's `ThemeScreen` content).
- **Player design** — the existing `PlayerDesignScreen` gallery (Classic/Ring/Full
  Art/Record/Cassette), surfaced here.
- **Mini-player** — the 4-design skeleton picker + background style we just built.
- **Home layout** — default tab, rails, grid density.
- **Lyrics look** — position + animation (a pointer/mirror of the Lyrics screen).

This gives the previews we've already built **one coherent home** instead of
being scattered mid-scroll in a 2,000-line screen.

### 2.2 Quick toggles + profile header
Most users change the same 3–4 things. Put them on the landing screen:
a compact profile row (avatar, name or "Guest", → Account) and a row of
segmented quick toggles (theme mode, dynamic color, pure black, sleep timer).
Everything else stays one tap deeper.

### 2.3 Search upgrade
Today search filters only top-level rows by title/subtitle. Upgrade it to index
**every leaf setting** across all sub-screens (flatten the setting registry) so
typing "crossfade" or "romaji" jumps straight to the control — the single fastest
way to make a big settings tree feel small.

---

## 3. Live-preview architecture (the core of "make it great")

### 3.1 What we already have (reuse, don't rebuild)
- `ThemeScreen.kt` → **`ThemePhoneFrame`** (metallic bezel, drop shadow,
  aspectRatio 9/19.3) + **`ThemePhonePreview(darkMode, pureBlack, themeColor)`**
  — a `BlazifyTheme`-wrapped mock home (header, hero card, search, chips, card
  rail, mini-player, nav). Already responsive + scrollable.
- `AppearanceSettings.kt` → **`MiniPlayerDesignPreview`** — lightweight skeleton
  (placeholder art + title/subtitle bars + real control icons), background-style
  aware, 2-per-row cards.
- `PlayerDesignScreen.kt` → **swipeable phone-frame gallery** of player designs
  with live real-song previews.

Three preview systems already exist — they just live in three screens with three
looks. The opportunity is to **unify them under one preview surface**.

### 3.2 The unifying idea: one `LookAndFeelPreview` driven by editable state
The theme system is already a pure function of four values
(`MainActivity.kt:530–637`): `dynamicTheme`, `darkMode`, `pureBlack`,
`selectedThemeColor` (+ album-art-derived `themeColor`). Because
`BlazifyTheme(darkTheme, pureBlack, themeColor)` (`Theme.kt:36`) is a pure wrapper,
we can render an isolated, fully-themed preview from *draft* values without
touching the live app — exactly what `ThemePhonePreview` already does.

Extend that to a **preview that switches its inner content** based on which
sub-section is focused:

```kotlin
// ui/screens/settings/lookandfeel/LookAndFeelPreview.kt  (NEW)

enum class PreviewFocus { HOME, PLAYER, MINI_PLAYER, LYRICS }

@Composable
fun LookAndFeelPreview(
    draft: LookAndFeelDraft,      // the in-progress, unsaved selections
    focus: PreviewFocus,
    modifier: Modifier = Modifier,
) {
    ThemePhoneFrame(modifier) {
        // One BlazifyTheme wrap → the whole preview reflects theme/color/mode.
        BlazifyTheme(
            darkTheme = draft.resolvedDark(),
            pureBlack = draft.pureBlack,
            themeColor = draft.seedColor,     // or album-art sample when dynamic
        ) {
            Crossfade(focus, label = "preview") { f ->
                when (f) {
                    PreviewFocus.HOME       -> MiniHomePreview(draft)      // reuse ThemePhonePreview body
                    PreviewFocus.PLAYER     -> MiniPlayerPagePreview(draft) // reuse PlayerDesignScreen bodies
                    PreviewFocus.MINI_PLAYER-> MiniPlayerBarPreview(draft)  // reuse MiniPlayerDesignPreview
                    PreviewFocus.LYRICS     -> MiniLyricsPreview(draft)
                }
            }
        }
    }
}

// Draft state — a plain holder the hub edits, then commits to DataStore on save
// (or writes through live for instant feedback; see §3.3).
data class LookAndFeelDraft(
    val darkMode: DarkMode,
    val pureBlack: Boolean,
    val dynamicColor: Boolean,
    val seedColor: Color,
    val playerDesign: PlayerDesign,
    val miniPlayerDesign: MiniPlayerDesign,
    val miniPlayerBg: MiniPlayerBackgroundStyle,
    val playerBg: PlayerBackgroundStyle,
    val sliderStyle: SliderStyle,
    val lyricsPosition: LyricsPosition,
    // …one field per visual pref
) {
    fun resolvedDark(): Boolean = when (darkMode) {
        DarkMode.ON -> true; DarkMode.OFF -> false; DarkMode.AUTO -> /* system */ true
    }
}
```

The hub screen: `LookAndFeelPreview` pinned at top; the section the user is
currently editing sets `focus` (tapping "Mini-player" flips the preview to the
mini-player bar; tapping "Player design" flips it to the full player). Each
control writes into `draft`, so the preview reacts instantly with zero device
round-trips.

### 3.3 Write-through vs draft-then-apply
Two valid patterns — recommend **write-through** for theme/mini-player (cheap,
instant, matches today's behaviour) and **draft-then-apply** only for the
full-screen player design (bigger visual jump, nice to preview before committing).
Today everything writes through immediately; keep that for most, add an "Apply"
affordance only where a preview-before-commit genuinely helps.

### 3.4 Preview coverage matrix
Every setting that changes something visual, and whether a preview exists today:

| Setting | Pref key | Preview today? | In unified hub |
|---|---|---|---|
| Theme mode (auto/light/dark) | `DarkModeKey` | ✅ ThemePhonePreview | Home focus |
| Pure black | `PureBlackKey` | ✅ | Home focus |
| Dynamic album-art color | `DynamicThemeKey` | ⚠️ static mock | Home focus (sample art) |
| Seed color | `SelectedThemeColorKey` | ✅ | Home focus |
| Player design | (PlayerDesign) | ✅ gallery | Player focus |
| Player background style | `PlayerBackgroundStyleKey`… | ❌ | Player focus |
| Slider style | (SliderStyle) | ❌ | Player focus |
| Mini-player design | `MiniPlayerDesignKey` | ✅ skeleton | Mini focus |
| Mini-player background | `MiniPlayerBackgroundStyleKey` | ✅ | Mini focus |
| Hide thumbnail / crop art | … | ❌ | Player focus |
| Lyrics position / animation | … | ❌ | Lyrics focus |
| Default tab / chips / grid | … | ❌ | Home focus |

> _Full redesign catalogue + preview-opportunity detail:_ see **§6**
> (populated from the redesign pass).

---

## 4. Detailed settings audit

### 4.1 Cross-screen problems (the patterns that recur everywhere)
1. **Missing descriptions are the dominant gap.** Controls with no subtitle: AccountSettings
   ("More content", the tri-state token row), PrivacySettings (both "Pause…" switches),
   BackupAndRestore (all 4 actions), Romanization (both switches), Android Auto section rows,
   Updater's two switches. Positive exceptions worth emulating: StreamSources (every switch
   documented) and the cache toggles.
2. **Jargon / opaque labels.** "More content" and the raw auth-token editor
   (`AccountSettings.kt`); the **entire StreamSources screen** (raw client IDs `WEB_REMIX`,
   `TVHTML5`, `WEB_CREATOR`, "throttle-gated", "MiB", "DroidGuard"); "Detect language line-by-line"
   (Romanization).
3. **Mislabeled group headers.** Android Auto **"Mixes"** header sits over a *YouTube suggested
   playlists* toggle; Romanization **"Default content language"** header sits over a *romanization
   language filter*; PrivacySettings uses **"Misc"** for what is really a security toggle.
4. **Hardcoded / unlocalized strings.** Romanization `"Play all"` (line 153) + **all 12 language
   names** (lines 44–57) are English literals, not `stringResource` → they don't translate; About's
   `"STABLE"`/`"DEBUG"` chips are hardcoded.
5. **Grouping inconsistency.** AccountSettings mixes two headerless low-contrast
   `Material3SettingsGroup`s with a manual `PreferenceEntry` column (three visual styles in one
   panel); BackupAndRestore puts data-level (Backup/Restore) and utility (M3U/CSV import) actions in
   one unnamed group, with items 3 & 4 **sharing the same `playlist_add` icon**.
6. **Redundancy.** Updater repeats "check for updates" as a switch label, a group header, AND a
   button in one short screen. Romanization's `"Play all"` is really *Select all* (a select-all
   toggle mislabeled as a playback action).
7. **Accessibility gaps.** About's three social buttons are icon-only with
   `contentDescription = null` (`AboutScreen.kt:291`) — screen readers announce nothing.
8. **Broken/placeholder link.** Changelog "View on GitHub" → `github.com/BlazifyGroup/Blazify/releases`
   (does not match the real repo `rajendra7169/blazify`) → will 404.

### 4.2 Good patterns already present (keep + spread these)
- **Live sizes + progress bars** in StorageSettings (`downloadCacheSize`, "used / max" fills).
- **Numbered "Stream order" chip row** in StreamSources — shows the *effect* of the toggles.
- The **restore-confirm dialog** (account avatar + email + signout warning) in BackupAndRestore.
- **Dependent-setting hiding** in Updater (notification switch hidden when auto-check is off).

### 4.3 Per-screen audit — smaller screens (verified item-by-item)

**AccountSettings.kt** _(compact panel, not a route; 8 items)_ — account row (avatar/login → `account`/`login`
+ logout dialog clearing `innerTubeCookie`); token editor (writes `innerTubeCookie`, `visitorData`,
`dataSyncId`, `accountName`, `accountEmail`, `accountChannelHandle`); **"More content"** switch
(`useLoginForBrowse`, no subtitle); **"Auto-sync"** switch (`ytmSync`); nav rows Together/Integrations/Settings;
update row. _Issues: 3 visual styles; opaque "More content"; raw auth internals exposed; switches silently
disabled when logged out._

**PrivacySettings.kt** _(5 items)_ — Pause listen history (`pauseListenHistory`), Clear listen history (action);
Pause search history (`pauseSearchHistory`), Clear search history (action); Disable screenshot
(`disableScreenshot`, only item with a subtitle). _Issues: "Pause…" switches need "doesn't delete existing"
note; "Misc" header for a security toggle._

**StorageSettings.kt** _(7 items, well-instrumented)_ — Downloaded songs (live size), Clear all downloads;
Enable song cache (`enableSongCache`), Max song cache size (`maxSongCacheSize`, stepper 0/128…8192/-1 MB +
progress bar), Clear song cache; Max image cache size (`maxImageCacheSize`, stepper + bar), Clear image cache.
_Issues: sliders+bars stuffed into the `description` slot (very tall rows); discrete steppers overload
"Disable"/"Unlimited" at the extremes — a labeled dropdown would be clearer; image cache lacks an Enable toggle._

**BackupAndRestore.kt** _(4 actions, one unnamed group)_ — Backup, Restore (rich confirm dialog),
Import M3U, Import CSV (column-mapping). _Issues: no subtitles; destructive data ops look identical to import
utilities; M3U/CSV share an icon; no text says what a backup contains._

**StreamSourcesSettings.kt** _(7 switches + live order row)_ — live numbered "Stream order" chips; WEB_REMIX,
TVHTML5, visionOS, Android VR, iOS (off), WEB_CREATOR, ANDROID_CREATOR (off) — each `streamSource*` key, each
documented. _Issues: deeply technical/jargon; order row is display-only though order matters (users may expect
drag-to-reorder)._

**AlarmSettings.kt** _(section, not a route)_ — Add alarm; per-alarm rows (time + playlist • Random/In-order •
"Next: …", inline enable Switch + delete icon); conditional exact-alarm + battery-optimization permission
prompts. Alarm data persisted as JSON via `MusicAlarmStore` (`alarmEntries`). _Issues: long sentence used as a
switch label; delete icon adjacent to switch (mis-tap); time/playlist buried in a dialog._

**AndroidAutoSettings.kt** _(8 settings)_ — reorderable Visible-sections list (Liked/Songs/Artists/Albums/
Playlists, `androidAutoSectionsOrder`), Quick-add destination (`androidAutoTargetPlaylist`), Show YT suggested
playlists (`androidAutoYoutubePlaylists`). _Issues: "Mixes" header mismatch; hint rendered as an empty-title
item; hard-coded list height inside a scroll container._

**RomanizationSettings.kt** _(15 items)_ — Show romanized as main (`lyricsRomanizeAsMain`), Detect line-by-line
(`lyricsRomanizeCyrillicByLine`), "Play all" tri-state, 12 language checkboxes (`lyricsRomanizeList`). _Issues:
mislabeled header; hardcoded/unlocalized "Play all" + 12 language names; no descriptions; flat 12-item list with
no search/grouping._

**UpdaterSettings.kt** _(3 items)_ — read-only version, Auto-check (`checkForUpdates`), Update notifications
(`updateNotifications`, hidden when auto-check off), manual check button. _Issues: "check for updates" repeated
3×; FOSS flavor disables the updater yet the screen still ships (confirm reachability)._

**AboutScreen.kt / ChangelogScreen.kt** — brand/credits page and a changelog bottom-sheet (no prefs).
_Issues: icon-only social buttons w/ null contentDescription; hardcoded "STABLE" chip; remote-URL dev avatar;
Changelog "View on GitHub" points at a 404 repo and filters to current-or-older releases (backwards)._

### 4.4 Big screens — item-by-item audit (read in full)

**AppearanceSettings.kt (2,162 lines) — the mega-screen: 7 groups / ~37 controls.**
- *Theme group* (`:1005`): high-refresh (`enableHighRefreshRate`), landscape scaling
  (`enableLandscapeScaling`), **dynamic theme** (`dynamicTheme`, `:1062`), **dynamic icon**
  (`enableDynamicIcon`, `:1087`), Theme→subscreen link.
- *Mini-player design picker* (`:1127`, skeleton preview) + *Mini-player group* (`:1135`): background
  style dialog (`miniPlayerBackgroundStyle`; greys out for FLAT).
- *Player group* (`:1185`): New player design (`useNewPlayerDesign`), background style
  (`playerBackgroundStyle`), hide thumbnail (`hidePlayerThumbnail`), crop art (`cropAlbumArt`),
  buttons style (`player_buttons_style`), **slider style** (`sliderStyle`/`squigglySlider`, *has live
  preview in dialog*), swipe thumbnail (`swipeThumbnail`) + sensitivity.
- *Lyrics group* (`:1424`, 10 controls): experimental lyrics (`experimentalLyrics`, **default ON**),
  and — only shown when it's OFF — glow/animation/size/spacing; plus text position, respect-agent,
  click-to-seek, auto-scroll, hide-status-bar.
- *Misc group* (`:1645`): default tab, default library chip, swipe-to-add/remove, slim nav
  (`slimNavBar`), Listen-Together-in-top-bar, grid cell size, display density (restart).
- *Auto-playlists group* (`:1794`): 5 visibility switches (liked/downloaded/top/cached/uploaded).
- `pureBlackMiniPlayer` is read (`:1121`) but never exposed here (dead read).

**PlayerSettings.kt (1,108 lines) — 4 groups + embedded Alarm section, ~34 controls.**
- *Player/audio* (`:287`): audio quality, crossfade (+duration slider-in-description +gapless),
  history duration (slider-in-description), skip-silence (+instant), audio normalization (**no
  subtitle**) +loudness level, offload, varispeed, hardware audio processing, Cast (GMS), progressive seek.
- *Sleep timer* (`:634`): enable, **"Repeat" (a fake Switch that opens a dialog, `:663-685`)**, stop-after-song, fade-out.
- *AlarmSettingsSection embedded inline* (`:742`) — the whole alarm screen lives here.
- *Queue* (`:747`): 12 documented switches (persistent queue, auto-load, auto-radio, autoplay, …).
- *Misc* (`:1007`): 4 switches, **none have descriptions** (stop-on-task-clear, pause-on-mute,
  resume-on-bluetooth, keep-screen-on).

**ContentSettings.kt (1,041 lines) — 7 groups.** General (content language/country, hide
explicit/video/shorts — **last three have no subtitle**); Artist page (3 switches, **no subtitles**);
**App language** (header *and* item share the label); Proxy (enable+configure); **Lyrics** (provider
selection + priority reorder + romanization link — the *source* half of lyrics); **Wrapped** (title is
a **hardcoded literal**, untranslatable); Misc (randomize home order, top-list length, quick-picks —
key is `discover`/`QuickPicksKey`, a name mismatch).

**ThemeScreen.kt (1,050 lines) — the best-designed screen.** On-screen title is **"Theme colors"**
(mismatches the "Theme" row that opens it). Live phone-frame preview + 4 mode circles
(System/Light/Dark/Pure-black → `darkMode`,`pureBlack`) + 21-swatch palette (`selectedThemeColor`;
picking a color also disables `dynamicTheme`). Note: dark-mode+pure-black live here but
dynamic-theme+dynamic-icon live one level up in Appearance → theme controls straddle two screens.

**PlayerDesignScreen.kt (838 lines).** Route `settings/appearance/player_design`, but launched from
the **player's top-right theme icon, not from Settings**. Live interactive gallery of 5 layouts
(Classic/Ring/Full Art/Record/Cassette → `playerDesign`). Collides conceptually with the
`useNewPlayerDesign` boolean in Appearance — **two different "player design" settings.**

**AiSettings.kt (663 lines) — power-user screen at top level.** Provider (OpenRouter/OpenAI/Claude/
Gemini/…/DeepL/Custom) + key/model setup (DeepL vs others) + translation mode/system-prompt/target
language (group header "AI translation mode" actually holds three different things).

**IntegrationScreen.kt (75 lines) — critical IA finding.** The Integrations hub (Discord, Last.fm)
is reachable **only** from `AccountSettings.kt:403`, and Account itself **only** from the Home header
avatar (`HomeScreen.kt:1193`) — **neither is in the Settings list at all.** Two of the most
settings-like features (scrobbling, Rich Presence) are hidden from Settings.

### 4.5 Top-15 settings UX problems (ranked by impact)
1. **AppearanceSettings is a 2,162-line mega-screen mixing 7 domains** (~37 controls, no in-page nav). Top clean-up target.
2. **"Player appearance" is fragmented across 3 surfaces with 2 colliding "design" concepts** — `useNewPlayerDesign` bool (`AppearanceSettings.kt:1188`) vs `playerDesign` enum gallery (`PlayerDesignScreen.kt`) vs background/buttons/slider (Appearance).
3. **Lyrics settings split across 3 screens** — look (Appearance `:1424`), sources/priority/romanization (Content `:906`), translation (AI).
4. **Account / Integrations / Discord / Last.fm are NOT in Settings** — only via the Home avatar → Account → Integrations. Hidden.
5. **Theme controls straddle two screens** — dark-mode+pure-black in ThemeScreen; dynamic-theme+dynamic-icon in Appearance.
6. **~13 switches have no description** while siblings do (Content explicit/video/shorts + artist toggles + proxy + wrapped card; Player normalization + all 4 Misc).
7. **Jargon labels** — "Respect agent positioning", "Use hardware audio processing", "Enable varispeed", "Progressive seek", "Instantly skip silence", "Disable auto-load in repeat-all".
8. **The Sleep-timer "Repeat" row is a fake switch** (`PlayerSettings.kt:663-685`) — looks like on/off, actually opens a dialog.
9. **Default-visible lyric-look controls disappear by default** — glow/animation/size/spacing render only when `experimentalLyrics` is OFF, but it defaults ON.
10. **"App language" appears twice** in ContentSettings (header + item).
11. **Mislabeled/hardcoded headers** — Content "Wrapped" is a raw literal (untranslatable); AI "AI translation mode" holds 3 different things.
12. **Misleading list subtitles** — `hint_player` says "…EQ" but there's no EQ in PlayerSettings (EQ is a *separate* `eq/` screen); `hint_appearance` says "dark mode" but it's buried in ThemeScreen.
13. **Duplicated DataStore key (data bug)** — `MixSortDescendingKey = "albumSortDescending"` (`PreferenceKeys.kt:244`) clobbers the album key; plus `QuickPicksKey="discover"` mismatch.
14. **Sliders buried in the description slot, no numeric entry** — crossfade duration (`:329`), history duration (`:366`) differ from every other slider (which is in a dialog).
15. **Library concerns live under Appearance** (auto-playlists `:1794`, default chip `:1664`); home-content controls scattered across Content; only the top-level Settings has search — the 30+-item mega-screens have none.

### 4.6 Quick wins vs bigger restructures
**Quick wins (cheap, high impact):** fix the fake Sleep-timer "Repeat" switch → link row; fix the
duplicated `MixSortDescendingKey` (+migration note); add the ~13 missing subtitles; drop "EQ" from
`hint_player` and fix `hint_appearance`; de-jargon labels; de-dupe "App language"; make "Wrapped" a
`stringResource`; rename ThemeScreen's title to match its row; **add an Integrations row to the
Settings list.**
**Bigger restructures:** split AppearanceSettings into focused sub-screens (the section titles already
map to routes); create one **Lyrics** home (sources+look+translation); consolidate **Player
appearance** behind one entry; reunify **Theme** controls in ThemeScreen; bring **Account** into the
Settings IA with Integrations/Listen-Together nested; extend the live-preview pattern to the text-only
enum dialogs (player bg, buttons, mini-player bg, slider, lyrics animation).

---

## 5. Missing / weak features & signature ideas

> Context: Blazify is already a **very** feature-dense fork — the streaming/playback
> core inherited from Metrolist is close to best-in-class for a YouTube-Music
> client. Real gaps cluster in **local-media playback**, **wearables/cross-cast**,
> and **novel/differentiating** features, not basic parity.

### 5.1 Notable strengths already ahead of typical FOSS peers
Custom **parametric EQ** with AutoEq GitHub-profile search + wizard + response graph
(`eq/`); built-in **Shazam-style recognition** (`recognition/ShazamSignatureGenerator.kt`);
**Discord Rich Presence** with full gateway/OAuth (`discord/`); multi-source **AI lyric
translation** (OpenRouter/Claude/Gemini/…) + **romanization**; **Listen Together**
(own server, room codes, volume sync, reconnection); **scheduled sleep timer + alarm**;
**crossfade + skip-silence + ReplayGain/LUFS normalization + speed & pitch**; a
**turntable home-screen widget**; **Wrapped/Stats**. Keep leaning on these.

### 5.2 Missing / weak features — ranked by value ÷ effort
Effort S/M/L. Every row names where it hooks into the existing architecture.

| # | Feature | Value | Effort | Hook |
|---|---|---|---|---|
| 1 | **Local audio-file playback + folder browsing** — no `MediaStore` audio scan / no `READ_MEDIA_AUDIO`; today it's stream + YT-uploads only. Biggest single gap for "music player" positioning (Poweramp/Musicolet/Symfonium/Apple Music all do this). | High | L | New `LocalMediaScanner` → `SongEntity` w/ a local-source flag; `ListQueue` of local `MediaItem`s already works in `playback/queues/`; add a "Folders" tab in `library/` + permission prompt. |
| 2 | **Import external playlists** (Spotify/CSV/M3U/JSON) — only *export* exists (`utils/PlaylistExporter.kt`); note the CSV/M3U import in `BackupAndRestore` is separate/limited. | Med | S–M | Mirror `PlaylistExporter` with an importer; resolve titles via `innertube` search → `PlaylistEntity`+`PlaylistSongMap`. Reuse `BackupAndRestore.kt` file-picker plumbing. |
| 3 | **Smart / auto-updating playlists** (rules: "liked + genre X", "played >5×", "added this month"). Only fixed auto-playlists (Top-50, weekly/monthly) exist today. | Med | M | `SmartPlaylistRule` entity + query builder over `DatabaseDao`; render through the existing `auto_playlist/*` route in `NavigationBuilder.kt`. |
| 4 | **On-device personalized mixes / "Daily Mix" from history** — recommendations are 100% YTM-served; nothing from local play-count/affinity. | Med | M | Seed from `PlayCountEntity`/`Event` in `HomeViewModel` → feed `YouTubeQueue`/radio in `playback/queues/`. |
| 5 | **Full podcast support** — online-only today (`ui/screens/podcast/`, `PodcastEntity`); no subscriptions, episode downloads, per-show speed, resume position. | Med | M | Extend `PodcastEntity` + `ExoDownloadService` (already handles downloads); subscribe/queue UI in `LibraryPodcastsScreen.kt`. |
| 6 | **Collaborative *persistent* playlists** — Listen Together syncs a session but doesn't persist a co-edited playlist. | Med | M–L | Add a shared-playlist op-type to `listentogether/ListenTogetherManager.kt` protocol; persist to `PlaylistEntity`. Infra (server, presence) already exists. |
| 7 | **DLNA/UPnP casting for FOSS builds** — Chromecast is GMS-only (`app/src/gms/.../CastManager.kt`); FOSS users can't cast at all. | Med | M–L | jUPnP renderer in the `foss` sourceset paralleling `CastConnectionHandler.kt`. |
| 8 | **Wear OS companion / complications** — no `wear` module anywhere. | Med | L | New `wear/` module w/ a Media3 `MediaController` bound to `MusicService`. |
| 9 | **Tag / metadata editor** — none (only meaningful once #1 lands). | Low–Med | M | JAudioTagger-backed editor from `ui/menu/` for local songs. |
| 10 | **True per-album gapless for streamed sources** — crossfade has a `crossfadeGapless` flag but gap-free streamed album transitions aren't guaranteed. | Low–Med | M | Tune `DefaultAudioSink`/`LoadControl` in `MusicService.kt`; preload next stream URL earlier. |

### 5.3 Signature features (bold differentiators — all feasible on this stack)
1. **AI DJ / conversational queue** — "play something like this but more upbeat", "make a
   30-min focus set". The OpenRouter/Claude/Gemini plumbing already exists
   (`settings/AiSettings.kt`, `LyricsTranslationHelper.kt` shows the call pattern). LLM emits
   seed artists/tracks → resolve via `innertube` → build a `ListQueue`. **No new backend.**
2. **Real karaoke mode** — Blazify already has Karaoke/Apple lyric animations *and* a working
   custom `AudioProcessor` pipeline. Add a **center-channel vocal-removal `AudioProcessor`** +
   mic scoring. Pairs uniquely with the synced-lyrics engine. (Stereo center-cut quality is a
   limit, but it's a genuine standout.)
3. **"Blaze Wrapped, anytime" shareable story cards** — Wrapped/Stats exist (`ui/screens/wrapped/`)
   and `utils/ComposeToImage.kt` renders Compose → shareable image. Branded story cards (top
   artist this week, streak, minutes) + share intent. **All pieces present.**
4. **Elevate Listen Together into a "Blaze Party" social layer** — persistent collaborative
   playlists, live reactions/emojis, a shared queue anyone can add to. The clearest moat vs
   other YTM forks (InnerTune/Metrolist have nothing comparable). Protocol extension on existing infra.
5. **Recognition → auto-crate** — "keep listening" continuous recognition (e.g. at an event) that
   auto-appends every identified song to a dated playlist. Recognizer + foreground service +
   `PlaylistEntity` all exist; it's orchestration, not new capability.

### 5.4 Bug spotted in passing (not asked, worth fixing)
`constants/PreferenceKeys.kt:244` — `MixSortDescendingKey = booleanPreferencesKey("albumSortDescending")`
reuses the **album** key name, so Mix and Album "descending" states collide in DataStore. Likely copy-paste.

---

## 6. Redesign opportunities & preview surfaces

### 6.1 Design-maturity map (what's already Blazified vs still stock Metrolist)
| Surface | Maturity | Anchor |
|---|---|---|
| Home header (greeting/search) | ✅ Fully Blazified | `ui/component/BlazeHomeHeader.kt` |
| **Home content rails (below header)** | ❌ Stock Metrolist | `HomeScreen.kt:1201+` |
| Library landing | ✅ Fully Blazified | `library/BlazeLibraryHome.kt` |
| **Search** | ❌ Stock Metrolist | `search/SearchScreen.kt` |
| Player full-screen | ✅ Heavily Blazified (5 layouts) | `ui/player/Player.kt` |
| Mini-player | ✅ Blazified (4 layouts) | `ui/player/MiniPlayer.kt` |
| Queue | ◐ Partly (sleep dialog only) | `ui/player/Queue.kt` |
| Lyrics | ✅ Blazified | `Lyrics.kt`, `ExperimentalLyrics.kt` |
| Nav bar / shell | ◐ Stock M3 + pure-black override | `ui/component/AppNavigation.kt` |
| Settings rows | ◐ Generic `Material3SettingsGroup` | `settings/AppearanceSettings.kt` |

**The single most noticeable inconsistency in the app** is the seam between the fully-custom
Home *header* and the stock Metrolist *rails* right below it.

### 6.2 Highest-value redesigns
- **Home content rails (HIGH).** Below `BlazeHomeHeader`, everything is stock: `ChipsRow`
  (`HomeScreen.kt:1201`), `NavigationTitle` + `ytGridItem` + `LazyRow` rails (`:1239`, `:1254`),
  `CommunityPlaylistCard` (`:208`) and `DailyDiscoverCard` (`:501`) use plain `Card` with no brand
  language. → Replace `NavigationTitle` with the Library's `BlazeSectionHeader`
  (`BlazeLibraryHome.kt:186`); reskin rails to `BlazeMusicCard`/`BlazePlaylistCard` (20dp radii,
  gradient seeds, 12dp gaps); give Daily Discover / Community cards the amber-gradient greeting-card
  treatment; make Speed-Dial chips 28–30dp pills to match the search bar.
- **Search (HIGH).** `SearchScreen.kt` is untouched Metrolist (plain `TopAppBar` + `BasicTextField`,
  `:181-278`). → Port the Home 30dp search-pill shape (amber mic accent) for Home→Search continuity;
  reskin the `SearchSource` toggle (`:240-261`) as an amber segmented pill (Library / YT Music);
  brand the result rows + recognition FAB.
- **Nav bar / shell (MEDIUM).** Stock `NavigationBar` w/ only a pure-black override
  (`AppNavigation.kt:146`). → Branded selected-indicator (amber pill / gradient underline) + animated
  icon fill; consider a floating/rounded nav to match the "floating" mini-player; give `slimNav` a
  distinct compact brand treatment rather than just dropping labels (`:209-217`).
- **Queue (LOW–MED).** Reskin rows + top control bar (rounded now-playing highlight, amber indicators).
- **Player / Mini-player / Lyrics (LOW).** Already strong; polish only — unify the "Now Playing"
  header across the 5 player designs; the FLAT mini-player bar is the least on-brand and could be
  retired/restyled; expose lyric-animation styles with visual previews (see §6.3).

### 6.3 Live-preview coverage — the real gap
Of **~27 settings that change something visual, only 4 clusters have real live previews today**
(theme, player layout, mini-player layout, slider style). ~20 have none. High-value additions:

| Setting | Key | Preview today | Add |
|---|---|---|---|
| Dynamic album-art theming | `DynamicThemeKey` (`AppearanceSettings.kt:1058`) | none | frame seeds from a sample art vs fixed swatch |
| Player background style | `PlayerBackgroundStyleKey` (dialog `:599`) | none in dialog | BLUR/GRADIENT/DEFAULT thumbnails |
| Player buttons style | `PlayerButtonsStyleKey` (dialog `:579`) | none | 3 mini transport rows |
| Lyrics position | `LyricsTextPositionKey` (`:410`) | none | left/center/right mini block |
| Lyrics size / spacing | `:1509` / `:1517` | none (sp number) | live-scaling sample line |
| Lyrics animation style | `LyricsAnimationStyleKey` (`:1490`) | none | animated sample per style (NONE/FADE/GLOW/SLIDE/KARAOKE/APPLE) |
| Lyrics glow | `LyricsGlowEffectKey` (`:1465`) | none | sample line w/ + w/o glow |
| Hide thumbnail / crop art | `HidePlayerThumbnailKey` `:1224` / `CropAlbumArtKey` `:1246` | none | gallery frame toggles art / fit-vs-crop |
| Slim nav | `SlimNavBarKey` (`:1723`) | none | nav strip w/ + w/o labels |
| Grid size / density | `GridItemsSizeKey` `:692` / `DensityScaleKey` `:754` | none | 2-up vs 3-up / scaled row |

### 6.4 Existing preview infrastructure (REUSE map — the hub is mostly assembly)
Blazify already has **two** strong, independent live-preview systems + a skeleton + inline demos:
| Component | File:line | Reuse as |
|---|---|---|
| `BlazifyTheme` | `Theme.kt:36` | wrap every preview interior |
| `ThemePhoneFrame` | `ThemeScreen.kt:682` | ← merge with… |
| `PhoneFrame` (player) | `PlayerDesignScreen.kt:230` | …this near-identical dup → one `BlazePhoneFrame` |
| `ThemePhonePreview` | `ThemeScreen.kt:728` | **Home tab** of the unified preview |
| `ThemeControls`/`ModeCircle`/`PaletteItem` | `ThemeScreen.kt:305–678` | theme controls block |
| `LivePreview` (dispatch 5 player designs) | `PlayerDesignScreen.kt:290` | **Player tab** (interactive, seekable) |
| player-design gallery (HorizontalPager) | `PlayerDesignScreen.kt:145` | link from hub |
| `rememberPreviewGradient`/`PreviewBackground` | `PlayerDesignScreen.kt:314–377` | bg-style previews |
| `MiniPlayerDesignPicker` + skeleton `MiniPlayerDesignPreview` | `AppearanceSettings.kt:1976`, `:2071` | **Mini tab** + controls |
| inline `SquigglySlider`/`Slider` demos | `AppearanceSettings.kt:844`, `:977` | slider preview |
| `BlazeSectionHeader`/`BlazeMusicCard`/`BlazePlaylistCard` | `BlazeLibraryHome.kt:37–40` | Home-rail redesign + Library preview |

**Two discoverability/cleanup facts to exploit:**
- The excellent player-design gallery is reachable **only** from the player's palette icon
  (`Player.kt:1995`) and is **not linked from AppearanceSettings** — the hub should surface it
  (route already exists: `NavigationBuilder.kt:390`).
- `ThemeScreen.kt` still carries two **unused** legacy mockups `ThemeMockup` (`:832`) /
  `ThemeMockupPortrait` (`:940`) — safe to delete when consolidating.

> This confirms §3: the unified "Look & Feel" hub is **mostly assembly + consolidation** —
> merge the two phone frames, host the existing preview interiors in one tabbed frame driven by the
> DataStore keys they already read, route in the orphaned gallery. Net-new work = reskin Home
> rails + Search, and add small previews for the ~20 preview-less settings.

---

## 7. Concrete code proposals (top priorities)

Grounded in the real APIs verified in this pass: `Material3SettingsGroup`/`Material3SettingsItem`
(`Material3SettingsGroup.kt:43,210`), `BlazifyTheme(darkTheme, pureBlack, themeColor)` (`Theme.kt:36`),
`rememberPreference`/`rememberEnumPreference`, and the existing preview interiors.

### 7.1 Extract ONE shared phone frame (delete the duplicate)
`ThemePhoneFrame` (`ThemeScreen.kt:682`) and `PhoneFrame` (`PlayerDesignScreen.kt:230`) are
near-identical. Consolidate into `ui/component/BlazePhoneFrame.kt` and point both callers at it:

```kotlin
// ui/component/BlazePhoneFrame.kt  (NEW — body lifted verbatim from ThemePhoneFrame)
@Composable
fun BlazePhoneFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val frameShape = RoundedCornerShape(38.dp)
    Box(
        modifier = modifier
            .aspectRatio(9f / 19.3f)
            .shadow(24.dp, frameShape, clip = false,
                ambientColor = Color.White.copy(alpha = 0.30f),
                spotColor = Color.White.copy(alpha = 0.50f))
            .clip(frameShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF44454A), Color(0xFF26272B), Color(0xFF1A1B1E))))
            .border(1.5.dp, Brush.verticalGradient(listOf(
                Color.White.copy(0.45f), Color.White.copy(0.10f), Color.White.copy(0.28f))), frameShape)
            .padding(6.dp)
            .clip(RoundedCornerShape(32.dp)),
    ) { content(); /* speaker slit */ }
}
```
Then `ThemeScreen` and `PlayerDesignScreen` both call `BlazePhoneFrame { … }`, and the two legacy
mockups `ThemeMockup`/`ThemeMockupPortrait` (`ThemeScreen.kt:832,940`) are deleted.

### 7.2 The unified "Look & Feel" hub
One route (`settings/appearance/look_and_feel`) — pinned preview on top, accordion controls below.
Because the preview interiors already read the same DataStore keys the controls write, **edits
propagate with no extra plumbing** (recompose on DataStore change), exactly as the mini-player
skeleton already does (`AppearanceSettings.kt:2074`).

```kotlin
// ui/screens/settings/lookandfeel/LookAndFeelScreen.kt  (NEW)
enum class LFTab(@StringRes val label: Int) { THEME(R.string.theme), PLAYER(R.string.player),
    MINI(R.string.mini_player), LYRICS(R.string.lyrics), HOME(R.string.home) }

@Composable
fun LookAndFeelScreen(navController: NavController, playerConnection: PlayerConnection?) {
    var tab by rememberSaveable { mutableStateOf(LFTab.THEME) }

    // Draft theme values so the frame reacts instantly (theme is a pure fn of these).
    val (darkMode)  = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val (pureBlack) = rememberPreference(PureBlackKey, true)
    val (seedInt)   = rememberPreference(SelectedThemeColorKey, BlazeThemeColor.toArgb())

    Column(Modifier.verticalScroll(rememberScrollState())) {
        // ── Pinned preview ──────────────────────────────────────────────
        BlazePhoneFrame(Modifier.fillMaxWidth(0.62f).align(Alignment.CenterHorizontally)) {
            BlazifyTheme(darkTheme = darkMode.resolved(), pureBlack = pureBlack, themeColor = Color(seedInt)) {
                Crossfade(tab, label = "lf") { t -> when (t) {
                    LFTab.HOME, LFTab.THEME -> ThemePhonePreviewInterior()          // ThemeScreen.kt:740
                    LFTab.PLAYER            -> LivePreview(currentPlayerDesign(), playerConnection) // PlayerDesignScreen.kt:290
                    LFTab.MINI              -> MiniPlayerBarPreviewInterior()        // reuse MiniPlayerDesignPreview shape logic
                    LFTab.LYRICS            -> LyricsSampleInterior()                // NEW small sample block
                } }
            }
        }
        // ── Tab strip drives what the frame shows ───────────────────────
        LFTabRow(selected = tab, onSelect = { tab = it })

        // ── Controls for the active tab (all existing components) ───────
        when (tab) {
            LFTab.THEME  -> ThemeControls(/* mode circles + palette, ThemeScreen.kt:305 */)
            LFTab.PLAYER -> PlayerLookControls(navController)  // buttons/bg/slider dialogs + "Open gallery" → NavigationBuilder.kt:390
            LFTab.MINI   -> MiniPlayerDesignPicker(/* AppearanceSettings.kt:1976 */) + MiniPlayerBackgroundRow()
            LFTab.LYRICS -> LyricsLookControls()   // position/size/spacing/animation/glow
            LFTab.HOME   -> HomeLayoutControls()   // default tab, chips, grid, slim nav
        }
    }
}

private fun DarkMode.resolved(): Boolean = when (this) {
    DarkMode.ON -> true; DarkMode.OFF -> false; DarkMode.AUTO -> /* isSystemInDarkTheme() */ true
}
```
Net-new code is small: the tab strip, the `LyricsSampleInterior`, and thin wrappers that expose the
existing dialogs/pickers. Everything heavy (frame, theme preview, player preview, mini skeleton,
slider demos) is reused.

### 7.3 Landing: profile header + quick toggles
Add to the top of `SettingsScreen.kt` (`:157`, before the groups loop):

```kotlin
// Profile row → account
Material3SettingsGroup(items = listOf(Material3SettingsItem(
    leadingContent = { AccountAvatar(size = 44.dp) },              // reuse AccountSettings avatar
    title = { Text(accountName ?: stringResource(R.string.guest)) },
    description = { Text(stringResource(if (isLoggedIn) R.string.manage_account else R.string.tap_to_sign_in)) },
    onClick = { navController.navigate(if (isLoggedIn) "account" else "login") },
)))

// One-tap quick toggles (segmented, write straight to DataStore)
QuickToggleRow(
    themeMode = darkMode, onThemeMode = { darkMode = it },          // DarkModeKey
    dynamicColor = dynamicColor, onDynamicColor = { dynamicColor = it }, // DynamicThemeKey
    pureBlack = pureBlack, onPureBlack = { pureBlack = it },        // PureBlackKey
    onSleepTimer = { showSleepSheet = true },                      // BlazeSleepTimerDialog
)
```

### 7.4 Make search hit every leaf setting
Today `SettingsScreen` search filters only the 8 top-level rows. Flatten a registry once and search it:

```kotlin
// A leaf entry per real setting, built once (could be generated per screen).
data class SettingLeaf(val title: String, val keywords: String, val screenRoute: String, val anchor: String)

val settingsIndex: List<SettingLeaf> = listOf(
    SettingLeaf("Crossfade", "gapless fade overlap", "settings/player", "crossfade"),
    SettingLeaf("Romanization", "romaji romanize pinyin cyrillic", "settings/romanization", ""),
    SettingLeaf("Lyrics text position", "align left center right", "settings/appearance/look_and_feel", "lyrics"),
    /* … one per leaf; typing "crossfade" navigates straight to it … */
)
```
This is the single fastest way to make a large settings tree feel small — the user stops hunting.

### 7.5 Cheap correctness fixes (do alongside)
- `PreferenceKeys.kt:244` → change `booleanPreferencesKey("albumSortDescending")` to a unique
  `"mixSortDescending"` (Mix/Album descending currently collide).
- `AboutScreen.kt:291` → give each social button a real `contentDescription`.
- Changelog "View on GitHub" URL → point at the real repo (or hide until public).
- Romanization → move `"Play all"` + 12 language names into `values/blazify_strings.xml`; relabel
  the header and "Play all"→"Select all".

---

## 8. Suggested phased roadmap

- **Phase 1 (structure):** carve `AppearanceSettings` into the "Look & Feel" hub;
  pull lyrics settings into one screen; de-dupe `app_language`; adopt
  `Material3SettingsGroup` everywhere. _No new features, pure IA + consistency._
- **Phase 2 (previews):** build `LookAndFeelPreview` + `LookAndFeelDraft`, wire
  theme/mini-player/player focuses to the shared frame; add quick toggles +
  profile header to the landing screen.
- **Phase 3 (search + polish):** flatten the setting registry so search hits every
  leaf; add preview coverage for the ❌ rows in §3.4.
- **Phase 4 (features):** ship the top 2–3 items from §5 by value/effort, then a
  signature feature.

---

## Appendix A — sub-screen line counts
AppearanceSettings 2162 · PlayerSettings 1108 · ThemeScreen 1050 · ContentSettings
1041 · PlayerDesignScreen 838 · AiSettings 663 · AlarmSettings 539 · StorageSettings
492 · BackupAndRestore 470 · AccountSettings 461 · SettingsScreen 343 · AboutScreen
342 · AndroidAutoSettings 342 · ChangelogScreen 282 · PrivacySettings 264 ·
UpdaterSettings 259 · RomanizationSettings 205 · StreamSourcesSettings 198.

## Appendix B — key anchors
- Landing IA: `SettingsScreen.kt:99-155`
- Mega-screen groups: `AppearanceSettings.kt:417,437,586,606,626,652,676,703,1005,1135,1185,1424,1645,1794`
- Component standard: `Material3SettingsGroup.kt:43,210`; deprecated `Preference.kt:33,93`
- Theme fn: `Theme.kt:36`; theme prefs: `MainActivity.kt:530,561,573,579`
- Preview infra: `ThemeScreen.kt` (ThemePhoneFrame/ThemePhonePreview),
  `AppearanceSettings.kt` (MiniPlayerDesignPreview), `PlayerDesignScreen.kt` (gallery)
- Nav tabs: `Screens.kt` (Home, Search, ListenTogether, Yours, Library)
