package enemyinfo;

public final class HoverPolicy{
    private HoverPolicy(){}
    public static boolean eligible(boolean valid,boolean sameTeam,boolean neutral,boolean hidden){
        return valid && !sameTeam && !neutral && !hidden;
    }
    public static boolean hovered(float distanceSquared,float hitSize){
        float radius=Math.max(4f,hitSize/2f);
        return distanceSquared<=radius*radius;
    }
}
