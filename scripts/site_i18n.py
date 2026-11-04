#!/usr/bin/env python3
"""
The website in other languages, built from the English page.

docs/index.html is the only page anybody edits. This script reads it, finds every
piece of text a visitor reads (a paragraph, a heading, a button, with its bold and
links kept inside it as one unit), and writes a full copy of the page per language
to docs/<lang>/index.html, with that language's text, its own address, and links
telling search engines which page is which language.

    python3 scripts/site_i18n.py extract   # website-i18n/source.json: every English unit
    python3 scripts/site_i18n.py build     # docs/<lang>/index.html for each website-i18n/<lang>.json
    python3 scripts/site_i18n.py missing   # what each language still lacks after the English changed

A unit with no translation stays English on that page, so a change to the English
page never breaks the other languages; it only shows up in `missing`.
"""
import html
import json
import posixpath
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
# Every page that has a copy in each language, as its folder under docs/ ("" is the
# home page). A page not written yet is simply skipped.
PAGES = ["", "spotify-alternative/", "youtube-music-no-ads/", "iphone/", "windows/", "linux/", "compare/"]
STRINGS = ROOT / "website-i18n"
SITE = "https://rajendra7169.github.io/blazify/"

# Code, the name shown in the picker in its own language, and the page's direction.
LANGUAGES = [
    ("en", "English", "ltr"),
    ("es", "Español", "ltr"),
    ("pt", "Português", "ltr"),
    ("fr", "Français", "ltr"),
    ("de", "Deutsch", "ltr"),
    ("it", "Italiano", "ltr"),
    ("ru", "Русский", "ltr"),
    ("tr", "Türkçe", "ltr"),
    ("ar", "العربية", "rtl"),
    ("hi", "हिन्दी", "ltr"),
    ("id", "Bahasa Indonesia", "ltr"),
    ("vi", "Tiếng Việt", "ltr"),
    ("ja", "日本語", "ltr"),
    ("ko", "한국어", "ltr"),
    ("zh-CN", "简体中文", "ltr"),
    ("zh-TW", "繁體中文", "ltr"),
]

INLINE = {"a", "b", "strong", "i", "em", "span", "small", "br", "code", "sup", "sub", "abbr",
          "kbd", "mark", "s", "u", "time", "wbr", "q"}
# Kept whole and never looked into: their text is code, styling or drawing.
OPAQUE = ("script", "style", "svg", "template", "noscript")
TRANSLATED_ATTRS = ("alt", "title", "aria-label", "placeholder")
TRANSLATED_META = {"description", "og:title", "og:description", "og:image:alt",
                   "twitter:title", "twitter:description", "twitter:image:alt"}

TOKEN = re.compile(r"<!--.*?-->|<[^>]+>|[^<]+", re.S)


def mask(page):
    """Opaque blocks swapped for markers, so the tokenizer never walks into them."""
    kept = []

    def hide(m):
        kept.append(m.group(0))
        return f"\x00{len(kept) - 1}\x00"

    for tag in OPAQUE:
        page = re.sub(rf"<{tag}\b.*?</{tag}>", hide, page, flags=re.S | re.I)
    # The language picker is written per page by with_shell(), never translated.
    page = re.sub(r'<details class="lang".*?</details>', hide, page, flags=re.S)
    return page, kept


def unmask(page, kept):
    # Repeated, because a hidden block can hold another one (the picker holds its icon).
    while "\x00" in page:
        page = re.sub(r"\x00(\d+)\x00", lambda m: kept[int(m.group(1))], page)
    return page


def tag_name(tok):
    m = re.match(r"</?\s*([a-zA-Z0-9-]+)", tok)
    return m.group(1).lower() if m else None


def readable(fragment):
    """Has words a person reads, not just a number, a symbol or whitespace."""
    text = html.unescape(re.sub(r"<[^>]+>", "", fragment))
    return bool(re.search(r"[^\W\d_]{2,}", text))


def norm(s):
    return re.sub(r"\s+", " ", s).strip()


