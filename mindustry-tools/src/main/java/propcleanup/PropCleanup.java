package propcleanup;

/** Pure selection rule for natural, automatically replaceable map features. */
public final class PropCleanup{
    private PropCleanup(){}
    public static boolean eligible(boolean naturalProp,boolean alwaysReplace,boolean hasBuilding,
                                   boolean canBreak,boolean alreadyPlanned){
        return naturalProp && alwaysReplace && !hasBuilding && canBreak && !alreadyPlanned;
    }
}
