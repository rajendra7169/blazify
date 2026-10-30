#!/usr/bin/env python3
"""Copies a GitHub release of Blazify to the backup hosts and writes the
website's update file.

The app asks GitHub first. When GitHub cannot answer, it reads
rajendrapandey.info.np/blazify/home.json, which this script writes: the
version, the notes, and for each APK its address on GitLab (fast) and then on
Codeberg (slow, last resort). The app tries them in that order and carries a
broken download on from one to the next. Run after each GitHub release:

    scripts/publish_backup.py v9.17.0

The tag must already be pushed to Codeberg. Notes and APKs are taken from the
GitHub release itself, so every copy says the same thing. Tokens are read from
~/.config/blazify-gitlab-token and ~/.config/blazify-codeberg-token and never
printed. The website change is committed in ~/Documents/GitHub/rajendra-pandey-portfolio
but not pushed; push it with the command printed at the end. (Until 2026-10-17 the site
was ~/Documents/GitHub/portfolio-site; that folder no longer publishes anything.)
"""
import json
import os
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

GITHUB = "https://api.github.com/repos/rajendra7169/blazify"
GITLAB_PROJECT = os.environ.get("GITLAB_PROJECT", "hackerraja7169/blazify")
GITLAB = "https://gitlab.com/api/v4/projects/" + urllib.parse.quote(GITLAB_PROJECT, safe="")
CODEBERG = "https://codeberg.org/api/v1/repos/" + os.environ.get("CODEBERG_REPO", "rajaindra7169/blazify")
# The live site since 2026-10-17; it serves public/ as the site's root.
SITE = Path("~/Documents/GitHub/rajendra-pandey-portfolio").expanduser()


def token(name):
    return Path(f"~/.config/blazify-{name}-token").expanduser().read_text().strip()


def call(url, data=None, method=None, headers=None, raw=False):
    req = urllib.request.Request(url, data=data, method=method, headers=headers or {})
    if url.startswith("https://codeberg.org"):
        req.add_header("Authorization", f"token {token('codeberg')}")
    elif url.startswith("https://gitlab.com"):
        req.add_header("PRIVATE-TOKEN", token("gitlab"))
    with urllib.request.urlopen(req, timeout=900) as r:
        body = r.read()
    return body if raw else json.loads(body or b"null")


def to_gitlab(version, apk, blob):
    """GitLab's package registry: a plain file at a fixed address, no release needed."""
    url = f"{GITLAB}/packages/generic/blazify/{version}/{apk['name']}"
    try:
        with urllib.request.urlopen(urllib.request.Request(url, method="HEAD"), timeout=60) as r:
            if int(r.headers.get("Content-Length", -1)) == apk["size"]:
                print(f"  GitLab   {apk['name']}: already there")
                return url
    except urllib.error.HTTPError:
        pass
    call(url, data=blob(), method="PUT", headers={"Content-Type": "application/octet-stream"})
    print(f"  GitLab   {apk['name']}: uploaded")
    return url


def codeberg_release(tag, gh):
    try:
        return call(f"{CODEBERG}/releases/tags/{tag}")
    except urllib.error.HTTPError as e:
        if e.code != 404:
            raise
        return call(f"{CODEBERG}/releases", method="POST",
                    headers={"Content-Type": "application/json"},
                    data=json.dumps({"tag_name": tag, "name": gh["name"] or tag,
                                     "body": gh["body"] or "", "prerelease": gh["prerelease"]}).encode())


def to_codeberg(rel, apk, blob):
    have = {a["name"]: a for a in rel.get("assets", [])}
    if apk["name"] in have and have[apk["name"]]["size"] == apk["size"]:
        print(f"  Codeberg {apk['name']}: already there")
        return have[apk["name"]]["browser_download_url"]
    boundary = "blazify" + os.urandom(8).hex()
    form = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"attachment\"; "
            f"filename=\"{apk['name']}\"\r\nContent-Type: application/vnd.android.package-archive\r\n\r\n"
            ).encode() + blob() + f"\r\n--{boundary}--\r\n".encode()
    up = call(f"{CODEBERG}/releases/{rel['id']}/assets?name={apk['name']}", data=form, method="POST",
              headers={"Content-Type": f"multipart/form-data; boundary={boundary}"})
    print(f"  Codeberg {apk['name']}: uploaded")
    return up["browser_download_url"]


def main(tag):
    gh = call(f"{GITHUB}/releases/tags/{tag}")
    version = tag.removeprefix("v")
    # The F-Droid build is never offered by the in-app updater, so it is not copied.
    apks = [a for a in gh["assets"] if a["name"].endswith(".apk") and "-izzy" not in a["name"].lower()]
    print(f"{tag}: {len(apks)} APKs on GitHub")
    rel = codeberg_release(tag, gh)

    listed = []
    for a in apks:
        cache = {}

        def blob(a=a, cache=cache):
            if "b" not in cache:
                b = call(a["browser_download_url"], raw=True)
                if len(b) != a["size"]:
                    sys.exit(f"  {a['name']}: downloaded {len(b)} bytes, GitHub says {a['size']}")
                cache["b"] = b
            return cache["b"]

        urls = [to_gitlab(version, a, blob), to_codeberg(rel, a, blob)]
        listed.append({"name": a["name"], "size": a["size"], "urls": urls})

    home = {
        "_about": "Blazify's backup update source, written by scripts/publish_backup.py. The app "
                  "reads this only when GitHub cannot answer, and tries each APK's urls in order.",
        "latest": {
            "version": version,
            "date": gh["published_at"][:10],
            "notes": gh["body"] or "",
            "prerelease": gh["prerelease"],
            "apks": listed,
        },
    }
    out = SITE / "public/blazify/home.json"
    out.write_text(json.dumps(home, indent=2, ensure_ascii=False) + "\n")
    git = ["git", "-C", str(SITE), "-c", "user.name=Rajendra Pandey",
           "-c", "user.email=rajendrapandey199971@gmail.com"]
    subprocess.run(git + ["add", "public/blazify/home.json"], check=True)
    if subprocess.run(git + ["diff", "--cached", "--quiet"]).returncode:
        subprocess.run(git + ["commit", "-q", "-m", f"Blazify {version} on the backup hosts"], check=True)
        print("Website file committed. Publish it with:\n"
              f"  git -C {SITE} push https://github.com/rajendra7169/portfolio.git main")
    else:
        print("Website file already up to date")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
