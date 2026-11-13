package unitbars;

public final class BarPolicy{
    private BarPolicy(){}
    public static boolean eligible(boolean alive,boolean hidden){return alive && !hidden;}
    public static float fraction(float value,float capacity){
        if(!Float.isFinite(value) || !Float.isFinite(capacity) || capacity<=0f)return 0f;
        return Math.max(0f,Math.min(1f,value/capacity));
    }
}
