package Util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertTrue;


/**
 * Created by shuruiz on 5/24/17.
 */
public class testGeneratingComb {

    @Test
    public void test_1() {
        for(int maxLevel = 1;maxLevel<=10;maxLevel++) {
            HashSet<String> thisCombineSet = new HashSet<>();
            thisCombineSet.add("A");
            String nextStr = "";
            String[] children = {"A"};
            List<String> stopSplitClusters = new ArrayList<>();
            new GenerateCombination("", maxLevel).getNextSteps(stopSplitClusters, thisCombineSet, nextStr, children);
            System.out.println(maxLevel + " : " + thisCombineSet.size());
        }
    }

    @Test
    public void test_2() {
      int maxLevel = 2;
            HashSet<String> thisCombineSet = new HashSet<>();
            thisCombineSet.add("A");
            String nextStr = "";
            String[] children = {"A"};
            List<String> stopSplitClusters = new ArrayList<>();
            stopSplitClusters.add("A_2");
            new GenerateCombination("", maxLevel).getNextSteps(stopSplitClusters, thisCombineSet, nextStr, children);
            System.out.println(maxLevel + " : " + thisCombineSet.size());
          assertTrue(!thisCombineSet.contains("A_2_1"));

    }

}
