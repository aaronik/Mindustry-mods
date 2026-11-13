package waterpack;

import autodrill.Planner;

/** Two pump rows share each liquid-router lane; retain the connected border. */
public final class WaterPlanner{
    private WaterPlanner(){}
    public static Planner.Layout plan(Planner.Grid grid,int x,int y,int size,int offset,int limit){
        return Planner.plan(grid,x,y,size,offset,limit,size==1 ? 3 : size+1);
    }
}
