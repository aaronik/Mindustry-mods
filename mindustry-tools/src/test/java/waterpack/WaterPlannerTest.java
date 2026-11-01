package waterpack;

import autodrill.Planner;
import java.util.*;
public class WaterPlannerTest{
    static class Grid implements Planner.Grid{
        final Set<Planner.Point> water=new HashSet<>(),obstacles=new HashSet<>();
        int width=24,height=24;
        public boolean inBounds(int x,int y){return x>=0&&y>=0&&x<width&&y<height;}
        public boolean ore(int x,int y){return water.contains(new Planner.Point(x,y));}
        public boolean mineable(int x,int y){return ore(x,y);}
        public boolean free(int x,int y){return !obstacles.contains(new Planner.Point(x,y));}
        public boolean validDrill(int x,int y){return inBounds(x,y);}
        public boolean validRouter(int x,int y){return inBounds(x,y);}
    }
    static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
    static void verify(Grid grid,Planner.Layout layout){
        Set<Planner.Point> pumps=new HashSet<>(layout.drills()),routers=new HashSet<>(layout.routers());
        check(pumps.size()==layout.drills().size(),"duplicate pump plans");
        check(routers.size()==layout.routers().size(),"duplicate router plans");
        for(var p:pumps){
            check(grid.ore(p.x(),p.y())&&grid.free(p.x(),p.y()),"pump must sit on unobstructed water");
            check(routers.stream().anyMatch(r->Math.abs(r.x()-p.x())+Math.abs(r.y()-p.y())==1),"pump needs adjacent liquid router");
        }
        for(var r:routers)check(grid.free(r.x(),r.y())&&!pumps.contains(r),"router overlaps pump or building");
        if(!routers.isEmpty()){
            var seen=new HashSet<Planner.Point>();var queue=new ArrayDeque<Planner.Point>();
            seen.add(routers.iterator().next());queue.add(seen.iterator().next());
            while(!queue.isEmpty()){
                var p=queue.remove();
                for(var r:routers)if(Math.abs(r.x()-p.x())+Math.abs(r.y()-p.y())==1&&seen.add(r))queue.add(r);
            }
            check(seen.size()==routers.size(),"liquid routers must form one side-connected network");
        }
    }
    public static void main(String[] args){
        Grid pond=new Grid();
        for(int x=3;x<19;x++)for(int y=3;y<17;y++)pond.water.add(new Planner.Point(x,y));
        pond.obstacles.add(new Planner.Point(7,7));
        pond.obstacles.add(new Planner.Point(8,7));
        var result=WaterPlanner.plan(pond,10,9,1,0,1024);
        check(result.oreTiles()==16*14,"connected water patch scanned");
        check(result.coveredTiles()>50,"interior pumps must be planned");
        verify(pond,result);
        check(WaterPlanner.plan(pond,1,1,1,0,1024).drills().isEmpty(),"dry click must not plan");
        Grid narrow=new Grid();
        for(int y=1;y<20;y++)narrow.water.add(new Planner.Point(11,y));
        verify(narrow,WaterPlanner.plan(narrow,11,5,1,0,1024));
        Grid cutoff=new Grid();
        for(int x=3;x<19;x++)cutoff.water.add(new Planner.Point(x,11));
        for(int y=0;y<24;y++)if(y!=11)cutoff.obstacles.add(new Planner.Point(10,y));
        verify(cutoff,WaterPlanner.plan(cutoff,3,11,1,0,1024));
        System.out.println("Water-pack layout tests passed");
    }
}
