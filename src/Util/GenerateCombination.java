package Util;

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
        if (noFurtherSplittingStepSet.size() > 0) {
            noFurtherSplittingStepSet.forEach(stopCluster -> {
                String[] clusterid = stopCluster.split("_");
                int index = clusterid.length - 1;
                String cluster = clusterid[0];


                HashMap<Integer, ArrayList<String>> split_nodelist = resultMap.get(cluster);
                split_nodelist.get(index).add(stopCluster);
                resultMap.put(cluster, split_nodelist);
            });
        }
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
            HashSet<String> thisCombineSet = new HashSet<>();
            thisCombineSet.add(clusterID);
            String nextStr = "";
            String[] children = new String[]{clusterID};

            getNextSteps(stopSplitClusters, thisCombineSet, nextStr, children);

            all_cluster_combineList.add(new ArrayList<>(thisCombineSet));

        }
        return all_cluster_combineList;
    }


    public void getNextSteps(List<String> stopSplitClusters, HashSet<String> thisCombineSet, final String currentStr, final String[] children) {

        for (int i = 0; i < children.length; i++) {

            String child = children[i];
            if (child.split("_").length <= max_numberOfCut) {
                String nextStr = currentStr;
                if (!stopSplitClusters.contains(child)) {
                    nextStr += "~" + getChildren(child);
                } else {
                    continue;
                }

                nextStr = removePrefixSymbol(nextStr);
                String[] next = Arrays.copyOf(children, children.length);
                next[i] = nextStr;

                String nextStep = "";
                for (String n : next) {
                    nextStep += "~" + n;
                }
                nextStep = removePrefixSymbol(nextStep);
                if (thisCombineSet.contains(nextStep)) {

                    continue;
                }

                thisCombineSet.add(nextStep);


                String[] children2 = nextStep.split("~");
                nextStr = "";
                getNextSteps(stopSplitClusters, thisCombineSet, nextStr, children2);
            } else {
                continue;
            }

        }
    }

    private String removePrefixSymbol(String nextStr) {
        while (nextStr.startsWith("~")) {
            nextStr = nextStr.substring(1);
        }
        return nextStr;
    }


    public static void main(String... a) {
    System.out.print("Blah");
    }


}