package surgeroute;

import java.util.*;
public class SurgePlannerTest{
    static class Grid implements SurgePlanner.Grid{
        Set<SurgePlanner.Point> blocked=new HashSet<>(), wallAdjacent=new HashSet<>();
        public boolean inBounds(int x,int y){return x>=0 && y>=0 && x<=40 && y<=40;}
        public boolean validTower(int x,int y){
            return inBounds(x+1,y+1) && !blocked.contains(new SurgePlanner.Point(x,y));
        }
        public int wallEdges(int x,int y){return wallAdjacent.contains(new SurgePlanner.Point(x,y)) ? 3 : 0;}
    }
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    public static void main(String[] args){
        Grid g=new Grid();
        var near=new SurgePlanner.Source(new SurgePlanner.Point(8,10),2);
        var far=new SurgePlanner.Source(new SurgePlanner.Point(2,10),2);
        var power=new SurgePlanner.Source(new SurgePlanner.Point(18,10),1);
        var dst=new SurgePlanner.Point(24,10);
        var route=SurgePlanner.plan(g,dst,List.of(far,near),List.of(power),40);
        check(route!=null && route.usedExistingTower(),"prefer reachable Surge Towers");
        check(route.source().equals(near),"choose nearest reachable Surge chain");
        check(!route.towers().isEmpty(),"queue towers");
        check(SurgePlanner.touches(route.towers().get(0),2,near.center(),near.size()),"touch source by side");
        for(int i=1;i<route.towers().size();i++){
            check(SurgePlanner.touches(route.towers().get(i-1),2,route.towers().get(i),2),"chain must connect by sides");
        }
        check(!SurgePlanner.touches(new SurgePlanner.Point(0,0),2,new SurgePlanner.Point(2,2),2),"diagonal corners do not connect");
        check(SurgePlanner.touches(new SurgePlanner.Point(0,0),2,new SurgePlanner.Point(2,0),2),"edge-connected towers do connect");
        check(SurgePlanner.touches(new SurgePlanner.Point(0,0),2,new SurgePlanner.Point(3,0),3),"2x2 tower can touch 3x3 battery");
        // Two equal-length routes: favor the one bordering black map walls.
        Grid walled=new Grid();
        for(int x=4;x<=22;x+=2) walled.wallAdjacent.add(new SurgePlanner.Point(x,8));
        var wallRoute=SurgePlanner.plan(walled,new SurgePlanner.Point(22,10),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(2,10),2)),List.of(),35);
        check(wallRoute!=null && wallRoute.towers().stream().anyMatch(p->p.y()==8),
            "route should prefer near-wall placements when feasible");
        var fallback=SurgePlanner.plan(g,dst,List.of(),List.of(power),40);
        check(fallback!=null && !fallback.usedExistingTower(),"fallback to existing powered structure");
        var largeBattery=new SurgePlanner.Source(new SurgePlanner.Point(16,10),3);
        var fromLarge=SurgePlanner.plan(g,dst,List.of(),List.of(largeBattery),40);
        check(fromLarge!=null && SurgePlanner.touches(fromLarge.towers().get(0),2,largeBattery.center(),largeBattery.size()),
            "large battery fallback must join by a shared side");
        // The only Surge source is enclosed by illegal tower positions.
        Grid enclosed=new Grid();
        for(int x=0;x<=40;x++)for(int y=0;y<=40;y++)if(x<14)enclosed.blocked.add(new SurgePlanner.Point(x,y));
        check(SurgePlanner.plan(enclosed,dst,List.of(far),List.of(power),40).source().equals(power),"unreachable Surge chain falls back to power");
        check(SurgePlanner.plan(g,dst,List.of(),List.of(),40)==null,"no power source must fail safely");
        System.out.println("Surge route tests passed");
    }
}
