package ColorCode;
import java.util.ArrayList;
import java.util.List;
/**
 * Created by shuruiz on 8/29/16.
 */
public class BackgroundColor {
    static List<String> colorList = new ArrayList<>();


    public static List<String> getExpectColorList(){
        colorList=new ArrayList<>();
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
}
