package padredirect;

import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.type.Sector;

/** Redirect all exporting sectors on the current planet to this sector. */
public class PadRedirectMod extends Mod{
    public void redirect(){
        if(Vars.state==null || !Vars.state.isCampaign() || Vars.state.getSector()==null ||
            Vars.net==null || Vars.net.client()){
            if(Vars.ui!=null && Vars.ui.hudfrag!=null)
                Vars.ui.hudfrag.showToast("Redirect pads requires a local campaign sector.");
            return;
        }
        if(Vars.state.getPlanet().campaignRules.legacyLaunchPads){
            Vars.ui.hudfrag.showToast("Landing-pad imports are disabled by legacy launch-pad rules.");
            return;
        }
        Sector here=Vars.state.getSector();
        for(Sector source:Vars.state.getPlanet().sectors){
            if(!RedirectPolicy.shouldRedirect(source.hasBase(),source==here,
                source.info!=null && source.info.anyExports(),
                source.info!=null && source.info.destination==here))continue;
            source.info.destination=here;
            source.saveInfo();
        }
        here.info.refreshImportRates(Vars.state.getPlanet());
    }
}
