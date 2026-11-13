# Mindustry Mods — agent workflow

This repository's active mod is `mindustry-tools/`. The other project directories contain legacy standalone source; do not install their JARs alongside Mindustry Tools. The target is the Steam macOS desktop build of Mindustry 160.7.

## For every gameplay or HUD change

1. Edit `mindustry-tools/` and update the relevant usage text in the root `README.md` and `mindustry-tools/README.md`. Keep the existing tool behavior unless the user asks for a behavior change.
2. Build and run all included tests: `cd mindustry-tools && ./build.sh`. This produces `mindustry-tools/dist/mindustry-tools.jar`; **building does not install it**. Automated planner/policy tests do not verify the live in-game HUD.
3. Before installation, check whether Mindustry is running: `pgrep -fl 'Mindustry.app/Contents/MacOS/Mindustry' || true`. **Do not replace the installed JAR while the game is running**, even if the user intends to restart afterward. Ask the user to fully quit Mindustry and confirm it is closed; recheck the process before proceeding. Do not kill the game yourself or silently defer installation without making the pending step explicit.
4. When Mindustry has stopped, run `cd mindustry-tools && ./install.sh`. It rebuilds and tests, checks the JAR, atomically installs it into the Steam mod directory, and moves the five legacy standalone JARs out of the active mods directory. The default directory is `~/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/saves/mods/`. The script supports `MINDUSTRY_JAR` and `MINDUSTRY_MODS` overrides.
5. Verify installation, not just the build: compare `shasum -a 256 mindustry-tools/dist/mindustry-tools.jar "$HOME/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/saves/mods/mindustry-tools.jar"` and confirm the hashes match. Check that no standalone `auto-drill-routers.jar`, `solar-pack.jar`, `battery-pack.jar`, `wall-pack.jar`, or `mega-priority.jar` remains directly in the active mods folder. These can live in `legacy-mindustry-mods-backup/`.
6. Tell the user to restart Mindustry **after** installation and manually check the change in game. State clearly whether the change was built, installed, verified on disk, and/or tested in game; never describe a built-only change as ready after a restart. If installation was blocked because the game is running, explicitly say that the running game still has the old JAR and follow up with the install once the user quits.

If the user asks for a public update, publish only after build/test and commit/release steps; check the release asset matches the intended JAR. Do not imply an uncommitted or locally installed change has reached GitHub.
