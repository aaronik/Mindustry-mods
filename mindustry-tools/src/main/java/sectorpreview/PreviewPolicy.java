package sectorpreview;

public final class PreviewPolicy{
    private PreviewPolicy(){}
    public static boolean mayRequest(boolean hasSlot,boolean hasSave){return hasSlot && hasSave;}
}
