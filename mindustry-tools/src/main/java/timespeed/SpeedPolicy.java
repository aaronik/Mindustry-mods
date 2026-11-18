package timespeed;

public final class SpeedPolicy{
    private SpeedPolicy(){}
    public static boolean available(boolean inGame,boolean networkActive,boolean editor,boolean gameOver){
        return inGame && !networkActive && !editor && !gameOver;
    }
    public static boolean preset(int speed){return speed==2 || speed==5 || speed==100;}
}
