package ColorCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shuruiz on 8/29/16.
 */
public class BackgroundColor {
    static List<String> colorList = new ArrayList<>();


    public static List<String> getExpectColorList() {
        colorList = new ArrayList<>();
        colorList.add("Red");
        colorList.add("Yellow");
        colorList.add("RoyalBlue");
        colorList.add("Cyan");
        colorList.add("Green");
        colorList.add("DeepPink");
        colorList.add("Orange");

        colorList.add("Pink");
        colorList.add("LightGreen");
        colorList.add("MediumPurple ");
        colorList.add("PaleTurquoise ");
        colorList.add("SandyBrown  ");
        colorList.add("Gold ");
        colorList.add("Fuchsia");

        colorList.add("LightCyan");
        colorList.add("HotPink");
        colorList.add("BurlyWood");

        colorList.add("DarkGoldenRod");
        colorList.add("Plum");
        colorList.add("Teal");
        colorList.add("PeachPuff");
        colorList.add("Salmon");
        colorList.add("Sienna");

        return colorList;
    }


    public static ArrayList<ArrayList<String>> getColorFamilyList() {

        ArrayList<ArrayList<String>> colorfamiliy_List = new ArrayList<>();

        //Red
        ArrayList<String> colorList_1 = new ArrayList<>();
        colorList_1.add("#ff0000");
        colorList_1.add("#cc0000");
        colorList_1.add("#ff3300");
        colorList_1.add("#990033");
        colorList_1.add("#ff1a66");
        colorfamiliy_List.add(colorList_1);

        //Blue
        ArrayList<String> colorList_2 = new ArrayList<>();
        colorList_2.add("#0000ff");
        colorList_2.add("#9999ff");
        colorList_2.add("#00ccff");
        colorList_2.add("#3366cc");
        colorList_2.add("#66ccff");
        colorfamiliy_List.add(colorList_2);

        //Yellow
        ArrayList<String> colorList_3 = new ArrayList<>();
        colorList_3.add("#ffff00");
        colorList_3.add("#ccff33");
        colorList_3.add("#ffff99");
        colorList_3.add("#cccc00");
        colorList_3.add("#b3b300");
        colorfamiliy_List.add(colorList_3);

        //Orange
        ArrayList<String> colorList_4 = new ArrayList<>();
        colorList_4.add("#ff9933");
        colorList_4.add("#ff6600");
        colorList_4.add("#ff00ff");
        colorList_4.add("#cc0099");
        colorList_4.add("#9900ff");
        colorfamiliy_List.add(colorList_4);

        //Purple
        ArrayList<String> colorList_5 = new ArrayList<>();
        colorList_5.add("#9900cc");
        colorList_5.add("#cc33ff");
        colorList_5.add("#ffb380");
        colorList_5.add("#cc3300");
        colorList_5.add("#993300");
        colorfamiliy_List.add(colorList_5);
        return colorfamiliy_List;
    }
}
