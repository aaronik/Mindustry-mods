package propcleanup;

import arc.Events;
import arc.struct.IntSet;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.mod.Mod;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Prop;

/** Opt-in map-wide break plans for natural auto-replaceable props only. */
public class PropCleanupMod extends Mod{
    private static final int scanPerFrame=3000;
    private static final int queuePerFrame=100;
    private static final int maxNewPlans=10000;
    private int cursor,width,height,added;
    private boolean running;
    private Object searchWorld;
    private IntSet planned,protectedTiles;

    public PropCleanupMod(){
        Events.on(WorldLoadEvent.class,event -> cancel());
        Events.run(Trigger.update,this::update);
    }
    public boolean running(){return running;}
    public void toggle(){
        if(!ready())return;
        if(running){cancel();return;}
        Vars.ui.showConfirm("Remove natural props", "Queue removal of all breakable, automatically replaceable natural props on this map? Existing buildings and black map walls will be left alone.", this::start);
    }
    private boolean ready(){
        return Vars.state!=null && Vars.state.isGame() && Vars.player!=null &&
            !Vars.player.dead() && Vars.player.isBuilder() && Vars.player.unit()!=null &&
            Vars.player.unit().canBuild() && Vars.world!=null;
    }
    private void start(){
        if(!ready())return;
        cancel();
        width=Vars.world.width();height=Vars.world.height();
        if(width<=0 || height<=0 || (long)width*height>12_000_000L){
            Vars.ui.hudfrag.showToast("Map is too large to scan for removable props.");return;
        }
        planned=new IntSet();
        protectedTiles=new IntSet();
        for(BuildPlan plan:Vars.player.unit().plans()){
            if(plan.breaking){planned.add(plan.x+plan.y*width);continue;}
            if(plan.block==null)continue;
            for(int dx=0;dx<plan.block.size;dx++) for(int dy=0;dy<plan.block.size;dy++){
                int px=plan.x+plan.block.sizeOffset+dx;
                int py=plan.y+plan.block.sizeOffset+dy;
                if(px>=0 && py>=0 && px<width && py<height) protectedTiles.add(px+py*width);
            }
        }
        searchWorld=Vars.world;
        added=0;
        running=true;
    }
    private void cancel(){
        running=false;searchWorld=null;planned=null;protectedTiles=null;cursor=0;
    }
    private void update(){
        if(!running)return;
        if(!ready() || Vars.world!=searchWorld || width!=Vars.world.width() || height!=Vars.world.height()){
            cancel();return;
        }
        int examined=0,queued=0,total=width*height;
        while(cursor<total && examined<scanPerFrame && queued<queuePerFrame){
            int pos=cursor++;
            examined++;
            int x=pos%width,y=pos/width;
            Tile tile=Vars.world.tile(x,y);
            if(tile==null)continue;
            if(protectedTiles.contains(pos))continue;
            boolean prop=tile.block() instanceof Prop;
            if(!PropCleanup.eligible(prop,tile.block().alwaysReplace,tile.build!=null,
                Build.validBreak(Vars.player.team(),x,y),planned.contains(pos)))continue;
            planned.add(pos);
            Vars.player.unit().addBuild(new BuildPlan(x,y));
            added++;
            queued++;
        }
        if(added>=maxNewPlans){
            cancel();
            Vars.ui.hudfrag.showToast("Queued 10,000 prop removals; run cleanup again for more.");
        }else if(cursor>=total)cancel();
    }
}
