package unitbars;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import mindustry.Vars;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import java.util.Map;
import java.util.WeakHashMap;

/** Read-only, opt-in bars for visible friendly and enemy units. */
public class UnitBarsMod{
    private boolean enabled;
    private final Map<Unit,Float> shieldPeaks=new WeakHashMap<>();

    public UnitBarsMod(){
        Events.on(WorldLoadEvent.class,event->reset());
        Events.on(ResetEvent.class,event->reset());
        Events.run(Trigger.draw,this::draw);
    }
    public boolean enabled(){return enabled;}
    public void toggle(){
        if(Vars.state!=null && Vars.state.isGame())enabled=!enabled;
    }
    private void reset(){enabled=false;shieldPeaks.clear();}

    private void draw(){
        if(!enabled || Vars.headless || Vars.state==null || !Vars.state.isGame() || Vars.state.isEditor() ||
            Vars.player==null || !Vars.ui.hudfrag.shown() || Core.scene.hasDialog())return;
        float oldZ=Draw.z();
        Draw.z(Layer.overlayUI);
        float scale=Core.camera.width/Core.graphics.getWidth();
        float left=Core.camera.position.x-Core.camera.width/2f;
        float bottom=Core.camera.position.y-Core.camera.height/2f;
        Groups.unit.intersect(left-40f*scale,bottom-40f*scale,Core.camera.width+80f*scale,Core.camera.height+80f*scale,unit->{
            boolean hidden=unit.inFogTo(Vars.player.team()) ||
                (Vars.fogControl!=null && !Vars.fogControl.isVisible(Vars.player.team(),unit.x,unit.y));
            if(!BarPolicy.eligible(unit.isValid() && !unit.dead(),hidden))return;
            float width=Math.max(24f*scale,Math.min(60f*scale,unit.hitSize));
            float height=3f*scale;
            float y=unit.y+unit.hitSize/2f+6f*scale;
            bar(unit.x,y,width,height,BarPolicy.fraction(unit.health,unit.maxHealth),Pal.health,scale);
            float capacity=0f;
            for(var ability:unit.abilities)if(ability instanceof ForceFieldAbility field)capacity=Math.max(capacity,field.scaledMax(unit));
            // Shields granted by other units do not expose a universal maximum.
            // Track the highest observed value instead of assuming health is shield capacity.
            if(unit.shield>0f){
                float peak=Math.max(shieldPeaks.getOrDefault(unit,0f),unit.shield);
                shieldPeaks.put(unit,peak);
                capacity=Math.max(capacity,peak);
            }
            capacity=Math.max(capacity,shieldPeaks.getOrDefault(unit,0f));
            if(capacity>0f)bar(unit.x,y+6f*scale,width,height,BarPolicy.fraction(unit.shield,capacity),Pal.shield,scale);
        });
        Draw.reset();Draw.z(oldZ);
    }
    private void bar(float x,float y,float width,float height,float fraction,Color color,float scale){
        Draw.color(Color.black,0.8f);
        Fill.rect(x,y,width+2f*scale,height+2f*scale);
        if(fraction>0f){
            Draw.color(color);
            Fill.rect(x-width/2f+width*fraction/2f,y,width*fraction,height);
        }
    }
}
