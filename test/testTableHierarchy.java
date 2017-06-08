import MocGithub.DrawTableHierarchy;

import static MocGithub.DrawTableHierarchy.Cell.*;
import static MocGithub.DrawTableHierarchy.Cell.none;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
/**
 * Created by shuruiz on 6/2/17.
 */
public class testTableHierarchy {
    DrawTableHierarchy drawTableHierarchy =new DrawTableHierarchy();
    @Test
    public void test1() {

        DrawTableHierarchy.Cell[][] actual = drawTableHierarchy.calculatingArray("", "A_1~A_2");
        DrawTableHierarchy.Cell[][] expected = new DrawTableHierarchy.Cell[4][2];
        expected[0] = new DrawTableHierarchy.Cell[]{none, none};
        expected[1] = new DrawTableHierarchy.Cell[]{bottom, topLeft};
        expected[2] = new DrawTableHierarchy.Cell[]{none, bottomLeft};
        expected[3] = new DrawTableHierarchy.Cell[]{none, none};

        assertArrayEquals(expected, actual);
    }

    @Test
    public void test2() {

        DrawTableHierarchy.Cell[][] actual = drawTableHierarchy.calculatingArray("", "A_1_1~A_1_2~A_2");
        DrawTableHierarchy.Cell[][] expected = new DrawTableHierarchy.Cell[6][3];
        expected[0] = new DrawTableHierarchy.Cell[]{none, none, none};
        expected[1] = new DrawTableHierarchy.Cell[]{none, bottom, topLeft};
        expected[2] = new DrawTableHierarchy.Cell[]{bottom, topLeft, bottomLeft};
        expected[3] = new DrawTableHierarchy.Cell[]{none, left, none};
        expected[4] = new DrawTableHierarchy.Cell[]{none, bottomLeft, bottom};
        expected[5] = new DrawTableHierarchy.Cell[]{none, none, none};

        assertArrayEquals(expected, actual);
    }

    @Test
    public void test3() {

        DrawTableHierarchy.Cell[][] actual = drawTableHierarchy.calculatingArray("", "A_1~A_2_1~A_2_2");
        DrawTableHierarchy.Cell[][] expected = new DrawTableHierarchy.Cell[6][3];
        expected[0] = new DrawTableHierarchy.Cell[]{none, none, bottom};
        expected[1] = new DrawTableHierarchy.Cell[]{none, topLeft, none};
        expected[2] = new DrawTableHierarchy.Cell[]{bottom, left, none};
        expected[3] = new DrawTableHierarchy.Cell[]{none, bottomLeft, topLeft};
        expected[4] = new DrawTableHierarchy.Cell[]{none, none, bottomLeft};
        expected[5] = new DrawTableHierarchy.Cell[]{none, none, none};

        assertArrayEquals(expected, actual);
    }

    @Test
    public void test4() {

        DrawTableHierarchy.Cell[][] actual = drawTableHierarchy.calculatingArray("", "A_1_1~A_1_2~A_2_1~A_2_2");
        DrawTableHierarchy.Cell[][] expected = new DrawTableHierarchy.Cell[8][3];
        expected[0] = new DrawTableHierarchy.Cell[]{none, none, none};
        expected[1] = new DrawTableHierarchy.Cell[]{none, bottom, topLeft};
        expected[2] = new DrawTableHierarchy.Cell[]{none, topLeft, bottomLeft};
        expected[3] = new DrawTableHierarchy.Cell[]{bottom, left, none};
        expected[4] = new DrawTableHierarchy.Cell[]{none, left, none};
        expected[5] = new DrawTableHierarchy.Cell[]{none, bottomLeft, topLeft};
        expected[6] = new DrawTableHierarchy.Cell[]{none, none, bottomLeft};
        expected[7] = new DrawTableHierarchy.Cell[]{none, none, none};

        assertArrayEquals(expected, actual);
    }

