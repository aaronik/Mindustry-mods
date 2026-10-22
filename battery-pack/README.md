# Battery-pack (Mindustry Steam 160.7)

A separate desktop construction-planner mod. Click **Battery-pack** immediately left of Solar-pack, then drag a rectangle on the map. It queues as many **3×3 large batteries** as the layout heuristic can fit, then fills remaining legal tiles with **1×1 small batteries**. It doesn't build anything instantly.

Both battery types must be unlocked and placeable. It skips existing structures and the local player's queued placements. Natural props that Mindustry normally clears automatically can be built over. Large batteries stay entirely within the selected rectangle. Maximum selection: 200 tiles per side and 40,000 tiles total. Click the button again before dragging to cancel. Activation, cancellation and success are silent; invalid actions show error toasts.

The layout planner searches all nine possible 3×3 grid offsets and greedily fills gaps. It prioritizes large-battery count, then small-battery count; it does not guarantee a global optimum for irregular obstacles. It does not add power nodes or output connections.

Build with `./build.sh` (Java 17+ and a Mindustry 160.7 desktop JAR; set `MINDUSTRY_JAR=/path/to/desktop.jar` if needed). Output: `dist/battery-pack.jar`. Tests cover packing, obstacles, 200×200 selection, and replaceable terrain. In-game interaction has not been manually tested. Fully quit and restart Mindustry after installing or updating.
