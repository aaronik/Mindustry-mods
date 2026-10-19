# Mega Priority

Optional desktop mod for Mindustry Steam 160.7. Select one or more **Megas** with the game's unit-command selection, then click **Mega priority** (above Wall-pack). The selected Megas automatically use the game's existing commands in this order:

1. **Rebuild** destroyed structures listed in your team's rebuild queue when valid places are available.
2. **Repair** damaged friendly structures, excluding construction sites.
3. **Mine** when neither higher-priority job is available.

The mod **does not implement its own mining, healing or rebuilding AI**. It only requests vanilla command changes for opted-in units, using Mindustry's normal multiplayer command mechanism. It checks work about every 0.9 seconds, moves to higher-priority work promptly, and waits ~1.8 seconds before falling back to lower-priority work. It avoids abandoning an active build plan during fallback. The game still decides which buildings to repair, which plans to rebuild, and what to mine. No mineable ore, no available core space, or insufficient construction materials can leave a Mega idle even while assigned to a mode.

On activation, a toast reports how many Megas were enabled and the current target command; the button stays highlighted while all selected Megas are managed. **Keep Shift held while clicking if you use hold-to-command mode.** Click the button with the same selected Megas again to turn the automation off (their last vanilla command remains active). Automation stays enabled until you turn it off with the button or change maps; external command changes are retried after a short grace period. Enablement is local to this client and resets on world load; it is not saved in the map. On multiplayer servers, command changes are subject to server permissions and latency.

Build with `./build.sh` (Java 17+, Mindustry 160.7 desktop JAR). Set `MINDUSTRY_JAR=/path/to/desktop.jar` if necessary. The output is `dist/mega-priority.jar`. Automated tests cover priority and transition behavior, but **in-game command/UI behavior has not been manually tested**. Fully quit and restart the game when installing or updating.