def units(page):
    """
    (start, end, source) for every run of text and inline tags, in page order.
    A run ends at any block tag, comment or opaque marker, so each unit is one
    paragraph, heading, list item, label or button text.
    """
    out = []
    run = []  # (start, end, token)

    def close():
        while run and not run[0][2].strip():
            run.pop(0)
        while run and not run[-1][2].strip():
            run.pop()
        # An inline tag left open or closed at an edge belongs outside the unit.
        while run and run[-1][2].startswith("<") and not run[-1][2].startswith("</") \
                and tag_name(run[-1][2]) in INLINE and tag_name(run[-1][2]) != "br":
            run.pop()
        while run and run[0][2].startswith("</"):
            run.pop(0)
        # A closing tag whose opening sits before the run (a link that starts at
        # an icon) belongs outside it too, or the whole unit would be dropped.
        while run and run[-1][2].startswith("</") and not balanced(page[run[0][0]:run[-1][1]]):
            run.pop()
            while run and not run[-1][2].strip():
                run.pop()
        if run:
            frag = page[run[0][0]:run[-1][1]]
            if readable(frag) and balanced(frag):
                out.append((run[0][0], run[-1][1], frag))
        run.clear()

    for m in TOKEN.finditer(page):
        tok = m.group(0)
        if tok.startswith("<!--") or "\x00" in tok:
            close()
            continue
        if tok.startswith("<"):
            if tag_name(tok) in INLINE:
                run.append((m.start(), m.end(), tok))
            else:
                close()
        else:
            run.append((m.start(), m.end(), tok))
    close()
    return out


def balanced(frag):
    stack = []
    for t in re.findall(r"<[^>]+>", frag):
        name = tag_name(t)
        if name in ("br", "wbr") or t.endswith("/>"):
            continue
        if t.startswith("</"):
            if not stack or stack[-1] != name:
                return False
            stack.pop()
        else:
            stack.append(name)
    return not stack


def attr_units(page):
    """(start, end, source) for readable attribute values and meta descriptions."""
    out = []
    for m in re.finditer(r"<[a-zA-Z][^>]*>", page):
        tag = m.group(0)
        if tag.startswith("<meta"):
            key = re.search(r'(?:name|property)="([^"]+)"', tag)
            if key and key.group(1) in TRANSLATED_META:
                c = re.search(r'content="([^"]*)"', tag)
                if c and readable(c.group(1)):
                    out.append((m.start() + c.start(1), m.start() + c.end(1), c.group(1)))
            continue
        for a in TRANSLATED_ATTRS:
            v = re.search(rf'\s{a}="([^"]*)"', tag)
            if v and readable(v.group(1)):
                out.append((m.start() + v.start(1), m.start() + v.end(1), v.group(1)))
    return out


def existing_pages():
    return [p for p in PAGES if (DOCS / p / "index.html").exists()]


def source_units(path=""):
    page = (DOCS / path / "index.html").read_text(encoding="utf-8")
    masked, kept = mask(page)
    return page, masked, kept, units(masked) + attr_units(masked)


def tags_of(s):
    return sorted(re.sub(r"\s+", " ", t) for t in re.findall(r"<[^>]+>", s))


