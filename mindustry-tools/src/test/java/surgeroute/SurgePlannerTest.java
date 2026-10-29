package surgeroute;

import java.util.*;
public class SurgePlannerTest{
    static class Grid implements SurgePlanner.Grid{
        int width,height;
        Set<SurgePlanner.Point> blocked=new HashSet<>(), wallAdjacent=new HashSet<>();
        Grid(int width,int height){this.width=width;this.height=height;}
        public int width(){return width;}
        public boolean linkClear(int x,int y,int x2,int y2){return true;}
        public int height(){return height;}
        public boolean validTower(int x,int y){
            return x>=0 && y>=0 && x+1<width && y+1<height && !blocked.contains(new SurgePlanner.Point(x,y));
        }
        public int wallEdges(int x,int y){return wallAdjacent.contains(new SurgePlanner.Point(x,y)) ? 3 : 0;}
    }
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    static SurgePlanner.Route plan(Grid grid,SurgePlanner.Point dst,List<SurgePlanner.Source> towers,List<SurgePlanner.Source> fallback,int budget){
        SurgePlanner.Search search=new SurgePlanner.Search(grid,dst,towers,fallback);
        int iterations=0;
        while(!search.advance(budget)){
            check(++iterations<100000,"incremental search must terminate");
        }
        return search.result();
    }
    static void verify(SurgePlanner.Route route,Grid grid){
        check(route!=null && !route.towers().isEmpty(),"route must queue at least one tower");
        var src=route.source();
        if(route.usedExistingTower()){
            check(SurgePlanner.touches(route.towers().get(0),2,src.center(),src.size()),
                "first tower must side-touch existing Surge Tower");
        }else{
            check(SurgePlanner.inRange(route.towers().get(0),src.center()),
                "first tower must laser-link to other power source in range");
        }
        for(int i=0;i<route.towers().size();i++){
            var p=route.towers().get(i);
            check(grid.validTower(p.x(),p.y()),"route must use valid placements");
            if(i>0)check(SurgePlanner.inRange(route.towers().get(i-1),p),"route must be within laser range");
        }
    }
    public static void main(String[] args){
        Grid g=new Grid(45,45);
        var near=new SurgePlanner.Source(new SurgePlanner.Point(8,10),2);
        var far=new SurgePlanner.Source(new SurgePlanner.Point(2,10),2);
        var power=new SurgePlanner.Source(new SurgePlanner.Point(18,10),1);
        var dst=new SurgePlanner.Point(24,10);
        var route=plan(g,dst,List.of(far,near),List.of(power),4);
        check(route!=null && route.usedExistingTower(),"prefer reachable Surge Towers");
        check(route.source().equals(near),"choose nearest reachable Surge chain");
        verify(route,g);
        check(!SurgePlanner.inRange(new SurgePlanner.Point(0,0),new SurgePlanner.Point(40,0)),"40 tiles is outside conservative hop range");
        Grid walled=new Grid(45,45);
        for(int x=4;x<=22;x+=2) walled.wallAdjacent.add(new SurgePlanner.Point(x,8));
        var wallRoute=plan(walled,new SurgePlanner.Point(22,10),List.of(new SurgePlanner.Source(new SurgePlanner.Point(2,10),2)),List.of(),9);
        verify(wallRoute,walled);
        check(wallRoute.towers().stream().anyMatch(p->p.y()==8),"favor wall edge on comparable routes");
        Grid wallDetour=new Grid(320,320){
            @Override public int wallEdges(int x,int y){return y>=96 && y<=104 ? 2 : 0;}
        };
        Grid openField=new Grid(320,320);
        var detoured=plan(wallDetour,new SurgePlanner.Point(260,150),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(20,150),2)),List.of(),100);
        var straight=plan(openField,new SurgePlanner.Point(260,150),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(20,150),2)),List.of(),100);
        check(detoured.towers().stream().anyMatch(p->p.y()>=96 && p.y()<=104),
            "wall-hugging route must be selected when a longer detour exists");
        check(detoured.towers().size()>straight.towers().size(),
            "wall preference must beat a shorter open-field route");
        // Perpendicular branch: wall preference behind the existing chain
        // must not put a stray first tower on the opposite side.
        Grid perpendicular=new Grid(260,260){
            @Override public int wallEdges(int x,int y){return y>=95 && y<=97 ? 2 : 0;}
        };
        var branch=plan(perpendicular,new SurgePlanner.Point(100,220),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(100,100),2)),List.of(),10);
        verify(branch,perpendicular);
        check(branch.towers().get(0).y()>100,
            "branch should attach on destination-facing side, not behind source: "+branch.towers());
        Grid blockedLinks=new Grid(120,60){
            @Override public boolean linkClear(int x,int y,int x2,int y2){
                return !(y==20 && y2==20 && Math.abs(x-x2)>10);
            }
        };
        var detour=plan(blockedLinks,new SurgePlanner.Point(106,20),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(5,20),2)),List.of(),12);
        verify(detour,blockedLinks);
        check(detour.towers().stream().anyMatch(p->p.y()!=20),"insulated straight hop should be detoured");
        Grid fallbackRange=new Grid(140,12);
        var distantPower=new SurgePlanner.Source(new SurgePlanner.Point(4,5),1);
        var remote=plan(fallbackRange,new SurgePlanner.Point(98,5),List.of(),List.of(distantPower),10);
        verify(remote,fallbackRange);
        int firstGap=Math.abs(remote.towers().get(0).x()-distantPower.center().x());
        check(firstGap>=30 && firstGap<SurgePlanner.maxHop,
            "fallback first tower should use a long-range link, got "+firstGap);
        check(SurgePlanner.touches(new SurgePlanner.Point(0,0),2,new SurgePlanner.Point(2,0),2),
            "side-touching towers connect without a laser link");
        check(!SurgePlanner.touches(new SurgePlanner.Point(0,0),2,new SurgePlanner.Point(2,2),2),
            "diagonal corners cannot connect as Surge sources");
        var fallback=plan(g,dst,List.of(),List.of(power),7);
        check(fallback!=null && !fallback.usedExistingTower(),"fallback to power structure");
        verify(fallback,g);
        var battery=new SurgePlanner.Source(new SurgePlanner.Point(16,10),3);
        verify(plan(g,dst,List.of(),List.of(battery),3),g);
        Grid enclosed=new Grid(45,45){
            @Override public boolean linkClear(int x,int y,int x2,int y2){
                return !(x2==far.center().x() && y2==far.center().y());
            }
        };
        check(plan(enclosed,dst,List.of(far),List.of(power),5).source().equals(power),"unreachable Surge chain falls back to power");
        check(plan(g,dst,List.of(),List.of(),10)==null,"no power source fails safely");
        // Beyond former 65-tile limit: long-range hops need only ~53 towers.
        Grid huge=new Grid(2200,5);
        var longRoute=plan(huge,new SurgePlanner.Point(2010,1),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(2,1),2)),List.of(),120);
        verify(longRoute,huge);
        check(longRoute.towers().size()<90,"long-range transmission should use dozens, not hundreds, of towers");
        System.out.println("Surge route tests passed: "+longRoute.towers().size()+" towers across >2000 tiles");
    }
}
