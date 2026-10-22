package batterypack;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.InputProcessor;
import arc.input.KeyCode;
import arc.scene.event.Touchable;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.mod.Mod;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Tile;

import java.util.HashSet;
import java.util.Set;

/** HUD button + map drag tool; never places finished blocks directly. */
public class BatteryPackMod extends Mod{
    private static final int maxSide = 200;
    private static final int maxArea = 40000;
    private boolean armed, dragging, hudInstalled;
    private int startX, startY, endX, endY;

    private final InputProcessor selector = new InputProcessor(){
        @Override public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button){
            if(!armed || dragging || button != KeyCode.mouseLeft || pointer != 0 || !ready() ||
                Core.scene.hasMouse() || Core.scene.hasDialog()) return false;
            startX = endX = tileX(screenX, screenY);
            startY = endY = tileY(screenX, screenY);
            dragging = true;
            return true;
        }
        @Override public boolean touchDragged(int screenX, int screenY, int pointer){
            if(!dragging || pointer != 0) return false;
            endX = tileX(screenX, screenY);
            endY = tileY(screenX, screenY);
            return true;
        }
        @Override public boolean touchUp(int screenX, int screenY, int pointer, KeyCode button){
            if(!dragging || pointer != 0 || button != KeyCode.mouseLeft) return false;
            endX = tileX(screenX, screenY);
            endY = tileY(screenX, screenY);
            dragging = armed = false;
            if(ready()) queueArea(Math.min(startX,endX),Math.min(startY,endY),Math.max(startX,endX),Math.max(startY,endY));
            return true;
        }
    };

    public BatteryPackMod(){
        // Resolve helper classes immediately rather than at the first map
        // drag, so a broken/stale JAR fails while mods are loading.
        BatteryPlanner.plan(new BatteryPlanner.Grid(){
            public boolean free(int x,int y){return false;}
            public boolean validPanel(int x,int y){return false;}
            public boolean validBattery(int x,int y){return false;}
        },1,1,0,0,3,-1);
        Events.on(ClientLoadEvent.class, event -> {
            if(hudInstalled) return;
            hudInstalled = true;
            Core.input.getInputProcessors().insert(0, selector);
            Vars.ui.hudGroup.fill(hud -> {
                hud.touchable = Touchable.childrenOnly;
                hud.bottom().right();
                hud.visible(() -> ready() && Vars.ui.hudfrag.shown() && !Vars.control.input.commandMode);
                hud.table(buttons -> buttons.button("Battery-pack", Styles.togglet, this::toggle)
                    .checked(button -> armed).size(155f,45f)
                    .tooltip("Click Battery-pack, then drag a rectangle over the map. Click again to cancel."))
                    .padRight(477f).padBottom(57f);
            });
        });
        Events.run(Trigger.update, () -> {
            if((armed || dragging) && !ready()){armed = dragging = false;}
        });
        Events.run(Trigger.draw, () -> {
            if(!dragging || !ready()) return;
            float size = Vars.tilesize;
            float left = Math.min(startX,endX)*size-size/2f;
            float bottom = Math.min(startY,endY)*size-size/2f;
            float width = (Math.abs(startX-endX)+1)*size;
            float height = (Math.abs(startY-endY)+1)*size;
            float previousZ = Draw.z();
            Draw.z(Layer.overlayUI);
            Draw.color(Pal.accent);
            Lines.stroke(2f);
            Lines.rect(left,bottom,width,height);
            Draw.reset();
            Draw.z(previousZ);
        });
    }
    private void toggle(){
        if(!ready()) return;
        if(armed){armed = dragging = false; return;}
        if(!Blocks.batteryLarge.unlockedNow() || !Blocks.batteryLarge.isPlaceable() ||
            !Blocks.battery.unlockedNow() || !Blocks.battery.isPlaceable()){
            toast("Unlock large and small batteries first.");return;
        }
        armed = true;
    }
    private boolean ready(){
        return Vars.state != null && Vars.state.isGame() && Vars.player != null &&
            !Vars.player.dead() && Vars.player.isBuilder() && Vars.player.unit() != null &&
            Vars.control != null && Vars.control.input != null;
    }
    private int tileX(int screenX,int screenY){return Vars.world.toTile(Core.input.mouseWorld(screenX,screenY).x);}
    private int tileY(int screenX,int screenY){return Vars.world.toTile(Core.input.mouseWorld(screenX,screenY).y);}
    private void toast(String message){
        if(Vars.ui != null && Vars.ui.hudfrag != null) Vars.ui.hudfrag.showToast(message);
    }
    static boolean mayPlaceOver(boolean air,boolean alwaysReplace,boolean hasBuilding){
        return !hasBuilding && (air || alwaysReplace);
    }
    private void queueArea(int left,int bottom,int right,int top){
        if(right-left+1 > maxSide || top-bottom+1 > maxSide ||
            (long)(right-left+1)*(top-bottom+1) > maxArea){
            toast("Area too large; max side " + maxSide + ", max area " + maxArea + " tiles."); return;
        }
        Block panel=Blocks.batteryLarge, battery=Blocks.battery;
        Set<BatteryPlanner.Point> reserved=new HashSet<>();
        for(BuildPlan plan:Vars.player.unit().plans()) if(plan.block != null && !plan.breaking){
            for(int dx=0;dx<plan.block.size;dx++)for(int dy=0;dy<plan.block.size;dy++){
                reserved.add(new BatteryPlanner.Point(plan.x+plan.block.sizeOffset+dx,plan.y+plan.block.sizeOffset+dy));
            }
        }
        BatteryPlanner.Grid grid=new BatteryPlanner.Grid(){
            public boolean free(int x,int y){
                Tile t=Vars.world.tile(x,y);
                return t!=null && mayPlaceOver(t.block()==Blocks.air,t.block().alwaysReplace,t.build!=null) &&
                    !reserved.contains(new BatteryPlanner.Point(x,y));
            }
            public boolean validPanel(int x,int y){return Build.validPlace(panel,Vars.player.team(),x,y,0);}
            public boolean validBattery(int x,int y){return Build.validPlace(battery,Vars.player.team(),x,y,0);}
        };
        BatteryPlanner.Layout layout=BatteryPlanner.plan(grid,left,bottom,right,top,panel.size,panel.sizeOffset);
        for(var p:layout.panels()){
            BuildPlan plan=new BuildPlan(p.x(),p.y(),0,panel);
            panel.onNewPlan(plan);
            Vars.player.unit().addBuild(plan);
        }
        for(var p:layout.batteries()){
            BuildPlan plan=new BuildPlan(p.x(),p.y(),0,battery);
            battery.onNewPlan(plan);
            Vars.player.unit().addBuild(plan);
        }
    }
}
