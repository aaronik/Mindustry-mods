package megapriority;

import arc.Events;
import arc.struct.IntSeq;
import arc.struct.IntSet;
import arc.struct.IntMap;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.CommandAI;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.Teams.BlockPlan;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.input.InputHandler;
import mindustry.mod.Mod;
import mindustry.ui.Styles;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock.ConstructBuild;

/** Client-side, opt-in controller that selects existing Mega unit commands. */
public class MegaPriorityMod extends Mod{
    private static final long pollMillis = 900L, fallbackMillis = 1800L;
    private final IntSet managed = new IntSet();
    private final IntMap<Priority.Mode> lastRequested = new IntMap<>();
    private final IntMap<Long> requestTime = new IntMap<>();
    private boolean hudInstalled;
    private long lastPoll, lowerSince;
    private Priority.Mode lastDesired;

    public MegaPriorityMod(){
        Events.on(WorldLoadEvent.class, event -> {
            managed.clear(); lastRequested.clear(); requestTime.clear(); lastDesired = null;
        });
        Events.on(ClientLoadEvent.class, event -> {
            if(hudInstalled) return;
            hudInstalled = true;
            Vars.ui.hudGroup.fill(hud -> {
                hud.touchable = arc.scene.event.Touchable.childrenOnly;
                hud.bottom().right();
                hud.visible(() -> ready() && Vars.ui.hudfrag.shown());
                hud.table(t -> t.button("Mega priority", Styles.togglet, this::toggleSelection)
                    .checked(b -> selectedAllManaged()).size(155f,45f)
                    .tooltip("Select Megas, then toggle automatic Rebuild > Repair > Mine."))
                    .padRight(316f).padBottom(159f);
            });
        });
        Events.run(Trigger.update, this::update);
    }

    private boolean ready(){
        return Vars.state != null && Vars.state.isGame() && Vars.player != null &&
            Vars.control != null && Vars.control.input != null;
    }
    private IntSeq selectedMegas(){
        IntSeq result = new IntSeq();
        if(!ready()) return result;
        for(Unit unit : Vars.control.input.selectedUnits){
            if(unit.type() == UnitTypes.mega && unit.team() == Vars.player.team() && unit.controller() instanceof CommandAI){
                result.add(unit.id());
            }
        }
        return result;
    }
    private boolean selectedAllManaged(){
        IntSeq selected = selectedMegas();
        if(selected.size == 0) return false;
        for(int i = 0; i < selected.size; i++) if(!managed.contains(selected.get(i))) return false;
        return true;
    }
    private void toggleSelection(){
        IntSeq selected = selectedMegas();
        if(selected.size == 0){toast("Select one or more friendly Megas first."); return;}
        boolean disable = selectedAllManaged();
        for(int i = 0; i < selected.size; i++){
            int id = selected.get(i);
            if(disable){managed.remove(id); lastRequested.remove(id); requestTime.remove(id);}
            else{managed.add(id); lastRequested.remove(id); requestTime.remove(id);}
        }
        toast((disable ? "Disabled" : "Enabled") + " priority mode for " + selected.size + " Mega(s).");
        if(!disable) poll(true);
    }
    private void toast(String text){
        if(Vars.ui != null && Vars.ui.hudfrag != null) Vars.ui.hudfrag.showToast(text);
    }
    private void update(){
        if(!ready()){
            managed.clear(); lastRequested.clear(); requestTime.clear(); lastDesired = null; return;
        }
        if(Time.timeSinceMillis(lastPoll) < pollMillis) return;
        poll(false);
    }
    private void poll(boolean immediate){
        long now = Time.millis();
        lastPoll = now;
        if(managed.size == 0) return;
        boolean rebuild = hasRebuildWork();
        boolean repair = !rebuild && hasRepairWork();
        Priority.Mode desired = Priority.desired(rebuild,repair);
        if(desired != lastDesired){lastDesired = desired; lowerSince = now;}
        boolean stable = now - lowerSince >= fallbackMillis;
        IntSeq toRebuild = new IntSeq(), toRepair = new IntSeq(), toMine = new IntSeq();
        IntSeq invalid = new IntSeq();
        for(IntSet.IntSetIterator it = managed.iterator(); it.hasNext;){
            int id = it.next();
            Unit unit = Groups.unit.getByID(id);
            if(unit == null || unit.type() != UnitTypes.mega || unit.team() != Vars.player.team() ||
                !(unit.controller() instanceof CommandAI ai)){
                invalid.add(id); continue;
            }
            UnitCommand currentCommand = ai.currentCommand();
            Priority.Mode current = mode(currentCommand);
            Priority.Mode requested = lastRequested.get(id);
            // Give server commands time to arrive before treating a mismatch
            // as a manual change. If the request never lands, stop retrying.
            if(requested != null && current != requested && !immediate &&
                now - requestTime.get(id, 0L) < 3000L) continue;
            if(current == null || requested != null && current != requested && !immediate){
                invalid.add(id); continue;
            }
            Priority.Mode next = Priority.choose(current,desired,unit.buildPlan() != null,stable);
            if(next == current) continue;
            switch(next){
                case rebuild -> toRebuild.add(id);
                case repair -> toRepair.add(id);
                case mine -> toMine.add(id);
            }
        }
        for(int i = 0; i < invalid.size; i++){
            managed.remove(invalid.get(i)); lastRequested.remove(invalid.get(i)); requestTime.remove(invalid.get(i));
        }
        request(toRebuild,UnitCommand.rebuildCommand,Priority.Mode.rebuild);
        request(toRepair,UnitCommand.repairCommand,Priority.Mode.repair);
        request(toMine,UnitCommand.mineCommand,Priority.Mode.mine);
    }
    private static Priority.Mode mode(UnitCommand command){
        if(command == UnitCommand.rebuildCommand) return Priority.Mode.rebuild;
        if(command == UnitCommand.repairCommand) return Priority.Mode.repair;
        if(command == UnitCommand.mineCommand) return Priority.Mode.mine;
        return null;
    }
    private void request(IntSeq ids,UnitCommand command,Priority.Mode mode){
        if(ids.size == 0) return;
        InputHandler.setUnitCommandChunked(ids,command);
        for(int i = 0; i < ids.size; i++){
            lastRequested.put(ids.get(i),mode);
            requestTime.put(ids.get(i),Time.millis());
        }
    }
    private boolean hasRebuildWork(){
        for(BlockPlan plan : Vars.player.team().data().plans){
            Tile tile = Vars.world.tile(plan.x,plan.y);
            if(plan.removed || tile == null || plan.block == null) continue;
            if(tile.block() == plan.block && !(tile.build instanceof ConstructBuild)) continue;
            if(Build.validPlace(plan.block,Vars.player.team(),plan.x,plan.y,plan.rotation)) return true;
        }
        return false;
    }
    private boolean hasRepairWork(){
        return Vars.indexer.getDamaged(Vars.player.team()).contains(b ->
            b != null && !(b instanceof ConstructBuild) && b.isValid() && b.damaged());
    }
}
