package wallpack;

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
public class WallPackMod extends Mod{
    private static final int maxSide = 60;
    private static final int maxArea = 2500;
    private boolean armed, dragging, hudInstalled;
    private Block largeWall, smallWall;
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

    public WallPackMod(){
        Events.on(ClientLoadEvent.class, event -> {
            if(hudInstalled) return;
            hudInstalled = true;
            Core.input.getInputProcessors().insert(0, selector);
            Vars.ui.hudGroup.fill(hud -> {
                hud.touchable = Touchable.childrenOnly;
                hud.bottom().right();
                hud.visible(() -> ready() && Vars.ui.hudfrag.shown() && !Vars.control.input.commandMode);
                hud.table(buttons -> buttons.button("Wall-pack", Styles.togglet, this::toggle)
                    .checked(button -> armed).size(155f,45f)
                    .tooltip("Select any wall material, click Wall-pack, then drag an area. Click again to cancel."))
                    .padRight(316f).padBottom(108f);
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
        Block[] pair = wallPair(Vars.control.input.block);
        smallWall = pair[0];
        largeWall = pair[1];
        if(!largeWall.unlockedNow() || !largeWall.isPlaceable() ||
            !smallWall.unlockedNow() || !smallWall.isPlaceable()){
            toast("Unlock both sizes of " + smallWall.localizedName + " first."); return;
        }
        armed = true;
    }
    /** Use whichever wall size is selected, or copper if none is selected. */
    private Block[] wallPair(Block selected){
        Block[][] families = {
            {Blocks.copperWall, Blocks.copperWallLarge},
            {Blocks.titaniumWall, Blocks.titaniumWallLarge},
            {Blocks.plastaniumWall, Blocks.plastaniumWallLarge},
            {Blocks.thoriumWall, Blocks.thoriumWallLarge},
            {Blocks.phaseWall, Blocks.phaseWallLarge},
            {Blocks.surgeWall, Blocks.surgeWallLarge},
            {Blocks.berylliumWall, Blocks.berylliumWallLarge},
            {Blocks.tungstenWall, Blocks.tungstenWallLarge},
            {Blocks.reinforcedSurgeWall, Blocks.reinforcedSurgeWallLarge}
        };
        for(Block[] family: families){
            if(selected == family[0] || selected == family[1]) return family;
        }
        return families[0];
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
    private void queueArea(int left,int bottom,int right,int top){
        if(right-left+1 > maxSide || top-bottom+1 > maxSide ||
            (long)(right-left+1)*(top-bottom+1) > maxArea){
            toast("Area too large; max side " + maxSide + ", max area " + maxArea + " tiles."); return;
        }
        Block panel=largeWall, battery=smallWall;
        Set<WallPlanner.Point> reserved=new HashSet<>();
        for(BuildPlan plan:Vars.player.unit().plans()) if(plan.block != null && !plan.breaking){
            for(int dx=0;dx<plan.block.size;dx++)for(int dy=0;dy<plan.block.size;dy++){
                reserved.add(new WallPlanner.Point(plan.x+plan.block.sizeOffset+dx,plan.y+plan.block.sizeOffset+dy));
            }
        }
        WallPlanner.Grid grid=new WallPlanner.Grid(){
            public boolean free(int x,int y){
                Tile t=Vars.world.tile(x,y);
                return t!=null && t.block()==Blocks.air && t.build==null && !reserved.contains(new WallPlanner.Point(x,y));
            }
            public boolean validPanel(int x,int y){return Build.validPlace(panel,Vars.player.team(),x,y,0);}
            public boolean validBattery(int x,int y){return Build.validPlace(battery,Vars.player.team(),x,y,0);}
        };
        WallPlanner.Layout layout=WallPlanner.plan(grid,left,bottom,right,top,panel.size,panel.sizeOffset);
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
