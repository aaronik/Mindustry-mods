package surgeroute;

import java.util.*;

/** Side-connected 2x2 placements; lower costs prefer map-wall edges. */
public final class SurgePlanner{
    private SurgePlanner(){}
    public record Point(int x, int y){}
    public record Source(Point center, int size){}
    public interface Grid{
        boolean inBounds(int x,int y);
        boolean validTower(int x,int y);
        /** Number of black/static map wall tiles beside the tower footprint. */
        int wallEdges(int x,int y);
    }
    public record Route(List<Point> towers, Source source, boolean usedExistingTower){}
    private static final int[][] directions={{1,0},{-1,0},{0,1},{0,-1}};

    public static Route plan(Grid grid,Point destination,List<Source> towers,List<Source> fallback,int radius){
        Route preferred = search(grid,destination,towers,radius,true);
        return preferred != null ? preferred : search(grid,destination,fallback,radius,false);
    }

    private static Route search(Grid grid,Point destination,List<Source> sources,int radius,boolean existing){
        if(sources.isEmpty() || radius<1) return null;
        // Dijkstra: each tower costs 100; a tile beside black map terrain
        // saves up to 10. A detour is allowed if enough of it follows walls.
        record Node(Point point,int cost,int steps,int edges){}
        PriorityQueue<Node> queue=new PriorityQueue<>(Comparator.comparingInt(Node::cost));
        Map<Point,Integer> costs=new HashMap<>();
        Map<Point,Point> parents=new HashMap<>();
        int minX=destination.x-radius,maxX=destination.x+radius;
        int minY=destination.y-radius,maxY=destination.y+radius;
        int destinationLimit=2;
        for(int x=destination.x-destinationLimit;x<=destination.x+destinationLimit;x++){
            for(int y=destination.y-destinationLimit;y<=destination.y+destinationLimit;y++){
                if(!grid.inBounds(x,y) || !grid.validTower(x,y)) continue;
                Point start=new Point(x,y);
                int distance=Math.abs(x-destination.x)+Math.abs(y-destination.y);
                int edges=Math.min(4,Math.max(0,grid.wallEdges(x,y)));
                int cost=100+distance*6-edges*10;
                costs.put(start,cost);
                queue.add(new Node(start,cost,1,edges));
            }
        }
        while(!queue.isEmpty()){
            Node node=queue.poll();
            Point current=node.point;
            if(node.cost!=costs.get(current)) continue;
            for(Source source:sources){
                if(touches(current,2,source.center,source.size)){
                    List<Point> path=new ArrayList<>();
                    for(Point p=current;p!=null;p=parents.get(p)) path.add(p);
                    return new Route(path,source,existing);
                }
            }
            // Every 2x2 tower has anchor-step neighbours at (±2, 0) and
            // (0, ±2). Adjacent turns stay side-connected without overlap.
            for(int[] dir:directions){
                Point next=new Point(current.x+dir[0]*2,current.y+dir[1]*2);
                if(next.x<minX || next.x>maxX || next.y<minY || next.y>maxY ||
                    !grid.inBounds(next.x,next.y) || !grid.validTower(next.x,next.y)) continue;
                int edge=Math.min(4,Math.max(0,grid.wallEdges(next.x,next.y)));
                int nextCost=node.cost+100-edge*10;
                if(nextCost>=costs.getOrDefault(next,Integer.MAX_VALUE)) continue;
                costs.put(next,nextCost);
                parents.put(next,current);
                queue.add(new Node(next,nextCost,node.steps+1,node.edges+edge));
            }
        }
        return null;
    }

    /** True only when footprints share a complete side (not just a corner). */
    public static boolean touches(Point a,int as,Point b,int bs){
        int ax=a.x-((as-1)/2),ay=a.y-((as-1)/2);
        int bx=b.x-((bs-1)/2),by=b.y-((bs-1)/2);
        boolean xOverlap=ax<bx+bs && bx<ax+as;
        boolean yOverlap=ay<by+bs && by<ay+as;
        return (ax+as==bx || bx+bs==ax) && yOverlap ||
            (ay+as==by || by+bs==ay) && xOverlap;
    }
}
