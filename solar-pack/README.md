# Solar-pack (Mindustry Steam 160.7)

Separate desktop client mod. Click the **Solar-pack** button on the bottom edge, directly above the Auto-drill button, aligned on their right edges, then drag a rectangle on the map. A selection outline appears while dragging. Releasing the mouse queues as many 3×3 large solar panels as the layout heuristic can fit, then fills remaining legal tiles with 1×1 small batteries. Nothing builds instantly.

Requires both blocks to be unlocked/placeable. It skips existing buildings, the local builder's queued placements, invalid terrain, and map restrictions. The entire footprint of every panel must be inside the rectangle. Selection limit: 200 tiles per side and 40,000 tiles in area. A fully open 200×200 rectangle queues about 5,152 build plans, so construction itself may take considerable time. Click the button again before dragging to cancel. There is no automatic power node or outgoing connection.

Packing searches all nine 3×3 grid offsets and greedily fills valid gaps. It prioritizes **panel count first**, battery count second. This is heuristic and does not mathematically guarantee the maximum panel count in an irregular area. Battery infill uses every remaining individually legal free tile.

Run `./build.sh` to compile against the locally installed Steam JAR and run layout tests. Set `MINDUSTRY_JAR=/path/to/desktop.jar` to override. The compiled JAR is `dist/solar-pack.jar`; import it with Mindustry's Mods menu or place it in the active Steam saves/mods directory. In-game drag and UI placement have not been manually tested.

**Updating safely:** Fully quit Mindustry before installing updated JARs; on this Steam macOS installation use `./install.sh` to build, verify the helper classes, and replace the installed JAR atomically. Overwriting a live Java mod archive in place may leave later-loaded classes unavailable until restart.
