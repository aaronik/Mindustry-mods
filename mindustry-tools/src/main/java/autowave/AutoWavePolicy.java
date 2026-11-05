package autowave;

/** Mirrors vanilla skip-wave availability, plus a per-wave cooldown guard. */
public final class AutoWavePolicy{
    private AutoWavePolicy(){}
    public static boolean available(boolean waves,boolean waveSending,boolean permitted,
                                    int enemies,boolean spawning,boolean inGame,boolean gameOver){
        return inGame && !gameOver && waves && waveSending && permitted && enemies==0 && !spawning;
    }
    public static boolean shouldSkip(boolean enabled,boolean available,int currentWave,
                                     int lastRequestedWave,long elapsedMillis,long cooldownMillis){
        return enabled && available && currentWave!=lastRequestedWave && elapsedMillis>=cooldownMillis;
    }
}
