package Util;

import CommunityDetection.AnalyzingCommunityDetectionResult;

import java.util.*;

/**
 * Created by shuruiz on 1/6/17.
 */


public class GenerateCombination {

    static HashSet<HashSet<Integer>> allPairs = new HashSet<>();
    static HashSet<ArrayList<Integer>> allPairs_array = new HashSet<>();
    static HashSet<HashSet<String>> allPairs_string = new HashSet<>();

    public GenerateCombination() {
        allPairs = new HashSet<>();
    }

    /**
     * This function generates all the pairs of the set
     *
     * @param set
     * @return
     */
    public static HashSet<HashSet<Integer>> getAllPairs(HashSet<Integer> set) {

        List<Integer> list = new ArrayList<>(set);
        Integer first = list.remove(0);
        for (Integer node : list) {
            System.out.println(allPairs.size());
            HashSet<Integer> currentPair = new HashSet<>();
            currentPair.add(first);
            currentPair.add(node);
            allPairs.add(currentPair);

        }
        if (set.size() > 2) {
            set.remove(first);
            getAllPairs(set);
        } else {
            allPairs.add(set);
        }

        return allPairs;
    }


    public static String[] getAllLists(String[] elements, int lengthOfList) {
        //initialize our returned list with the number of elements calculated above
        String[] allLists = new String[(int) Math.pow(elements.length, lengthOfList)];

        //lists of length 1 are just the original elements
        if (lengthOfList == 1) return elements;
        else {
            //the recursion--get all lists of length 3, length 2, all the way up to 1
            String[] allSublists = getAllLists(elements, lengthOfList - 1);

            //append the sublists to each element
            int arrayIndex = 0;

            for (int i = 0; i < elements.length; i++) {
                for (int j = 0; j < allSublists.length; j++) {
                    //add the newly appended combination to the list
                    allLists[arrayIndex] = elements[i] + allSublists[j];
                    arrayIndex++;
                }
            }
            return allLists;
        }
    }

    public static String[] getAllLists(int numOfElement, int lengthOfList) {
        String[] element = new String[numOfElement];
        for (int i = 0; i < numOfElement; i++) {
            element[i]=i+1+"";
        }
        return  getAllLists(element,lengthOfList);
    }


    public static HashSet<HashSet<String>> getAllPairs_string(HashSet<String> set) {
        List<String> list = new ArrayList<>(set);
        String first = list.remove(0);
        for (String node : list) {
            HashSet<String> currentPair = new HashSet<>();
            currentPair.add(first);
            currentPair.add(node);
            allPairs_string.add(currentPair);
        }
        if (set.size() > 2) {
            set.remove(first);
            getAllPairs_string(set);
        } else {
            allPairs_string.add(set);
        }

        return allPairs_string;
    }

    public static void main(String[] args) {
//        HashSet<Integer> set = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6));
//        System.out.print(getAllPairs(set));

        String[] elements = new String[]{"1", "2"};
        int lengthOfList = 3;

        String[] aa = getAllLists(3,lengthOfList);
        System.out.print("");

//
//        HashMap<Integer, HashSet<Integer>> groundTruthClusters = new HashMap<>();
//        HashSet<Integer> gt_one = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
//        HashSet<Integer> gt_two = new LinkedHashSet<>(Arrays.asList(4, 5, 6));
//        groundTruthClusters.put(1, gt_one);
//        groundTruthClusters.put(2, gt_two);
//
//        HashMap<Integer, HashSet<Integer>> test_Clusters = new HashMap<>();
//        HashSet<Integer> test_one = new LinkedHashSet<>(Arrays.asList(1, 3));
//        HashSet<Integer> test_two = new LinkedHashSet<>(Arrays.asList(2));
//        HashSet<Integer> test_three = new LinkedHashSet<>(Arrays.asList(4, 5, 6));
//        test_Clusters.put(1, test_one);
//        test_Clusters.put(2, test_two);
//        test_Clusters.put(3, test_three);

//        new AnalyzingCommunityDetectionResult().calculatingAccuracy(groundTruthClusters, test_Clusters,false);

    }

}