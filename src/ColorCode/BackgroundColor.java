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
        colorList_1.add("#E74C3C");
        colorList_1.add("#C0392B");
        colorList_1.add("#CB4335");
        colorList_1.add("#A93226");
        colorList_1.add("#B03A2E");
        colorList_1.add("#922B21");
        colorList_1.add("#943126");
        colorList_1.add("#641E16");
        colorList_1.add("#EC7063");
        colorList_1.add("#CD6155");
        colorList_1.add("#F1948A");
        colorList_1.add("#D98880");
        colorList_1.add("#E6B0AA");
        colorList_1.add("#F5B7B1");
        colorList_1.add("#FADBD8");
        colorList_1.add("#F2D7D5");
        colorList_1.add("#F9EBEA");
        colorList_1.add("#FDEDEC");
        colorList_1.add("red");
        colorList_1.add("pink");

        colorfamiliy_List.add(colorList_1);

        //Blue
        ArrayList<String> colorList_2 = new ArrayList<>();
        colorList_2.add("#3498DB");
        colorList_2.add("#2980B9");
        colorList_2.add("#5DADE2");
        colorList_2.add("#5499C7");
        colorList_2.add("#85C1E9");
        colorList_2.add("#7FB3D5");
        colorList_2.add("#AED6F1");
        colorList_2.add("#1B4F72");
        colorList_2.add("#154360");
        colorList_2.add("#21618C");
        colorList_2.add("#1A5276");
        colorList_2.add("#2874A6");
        colorList_2.add("#1F618D");
        colorList_2.add("#2E86C1");
        colorList_2.add("#2471A3");
        colorList_2.add("#A9CCE3");
        colorList_2.add("#D6EAF8");
        colorList_2.add("#D4E6F1");
        colorList_2.add("#EBF5FB");
        colorList_2.add("#EAF2F8");
        colorList_2.add("Blue");
        colorfamiliy_List.add(colorList_2);

        //Yellow
        ArrayList<String> colorList_3 = new ArrayList<>();
        colorList_3.add("#F1C40F");
        colorList_3.add("#F39C12");
        colorList_3.add("#D4AC0D");
        colorList_3.add("#D68910");
        colorList_3.add("#B9770E");
        colorList_3.add("#B7950B");
        colorList_3.add("#9A7D0A");
        colorList_3.add("#9C640C");
        colorList_3.add("#7E5109");
        colorList_3.add("#7D6608");
        colorList_3.add("#F4D03F");
        colorList_3.add("#F5B041");
        colorList_3.add("#F8C471");
        colorList_3.add("#F7DC6F");
        colorList_3.add("#F9E79F");
        colorList_3.add("#FAD7A0");
        colorList_3.add("#FDEBD0");
        colorList_3.add("#FCF3CF");
        colorList_3.add("#FEF5E7");
        colorList_3.add("#FEF9E7");
        colorfamiliy_List.add(colorList_3);

        //Purple
        ArrayList<String> colorList_4 = new ArrayList<>();
        colorList_4.add("#8E44AD");
        colorList_4.add("#9B59B6");
        colorList_4.add("#884EA0");
        colorList_4.add("#7D3C98");
        colorList_4.add("#6C3483");
        colorList_4.add("#76448A");
        colorList_4.add("#5B2C6F");
        colorList_4.add("#633974");
        colorList_4.add("#512E5F");
        colorList_4.add("#4A235A");
        colorList_4.add("#A569BD");
        colorList_4.add("#AF7AC5");
        colorList_4.add("#BB8FCE");
        colorList_4.add("#C39BD3");
        colorList_4.add("#D7BDE2");
        colorList_4.add("#D2B4DE");
        colorList_4.add("#EBDEF0");
        colorList_4.add("#E8DAEF");
        colorList_4.add("#F4ECF7");
        colorList_4.add("#F5EEF8");

        colorfamiliy_List.add(colorList_4);

       //Yellow
        ArrayList<String> colorList_5 = new ArrayList<>();
        colorList_5.add("#D35400");
        colorList_5.add("#E67E22");
        colorList_5.add("#CA6F1E");
        colorList_5.add("#BA4A00");
        colorList_5.add("#A04000");
        colorList_5.add("#AF601A");
        colorList_5.add("#873600");
        colorList_5.add("#935116");
        colorList_5.add("#6E2C00");
        colorList_5.add("#784212");
        colorList_5.add("#DC7633");
        colorList_5.add("#EB984E");
        colorList_5.add("#E59866");
        colorList_5.add("#F0B27A");
        colorList_5.add("#EDBB99");
        colorList_5.add("#F5CBA7");
        colorList_5.add("#FAE5D3");
        colorList_5.add("#F6DDCC");
        colorList_5.add("#FBEEE6");
        colorList_5.add("#FDF2E9");
        colorfamiliy_List.add(colorList_5);
        return colorfamiliy_List;
    }
}
