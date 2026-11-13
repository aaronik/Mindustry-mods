package sectorlist;

import arc.Core;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.type.Item;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import java.util.*;

/** Read-only campaign browser; navigating to a sector never launches or loads it. */
public class SectorListMod{
    private BaseDialog dialog;
    private Table rows;
    private TextButton itemButton,sortButton;
    private Planet planet;
    private Item filter;
    private String search="";
    private boolean byName;
    private final List<ExportIndex.Row> snapshot=new ArrayList<>();
    private final Map<Integer,Sector> sectors=new HashMap<>();

    public void show(){
        if(Vars.state==null || !Vars.state.isCampaign() || Vars.state.getPlanet()==null || Vars.net.client()){
            Vars.ui.hudfrag.showToast("Sectors requires a local campaign sector.");return;
        }
        planet=Vars.state.getPlanet();
        filter=null;search="";byName=false;
        dialog=new BaseDialog("Sectors — "+planet.localizedName);
        dialog.addCloseButton();
        float width=Math.min(950f,Core.scene.getWidth()/Scl.scl(1f)-60f);
        dialog.cont.add("Recorded exports/min — estimates, not guaranteed live throughput.").wrap().width(width).padBottom(8f).row();
        dialog.cont.table(controls->{
            controls.add("Search:").padRight(6f);
            controls.field("",value->{search=value;rebuild();}).width(180f).padRight(8f);
            itemButton=controls.button("All items",this::chooseItem).width(160f).get();
            sortButton=controls.button("Sort: exports",()->{
                byName=!byName;sortButton.setText(byName?"Sort: name":"Sort: exports");rebuild();
            }).width(150f).padLeft(8f).get();
            controls.button("Refresh",()->{capture();rebuild();}).width(100f).padLeft(8f);
        }).growX().row();
        rows=new Table();
        rows.top().left();
        dialog.cont.pane(rows).width(width).height(Math.min(600f,Core.scene.getHeight()/Scl.scl(1f)-230f)).padTop(10f).row();
        dialog.cont.add("Select a sector to locate it on the planet map (no launch).").color(arc.graphics.Color.lightGray).padTop(8f);
        capture();rebuild();
        dialog.show();
    }

    private void capture(){
        snapshot.clear();sectors.clear();
        for(Sector sector:planet.sectors){
            if(!sector.hasBase() || sector.info==null)continue;
            Map<Integer,Double> exports=new HashMap<>();
            for(var entry:sector.info.export){
                double rate=ExportIndex.perMinute(entry.value.mean);
                if(rate>0d)exports.put((int)entry.key.id,rate);
            }
            snapshot.add(new ExportIndex.Row(sector.id,sector.name(),exports));
            sectors.put(sector.id,sector);
        }
    }

    private void chooseItem(){
        BaseDialog picker=new BaseDialog("Export item");
        picker.addCloseButton();
        picker.cont.button("All items",()->{filter=null;itemButton.setText("All items");rebuild();picker.hide();}).growX().row();
        picker.cont.pane(list->{
            int index=0;
            for(Item item:Vars.content.items()){
                boolean exported=snapshot.stream().anyMatch(row->row.rate((int)item.id)>0d);
                if(!exported)continue;
                list.button(item.localizedName,()->{
                    filter=item;byName=false;sortButton.setText("Sort: exports");
                    itemButton.setText(item.localizedName);rebuild();picker.hide();
                }).width(180f).height(45f).pad(3f);
                if(++index%3==0)list.row();
            }
        }).maxHeight(450f);
        picker.show();
    }

    private void rebuild(){
        if(rows==null)return;
        rows.clearChildren();
        var selected=ExportIndex.select(snapshot,search,filter==null?null:(int)filter.id,byName);
        rows.add(selected.size()+" bases • "+(filter==null?"all exports":filter.localizedName)+"/min").left().padBottom(8f).row();
        if(selected.isEmpty()){rows.add("No matching bases.").left();return;}
        for(var row:selected){
            Sector sector=sectors.get(row.id());
            String marker=sector.isAttacked()?"[scarlet]Under attack[]":sector.isBeingPlayed()?"[accent]Current sector[]":"";
            var best=row.exports().entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
            String top=best==null?"None":Vars.content.item(best.getKey()).localizedName+" ("+rate(best.getValue())+"/min)";
            String destination=sector.info.destination==null?"None":sector.info.destination.name()+" (#"+sector.info.destination.id+")";
            rows.button(button->{
                button.left().margin(10f);
                button.add(row.name()+" [lightgray](#"+row.id()+")[]").left().growX();
                button.add(marker).right().row();
                button.add("[accent]"+rate(row.rate(filter==null?null:(int)filter.id))+"/min[]"+(filter==null?" total exports":" "+filter.localizedName)).left().colspan(2).row();
                button.add("Biggest: "+top+"   → "+destination).left().colspan(2).wrap().growX();
            },Styles.flatBordert,()->locate(sector)).growX().minHeight(100f).padBottom(5f).row();
        }
    }

    private void locate(Sector sector){
        dialog.hide();
        Vars.ui.planet.show();
        Vars.ui.planet.viewPlanet(sector.planet,false);
        // Disable the opening preset tour so it cannot move away from our selection.
        Vars.ui.planet.newPresets.clear();
        Vars.ui.planet.lookAt(sector);
        Vars.ui.planet.selectSector(sector);
    }

    private static String rate(double value){return Strings.autoFixed((float)value,1);}
}
