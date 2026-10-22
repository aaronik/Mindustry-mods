package megapriority;

/** Pure decision logic; command execution remains vanilla. */
public final class Priority{
    private Priority(){}
    public enum Mode{rebuild, repair, mine}
    public static Mode desired(boolean hasRebuildWork, boolean hasRepairWork){
        return hasRebuildWork ? Mode.rebuild : hasRepairWork ? Mode.repair : Mode.mine;
    }
    public static Mode choose(Mode current, Mode desired, boolean activeBuildPlan, boolean lowerPriorityStable){
        if(current == desired) return current;
        // Never clear a unit's active build plan while switching to lower priority work.
        if(current == Mode.rebuild && activeBuildPlan && desired != Mode.rebuild) return current;
        if(desired.ordinal() < current.ordinal()) return desired;
        return lowerPriorityStable ? desired : current;
    }
}
