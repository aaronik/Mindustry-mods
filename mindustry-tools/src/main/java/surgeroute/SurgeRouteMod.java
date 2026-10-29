package surgeroute;

import arc.Core;
import arc.Events;
import arc.input.InputProcessor;
import arc.input.KeyCode;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.mod.Mod;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;

import java.util.*;

/** Click destination; queue laser-linked Surge Towers from existing power. */
public class SurgeRouteMod extends Mod{
    private static final int nodesPerFrame=1200;
    private boolean armed,hooksInstalled;
    private SurgePlanner.Search search;
    private Object searchWorld;
    private final List<Link> pendingLinks=new ArrayList<>();
    private long lastReconcile;
    private static final class Link{
        final SurgePlanner.Point from,to;
        long lastRequest;
        Link(SurgePlanner.Point from,SurgePlanner.Point to){this.from=from;this.to=to;}
    }
    private final InputProcessor clickHandler=new InputProcessor(){
        @Override public boolean touchDown(int sx,int sy,int pointer,KeyCode button){
            if(!armed || pointer!=0 || button!=KeyCode.mouseLeft || !ready() ||
                Core.scene.hasMouse() || Core.scene.hasDialog()) return false;
            armed=false;
            var world=Core.input.mouseWorld(sx,sy);
            queueTo(Vars.world.toTile(world.x),Vars.world.toTile(world.y));
            return true;
        }
    };
    public SurgeRouteMod(){
        Events.on(ClientLoadEvent.class,event -> {
            if(hooksInstalled) return;
            hooksInstalled=true;
            Core.input.getInputProcessors().insert(0,clickHandler);
        });
        Events.on(WorldLoadEvent.class,event -> {armed=false;search=null;searchWorld=null;pendingLinks.clear();});
        Events.run(Trigger.update,this::update);
    }
    public boolean armed(){return armed;}
    public void toggle(){
        if(!ready()) return;
        if(armed){armed=false;return;}
        if(search!=null){search=null;searchWorld=null;return;}
        if(!Blocks.surgeTower.unlockedNow() || !Blocks.surgeTower.isPlaceable()){
            toast("Unlock Surge Towers first.");return;
        }
        armed=true;
    }
    private boolean ready(){
        return Vars.state!=null && Vars.state.isGame() && Vars.player!=null &&
            !Vars.player.dead() && Vars.player.isBuilder() && Vars.player.unit()!=null &&
            Vars.control!=null && Vars.control.input!=null;
    }
    private void toast(String message){
        if(Vars.ui!=null && Vars.ui.hudfrag!=null) Vars.ui.hudfrag.showToast(message);
    }
    private void update(){
        if(!ready()){
            armed=false;search=null;searchWorld=null;
            return;
        }
        if(search==null){
            if(!pendingLinks.isEmpty() && arc.util.Time.timeSinceMillis(lastReconcile)>450L){
                lastReconcile=arc.util.Time.millis();
                reconcileLinks();
            }
            return;
        }
        if(searchWorld!=Vars.world){search=null;searchWorld=null;return;}
        if(search.advance(nodesPerFrame)){
            SurgePlanner.Route route=search.result();
            search=null;searchWorld=null;
            if(route==null){toast("No placeable Surge Tower route to the selected point.");return;}
            Block tower=Blocks.surgeTower;
            for(int i=0;i<route.towers().size();i++){
                SurgePlanner.Point p=route.towers().get(i);
                SurgePlanner.Point target=i==0 ? route.source().center() : route.towers().get(i-1);
                // Reconcile links when both endpoint buildings exist; vanilla
                // builders may construct queued plans out of order.
                BuildPlan plan=new BuildPlan(p.x(),p.y(),0,tower);
                if(i>0 || !route.usedExistingTower()){
                    // Adjacent Surge Towers conduct even when both laser slots
                    // are full, so no explicit first link is required.
                    pendingLinks.add(new Link(p,target));
                }
                tower.onNewPlan(plan);
                Vars.player.unit().addBuild(plan);
            }
        }
    }
    private void reconcileLinks(){
        var iter=pendingLinks.iterator();
        while(iter.hasNext()){
            Link link=iter.next();
            Building from=Vars.world.build(link.from.x(),link.from.y());
            Building to=Vars.world.build(link.to.x(),link.to.y());
            if(from==null || to==null || from.block!=Blocks.surgeTower ||
                from.power==null || to.power==null || from.team()!=Vars.player.team() ||
                to.team()!=Vars.player.team()) continue;
            if(from.power.links.contains(to.pos())){iter.remove();continue;}
            if(from.power.links.size>=2){
                // Do not disturb existing connections; leave the route link
                // pending rather than toggling a different neighbor off.
                continue;
            }
            if(Blocks.surgeTower instanceof mindustry.world.blocks.power.PowerNode node &&
                node.linkValid(from,to) &&
                !mindustry.world.blocks.power.PowerNode.insulated(from,to) &&
                arc.util.Time.timeSinceMillis(link.lastRequest)>2000L){
                from.configure(to.pos());
                link.lastRequest=arc.util.Time.millis();
            }
        }
    }
    private void queueTo(int x,int y){
        Block tower=Blocks.surgeTower;
        if(Vars.world.tile(x,y)==null){toast("Click inside the map.");return;}
        Set<SurgePlanner.Point> reserved=new HashSet<>();
        for(BuildPlan plan:Vars.player.unit().plans()) if(!plan.breaking && plan.block!=null){
            for(int dx=0;dx<plan.block.size;dx++) for(int dy=0;dy<plan.block.size;dy++){
                reserved.add(new SurgePlanner.Point(plan.x+plan.block.sizeOffset+dx,
                    plan.y+plan.block.sizeOffset+dy));
            }
        }
        List<SurgePlanner.Source> existing=new ArrayList<>(), fallback=new ArrayList<>();
        for(Building build:Vars.player.team().data().buildings){
            if(build==null || !build.isValid() || build.team()!=Vars.player.team() ||
                build.power==null) continue;
            SurgePlanner.Source source=new SurgePlanner.Source(new SurgePlanner.Point(build.tileX(),build.tileY()),build.block.size);
            if(build.block==tower) existing.add(source);
            else if(isFallback(build.block) &&
                (!(build.block instanceof mindustry.world.blocks.power.PowerNode node) ||
                    build.power.links.size < node.maxNodes)) fallback.add(source);
        }
        if(existing.isEmpty() && fallback.isEmpty()){
            toast("No friendly Surge Tower, power node, solar panel or battery to connect to.");return;
        }
        SurgePlanner.Grid grid=new SurgePlanner.Grid(){
            public int width(){return Vars.world.width();}
            public int height(){return Vars.world.height();}
            public boolean linkClear(int x1,int y1,int x2,int y2){
                return !mindustry.world.blocks.power.PowerNode.insulated(x1,y1,x2,y2);
            }
            public boolean validTower(int tx,int ty){
                if(!Build.validPlace(tower,Vars.player.team(),tx,ty,0)) return false;
                for(int dx=0;dx<tower.size;dx++)for(int dy=0;dy<tower.size;dy++){
                    int px=tx+tower.sizeOffset+dx,py=ty+tower.sizeOffset+dy;
                    Tile tile=Vars.world.tile(px,py);
                    if(tile==null || tile.build!=null ||
                        !(tile.block()==Blocks.air || tile.block().alwaysReplace) ||
                        reserved.contains(new SurgePlanner.Point(px,py))) return false;
                }
                return true;
            }
            public int wallEdges(int tx,int ty){
                int score=0;
                for(int dx=-1;dx<=tower.size;dx++) for(int dy=-1;dy<=tower.size;dy++){
                    if(dx>=0 && dx<tower.size && dy>=0 && dy<tower.size) continue;
                    if((dx<0 || dx==tower.size) && (dy<0 || dy==tower.size)) continue;
                    int px=tx+tower.sizeOffset+dx,py=ty+tower.sizeOffset+dy;
                    Tile t=Vars.world.tile(px,py);
                    if(t==null || (t.block().isStatic() && t.build==null) ||
                        Vars.world.getDarkness(px,py)>=3 || !t.floor().placeableOn) score++;
                }
                return score;
            }
        };
        search=new SurgePlanner.Search(grid,new SurgePlanner.Point(x,y),existing,fallback);
        searchWorld=Vars.world;
    }
    private static boolean isFallback(Block block){
        return block==Blocks.powerNode || block==Blocks.powerNodeLarge ||
            block==Blocks.solarPanel || block==Blocks.largeSolarPanel ||
            block==Blocks.battery || block==Blocks.batteryLarge;
    }
}