    @Test
    public void test5() {

        DrawTableHierarchy.Cell[][] actual = drawTableHierarchy.calculatingArray("", "A_1~A_2_1_1~A_2_1_2~A_2_2");
        DrawTableHierarchy.Cell[][] expected = new DrawTableHierarchy.Cell[8][4];
        expected[0] = new DrawTableHierarchy.Cell[]{none, none, bottom, bottom};
        expected[1] = new DrawTableHierarchy.Cell[]{none, topLeft, none, none};
        expected[2] = new DrawTableHierarchy.Cell[]{none, left, none, none};
        expected[3] = new DrawTableHierarchy.Cell[]{bottom, left, bottom, topLeft};
        expected[4] = new DrawTableHierarchy.Cell[]{none, bottomLeft, topLeft, bottomLeft};
        expected[5] = new DrawTableHierarchy.Cell[]{none, none, left, none};
        expected[6] = new DrawTableHierarchy.Cell[]{none, none, bottomLeft, bottom};
        expected[7] = new DrawTableHierarchy.Cell[]{none, none, none, none};

        assertArrayEquals(expected, actual);
    }

    @Test
    public void test6() {

        DrawTableHierarchy.Cell[][] actual = drawTableHierarchy.calculatingArray("", "A_1_1~A_1_2~A_2_1_1~A_2_1_2~A_2_2");
        DrawTableHierarchy.Cell[][] expected = new DrawTableHierarchy.Cell[10][4];
        expected[0] = new DrawTableHierarchy.Cell[]{none, none, none, bottom};
        expected[1] = new DrawTableHierarchy.Cell[]{none, bottom, topLeft, none};
        expected[2] = new DrawTableHierarchy.Cell[]{none, topLeft, bottomLeft, bottom};
        expected[3] = new DrawTableHierarchy.Cell[]{none, left, none, none};
        expected[4] = new DrawTableHierarchy.Cell[]{bottom, left, none, none};
        expected[5] = new DrawTableHierarchy.Cell[]{none, left, bottom, topLeft};
        expected[6] = new DrawTableHierarchy.Cell[]{none, bottomLeft, topLeft, bottomLeft};
        expected[7] = new DrawTableHierarchy.Cell[]{none, none, left, none};
        expected[8] = new DrawTableHierarchy.Cell[]{none, none, bottomLeft, bottom};
        expected[9] = new DrawTableHierarchy.Cell[]{none, none, none, none};

        assertArrayEquals(expected, actual);
    }


    @Test
    public void test7() {

        DrawTableHierarchy.Cell[][] actual = drawTableHierarchy.calculatingArray("", "A_1_1_1~A_1_1_2~A_1_2~A_2_1~A_2_2");
        DrawTableHierarchy.Cell[][] expected = new DrawTableHierarchy.Cell[10][4];
        expected[0] = new DrawTableHierarchy.Cell[]{none, none, none, none};
        expected[1] = new DrawTableHierarchy.Cell[]{none, none,bottom, topLeft};
        expected[2] = new DrawTableHierarchy.Cell[]{none, bottom,topLeft, bottomLeft};
        expected[3] = new DrawTableHierarchy.Cell[]{none, topLeft, left, none};
        expected[4] = new DrawTableHierarchy.Cell[]{bottom, left, bottomLeft, bottom};
        expected[5] = new DrawTableHierarchy.Cell[]{none, left, none, none};
        expected[6] = new DrawTableHierarchy.Cell[]{none, left, none, bottom};
        expected[7] = new DrawTableHierarchy.Cell[]{none, bottomLeft, topLeft,none};
        expected[8] = new DrawTableHierarchy.Cell[]{none, none,bottomLeft,bottom};
        expected[9] = new DrawTableHierarchy.Cell[]{none, none, none, none};

        assertArrayEquals(expected, actual);
    }



    @Test
    public void test8() {

        DrawTableHierarchy.Cell[][] actual = drawTableHierarchy.calculatingArray("", "A_1~A_2_1~A_2_2_1~A_2_2_2");
        DrawTableHierarchy.Cell[][] expected = new DrawTableHierarchy.Cell[8][4];
        expected[0] = new DrawTableHierarchy.Cell[]{none, none, bottom,bottom};
        expected[1] = new DrawTableHierarchy.Cell[]{none, topLeft,none, none};
        expected[2] = new DrawTableHierarchy.Cell[]{none, left,none, bottom};
        expected[3] = new DrawTableHierarchy.Cell[]{bottom, left, topLeft, none};
        expected[4] = new DrawTableHierarchy.Cell[]{none, bottomLeft, left, none};
        expected[5] = new DrawTableHierarchy.Cell[]{none, none, bottomLeft, topLeft};
        expected[6] = new DrawTableHierarchy.Cell[]{none, none, none, bottomLeft};
        expected[7] = new DrawTableHierarchy.Cell[]{none, none, none, none};

        assertArrayEquals(expected, actual);
    }

}
