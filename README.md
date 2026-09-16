# player-configs

Data branch, not code. The app downloads these two files at runtime:

- `player_configs.json`: how to read each YouTube player script (signature and n functions, signature timestamp). A new player gets its entry here and phones pick it up without an app update.
- `player-registry.json`: when each player was first seen, shown in the song details sheet.
