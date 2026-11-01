package waterpack;

import autodrill.Planner;

/** Use the proven connected-router planner for 1x1 water pumps. */
public final class WaterPlanner{
    private WaterPlanner(){}
    public static Planner.Layout plan(Planner.Grid grid,int x,int y,int size,int offset,int limit){
        return Planner.plan(grid,x,y,size,offset,limit);
    }
}
