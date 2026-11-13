package timespeed;

public final class SpeedPolicy{
    private SpeedPolicy(){}
    public static boolean available(boolean inGame,boolean networkActive,boolean editor,boolean gameOver){
        return inGame && !networkActive && !editor && !gameOver;
    }
    public static float simulationDelta(float delta){
        return Float.isFinite(delta) && delta>0f && delta<=Float.MAX_VALUE/500f ? delta*500f : delta;
    }
}
