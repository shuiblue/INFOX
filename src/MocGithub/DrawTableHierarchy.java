package MocGithub;

import org.junit.Test;

import static MocGithub.DrawTableHierarchy.Cell.*;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;

/**
 * Created by shuruiz on 5/16/17.
 */
public class DrawTableHierarchy {

    enum Cell {
        topLeft, bottomLeft, bottom, left, none
    }

    public String drawTableHierarchy(String[] clusters, int colspan) {
        String levelStr = "";


        return "";
    }


    public int[][] calculatingArray(String[] clusters, int colspan) {

        int[][] hierachyArray = new int[][]{};

        return hierachyArray;
    }


    @Test
    public void test(){
        assertEquals(null, null);
        Cell[][] actual = new Cell[2][];
        actual[0] = new Cell[]{none,bottom,none,none};
        actual[1] = new Cell[]{none,topLeft,bottomLeft,topLeft};
        Cell[][] expected = new Cell[2][];
        expected[0] = new Cell[]{none,bottom,none,none};
        expected[1] = new Cell[]{none,topLeft,bottomLeft,none};

        assertArrayEquals(expected, actual);
    }

}
