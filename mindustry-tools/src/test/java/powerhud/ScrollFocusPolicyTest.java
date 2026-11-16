package powerhud;

import arc.scene.Element;
import arc.scene.Group;

public class ScrollFocusPolicyTest{
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[]args){
        Group panel=new Group(){},scroll=new Group(){};
        Element row=new Element(),world=new Element(),otherDialog=new Element();
        panel.addChild(scroll);scroll.addChild(row);
        check(!ScrollFocusPolicy.release(panel,scroll,row,true),"retain wheel scrolling inside power rows");
        check(ScrollFocusPolicy.release(panel,scroll,world,true),"release power focus over world to restore zoom");
        check(ScrollFocusPolicy.release(panel,scroll,null,true),"release when no hovered actor");
        check(ScrollFocusPolicy.release(panel,scroll,row,false),"hidden panel must release focus");
        check(!ScrollFocusPolicy.release(panel,otherDialog,world,true),"never clear another dialog's focus");
        check(!ScrollFocusPolicy.release(panel,null,world,true),"no focus is safe");
        System.out.println("Power HUD scroll focus regression tests passed");
    }
}
