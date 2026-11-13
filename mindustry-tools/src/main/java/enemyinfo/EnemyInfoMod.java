package enemyinfo;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Teamc;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.BaseTurret.BaseTurretBuild;
import mindustry.world.blocks.defense.turrets.Turret.TurretBuild;

/** Passive, click-through HUD; never inspects enemies hidden by current fog. */
public class EnemyInfoMod{
    private BaseTurretBuild hoveredTurret;
    private Unit hoveredUnit;
    private float nearest;
    private Table panel;
    private Label text;
    private String previous="";

    public EnemyInfoMod(){
        Events.on(ClientLoadEvent.class,event -> install());
    }

    private void install(){
        if(panel!=null || Vars.headless)return;
        panel=new Table(Tex.pane);
        panel.touchable=Touchable.disabled;
        panel.margin(10f);
        text=new Label("");
        panel.add(text).left();
        panel.visible=false;
        // Invisible actors do not act(); keep hover checks running while hidden.
        Events.run(mindustry.game.EventType.Trigger.update,this::update);
        Events.run(mindustry.game.EventType.Trigger.draw,this::drawRange);
        Vars.ui.hudGroup.addChild(panel);
    }

    private boolean eligible(Teamc target,boolean valid){
        return HoverPolicy.eligible(valid,target.team()==Vars.player.team(),target.team()==Team.derelict,
            target.inFogTo(Vars.player.team())) &&
            (Vars.fogControl==null || Vars.fogControl.isVisible(Vars.player.team(),target.x(),target.y()));
    }

    private void update(){
        hoveredTurret=null;
        panel.visible=false;
        if(Vars.state==null || !Vars.state.isGame() || Vars.state.isEditor() || Vars.player==null ||
            !Vars.ui.hudfrag.shown() || Core.scene.hasDialog() || Core.scene.hasMouse())return;
        float x=Core.input.mouseWorldX(),y=Core.input.mouseWorldY();
        hoveredUnit=null;
        nearest=Float.MAX_VALUE;
        // Spatial query intersects unit hitboxes, then use a circular hover test.
        Groups.unit.intersect(x-4f,y-4f,8f,8f,unit -> {
            float distance=unit.dst2(x,y);
            if(eligible(unit,unit.isValid() && !unit.dead()) && HoverPolicy.hovered(distance,unit.hitSize) && distance<nearest){
                nearest=distance;
                hoveredUnit=unit;
            }
        });
        String info;
        if(hoveredUnit!=null){
            Unit unit=hoveredUnit;
            info="[accent]"+unit.type.localizedName+"[] [lightgray]("+unit.team.localized()+")[]\n"+
                health(unit.health,unit.maxHealth)+"\nArmor: "+number(unit.armor)+
                (unit.shield>0f ? "   Shield: "+number(unit.shield) : "")+
                "\n"+(unit.isFlying()?"Flying":"Ground")+"   Range: "+number(unit.range()/Vars.tilesize)+" tiles";
        }else{
            Building build=Vars.world.buildWorld(x,y);
            if(build==null || !eligible(build,build.isValid()))return;
            info="[accent]"+build.block.localizedName+"[] [lightgray]("+build.team.localized()+")[]\n"+
                health(build.health,build.maxHealth)+"\nArmor: "+number(build.block.armor);
            if(build instanceof BaseTurretBuild turret){
                hoveredTurret=turret;
                info+="\nRange: "+number(turret.range()/Vars.tilesize)+" tiles";
            }
            if(build instanceof TurretBuild turret){
                var ammo=turret.getAmmoContent();
                info+="\nAmmo: "+(turret.hasAmmo() ? (ammo==null ? "Ready" : ammo.localizedName) : "Empty");
            }
            if(build.power!=null){
                info+="\nPower satisfaction: "+number(Mathf.clamp(build.power.status)*100f)+"%";
            }
        }
        if(!info.equals(previous)){
            previous=info;
            text.setText(info);
            panel.pack();
        }
        float stageWidth=Core.scene.getWidth(),stageHeight=Core.scene.getHeight();
        float px=Core.input.mouseX()+Scl.scl(18f),py=Core.input.mouseY()+Scl.scl(18f);
        if(px+panel.getWidth()>stageWidth)px=Core.input.mouseX()-panel.getWidth()-Scl.scl(18f);
        if(py+panel.getHeight()>stageHeight)py=Core.input.mouseY()-panel.getHeight()-Scl.scl(18f);
        panel.setPosition(Mathf.clamp(px,0f,Math.max(0f,stageWidth-panel.getWidth())),
            Mathf.clamp(py,0f,Math.max(0f,stageHeight-panel.getHeight())));
        panel.visible=true;
    }

    private void drawRange(){
        BaseTurretBuild turret=hoveredTurret;
        if(turret==null || !panel.visible || Vars.state==null || !Vars.state.isGame() ||
            Vars.state.isEditor() || Vars.player==null || !Vars.ui.hudfrag.shown() ||
            Core.scene.hasDialog() || Core.scene.hasMouse() || !eligible(turret,turret.isValid()))return;
        // Recheck the cursor at draw time so stale hover state cannot linger.
        if(Vars.world.buildWorld(Core.input.mouseWorldX(),Core.input.mouseWorldY())!=turret)return;
        float radius=turret.range();
        if(!HoverPolicy.showRange(radius))return;
        float previousZ=arc.graphics.g2d.Draw.z();
        arc.graphics.g2d.Draw.z(mindustry.graphics.Layer.overlayUI);
        mindustry.graphics.Drawf.dashCircle(turret.x,turret.y,radius,turret.team.color);
        arc.graphics.g2d.Draw.reset();
        arc.graphics.g2d.Draw.z(previousZ);
    }

    private static String health(float current,float max){
        return "Health: "+number(current)+" / "+number(max)+" ("+number(max>0f?Mathf.clamp(current/max)*100f:0f)+"%)";
    }
    private static String number(float value){return Strings.autoFixed(value,1);}
}
