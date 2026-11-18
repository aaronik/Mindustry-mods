package timespeed;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** Runs real ticks, never replacing many ticks with one large delta. */
public final class TickRunner{
    private TickRunner(){}
    public static int run(int requested,long budgetNanos,BooleanSupplier keepRunning,Runnable tick,
                          Runnable handoff,LongSupplier clock){
        long start=clock.getAsLong();
        int completed=0;
        do{
            tick.run();
            completed++;
            if(completed>=requested || !keepRunning.getAsBoolean() || clock.getAsLong()-start>=budgetNanos)break;
            handoff.run();
        }while(keepRunning.getAsBoolean());
        return completed;
    }
}
