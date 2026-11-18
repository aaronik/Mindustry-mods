package sectorpreview;

import arc.Core;
import arc.Events;
import arc.graphics.Texture;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.type.Sector;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

/** Uses only existing save previews. Never generates or loads a sector. */
public class SectorPreviewMod{
    private Table panel;
    private Image image;
    private Label status;
    private Sector selected;
    private Texture texture;
    private long nextCheck;

    public SectorPreviewMod(){
        Events.on(ClientLoadEvent.class,event->{
            if(Vars.headless)return;
            panel=new Table();
            image=new Image();image.setScaling(Scaling.fit);
            panel.button(button->button.add(image).size(220f,150f),Styles.cleart,this::enlarge).row();
            status=panel.add("Saved map preview").get();
            Events.run(Trigger.update,this::update);
        });
    }
    private void update(){
        var planet=Vars.ui.planet;
        if(!planet.isShown() || planet.selected==null)return;
        Sector sector=planet.selected;
        if(sector!=selected){
            selected=sector;texture=null;nextCheck=0L;
            image.setDrawable((arc.scene.style.Drawable)null);
        }
        // Vanilla rebuilds sectorTop when selection or its details change.
        if(panel.parent!=planet.sectorTop){
            panel.remove();
            planet.sectorTop.row();
            planet.sectorTop.add(panel).colspan(Math.max(1,planet.sectorTop.getColumns())).growX().padTop(6f).row();
            planet.sectorTop.pack();
        }
        long now=System.nanoTime();
        if(now<nextCheck)return;
        nextCheck=now+500_000_000L;
        if(!PreviewPolicy.mayRequest(sector.save!=null,sector.hasSave())){
            status.setText("No saved map preview");return;
        }
        try{
            Texture loaded=sector.save.previewTexture();
            if(loaded!=texture){
                texture=loaded;
                if(loaded!=null)image.setDrawable(new arc.graphics.g2d.TextureRegion(loaded));
                else image.setDrawable((arc.scene.style.Drawable)null);
            }
            status.setText(texture==null?"Saved preview unavailable / loading":"Saved map — click to enlarge");
        }catch(RuntimeException error){
            status.setText("Saved preview unavailable");
            nextCheck=Long.MAX_VALUE;
            Log.err("[Mindustry Tools] Could not read sector preview.",error);
        }
    }
    private void enlarge(){
        if(texture==null || selected==null)return;
        BaseDialog dialog=new BaseDialog(selected.name()+" — saved map");
        dialog.addCloseButton();
        Image large=new Image(texture);large.setScaling(Scaling.fit);
        float scale=arc.scene.ui.layout.Scl.scl(1f);
        dialog.cont.add(large).size(Math.max(100f,Math.min(850f,Core.scene.getWidth()/scale-80f)),
            Math.max(100f,Math.min(700f,Core.scene.getHeight()/scale-170f))).row();
        dialog.cont.add("Last saved preview — may be outdated; not a live map.");
        dialog.show();
    }
}
