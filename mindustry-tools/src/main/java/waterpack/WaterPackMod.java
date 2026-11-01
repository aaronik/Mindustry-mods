package waterpack;

import arc.Core;
import arc.Events;
import arc.input.InputProcessor;
import arc.input.KeyCode;
import autodrill.Planner;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.mod.Mod;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;

import java.util.HashSet;
import java.util.Set;

/** Click a water patch to queue pumps and a connected liquid-router border. */
public class WaterPackMod extends Mod{
    private boolean armed,hooksInstalled;
    private final InputProcessor clickHandler=new InputProcessor(){
        @Override public boolean touchDown(int sx,int sy,int pointer,KeyCode button){
            if(!armed || pointer!=0 || button!=KeyCode.mouseLeft || !ready() ||
                Core.scene.hasMouse() || Core.scene.hasDialog()) return false;
            armed=false;
            var point=Core.input.mouseWorld(sx,sy);
            queueAt(Vars.world.toTile(point.x),Vars.world.toTile(point.y));
            return true;
        }
    };
    public WaterPackMod(){
        Events.on(ClientLoadEvent.class,event -> {
            if(hooksInstalled)return;
            hooksInstalled=true;
            Core.input.getInputProcessors().insert(0,clickHandler);
        });
        Events.run(Trigger.update,()->{if(armed && !ready())armed=false;});
    }
    public boolean armed(){return armed;}
    public void toggle(){
        if(!ready())return;
        if(armed){armed=false;return;}
        if(!Blocks.mechanicalPump.unlockedNow() || !Blocks.mechanicalPump.isPlaceable() ||
            !Blocks.liquidRouter.unlockedNow() || !Blocks.liquidRouter.isPlaceable()){
            toast("Unlock Mechanical Pumps and Liquid Routers first.");return;
        }
        armed=true;
    }
    private boolean ready(){
        return Vars.state!=null && Vars.state.isGame() && Vars.player!=null &&
            !Vars.player.dead() && Vars.player.isBuilder() && Vars.player.unit()!=null &&
            Vars.control!=null && Vars.control.input!=null;
    }
    private void toast(String message){
        if(Vars.ui!=null && Vars.ui.hudfrag!=null)Vars.ui.hudfrag.showToast(message);
    }
    private void queueAt(int x,int y){
        Tile origin=Vars.world.tile(x,y);
        if(origin==null || origin.floor().liquidDrop!=Liquids.water){
            toast("Click a water tile.");return;
        }
        Block pump=Blocks.mechanicalPump,router=Blocks.liquidRouter;
        Set<Planner.Point> reserved=new HashSet<>();
        for(BuildPlan plan:Vars.player.unit().plans()) if(!plan.breaking && plan.block!=null){
            reserved.addAll(Planner.footprint(new Planner.Point(plan.x,plan.y),
                plan.block.size,plan.block.sizeOffset));
        }
        Planner.Grid grid=new Planner.Grid(){
            public boolean inBounds(int tx,int ty){return Vars.world.tile(tx,ty)!=null;}
            public boolean ore(int tx,int ty){
                Tile t=Vars.world.tile(tx,ty);
                return t!=null && t.floor().liquidDrop==Liquids.water;
            }
            public boolean mineable(int tx,int ty){return ore(tx,ty);}
            public boolean free(int tx,int ty){
                Tile t=Vars.world.tile(tx,ty);
                return t!=null && t.build==null &&
                    (t.block()==Blocks.air || t.block().alwaysReplace) &&
                    !reserved.contains(new Planner.Point(tx,ty));
            }
            public boolean validDrill(int tx,int ty){
                return Build.validPlace(pump,Vars.player.team(),tx,ty,0);
            }
            public boolean validRouter(int tx,int ty){
                return Build.validPlace(router,Vars.player.team(),tx,ty,0);
            }
        };
        Planner.Layout layout=WaterPlanner.plan(grid,x,y,pump.size,pump.sizeOffset,1024);
        if(layout.drills().isEmpty()){
            toast("No placeable pump and liquid-router network on this patch.");return;
        }
        for(Planner.Point p:layout.drills()){
            BuildPlan plan=new BuildPlan(p.x(),p.y(),0,pump);
            pump.onNewPlan(plan);
            Vars.player.unit().addBuild(plan);
        }
        for(Planner.Point p:layout.routers()){
            BuildPlan plan=new BuildPlan(p.x(),p.y(),0,router);
            router.onNewPlan(plan);
            Vars.player.unit().addBuild(plan);
        }
    }
}
