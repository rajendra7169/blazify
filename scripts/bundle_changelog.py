#!/usr/bin/env python3
"""Refresh the release notes the app carries (app/src/main/assets/changelog.json).

The Changelog page shows these at once and when there is no connection, so they must be
updated for every release, or the page opens on an old version. Takes the newest stable
GitHub releases, and optionally the notes of the release being made, which is not on
GitHub yet:

    scripts/bundle_changelog.py                     # newest 6 published releases
    scripts/bundle_changelog.py --next 9.19.0 notes.md
"""
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

REPO = "rajendra7169/blazify"
ASSET = Path(__file__).resolve().parent.parent / "app/src/main/assets/changelog.json"
KEEP = 6


def readable(body):
    """The notes without the download box at the top: inside the app there is nothing to download."""
    body = (body or "").strip()
    if body.startswith("<div") and "</div>" in body:
        body = body[body.index("</div>") + len("</div>"):].strip()
    while body.startswith("---"):
        body = body[3:].strip()
    return body


def published():
    out = subprocess.run(
        ["gh", "api", f"repos/{REPO}/releases?per_page=30"], capture_output=True, text=True, check=True
    ).stdout
    entries = []
    for r in json.loads(out):
        if r.get("draft") or r.get("prerelease"):
            continue
        tag = r["tag_name"]
        entries.append(
            {
                "tagName": tag,
                "versionName": tag.lstrip("v"),
                "releaseDate": (r.get("published_at") or r.get("created_at"))[:10] + "T00:00:00Z",
                "description": readable(r.get("body")),
            }
        )
    return entries


def main():
    entries = published()
    if "--next" in sys.argv:
        i = sys.argv.index("--next")
        version, notes = sys.argv[i + 1], readable(Path(sys.argv[i + 2]).read_text())
        entries = [e for e in entries if e["versionName"] != version]
        today = datetime.now(timezone.utc).strftime("%Y-%m-%dT00:00:00Z")
        entries.insert(0, {"tagName": f"v{version}", "versionName": version, "releaseDate": today, "description": notes})
    entries = entries[:KEEP]
    ASSET.write_text(json.dumps(entries, ensure_ascii=False, indent=2) + "\n")
    print("bundled:", ", ".join(e["tagName"] for e in entries))


if __name__ == "__main__":
    main()
