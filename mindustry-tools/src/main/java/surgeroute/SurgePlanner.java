package surgeroute;

import java.util.*;

/** Resumable laser-hop pathfinder; long hops and map-wall edges are preferred. */
public final class SurgePlanner{
    private SurgePlanner(){}
    public record Point(int x,int y){}
    public record Source(Point center,int size){}
    public interface Grid{
        int width();
        int height();
        boolean validTower(int x,int y);
        boolean linkClear(int x,int y,int x2,int y2);
        /** Number of black/static map-wall tiles beside the tower footprint. */
        int wallEdges(int x,int y);
    }
    public record Route(List<Point> towers,Source source,boolean usedExistingTower){}
    private record Node(int index,int cost){}
    // 38 tiles is a conservative margin below the vanilla 40-tile range.
    public static final int maxHop=38;
    private static final int[] strides={38,37,34,33,30,29,26,25,22,21,18,17,14,13,10,9,6,5,3,2};
    private static final int[][] directions={{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    public static boolean inRange(Point a,Point b){
        long dx=(long)a.x-b.x,dy=(long)a.y-b.y;
        return dx*dx+dy*dy <= (long)maxHop*maxHop;
    }

    /** Search for a Surge source first; only fall back if none is reachable. */
    public static final class Search{
        private final Grid grid;
        private final Point destination;
        private final int width,height;
        private final Map<Long,List<Source>> towerGoals,fallbackGoals;
        private final PriorityQueue<Node> frontier=new PriorityQueue<>(Comparator.comparingInt(Node::cost));
        private final int[] costs,parents;
        private final byte[] valid;
        private Route fallback,result,bestTower;
        private long bestSourceDistance=Long.MAX_VALUE;
        private int bestSourceCost=Integer.MAX_VALUE;
        private long bestAttachmentDistance=Long.MAX_VALUE;
        private int bestFallbackCount=Integer.MAX_VALUE;
        private long bestFallbackGap=-1;
        private boolean finished;

        public Search(Grid grid,Point destination,List<Source> towers,List<Source> fallbackSources){
            this.grid=grid;
            this.destination=destination;
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
            towerGoals=goals(towers,true);
            fallbackGoals=goals(fallbackSources,false);
            if(towerGoals.isEmpty() && fallbackGoals.isEmpty()){finished=true;return;}
            for(int x=destination.x-2;x<=destination.x+2;x++){
                for(int y=destination.y-2;y<=destination.y+2;y++){
                    if(!legal(x,y)) continue;
                    int index=idx(x,y);
                    int distance=Math.abs(x-destination.x)+Math.abs(y-destination.y);
                    int edge=Math.min(4,Math.max(0,grid.wallEdges(x,y)));
                    int cost=(edge>0 ? 150 : 1000)+distance*6;
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
                Source towerSource=findSource(towerGoals,current,grid,true);
                if(towerSource!=null){
                    Route candidate=new Route(backtrack(index),towerSource,true);
                    long distance=distanceSquared(towerSource.center,destination);
                    long attachment=distanceSquared(current,destination);
                    // After choosing the nearest reachable source, connect
                    // on its destination-facing side when possible. A wall
                    // bonus must not place a stray tower behind that source.
                    if(distance<bestSourceDistance || distance==bestSourceDistance &&
                        (attachment<bestAttachmentDistance ||
                            attachment==bestAttachmentDistance && node.cost<bestSourceCost)){
                        bestSourceDistance=distance;
                        bestAttachmentDistance=attachment;
                        bestSourceCost=node.cost;
                        bestTower=candidate;
                    }
                }
                Source otherSource=findSource(fallbackGoals,current,grid,false);
                if(otherSource!=null){
                    List<Point> path=backtrack(index);
                    Point first=path.get(0);
                    long gap=distanceSquared(first,otherSource.center);
                    if(path.size()<bestFallbackCount ||
                        path.size()==bestFallbackCount && gap>bestFallbackGap){
                        fallback=new Route(path,otherSource,false);
                        bestFallbackCount=path.size();
                        bestFallbackGap=gap;
                    }
                }
                for(int[] dir:directions) for(int stride:strides){
                    // Diagonal hops have shorter per-axis strides so their
                    // Euclidean distance stays below the 38-tile safety range.
                    int step=(dir[0]!=0 && dir[1]!=0 && stride>26) ? stride*7/10 : stride;
                    int nx=x+dir[0]*step,ny=y+dir[1]*step;
                    if(!legal(nx,ny) || !inRange(current,new Point(nx,ny)) ||
                        !grid.linkClear(x,y,nx,ny)) continue;
                    int next=idx(nx,ny);
                    int edge=Math.min(4,Math.max(0,grid.wallEdges(nx,ny)));
                    // A wall-hugging tower is much cheaper than an open-field
                    // one. Detours that keep towers beside map walls can win
                    // over a shorter route crossing open buildable space.
                    int cost=node.cost+(edge>0 ? 150 : 1000)+
                        (maxHop-(int)Math.hypot(nx-x,ny-y))*8;
                    if(cost>=costs[next]) continue;
                    costs[next]=cost;
                    parents[next]=index;
                    frontier.add(new Node(next,cost));
                }
            }
            if(frontier.isEmpty()){
                Route chosen=bestTower!=null ? bestTower : fallback;
                result=chosen==null ? null : simplify(chosen,grid);
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

    /** Remove redundant towers from a chosen route, while preserving the
     * destination and source attachment. Nearby wall-hugging candidates can
     * be cheap in the search but should not become a dense cluster. */
    public static Route simplify(Route route,Grid grid){
        List<Point> original=route.towers();
        if(original.size()<3) return route;
        List<Point> result=new ArrayList<>();
        int at=0;
        result.add(original.get(at));
        while(at<original.size()-1){
            int next=at+1;
            // Prefer the furthest later tower reachable by one unobstructed
            // laser hop. All intermediate towers can then be omitted.
            for(int candidate=original.size()-1;candidate>at+1;candidate--){
                Point from=original.get(at),to=original.get(candidate);
                if(inRange(from,to) && grid.linkClear(from.x,from.y,to.x,to.y)){
                    next=candidate;
                    break;
                }
            }
            at=next;
            result.add(original.get(at));
        }
        return new Route(result,route.source(),route.usedExistingTower());
    }

    private static long distanceSquared(Point a,Point b){
        long dx=(long)a.x-b.x,dy=(long)a.y-b.y;
        return dx*dx+dy*dy;
    }
    private static long key(int x,int y){return ((long)(x>>6)<<32) ^ ((y>>6)&0xffffffffL);}
    private static Map<Long,List<Source>> goals(List<Source> sources,boolean touching){
        Map<Long,List<Source>> buckets=new HashMap<>();
        for(Source source:sources){
            // A Surge source only needs its immediately adjacent buckets;
            // fallback power blocks can be reached by a long-range laser.
            int reach=touching ? source.size+2 : maxHop+4;
            for(int bx=(source.center.x-reach)>>6;bx<=((source.center.x+reach)>>6);bx++){
                for(int by=(source.center.y-reach)>>6;by<=((source.center.y+reach)>>6);by++){
                    buckets.computeIfAbsent(((long)bx<<32) ^ (by&0xffffffffL),k->new ArrayList<>()).add(source);
                }
            }
        }
        return buckets;
    }
    // Surge Tower sources require physical side contact; other source blocks
    // use a conservative center-to-center laser range for the first link.
    public static boolean touches(Point a,int as,Point b,int bs){
        int ax=a.x-((as-1)/2),ay=a.y-((as-1)/2);
        int bx=b.x-((bs-1)/2),by=b.y-((bs-1)/2);
        boolean xOverlap=ax<bx+bs && bx<ax+as;
        boolean yOverlap=ay<by+bs && by<ay+as;
        return ((ax+as==bx || bx+bs==ax) && yOverlap) ||
            ((ay+as==by || by+bs==ay) && xOverlap);
    }
    private static Source findSource(Map<Long,List<Source>> buckets,Point candidate,Grid grid,boolean touching){
        List<Source> sources=buckets.get(key(candidate.x,candidate.y));
        if(sources==null) return null;
        Source best=null;
        long distance=Long.MAX_VALUE;
        for(Source source:sources){
            long dx=(long)candidate.x-source.center.x,dy=(long)candidate.y-source.center.y;
            long d=dx*dx+dy*dy;
            if(d>=distance ||
                (touching ? !touches(candidate,2,source.center,source.size) : d>=(long)maxHop*maxHop) ||
                !grid.linkClear(candidate.x,candidate.y,source.center.x,source.center.y)) continue;
            best=source;distance=d;
        }
        return best;
    }
}
