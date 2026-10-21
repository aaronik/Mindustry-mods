package autodrill;

import arc.Core;
import arc.Events;
import arc.input.InputProcessor;
import arc.input.KeyCode;
import arc.scene.event.Touchable;
import mindustry.ui.Styles;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.mod.Mod;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.production.Drill;

import java.util.HashSet;
import java.util.Set;

/** Desktop tool: select a drill, click the HUD button, then click an ore patch. */
public class AutoDrillMod extends Mod{
    private boolean armed;
    private boolean hudInstalled;
    private Drill drill;
    private final InputProcessor clickHandler = new InputProcessor(){
        @Override public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button){
            if(!armed || button != KeyCode.mouseLeft || pointer != 0 || !ready() ||
                Core.scene.hasMouse() || Core.scene.hasDialog()) return false;
            armed = false;
            int x = Vars.world.toTile(Core.input.mouseWorld(screenX, screenY).x);
            int y = Vars.world.toTile(Core.input.mouseWorld(screenX, screenY).y);
            queueAt(x, y);
            return true; // don't mine or select a block on this click
        }
    };

    public AutoDrillMod(){
        Events.on(ClientLoadEvent.class, event -> {
            if(hudInstalled) return;
            hudInstalled = true;
            // Process an armed click before normal game input.
            Core.input.getInputProcessors().insert(0, clickHandler);
            // Build menu occupies the bottom-right; place the toggle along
            // the bottom immediately to its left, outside its control area.
            Vars.ui.hudGroup.fill(hud -> {
                hud.touchable = Touchable.childrenOnly;
                hud.bottom().right();
                hud.visible(() -> ready() && Vars.ui.hudfrag.shown() && !Vars.control.input.commandMode);
                hud.table(buttons -> {
                    buttons.button("Auto-drill", Styles.togglet, this::toggle)
                        .checked(button -> armed).size(155f, 45f)
                        .tooltip("Select a drill, click Auto-drill, then click ore. Click again to cancel.");
                }).padRight(316f).padBottom(6f);
            });
        });
        Events.run(Trigger.update, () -> {
            if(armed && !ready()) armed = false;
        });
    }

    private void toggle(){
        if(!ready()) return;
        if(armed){armed = false; return;}
        Block selected = Vars.control.input.block;
        drill = selected instanceof Drill selectedDrill ? selectedDrill : (Drill)Blocks.pneumaticDrill;
        if(!drill.unlockedNow() || !drill.isPlaceable()){
            toast(drill == Blocks.pneumaticDrill ? "Pneumatic drill unavailable; select an unlocked drill first." :
                "Selected drill is not unlocked or placeable."); return;
        }
        armed = true;
    }

    private boolean ready(){
        return Vars.state != null && Vars.state.isGame() && Vars.player != null &&
            !Vars.player.dead() && Vars.player.isBuilder() && Vars.player.unit() != null &&
            Vars.control != null && Vars.control.input != null;
    }
    private void toast(String text){
        if(Vars.ui != null && Vars.ui.hudfrag != null) Vars.ui.hudfrag.showToast(text);
    }
    static boolean replaceableTerrain(Tile tile){
        return tile != null && mayPlaceOver(tile.block() == Blocks.air, tile.block().alwaysReplace, tile.build != null);
    }
    static boolean mayPlaceOver(boolean air, boolean alwaysReplace, boolean hasBuilding){
        return !hasBuilding && (air || alwaysReplace);
    }
    private void queueAt(int x, int y){
        Tile origin = Vars.world.tile(x, y);
        if(origin == null || origin.drop() == null){toast("Click a mineral tile."); return;}
        Item target = origin.drop();
        if(drill == null || !drill.canMine(origin)){
            toast("This drill cannot mine " + target.localizedName + "."); return;
        }
        Block router = Blocks.router;
        if(!router.unlockedNow() || !router.isPlaceable()){
            toast("Router is not available here."); return;
        }
        // Treat ALL queued construction, including other players' visible
        // plans when known, as occupied so this tool never overwrites them.
        Set<Planner.Point> reserved = new HashSet<>();
        for(BuildPlan plan : Vars.player.unit().plans()){
            if(plan.block != null && !plan.breaking){
                reserved.addAll(Planner.footprint(new Planner.Point(plan.x, plan.y), plan.block.size, plan.block.sizeOffset));
            }
        }
        // Limit traversal on huge deposits to avoid a long frame stall.
        Planner.Grid grid = new Planner.Grid(){
            public boolean inBounds(int tx, int ty){return Vars.world.tile(tx, ty) != null;}
            public boolean ore(int tx, int ty){Tile t = Vars.world.tile(tx, ty); return t != null && t.drop() == target;}
            public boolean mineable(int tx, int ty){return drill.canMine(Vars.world.tile(tx, ty));}
            public boolean free(int tx, int ty){
                Tile t = Vars.world.tile(tx, ty);
                return replaceableTerrain(t) && !reserved.contains(new Planner.Point(tx, ty));
            }
            public boolean validDrill(int tx, int ty){return Build.validPlace(drill, Vars.player.team(), tx, ty, 0);}
            public boolean validRouter(int tx, int ty){return Build.validPlace(router, Vars.player.team(), tx, ty, 0);}
        };
        Planner.Layout layout = Planner.plan(grid, x, y, drill.size, drill.sizeOffset, 1024);
        if(layout.drills().isEmpty()){
            toast("No free, valid drill position on this patch."); return;
        }
        for(Planner.Point p : layout.drills()){
            BuildPlan plan = new BuildPlan(p.x(), p.y(), 0, drill);
            drill.onNewPlan(plan);
            Vars.player.unit().addBuild(plan);
        }
        for(Planner.Point p : layout.routers()){
            BuildPlan plan = new BuildPlan(p.x(), p.y(), 0, router);
            router.onNewPlan(plan);
            Vars.player.unit().addBuild(plan);
        }
    }
}
