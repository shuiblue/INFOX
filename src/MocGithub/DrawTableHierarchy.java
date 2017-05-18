package MocGithub;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Stack;

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

    class Cluster {
        String name;
        int start, end;
        int index;
        Cluster leftChild, rightChild;

        Cluster(String name, int index) {
            this.name = name;
            this.index = index;
        }

        Cluster(String name) {
            this.name = name;
        }

        public int getStart() {
            return leftChild.getStart();
        }

        public int getEnd() {
            return rightChild.getEnd();
        }

        public void setLeftChild(String leftChildName) {
            leftChild = new Cluster(leftChildName);
        }

        public void setRightChild(String rightChildName) {
            rightChild = new Cluster(rightChildName);
        }
    }

    public String drawTableHierarchy(String[] clusters, int colspan) {
        String levelStr = "";


        return "";
    }


    public Cell[][] calculatingArray(String clusters, int array_width) {

        ArrayList<Cluster> clusterTree = generateTree(clusters.split("_")[0], array_width);

        String[] clusterArray = clusters.split("~");
        int array_hight = clusterArray.length * 2;
        Cell[][] array = new Cell[array_hight][array_width];

        for (int j = 0; j < clusterArray.length; j++) {
            String subCluster = clusterArray[j];


            int level = subCluster.split("_").length;
            boolean hasPair = hasPair(clusters, subCluster);
            if (hasPair & subCluster.endsWith("_1")) {
                array[2 * j][array_width - level] = none;
                array[2 * j + 1][array_width - level] = topLeft;
            }
            if (hasPair && subCluster.endsWith("_2")) {
                array[2 * j][array_width - level] = bottomLeft;
                array[2 * j + 1][array_width - level] = none;
            }
        }

        return array;
    }

    private ArrayList<Cluster> generateTree(String clusterName, int max_split) {
        Stack clusterList = new Stack();
        clusterList.push(clusterName);
        ArrayList<Cluster> clusterTree = new ArrayList<>();


        String currentCname = (String) clusterList.pop();
        int index = currentCname.split("_").length;
        while (!clusterList.isEmpty()) {
            Cluster c = new Cluster(currentCname, index);
            if (index < max_split) {
                c.setLeftChild(clusterName + "_1");
                c.setRightChild(clusterName + "_2");
                clusterList.push(currentCname + "_1");
                clusterList.push(currentCname + "_2");
            }
            clusterTree.add(c);
            currentCname = (String) clusterList.pop();
            index = currentCname.split("_").length;
        }


        return clusterTree;


    }

    private boolean hasPair(String clusters, String subCluster) {
        if (subCluster.endsWith("_1")) {
            String pair = subCluster.replaceAll("[_1]$", "_2");
            if (clusters.contains("~" + pair)) {
                return true;
            }
        }
        if (subCluster.endsWith("_2")) {
            String pair = subCluster.replaceAll("[_2]$", "_1");
            if (clusters.contains(pair + "~")) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void test1() {

        Cell[][] actual = calculatingArray("A_1~A_2", 2);
        Cell[][] expected = new Cell[2][];
        expected[0] = new Cell[]{none, bottom, none, none};
        expected[1] = new Cell[]{none, topLeft, bottomLeft, none};

        assertArrayEquals(expected, actual);
    }

}