def extract():
    found = []
    for path in existing_pages():
        found += sorted(source_units(path)[3])
    seen, ordered = set(), []
    for _, _, src in found:
        key = norm(src)
        if key not in seen:
            seen.add(key)
            ordered.append(key)
    STRINGS.mkdir(exist_ok=True)
    (STRINGS / "source.json").write_text(json.dumps(ordered, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    words = sum(len(html.unescape(re.sub(r"<[^>]+>", " ", s)).split()) for s in ordered)
    print(f"{len(ordered)} units, {words} words -> {STRINGS / 'source.json'}")


def load(code):
    f = STRINGS / f"{code}.json"
    return json.loads(f.read_text(encoding="utf-8")) if f.exists() else {}


def check(code, table):
    """A translation that dropped or changed a tag is not used: the English stays instead."""
    bad = [k for k, v in table.items() if tags_of(k) != tags_of(v)]
    return {k: v for k, v in table.items() if k not in bad and v.strip()}, bad


def relocate(page, path=""):
    """
    Relative addresses one folder deeper, except links between pages that exist in
    every language: those stay inside the language they were followed from.
    """
    translated = {norm_dir(p) for p in PAGES}

    def fix(m):
        attr, val = m.group(1), m.group(2)
        if attr == "srcset":
            parts = [p.strip() for p in val.split(",")]
            return f'{attr}="' + ", ".join(fix_one(p) for p in parts) + '"'
        return f'{attr}="{fix_one(val)}"'

    def fix_one(v):
        if not v or v.startswith(("#", "http:", "https:", "//", "mailto:", "data:", "/", "tel:", "javascript:")):
            return v
        target = posixpath.normpath(posixpath.join(path, re.split(r"[#?]", v)[0] or "."))
        if norm_dir(target) in translated and not re.search(r"\.[a-z0-9]+$", re.split(r"[#?]", v)[0]):
            return v
        return "../" + v

    return re.sub(r'\b(href|src|srcset|poster)="([^"]*)"', fix, page)


def norm_dir(p):
    p = posixpath.normpath(p or ".").strip("/")
    return "" if p in (".", "index.html") else p.removesuffix("/index.html")


def url_of(code, path):
    return SITE + ("" if code == "en" else code + "/") + path


def alternates(path):
    lines = [f'<link rel="alternate" hreflang="{c}" href="{url_of(c, path)}">' for c, _, _ in LANGUAGES]
    lines.append(f'<link rel="alternate" hreflang="x-default" href="{url_of("en", path)}">')
    return "\n".join(lines)


def picker(current, path):
    depth = (current != "en") + path.count("/")
    items = []
    for c, name, _ in LANGUAGES:
        target = "../" * depth + ("" if c == "en" else c + "/") + path
        target = target or "./"
        cur = ' aria-current="page"' if c == current else ""
        items.append(f'        <li><a href="{target}" hreflang="{c}" lang="{c}"{cur}>{name}</a></li>')
    return "\n".join(items)


def with_shell(page, code, direction, path=""):
    """The parts that differ by language besides the text: lang, address, picker."""
    url = url_of(code, path)
    page = re.sub(r'<html lang="[^"]*"[^>]*>', f'<html lang="{code}" dir="{direction}">', page, count=1)
    page = re.sub(r'<link rel="canonical" href="[^"]*">', f'<link rel="canonical" href="{url}">', page, count=1)
    page = re.sub(r'<meta property="og:url" content="[^"]*">', f'<meta property="og:url" content="{url}">', page, count=1)
    page = re.sub(r'\n?(<link rel="alternate" hreflang="[^"]*" href="[^"]*">\n?)+', "\n", page)
    page = page.replace(f'<link rel="canonical" href="{url}">', f'<link rel="canonical" href="{url}">\n' + alternates(path), 1)
    page = re.sub(r'(<ul id="langList">)\s*.*?(\s*</ul>)', lambda m: m.group(1) + "\n" + picker(code, path) + m.group(2),
                  page, count=1, flags=re.S)
    short = code.split("-")[0].upper() if code not in ("zh-CN", "zh-TW") else {"zh-CN": "简", "zh-TW": "繁"}[code]
    page = re.sub(r'(<summary aria-label=")[^"]*("><svg class="ic" aria-hidden="true"><use href="#i-globe"/></svg><span>)[^<]*(</span>)',
                  lambda m: m.group(1) + "Language" + m.group(2) + short + m.group(3), page, count=1)
    return page


def build():
    report = {}
    for path in existing_pages():
        page, masked, kept, found = source_units(path)
        # The English page itself gets the picker and the alternate links too.
        english = with_shell(page, "en", "ltr", path)
        if english != page:
            (DOCS / path / "index.html").write_text(english, encoding="utf-8")
            page, masked, kept, found = source_units(path)
        for code, _, direction in LANGUAGES[1:]:
            table, bad = check(code, load(code))
            out, last, hit = [], 0, 0
            for start, end, src in sorted(found):
                out.append(masked[last:start])
                done = table.get(norm(src))
                if done is not None:
                    hit += 1
                out.append(done if done is not None else src)
                last = end
            out.append(masked[last:])
            translated = unmask("".join(out), kept)
            translated = with_shell(relocate(translated, path), code, direction, path)
            target = DOCS / code / path / "index.html"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(translated, encoding="utf-8")
            r = report.setdefault(code, [0, 0, len(bad)])
            r[0] += hit
            r[1] += len(found)
    for code, (hit, total, bad) in report.items():
        print(f"{code:6} {hit}/{total} units translated over {len(existing_pages())} pages" + (f", {bad} refused (tags changed)" if bad else ""))
    sitemap()


def sitemap():
    entries, ours = [], set()
    for path in existing_pages():
        links = "\n".join(f'    <xhtml:link rel="alternate" hreflang="{c}" href="{url_of(c, path)}"/>' for c, _, _ in LANGUAGES)
        for c, _, _ in LANGUAGES:
            ours.add(url_of(c, path))
            entries.append(f"  <url>\n    <loc>{url_of(c, path)}</loc>\n{links}\n  </url>")
    old = (DOCS / "sitemap.xml").read_text(encoding="utf-8")
    for loc in re.findall(r"<loc>([^<]+)</loc>", old):
        if loc not in ours and loc.rstrip("/") + "/" not in ours:
            entries.append(f"  <url>\n    <loc>{loc}</loc>\n  </url>")
    (DOCS / "sitemap.xml").write_text(
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">\n'
        + "\n".join(entries) + "\n</urlset>\n", encoding="utf-8")


def missing():
    source = json.loads((STRINGS / "source.json").read_text(encoding="utf-8"))
    for code, _, _ in LANGUAGES[1:]:
        table, bad = check(code, load(code))
        lack = [s for s in source if s not in table]
        print(f"{code:6} missing {len(lack)}" + (f", refused {len(bad)}" if bad else ""))


if __name__ == "__main__":
    {"extract": extract, "build": build, "missing": missing}[sys.argv[1] if len(sys.argv) > 1 else "build"]()
