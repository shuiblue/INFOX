package Util;

import CommunityDetection.AnalyzingCommunityDetectionResult;
import com.jcraft.jsch.HASH;

import java.io.IOException;
import java.util.*;

/**
 * Created by shuruiz on 1/6/17.
 */


public class GenerateCombination {

    static HashSet<HashSet<Integer>> allPairs = new HashSet<>();
    static HashSet<ArrayList<Integer>> allPairs_array = new HashSet<>();
    static HashSet<HashSet<String>> allPairs_string = new HashSet<>();
    static String analysisDir = "";
    static int max_numberOfCut;

    public GenerateCombination(String analysisDir, int max_numberOfCut) {
        allPairs = new HashSet<>();
        this.analysisDir = analysisDir;
        this.max_numberOfCut = max_numberOfCut;
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
            element[i] = i + 1 + "";
        }
        return getAllLists(element, lengthOfList);
    }


    public static HashMap<String, HashMap<Integer, ArrayList<String>>> getAllSplitSteps(int lengthOfList, ArrayList<String> topClusterList) {

        HashMap<String, HashMap<Integer, ArrayList<String>>> resultMap = new HashMap<>();
        for (String s : topClusterList) {
            String origin_clusterId = s.split("_")[0];
            HashMap<Integer, ArrayList<String>> map = new HashMap<>();
            ArrayList<String> nodeList = new ArrayList<>();
            nodeList.add(origin_clusterId);
            map.put(0, nodeList);


            for (int i = 1; i <= lengthOfList; i++) {
                nodeList = new ArrayList<>();
                map.put(i, nodeList);
            }
            resultMap.put(origin_clusterId, map);
        }

        ProcessingText pt = new ProcessingText();
        HashMap<Integer, List<String>> splitMap = new HashMap<>();
        Set<String> noFurtherSplittingStepSet;
        splitMap.put(1, topClusterList);

        String noSplitStepList[] = new String[0];
        try {
            noSplitStepList = pt.readResult(analysisDir + "noSplittingStepList.txt").split("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        noFurtherSplittingStepSet = new HashSet<>();
        for (String s : noSplitStepList) {
            if (s.length() > 0) {
                noFurtherSplittingStepSet.add(s);
            }
        }


        for (int split = 0; split < lengthOfList; split++) {

            for (String cluster : topClusterList) {
                ArrayList<String> clusters = resultMap.get(cluster).get(split);
                if (clusters != null && clusters.size() > 0) {
                    for (String cl : clusters) {
                        if (!noFurtherSplittingStepSet.contains(cl)) {
                            for (int i = 1; i <= 2; i++) {
                                String next = cl + "_" + i;

                                if (!noFurtherSplittingStepSet.contains(next)) {

                                    HashMap<Integer, ArrayList<String>> split_nodelist = resultMap.get(cluster);
                                    split_nodelist.get(split + 1).add(next);
                                    resultMap.put(cluster, split_nodelist);
                                }
                            }
                        }
                    }
                }
            }
        }
//        if(noFurtherSplittingStepSet.size()>0) {
//            noFurtherSplittingStepSet.forEach(stopCluster -> {
//                String[] clusterid = stopCluster.split("_");
//                int index = clusterid.length - 1;
//                String cluster = clusterid[0];
//
//                HashMap<Integer, ArrayList<String>> split_nodelist = resultMap.get(cluster);
//                split_nodelist.get(index).add(stopCluster);
//                resultMap.put(cluster, split_nodelist);
//            });
//        }
        return resultMap;
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


    public ArrayList<String> printAllCases(List<List<String>> totalList) {
        StringBuilder sb = new StringBuilder();
        List<String> result = new ArrayList<String>(totalList.get(0));

        for (int index = 1; index < totalList.size(); index++) {
            result = combineTwoLists(result, totalList.get(index));
        }

        /* print */
        int count = 0;
        for (String s : result) {
            System.out.printf("%d. %s\n", ++count, s);
            sb.append(s + "\n");
        }
        new ProcessingText().rewriteFile(sb.toString(), analysisDir + "splittingSteps.txt");
        return (ArrayList<String>) result;
    }

    private List<String> combineTwoLists(List<String> list1, List<String> list2) {
        List<String> result = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (String s1 : list1) {
            for (String s2 : list2) {
                sb.setLength(0);
                sb.append(s1).append("--").append(s2);
                result.add(sb.toString());
            }
        }
        return result;
    }

    /**
     * @param analysisDir
     * @param max_numberOfCut
     */
    public ArrayList<String> generateAllCombineResult(String analysisDir, int max_numberOfCut) {
        List<List<String>> totalList = getAllCombination();
        return printAllCases(totalList);
    }

    /**
     * This function generates two children of the parent node, _1 _2
     *
     * @param cluster
     * @return child1 + "~" + child2
     */
    private String getChildren(String cluster) {
        String child1 = cluster + "_1";
        String child2 = cluster + "_2";

        return child1 + "~" + child2;
    }

    private List<List<String>> getAllCombination() {
        List<List<String>> all_cluster_combineList = new ArrayList<>();
        ArrayList<String> topClusters = null;
        try {
            topClusters = new ArrayList<>(Arrays.asList(new ProcessingText().readResult(analysisDir + "topClusters.txt").split("\n")));

        } catch (IOException e) {
            e.printStackTrace();
        }

        List<String> stopSplitClusters = null;
        try {
            stopSplitClusters = Arrays.asList(new ProcessingText().readResult(analysisDir + "noSplittingStepList.txt").split("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String clusterID : topClusters) {
            HashSet<String> thisCombinelist = new HashSet<>();
            thisCombinelist.add(clusterID);
            String nextStr = "";
            String[] children = new String[]{clusterID};

            thisCombinelist = getNextSteps(stopSplitClusters, thisCombinelist, nextStr, children);
            all_cluster_combineList.add(new ArrayList<>(thisCombinelist));

        }
        return all_cluster_combineList;
    }
//
//    class Page {
//
//        final List<String> ids;
//
//        Page(List<String> ids) {
//            this.ids = ids;
//        }
//
//        @Override
//        public String toString() {
//            return "";
//        }
//    }


    public HashSet<String> getNextSteps(List<String> stopSplitClusters, HashSet<String> thisCombinelist, String nextStr, String[] children) {

        HashSet<String> tmpCombinelist = new HashSet<>();
        for (int i = 0; i < children.length; i++) {

            String child = children[i];
            if (child.split("_").length <= max_numberOfCut) {
                if (!stopSplitClusters.contains(child)) {
                    nextStr += "~" + getChildren(child);
                } else {
                    continue;
                }

                while (nextStr.startsWith("~")) {
                    nextStr = nextStr.substring(1);
                }

                String[] next = Arrays.copyOf(children, children.length);
                next[i] = nextStr;
                String nextStep = "";
                for (String n : next) {
                    nextStep += "~" + n;
                }
                while (nextStep.startsWith("~")) {
                    nextStep = nextStep.substring(1);
                }
                tmpCombinelist.add(nextStep);
                children = nextStep.split("~");
                nextStr = "";

                tmpCombinelist.addAll(getNextSteps(stopSplitClusters, thisCombinelist, nextStr, children));
            } else {
                continue;
            }

        }
        if (tmpCombinelist.size() > 0) {
            thisCombinelist.addAll(tmpCombinelist);
        }
        return thisCombinelist;
    }

    public static void main(String[] args) {
//        HashSet<Integer> set = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6));
//        System.out.print(getAllPairs(set));

        String[] elements = new String[]{"1", "2"};
        int lengthOfList = 3;

//        String[] aa = getAllLists(3, lengthOfList);
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