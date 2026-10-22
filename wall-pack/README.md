# Wall-pack (Mindustry Steam 160.7)

Separate desktop mod. Select a wall type (small or large) in the build menu, click **Wall-pack** above Battery-pack, then drag a rectangle across the map. On release it queues as many **2×2 large walls** of that material as the heuristic can fit, followed by **1×1 walls** on every remaining legal tile. No construction is instant. Copper walls are the fallback if no supported wall type is selected.

Supported pairs: copper, titanium, plastanium, thorium, phase, surge, beryllium, tungsten, and reinforced surge. Both wall sizes must be unlocked. Existing structures and your current builder's queued placements are not replaced. All wall footprints stay inside the selection; game placement rules still apply. Click the button again before dragging to cancel.

The planner searches four 2×2 grid offsets and greedily fills valid gaps. It prioritizes large-wall count over small-wall count, but does not guarantee a global optimum around irregular obstacles. Selection limit: 60 tiles per side, 2,500 total tiles.

Run `./build.sh` to compile against the installed Steam JAR and execute layout tests. Override with `MINDUSTRY_JAR=/path/to/desktop.jar`. The result is `dist/wall-pack.jar`. The in-game drag/UI behavior has not been manually tested.
