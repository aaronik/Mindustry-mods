package surgeroute;

import java.util.*;

/** Resumable side-connected 2x2 pathfinder; lower costs favor map-wall edges. */
public final class SurgePlanner{
    private SurgePlanner(){}
    public record Point(int x,int y){}
    public record Source(Point center,int size){}
    public interface Grid{
        int width();
        int height();
        boolean validTower(int x,int y);
        /** Number of black/static map-wall tiles beside the tower footprint. */
        int wallEdges(int x,int y);
    }
    public record Route(List<Point> towers,Source source,boolean usedExistingTower){}
    private record Node(int index,int cost){}
    private static final int[][] directions={{1,0},{-1,0},{0,1},{0,-1}};

    /** Search for a Surge source first; only fall back if none is reachable. */
    public static final class Search{
        private final Grid grid;
        private final int width,height;
        private final Map<Point,List<Source>> towerGoals,fallbackGoals;
        private final PriorityQueue<Node> frontier=new PriorityQueue<>(Comparator.comparingInt(Node::cost));
        private final int[] costs,parents;
        private final byte[] valid;
        private Route fallback,result;
        private boolean finished;

        public Search(Grid grid,Point destination,List<Source> towers,List<Source> fallbackSources){
            this.grid=grid;
            width=grid.width();height=grid.height();
            long area=(long)width*height;
            if(width<1 || height<1 || area>12_000_000L){
                throw new IllegalArgumentException("World dimensions unsupported for Surge routing");
            }
            costs=new int[(int)area];
            parents=new int[(int)area];
            valid=new byte[(int)area];
            Arrays.fill(costs,Integer.MAX_VALUE);
            Arrays.fill(parents,-1);
            towerGoals=goals(towers,width,height);
            fallbackGoals=goals(fallbackSources,width,height);
            if(towerGoals.isEmpty() && fallbackGoals.isEmpty()){finished=true;return;}
            for(int x=destination.x-2;x<=destination.x+2;x++){
                for(int y=destination.y-2;y<=destination.y+2;y++){
                    if(!legal(x,y)) continue;
                    int index=idx(x,y);
                    int distance=Math.abs(x-destination.x)+Math.abs(y-destination.y);
                    int edge=Math.min(4,Math.max(0,grid.wallEdges(x,y)));
                    int cost=100+distance*6-edge*10;
                    if(cost>=costs[index]) continue;
                    costs[index]=cost;
                    frontier.add(new Node(index,cost));
                }
            }
            if(frontier.isEmpty()) finished=true;
        }
        /** @return true once a route was found or all valid positions exhausted. */
        public boolean advance(int budget){
            if(finished) return true;
            for(int i=0;i<budget && !frontier.isEmpty();i++){
                Node node=frontier.poll();
                int index=node.index;
                if(node.cost!=costs[index]) continue;
                int x=index%width,y=index/width;
                Point current=new Point(x,y);
                List<Source> towerSources=towerGoals.get(current);
                if(towerSources!=null){
                    result=new Route(backtrack(index),towerSources.get(0),true);
                    finished=true;
                    return true;
                }
                List<Source> otherSources=fallbackGoals.get(current);
                if(fallback==null && otherSources!=null){
                    fallback=new Route(backtrack(index),otherSources.get(0),false);
                }
                for(int[] dir:directions){
                    int nx=x+dir[0]*2,ny=y+dir[1]*2;
                    if(!legal(nx,ny)) continue;
                    int next=idx(nx,ny);
                    int edge=Math.min(4,Math.max(0,grid.wallEdges(nx,ny)));
                    int cost=node.cost+100-edge*10;
                    if(cost>=costs[next]) continue;
                    costs[next]=cost;
                    parents[next]=index;
                    frontier.add(new Node(next,cost));
                }
            }
            if(frontier.isEmpty()){
                result=fallback;
                finished=true;
            }
            return finished;
        }
        public Route result(){
            if(!finished) throw new IllegalStateException("Search is still running");
            return result;
        }
        private List<Point> backtrack(int end){
            List<Point> path=new ArrayList<>();
            for(int index=end;index!=-1;index=parents[index]){
                path.add(new Point(index%width,index/width));
            }
            return path;
        }
        private int idx(int x,int y){return y*width+x;}
        private boolean legal(int x,int y){
            if(x<0 || y<0 || x>=width || y>=height) return false;
            int index=idx(x,y);
            if(valid[index]==0) valid[index]=(byte)(grid.validTower(x,y)?1:-1);
            return valid[index]==1;
        }
    }

    private static Map<Point,List<Source>> goals(List<Source> sources,int width,int height){
        Map<Point,List<Source>> result=new HashMap<>();
        for(Source source:sources){
            int bx=source.center.x-((source.size-1)/2);
            int by=source.center.y-((source.size-1)/2);
            // Candidate 2x2 tower anchors that touch any edge of this source.
            for(int x=bx-2;x<=bx+source.size;x++){
                for(int y=by-2;y<=by+source.size;y++){
                    if(x<0 || y<0 || x>=width || y>=height) continue;
                    Point candidate=new Point(x,y);
                    if(touches(candidate,2,source.center,source.size)){
                        result.computeIfAbsent(candidate,key->new ArrayList<>()).add(source);
                    }
                }
            }
        }
        return result;
    }

    /** True only if footprints share a side, not just a corner. */
    public static boolean touches(Point a,int as,Point b,int bs){
        int ax=a.x-((as-1)/2),ay=a.y-((as-1)/2);
        int bx=b.x-((bs-1)/2),by=b.y-((bs-1)/2);
        boolean xOverlap=ax<bx+bs && bx<ax+as;
        boolean yOverlap=ay<by+bs && by<ay+as;
        return (ax+as==bx || bx+bs==ax) && yOverlap ||
            (ay+as==by || by+bs==ay) && xOverlap;
    }
}
