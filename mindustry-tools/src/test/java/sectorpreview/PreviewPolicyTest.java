package sectorpreview;

public class PreviewPolicyTest{
    public static void main(String[]args){
        if(!PreviewPolicy.mayRequest(true,true))throw new AssertionError("saved sector allowed");
        if(PreviewPolicy.mayRequest(false,false) || PreviewPolicy.mayRequest(true,false) || PreviewPolicy.mayRequest(false,true))
            throw new AssertionError("missing saves must not request previews");
        System.out.println("Saved sector preview policy tests passed");
    }
}
