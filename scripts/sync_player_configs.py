#!/usr/bin/env python3
"""Bring our copy of the player config table and player registry up to date.

Usage: sync_player_configs.py <upstream configs> <upstream registry> <player-configs branch dir>

The app reads these files straight from the player-configs branch, so nothing
written here may ever be worse than what is already there:

- a download that doesn't look like a real table (bad JSON, wrong schema, too
  few players) changes nothing and fails the run, so GitHub reports it
- entries are checked with the same rules the app uses, and bad ones are dropped
- players upstream has are taken from upstream; players only we have are kept,
  so a player can never disappear from our copy
- the finished table is checked for a hash listed twice before it is written,
  because the app throws away a whole file that has one
"""
import json
import re
import sys
from pathlib import Path

SIG = re.compile(r"^[A-Za-z0-9$_]{1,8}\(\d+,\d+,INPUT\)$")
NCLASS = re.compile(r"^[A-Za-z0-9$_]{1,8}$")
HASH = re.compile(r"^[a-f0-9]{8}$")
MIN_PLAYERS = 100


def valid_entry(player_hash, entry):
    if not HASH.match(player_hash) or not isinstance(entry, dict):
        return False
    sig, n_class, sts = entry.get("sig"), entry.get("nClass"), entry.get("sts")
    aliases = entry.get("aliases", [])
    return (
        isinstance(sig, str) and SIG.match(sig) is not None
        and isinstance(n_class, str) and NCLASS.match(n_class) is not None
        and isinstance(sts, int) and not isinstance(sts, bool) and sts > 0
        and isinstance(aliases, list)
        and all(isinstance(alias, str) and HASH.match(alias) for alias in aliases)
    )


def keys_of(player_hash, entry):
    return [player_hash, *entry.get("aliases", [])]


def read_configs(path):
    root = json.loads(Path(path).read_text())
    players = root.get("players") if isinstance(root, dict) else None
    if root.get("schemaVersion") != 1 or not isinstance(players, dict):
        raise ValueError(f"{path} is not a schema 1 player table")
    good = {h: e for h, e in players.items() if valid_entry(h, e)}
    return good, len(players) - len(good)


def merge_configs(ours, theirs):
    taken = {key for h, e in theirs.items() for key in keys_of(h, e)}
    merged = {}
    for h, e in ours.items():
        if h in theirs:
            merged[h] = theirs[h]
        elif not set(keys_of(h, e)) & taken:
            merged[h] = e
    for h, e in theirs.items():
        merged.setdefault(h, e)
    return merged


def repeated_key(players):
    seen = set()
    for h, e in players.items():
        for key in keys_of(h, e):
            if key in seen:
                return key
            seen.add(key)
    return None


def format_configs(players):
    rows = []
    for h, e in players.items():
        fields = [f'"sig": {json.dumps(e["sig"])}', f'"nClass": {json.dumps(e["nClass"])}', f'"sts": {e["sts"]}']
        if e.get("aliases"):
            fields.append('"aliases": [' + ", ".join(json.dumps(a) for a in e["aliases"]) + "]")
        rows.append(f'    "{h}": {{ ' + ", ".join(fields) + " }")
    return '{\n  "schemaVersion": 1,\n  "players": {\n' + ",\n".join(rows) + "\n  }\n}\n"


def sync_configs(upstream_path, ours_path):
    theirs, skipped = read_configs(upstream_path)
    if len(theirs) < MIN_PLAYERS:
        raise ValueError(f"upstream table has only {len(theirs)} usable players")
    ours = read_configs(ours_path)[0] if ours_path.exists() else {}
    merged = merge_configs(ours, theirs)
    repeated = repeated_key(merged)
    if repeated:
        raise ValueError(f"the merged table would list {repeated} twice")
    text = format_configs(merged)
    if not ours_path.exists() or ours_path.read_text() != text:
        ours_path.write_text(text)
    print(f"player_configs.json: {len(merged)} players, {len(set(merged) - set(ours))} new, "
          f"{skipped} bad upstream entries skipped")


def read_registry(path):
    root = json.loads(Path(path).read_text())
    players = root.get("players") if isinstance(root, dict) else None
    if not isinstance(players, list):
        raise ValueError(f"{path} has no players list")
    good = [
        p for p in players
        if isinstance(p, dict)
        and isinstance(p.get("playerHash"), str) and HASH.match(p["playerHash"])
        and isinstance(p.get("firstSeenAt"), str)
    ]
    return root, good


def sync_registry(upstream_path, ours_path):
    root, theirs = read_registry(upstream_path)
    if len(theirs) < MIN_PLAYERS:
        raise ValueError(f"upstream registry has only {len(theirs)} usable players")
    ours_root, ours = read_registry(ours_path) if ours_path.exists() else ({}, [])
    known = {p["playerHash"] for p in theirs}
    players = theirs + [p for p in ours if p["playerHash"] not in known]
    # The upstream file is stamped on every run; only write when a player or the current one changed.
    if ours_root.get("players") == players and ours_root.get("current") == root.get("current"):
        print(f"player-registry.json: {len(players)} players, unchanged")
        return
    ours_path.write_text(json.dumps({**root, "players": players}, indent=2) + "\n")
    print(f"player-registry.json: {len(players)} players")


def main():
    upstream_configs, upstream_registry, branch = sys.argv[1], sys.argv[2], Path(sys.argv[3])
    failed = False
    for sync, upstream, ours in (
        (sync_configs, upstream_configs, branch / "player_configs.json"),
        (sync_registry, upstream_registry, branch / "player-registry.json"),
    ):
        try:
            sync(upstream, ours)
        except Exception as error:
            print(f"::error::{ours.name} kept as it was: {error}")
            failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
