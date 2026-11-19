package relanding;

public final class RelandingPolicy{
    private RelandingPolicy(){}
    public static boolean suppress(boolean serpulo,boolean local,int coreOverride,boolean hasSave,int attempts){
        // corePositionOverride is supplied by vanilla's lost-sector restoration path.
        return serpulo && local && coreOverride!=0 && hasSave && attempts>0;
    }
    public static boolean remove(boolean friendly,boolean core,boolean touchesLaunchLoadout){
        return friendly && !core && !touchesLaunchLoadout;
    }
}
