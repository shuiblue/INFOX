package CommunityDetection;

import ColorCode.BackgroundColor;
import ColorCode.ColorCode;
import DependencyGraph.DependencyGraph;
import MocGithub.ParseHtml;
import Util.GenerateCombination;
import Util.ProcessingText;
import com.jcraft.jsch.HASH;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by shuruiz on 8/29/16.
 */
public class AnalyzingCommunityDetectionResult {

    String analysisDir, testCaseDir, sourcecodeDir, testDir;
    //    String nodeListTxt = "/NodeList.txt";
    String nodeListTxt = "NodeList.txt";
    String forkAddedNodeTxt = "forkAddedNode.txt";
    String expectTxt = "expectCluster.txt";

    String forkAddedNode = "";
    int initialNumOfClusters = 0;
    HashMap<Integer, ArrayList<String>> clusterResultMap;
    static HashMap<Integer, String> nodeMap = new HashMap<>();
    String suffix_ClusterFile = "_clusterTMP.txt";

    static HashMap<String, String> expectNodeMap = new HashMap<>();
    HashMap<String, HashSet<Integer>> groundTruthClusters = new HashMap<>();
    List<String> bgcolorList = BackgroundColor.getExpectColorList();
    static final String FS = File.separator;


    static boolean isMS_CLUSTERCHANGES = false;

    HashMap<Integer, HashMap<Integer, HashMap<String, HashSet<Integer>>>> topClustersSplittingResult = new HashMap<>();

    HashMap<String, HashSet<Integer>> originalClusterMap = new HashMap<>();

    HashMap<String, HashMap<String, HashSet<Integer>>> allClusteringResult = new HashMap<>();
    int max_numberOfCut, numberOfBiggestClusters, clusterSizeThreshold;
    static HashMap<String, HashSet<Integer>> joined_clusters = null;
    ColorCode colorCode;

    public AnalyzingCommunityDetectionResult(String sourcecodeDir, String testCaseDir, String testDir, boolean isMS_CLUSTERCHANGES, int max_numberOfCut, int numberOfBiggestClusters, int clusterSizeThreshold) {
        this.sourcecodeDir = sourcecodeDir;
        if (testDir.equals("")) {
            this.analysisDir = testCaseDir;
        } else {
            this.analysisDir = testCaseDir + testDir + FS;
        }
        this.testCaseDir = testCaseDir;
        this.testDir = testDir;
        this.isMS_CLUSTERCHANGES = isMS_CLUSTERCHANGES;
        this.max_numberOfCut = max_numberOfCut;
        this.numberOfBiggestClusters = numberOfBiggestClusters;
        colorCode = new ColorCode(sourcecodeDir, testCaseDir, testDir, forkAddedNode, isMS_CLUSTERCHANGES);
        this.clusterSizeThreshold = clusterSizeThreshold;
    }

    public AnalyzingCommunityDetectionResult(String analysisDir) {
        this.analysisDir = analysisDir;
    }


    /**
     * This function get Cluster result map
     *
     * @param clusterID
     * @param isOriginalGraph
     * @return
     */
    public HashMap<Integer, HashMap<String, HashSet<Integer>>> getClusteringResultMapforClusterID(String clusterID, boolean isOriginalGraph, boolean isJoined) {
//    public HashMap<Integer, HashMap<String, HashSet<Integer>>> getClusteringResultMap(int clusterSizeThreshold,  boolean hasGroundTruth, String filePrefix, boolean isOriginalGraph) {
        HashMap<Integer, HashMap<String, HashSet<Integer>>> clusterResultMap = new HashMap<>();

        String isJoined_str = isJoined ? "_joined" : "";
        String clusterFilePath = analysisDir + clusterID + isJoined_str + suffix_ClusterFile;
        String clusterResultListString = "";

        ProcessingText processingText = new ProcessingText();

        /** get fork added node **/
        File forkAddedFile = new File(analysisDir + forkAddedNodeTxt);
        if (forkAddedFile.exists()) {
            try {
                forkAddedNode = processingText.readResult(analysisDir + forkAddedNodeTxt);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            clusterResultListString = processingText.readResult(clusterFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int pre_numberOfCommunites = 0;
        int index = 1;
        /** split edge cutting result  **/
        String[] resultArray = clusterResultListString.split("--------Graph-------");

        for (int i = 0; i < resultArray.length; i++) {
            if (i > 1)
                isOriginalGraph = false;
            /**  one edge cutting result **/
            String result = resultArray[i];
            if (result.contains("communities")) {
                /**     calculating cluster info, such as modularity, #edge cut ..  **/

                /**todo**/
                boolean tmp_isOriginalGraph = false;
                if (!result.contains("weight")) {
                    tmp_isOriginalGraph = false;
                }
                double[] clusterInfo = getClusterInfo(result.split("communities")[0], tmp_isOriginalGraph);
                int numberOfCommunities = (int) clusterInfo[0];

                if (numberOfCommunities >= 1) {

                    if (pre_numberOfCommunites != numberOfCommunities) {

                        ArrayList<String> clusters = getClusters(result);

                        if (clusterResultMap.size() == 0) {
                            initialNumOfClusters = numberOfCommunities;
                            processingText.writeTofile(initialNumOfClusters + ",0\n", analysisDir + clusterID + "_LOC_split.txt");
                        }
//                        clusterResultMap.put(numberOfCommunities, clusters);

                        /** generates current clustering result map
                         * #index  -> hashset of nodeid  **/
                        HashMap<String, HashSet<Integer>> current_clustering_result = generateCurrentClusteringResultMap(clusters, clusterID, isOriginalGraph, isJoined);
                        clusterResultMap.put(index, current_clustering_result);

                        pre_numberOfCommunites = numberOfCommunities;
                        index++;
                    }
                }

            }

        }
        return clusterResultMap;

    }

    private ArrayList<String> getClusters(String result) {
        result = result.split("communities\n")[1];
        String[] clusterArray = result.split("\n");

        return (ArrayList<String>) new ArrayList(Arrays.asList(clusterArray));
    }

    /**
     * This function generates clustering html table
     *
     * @param testCaseDir
     * @param testDir
     * @param numberOfCommunities
     * @param isJoiningTable
     * @param clusterSizeThreshold
     * @param current_clustering_result
     * @param hasGroundTruth
     */
    public void generatingClusteringTable(String testCaseDir, String testDir, String numberOfCommunities, boolean isJoiningTable, int clusterSizeThreshold, HashMap<String, HashSet<Integer>> current_clustering_result, boolean hasGroundTruth, boolean isOriginalGraph) {
        ProcessingText processingText = new ProcessingText();
        HashMap<String, HashMap<String, Integer>> resultTable = new HashMap<>();

        String filePath;
        if (!isJoiningTable) {
            filePath = analysisDir + numberOfCommunities + "_colorTable.txt";
        } else {
            filePath = analysisDir + numberOfCommunities + "_colorTable_join_bigSize-" + clusterSizeThreshold + ".txt";
        }
        try {

            String cssString = processingText.readResult(filePath);
            String[] colorArray = cssString.split("\n");
            ArrayList<String> nodeColorList = new ArrayList(Arrays.asList(colorArray));
            ArrayList<String> communityColorList = new ArrayList<>();
            HashMap<String, ArrayList<String>> color_nodeLabel = new HashMap<>();

            for (String node : nodeColorList) {
                if (node.length() > 0) {

                    String[] nodeInfo = node.split(",");
                    String id = nodeInfo[0];
                    String bgColor = nodeInfo[1];
                    if (!communityColorList.contains(bgColor)) {
                        communityColorList.add(bgColor);
                    }

                    if (hasGroundTruth) {
                        System.out.println(id);
                        String expectColor = nodeInfo[2];

                        HashMap<String, Integer> distributedColor = resultTable.get(expectColor.trim());
                        if (distributedColor != null) {
                            if (distributedColor.get(bgColor) == null) {
                                distributedColor.put(bgColor, 1);
                            } else {
                                int num = distributedColor.get(bgColor);
                                distributedColor.put(bgColor, num + 1);
                            }
                        } else {
                            distributedColor = new HashMap<>();
                            distributedColor.put(bgColor, 1);
                        }
                        resultTable.put(expectColor.trim(), distributedColor);
                    } else {

                        ArrayList<String> nodeLabelList;
                        nodeLabelList = color_nodeLabel.get(bgColor);
                        if (nodeLabelList != null) {
                            nodeLabelList.add(id);
                        } else {
                            nodeLabelList = new ArrayList<>();
                            nodeLabelList.add(id);
                            color_nodeLabel.put(bgColor, nodeLabelList);
                        }


                    }
                }
            }

            if (hasGroundTruth) {
                printResultTable(resultTable, communityColorList, numberOfCommunities, isJoiningTable, clusterSizeThreshold, hasGroundTruth);
            } else {
                printResultTable_noGroundTruth(color_nodeLabel, numberOfCommunities, isJoiningTable, clusterSizeThreshold, current_clustering_result);
            }

            if (!isMS_CLUSTERCHANGES && isOriginalGraph) {
                printClusterDistanceTable(originCombination);
//                printClusterDistanceTable(numberOfCommunities);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Print result table without ground truth
     *
     * @param color_nodeLabel
     * @param numberOfCommunities
     * @param isJoiningTable
     * @param clusterSizeThreshold
     * @param current_clustering_result
     */
    private void printResultTable_noGroundTruth(HashMap<String, ArrayList<String>> color_nodeLabel, String numberOfCommunities, boolean isJoiningTable, int clusterSizeThreshold, HashMap<String, HashSet<Integer>> current_clustering_result) {
        ProcessingText processingText = new ProcessingText();
        StringBuffer sb = new StringBuffer();

        HashMap<String, String> clusterIdToColorMap = getIdToColorMap(numberOfCommunities, isJoiningTable);
        Map<String, String> colorToClusterIdMap = new HashMap<>();
        for (Map.Entry<String, String> entry : clusterIdToColorMap.entrySet()) {
            colorToClusterIdMap.put(entry.getValue(), entry.getKey());
        }

        HashMap<String, Integer> clusterid_size_map = new HashMap<>();
        for (Map.Entry<String, HashSet<Integer>> entry : current_clustering_result.entrySet()) {
            clusterid_size_map.put(entry.getKey(), entry.getValue().size());
        }
        final String[] clusterID = new String[1];
        final int[] size = new int[1];
        final String[] color = {""};
        //print 1st line
        sb.append("<table id=\"cluster\"  border=1 frame=void rules=rows>\n" +
                "  <tr> \n" +
                "    <td> <button id=\"btn_hide_non_cluster_rows\" onclick=\"hide_non_cluster_rows()\">Hide non cluster code</button>\n" +
                "    </td> \n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "       <th>Cluster</th>\n" +
                "       <th>Keywords</th>\n" +
                "       <th>LOC</th>\n" +
                "       <th>Split Cluster </th>\n" +
                "        <th> keyword is representative?</th>\n" +
                "    </tr>");

        /** sort clusters by size **/
        //todo limit
        clusterid_size_map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(max_numberOfCut)
                .forEach(k -> {
                            clusterID[0] = k.toString().split("=")[0];
                            size[0] = Integer.parseInt(k.toString().split("=")[1]);
                            color[0] = clusterIdToColorMap.get(clusterID[0] + "");
                            sb.append(
                                    "    <tr>\n" +
                                            "       <td><div id='" + numberOfCommunities + "-cluster-" + clusterID[0] + "'>" + size[0] + "</div></td>\n" +
                                            "       <td bgcolor=\"#" + color[0] + "\">" + "sdfsdf" + "</td>\n" +
                                            "   </tr>\n");


                            System.out.println("Item : " + k.toString().split("=")[0] + " Count : " + k.toString().split("=")[1]);
                        }
                );


        sb.append("</table>");
        if (!isJoiningTable) {
            processingText.rewriteFile(sb.toString(), analysisDir + numberOfCommunities + ".color");

        } else {
            processingText.rewriteFile(sb.toString(), analysisDir + numberOfCommunities + "_join_bigSize-" + clusterSizeThreshold + ".color");
        }

    }

    /**
     * This function generate the cluster distance table
     *
     * @param numberOfCommunities
     */
    private void printClusterDistanceTable(String numberOfCommunities) {
        StringBuffer sb = new StringBuffer();
        HashMap<String, String[]> distanceTable = new HashMap<>();
        ProcessingText processingText = new ProcessingText();
        ArrayList<String> clusterIDList = new ArrayList<>();
        String[] colorList;
        String[] distanceList;
        try {
            String[] tmp = processingText.readResult(analysisDir + numberOfCommunities + "_clusterIdList.txt").split(",");
            for (String s : tmp) {
                if (!s.equals("\n")) {
                    clusterIDList.add(s);
                }

            }
            for (String s : clusterIDList) {
                if (!s.equals("\n")) {
                    distanceTable.put(s, new String[clusterIDList.size()]);
                }
            }

            // TODO: table color
            HashMap<String, String> idToColorMap = getIdToColorMap(numberOfCommunities, false);
            distanceList = processingText.readResult(analysisDir + numberOfCommunities + "_distanceBetweenCommunityies.txt").split("\n");
            for (String d : distanceList) {
                String[] content = d.split(",");
                String[] array = distanceTable.get(content[0]);
                array[Integer.valueOf(clusterIDList.indexOf(content[1]))] = content[2];
                distanceTable.put(content[0], array);
            }
            sb.append("<table id=\"distance\"> <tr> <td> </td>\n");
            for (String id : clusterIDList) {
                sb.append("<td bgcolor=\"#" + idToColorMap.get(id) + "\">" + id + "</td>\n");

            }
            sb.append("</tr>\n");

            for (String id : clusterIDList) {
                sb.append("<tr><td bgcolor=\"#" + idToColorMap.get(id) + "\">" + id + "</td>\n");

                for (String s : distanceTable.get(id)) {
                    if (s == null) {
                        s = "-";
                    } else {
                        if (s.equals("999999")) {
                            s = "&infin;";
                        }
                    }
                    sb.append("<td>" + s + "</td>\n");
                }
                sb.append("</tr>\n");
            }
            sb.append("</table>\n");
            processingText.rewriteFile(sb.toString(), analysisDir + numberOfCommunities + ".distanceTable");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //    private void printResultTable(HashMap<String, HashMap<String, Integer>> resultTable, ArrayList<String> communityColorList, int numberOfCommunities, ArrayList<String> macroList) {
    private void printResultTable(HashMap<String, HashMap<String, Integer>> resultTable, ArrayList<String> communityColorList, String numberOfCommunities, boolean isJoiningTable, int clusterSizeThreshold, boolean hasGroundTruth) {
        ProcessingText processingText = new ProcessingText();
        StringBuffer sb = new StringBuffer();
        ArrayList<String> featureList = null;
        featureList = getFeatureList();

        String[] colorList = null;
        try {
            colorList = processingText.readResult(analysisDir + numberOfCommunities + "_clusterColor.txt").split("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }


        HashMap<String, String> clusterIdToColorMap = getIdToColorMap(numberOfCommunities, isJoiningTable);
        Map<String, String> colorToClusterIdMap = new HashMap<>();
        for (Map.Entry<String, String> entry : clusterIdToColorMap.entrySet()) {
            colorToClusterIdMap.put(entry.getValue(), entry.getKey());
        }

        //print 1st line
        sb.append("<table id=\"cluster\">\n" +
                "    <tr>\n" +
                "        <td>\n" +
                "            <span>ClusterID</span>\n" +
                "            <hr/>\n" +
                "            <span>CRs ( LOC )</span>\n" +
                "        </td>\n");

        for (String color : communityColorList) {
            sb.append("<td bgcolor=\"#" + color + "\">" + colorToClusterIdMap.get(color) + "</td>\n");
        }
        sb.append("</tr>\n");
// ----------------for ifdef evaluation----------------------------------
        for (int i = 0; i < featureList.size(); i++) {
            String expectColor = bgcolorList.get(i);
            //print rest lines
            sb.append("  <tr>\n" +
                    "        <td bgcolor=\"" + expectColor + "\">\n" +
                    featureList.get(i).trim() +
                    "\n        </td>\n");

            HashMap<String, Integer> distributedMap = resultTable.get(expectColor.trim());

            if (distributedMap != null) {
                for (String s : communityColorList) {
                    String number;
                    if (distributedMap.get(s) == null) {
                        number = "-";
                    } else {
                        number = distributedMap.get(s).toString();
                    }
                    sb.append("<td>" + number + "</td>\n");
                }
                sb.append("</tr>\n");
            } else {
                System.out.println("distributedMap is null: " + expectColor);
            }
        }
        sb.append("</table>");
        if (!isJoiningTable) {
            processingText.rewriteFile(sb.toString(), analysisDir + numberOfCommunities + ".color");

        } else {
            processingText.rewriteFile(sb.toString(), analysisDir + numberOfCommunities + "_join_bigSize-" + clusterSizeThreshold + ".color");
        }
    }

    private HashMap<String, String> getIdToColorMap(String numberOfCommunities, boolean isJoiningTable) {
        ProcessingText processingText = new ProcessingText();
        String[] colorList = new String[0];
        try {
            colorList = processingText.readResult(analysisDir + originCombination + "_clusterColor.txt").split("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<String, String> colorTable = new HashMap();
        for (String c : colorList) {
            //[0] id  [1] current color  [2] expect color
            String[] content = c.split(",");
            String id = content[0];
            String current_color = content[1];
            colorTable.put(id, current_color);
        }
        return colorTable;
    }

    /**
     * This function get feature list from featureList.txt
     *
     * @return arraylist of features (String)
     */
    private ArrayList<String> getFeatureList() {
        ProcessingText processingText = new ProcessingText();
        try {
            String[] featureArray = processingText.readResult(testCaseDir + "featureList.txt").split("\n");
            return new ArrayList<>(Arrays.asList(featureArray));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    String originCombination;

    public HashMap<Integer, ArrayList<String>> generateClusteringResult(int max_numberOfCut, int numberOfBiggestClusters, int clusterSizeThreshold) {
        ProcessingText protext = new ProcessingText();
        boolean hasGroundTruth = false;

        originCombination = "original";

        /**  generate combinations if split steps of sub-cluster **/
        System.out.println("    generating all the possible splitting steps... E.g,. ABC, A1A2BC, ABC1C2");
        ArrayList<String> combination_list = new GenerateCombination(analysisDir, max_numberOfCut).generateAllCombineResult(testCaseDir, max_numberOfCut);

        for (String s : combination_list) {
            allClusteringResult.put(s, new HashMap<>());
        }


        /** for original clustering result , generate corresponding table, html ...**/
        System.out.println("    analyzing original changed code graph, get original cluster mapping...");
        parseEachUsefulClusteringResult(clusterSizeThreshold, hasGroundTruth, originCombination, true);
        HashMap<Integer, HashMap<String, HashSet<Integer>>> originalCluster = getClusteringResultMapforClusterID(originCombination, true, false);
        for (Map.Entry<Integer, HashMap<String, HashSet<Integer>>> entry : originalCluster.entrySet()) {
            originalClusterMap = entry.getValue();
        }


        System.out.println("    list top X biggest cluster id");

        ArrayList<String> topClusterList = getTopClusterList();

        /**get all splitting result mapping , storing in 'topClustersSplittingResult'.**/
        getAllSplittingResult(max_numberOfCut, topClusterList, combination_list);
        for (String com : combination_list) {
            if (com.replaceAll("1", "").length() == 0) {
                HashMap<String, HashSet<Integer>> origialClusters = getCopyOfOriginalClusters();
                allClusteringResult.put(com, origialClusters);
                continue;
            }

            System.out.println("generate Clustering Result By Combining Splitting Steps ...:");
            HashMap<String, HashSet<Integer>> currentCluster = generateClusteringResult_ByCombiningSplittingStep(com);

            /****/
            generateCompleteClusteringFiles(currentCluster, com);

            parseEachUsefulClusteringResult(clusterSizeThreshold, hasGroundTruth, com, false);
        }
        return clusterResultMap;
    }

    private ArrayList<String> getTopClusterList() {
        ArrayList<String> topClusterList = new ArrayList<>();

        String[] topClusters = null;
        try {
            topClusters = new ProcessingText().readResult(analysisDir + "topClusters.txt").split("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String tc : topClusters) {
            topClusterList.add(tc);
        }
        return topClusterList;
    }

    /**
     * get all splitting result mapping , storing in 'topClustersSplittingResult'
     * * @param max_numberOfCut
     *
     * @param topClusterList
     * @param combination_list
     * @return
     */
    public HashMap<Integer, HashMap<Integer, HashMap<String, HashSet<Integer>>>> getAllSplittingResult(int max_numberOfCut, ArrayList<String> topClusterList, ArrayList<String> combination_list) {
        System.out.println("get all splitting result mapping , storing in 'topClustersSplittingResult' ...");
        topClustersSplittingResult = new HashMap<>();

        List<String> noSplittingNode = new ArrayList<>();
        try {
            noSplittingNode = Arrays.asList(new ProcessingText().readResult(analysisDir + "noSplittingStepList.txt").split("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<String, HashMap<Integer, ArrayList<String>>> allSplitSteps_map = new GenerateCombination(analysisDir, max_numberOfCut).getAllSplitSteps(max_numberOfCut, topClusterList);

        allSplitSteps_map.forEach((k, v) -> {
            HashMap<Integer, HashMap<String, HashSet<Integer>>> map = new HashMap<>();
            topClustersSplittingResult.put(Integer.valueOf(k), map);
            v.forEach((k1, v1) -> {
                v1.forEach(v2 -> {
                    HashMap<String, HashSet<Integer>> tmpMap = new HashMap<String, HashSet<Integer>>();
                    tmpMap.put(v2, new HashSet<>());
                    map.put(k1, new HashMap<>());

                });
            });

        });
        HashSet<String> topClist = new HashSet<>();

        for (String com : combination_list) {
            for (String s : com.split("--")) {
                for (String ss : s.split("~")) {
                    topClist.add(ss);
                }
            }
        }
        for (String clusterID : topClist) {
            int originalClusterID = Integer.valueOf(clusterID.split("_")[0]);
            if (clusterID.split("_").length < max_numberOfCut + 1) {
                boolean isOriginal = false;
                if (!clusterID.contains("_")) {
                    isOriginal = true;
                }
                if (!noSplittingNode.contains(clusterID) || isOriginal) {
                    HashMap<Integer, HashMap<String, HashSet<Integer>>> tmpClustertwoSplit = getClusteringResultMapforClusterID(clusterID, isOriginal, false);
                    tmpClustertwoSplit.forEach((k, v) -> {
                        v.forEach((k1, v1) -> {
                            int index = k1.split("_").length - 1;
                            HashMap<Integer, HashMap<String, HashSet<Integer>>> map = topClustersSplittingResult.get(originalClusterID);
                            map.get(index).put(k1, v1);
                        });
                    });
                }
            }
        }

        return topClustersSplittingResult;
    }

    private void generateCompleteClusteringFiles(HashMap<String, HashSet<Integer>> currentCluster, String combination) {
        new R_CommunityDetection(analysisDir).printMemebershipOfCurrentGraph_new(currentCluster, combination + "_clusterTMP.txt");
    }


    private HashMap<String, HashSet<Integer>> getCopyOfOriginalClusters() {
        HashMap<String, HashSet<Integer>> currentCluster = new HashMap<>();
        for (Map.Entry<String, HashSet<Integer>> entry : originalClusterMap.entrySet()) {
            currentCluster.put(String.valueOf(entry.getKey()),
                    new HashSet<>(entry.getValue()));
        }
        return currentCluster;
    }


    /**
     * This function generate different clustering result by combining differernt splitting steps
     */
    private HashMap<String, HashSet<Integer>> generateClusteringResult_ByCombiningSplittingStep(String splitStep) {
        System.out.println("current splitting step :" + splitStep);
        ProcessingText pt = new ProcessingText();

//        HashMap<String, HashSet<Integer>> currentCluster = getCopyOfOriginalClusters();
        HashMap<String, HashSet<Integer>> currentCluster = new HashMap<>();

        String[] splitArray = splitStep.split("--");
        for (String cluster : splitArray) {
            String[] tmp = cluster.split("~");

            for (String cid : tmp) {
                String clusterID = cid;
                boolean isOriginalGraph = false;
                if (cid.split("_").length == 1) {
                    isOriginalGraph = true;
                }
                int cutNum;
                if (cid.contains("_")) {
                    cutNum = cid.split("_").length;
                    clusterID = cid.split("_")[0];
                } else {
                    cutNum = 1;
                }

                if (cutNum > 1) {
                    currentCluster.remove(cid.split("_")[0]);
                }

                HashMap<String, HashSet<Integer>> newCluster = topClustersSplittingResult.get(Integer.valueOf(clusterID)).get(cutNum - 1);

//                Set<String> cluster_keySet = newCluster.keySet();
//                Iterator<String> itr = cluster_keySet.iterator();
//
//                while (itr.hasNext()) {

                currentCluster.put(cid, newCluster.get(cid));
                if (currentCluster.get("original") != null && currentCluster.get(cid).size() == currentCluster.get("original").size()) {
                    currentCluster.remove("original");
                }

//                }
            }
        }

        allClusteringResult.put(splitStep, currentCluster);
        return currentCluster;
    }

    /**
     * This function generates color table for current Clustering result,
     * specificall, it will modify the color of the cluster that was split.
     */
    private void generateColorTable_4CurrentCluteringResult(int clusterID, HashMap<String, HashSet<Integer>> newCluster, String splitStep) {
        ProcessingText pt = new ProcessingText();
        HashMap<Integer, String> nodeID_label = new DependencyGraph().getNodeid2LableMap(analysisDir);


//        HashMap<String, String> clusterIdToColorMap = getIdToColorMap(originCombination, false);


        /**get code to color map**/
//        HashMap<String, String> originalCodeColorMap = new HashMap<>();
//        try {
//            String[] originalCodeColorArray = pt.readResult(analysisDir + splitStep + "_colorTable.txt").split("\n");
//            for (String s : originalCodeColorArray) {
//                String[] tmpstr = s.split(",");
//                originalCodeColorMap.put(tmpstr[0], tmpstr[1]);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        Set<String> cluster_keySet = newCluster.keySet();
        int clusterSize = cluster_keySet.size();
        Iterator<String> itr = cluster_keySet.iterator();
        StringBuilder sb = new StringBuilder();

//        while (itr.hasNext()) {
//            String cluster_index = itr.next();
//            String currentColor = new ColorCode().randomColor();
////            while (clusterIdToColorMap.values().contains(currentColor)) {
////                currentColor = new ColorCode().randomColor();
////            }
////            clusterIdToColorMap.put(clusterID + "_" + clusterSize--, currentColor);
//            sb.append(clusterID + "_" + clusterSize + "," + currentColor + ",\n ");
//
//            HashSet<Integer> currentCluster = newCluster.get(cluster_index);
//            Iterator<Integer> cc = currentCluster.iterator();
//
//            //replace new color
//            while (cc.hasNext()) {
//                int nodeID = cc.next();
//                originalCodeColorMap.put(nodeID_label.get(nodeID), currentColor);
//            }
//            pt.writeTofile(clusterID + "," + currentColor + ",\n", analysisDir + originCombination + "_clusterColor.txt");
//        }


//        printNewColorTable(originalCodeColorMap, splitStep);


    }

    private void printNewColorTable(HashMap<String, String> codeColorMap, String splitStep) {
        System.out.print("");
        StringBuilder sb = new StringBuilder();
        codeColorMap.forEach((k, v) -> {
            sb.append(k + "," + v + ",\n");
        });


        new ProcessingText().rewriteFile(sb.toString(), analysisDir + splitStep + "_colorTable.txt");
    }


    public ArrayList<ArrayList<Integer>> getCombinations(int max_numberOfCut, int numberOfBiggestClusters) {
        ArrayList<ArrayList<Integer>> combination = new ArrayList<>();
        String[] com = new String[max_numberOfCut + 1];

        for (int s = 1; s <= max_numberOfCut; s++) {
            for (int m = 1; m <= numberOfBiggestClusters; m++) {
                com[s] = String.valueOf(m);
            }

        }


        return combination;
    }

    /**
     * * This function parse the cluster.txt file, to analyze each clustering result after removing a bridge
     *
     * @param clusterSizeThreshold
     * @param hasGroundTruth
     * @param combination
     * @return
     */

         /*------------include dirNum----------------------
         * Used for marlin

    public void parseEachUsefulClusteringResult(String sourcecodeDir, String analysisDir, ArrayList<String> macroList) {
        //----for Marlin repo structure----
      */
    public HashMap<Integer, ArrayList<String>> parseEachUsefulClusteringResult(int clusterSizeThreshold, boolean hasGroundTruth, String combination, boolean isOriginalGraph) {
        clusterResultMap = new HashMap<>();


        String clusterFilePath = analysisDir + combination + suffix_ClusterFile;
        String clusterResultListString = "";

        ProcessingText processingText = new ProcessingText();
//        processingText.rewriteFile("", analysisDir + combination + "_edgeCuttingRecord.txt");
//        processingText.rewriteFile("", analysisDir + combination + "_LOC_split.txt");

        if (hasGroundTruth) {
//            processingText.rewriteFile("", analysisDir + combination + "_accuracy.txt");
//            processingText.rewriteFile("", analysisDir + combination + "_joined_accuracy.txt");
        }
        /** get fork added node **/
        File forkAddedFile = new File(testCaseDir + forkAddedNodeTxt);
        if (forkAddedFile.exists()) {
            try {
                forkAddedNode = processingText.readResult(testCaseDir + forkAddedNodeTxt);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        try {
            /** add tags for each line of source code in order to generate html page later, and it also generates toggle.js file  **/
//            colorCode.createSourceFileHtml();

            clusterResultListString = processingText.readResult(clusterFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int pre_numberOfCommunites = 0;
        int previous_cutted_edge_num = 0;
        String weight_List = "";
        /** split edge cutting result  **/
        String[] resultArray = clusterResultListString.split("--------Graph-------");

        for (int i = 0; i < resultArray.length; i++) {
            /**  one edge cutting result **/
            String result = resultArray[i];
            if (result.contains("communities")) {
                /**     calculating cluster info, such as modularity, #edge cut ..  **/
                double[] clusterInfo = getClusterInfo(result.split("communities")[0], isOriginalGraph);

                int numberOfCommunities = (int) clusterInfo[0];
                if (numberOfCommunities > 1) {
                    int weight = (int) clusterInfo[3];

                    if (pre_numberOfCommunites != numberOfCommunities) {

                        ArrayList<String> clusters = getClusters(result);
                        clusterResultMap.put(numberOfCommunities, clusters);

                        /** generates current clustering result map
                         * #index  -> hashset of nodeid  **/ //todo  isjoined = false as hard coded
//                        HashMap<String, HashSet<Integer>> current_clustering_result = generateCurrentClusteringResultMap(clusters, "original",true);
                        HashMap<String, HashSet<Integer>> current_clustering_result = generateCurrentClusteringResultMap(clusters, "original", true, false);

                        /** generateing toggle.js file for each cluster **/
//                        generateToggleFileForEachCluster(current_clustering_result, false, numberOfCommunities);

                        /** calculating accuracy for clustering result
                         * 0 - true_positive,1 - false_positive, 2 - true_negtive, 3 - false_negtive, 4- accuracy
                         * **/
//                        if (hasGroundTruth) {
//                            calculatingAccuracy(groundTruthClusters, current_clustering_result, false, combination);
//                        }
                        if (!isMS_CLUSTERCHANGES) {
//                            HashMap<String, HashSet<Integer>> joined_clusters = null;
//                            if (isOriginalGraph) {

                            /**  get list of clusterSet, each list represent a set of clusters that close to each other **/
                            ArrayList<HashSet<String>> closeClusters_list;

                            if (combination.equals("original")) {
                                closeClusters_list = colorCode.joiningCloseClusters(clusters, combination, clusterSizeThreshold);
                                /**   get joined clusters   **/
                                joined_clusters = getJoinedClusters(colorCode, combination, clusters, current_clustering_result, clusterSizeThreshold, closeClusters_list);
                            } else {
                                joined_clusters = getJoinedClustersForSplittingStep(combination);
                            }


//                                /** generateing toggle.js file for each cluster **/
//                                generateToggleFileForEachCluster(joined_clusters, true, numberOfCommunities);
                            if (combination.split("--").length > 5 || combination.equals("original")) {
                                processingText.writeToJoinClusterFile(analysisDir, joined_clusters, combination);
                            }

                            if (hasGroundTruth) {
                                /** calculate accuracy result for joined clusters **/
                                calculatingAccuracy(groundTruthClusters, joined_clusters, true, combination);
                            }
//                            }

                            if (isOriginalGraph) {
                                /**   write to css file        **/
                                colorCode.writeClusterToCSS(clusters, numberOfCommunities, clusterResultMap, nodeMap, expectNodeMap, clusterSizeThreshold, hasGroundTruth, combination);
                            }
                            /** remove 3  generatingClusteringTable    step for new infox
                             * generating clustering table  **/

                            //
////                            generatingClusteringTable(testCaseDir, testDir, String.valueOf(numberOfCommunities), false, clusterSizeThreshold, current_clustering_result, hasGroundTruth);
//                            generatingClusteringTable(testCaseDir, testDir, combination, false, clusterSizeThreshold, current_clustering_result, hasGroundTruth, isOriginalGraph);
////                            generatingClusteringTable(testCaseDir, testDir, numberOfCommunities, true, clusterSizeThreshold, current_clustering_result, hasGroundTruth);
//
//                            if (isOriginalGraph) {
//                                /** generating join clustering table  **/
//                                generatingClusteringTable(testCaseDir, testDir, combination, true, clusterSizeThreshold, joined_clusters, hasGroundTruth, isOriginalGraph);
//                            }

                        }
                        if (isMS_CLUSTERCHANGES && isOriginalGraph) {
                            /**   write to css file        **/
                            colorCode.writeClusterToCSS(clusters, numberOfCommunities, clusterResultMap, nodeMap, expectNodeMap, clusterSizeThreshold, hasGroundTruth, combination);
                        }
                        /** generating clustering table for MS approach **/
//                        generatingClusteringTable(testCaseDir, testDir, combination, false, 0, current_clustering_result, hasGroundTruth, isOriginalGraph);

                        /**  combining multifles in order to generate html files **/// remove
//                        colorCode.combineFiles(combination, clusterSizeThreshold, false);
//                        if (isOriginalGraph) {
//                            colorCode.combineFiles(combination, clusterSizeThreshold, true);
//                        }
                        pre_numberOfCommunites = numberOfCommunities;

                        int numberOfCutEdges = (int) clusterInfo[1] - 1;
                        double modularity = clusterInfo[2];
                        double betweenness = clusterInfo[4];

//                        processingText.writeTofile(numberOfCommunities + "," + (numberOfCutEdges - previous_cutted_edge_num) + "," + modularity + "," + weight_List + "," + betweenness + "\n", analysisDir + combination + "_edgeCuttingRecord.txt");
                        weight_List = weight + "->";
                        previous_cutted_edge_num = numberOfCutEdges;
                    } else {
                        weight_List += weight + "->";
                    }
                }
            }
        }
        /** generating cutting summary table **/
//        generateCuttingSummaryTable(clusterSizeThreshold, hasGroundTruth, combination);
        return clusterResultMap;
    }

    private HashMap<String, HashSet<Integer>> getJoinedClustersForSplittingStep(String combination) {
        ProcessingText processingText = new ProcessingText();
        ArrayList<String> topClusterList = getTopClusterList();
        ArrayList<HashSet<String>> closeClusters_list = new ArrayList<>();
        HashMap<String, HashSet<Integer>> joined_clustering_node_result = new HashMap<>();
        HashMap<String, HashSet<String>> joined_clustering_result = new HashMap<>();

        HashMap<String, ArrayList<Integer>> clusters_changed = new HashMap<>();


        if (!combination.contains("~")) {
            try {
                String closedCluster = processingText.readResult(analysisDir + "original_joined_cluster.txt");
                processingText.rewriteFile(closedCluster, analysisDir + combination + "_joined_clusterTMP.txt");

            } catch (IOException e) {
                e.printStackTrace();
            }


        } else {
            ArrayList<String> clusters = new ArrayList<>();
            try {
                String clusterResultListString = processingText.readResult(analysisDir + combination + "_clusterTMP.txt");
//                String[] clusterArray = clusterResultListString.split("communities")[1].split("\n");
//                clusters = new ArrayList(Arrays.asList(clusterArray));

                clusters = getClusters(clusterResultListString);
            } catch (IOException e) {
                e.printStackTrace();
            }

            HashMap<String, HashSet<Integer>> current_clustering_result = generateCurrentClusteringResultMap(clusters, combination, true, true);
            HashMap<String, HashSet<Integer>> copy = new HashMap<String, HashSet<Integer>>();
            for (Map.Entry<String, HashSet<Integer>> entry : current_clustering_result.entrySet()) {
                copy.put(entry.getKey(),
                        new HashSet<>(entry.getValue()));
            }


            String[] shortestPath_nodePair = {};
            try {
                shortestPath_nodePair = processingText.readResult(analysisDir + "original_shortestPath.txt").split("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (String pair : shortestPath_nodePair) {
                if (pair.trim().length() > 0) {
                    String node_1 = pair.split(",")[0];
                    String node_2 = pair.split(",")[1];

                    HashMap<String, String> nodeId_to_clusterID = new ParseHtml().genrate_NodeId_to_clusterIDList_Map(combination, originalClusterMap);

                    String cluster_1 = nodeId_to_clusterID.get(node_1);
                    String cluster_2 = nodeId_to_clusterID.get(node_2);

                    final boolean[] checked = {false};
                    joined_clustering_result.values().forEach(x -> {
                        if (x.contains(cluster_1) && x.contains(cluster_2)) checked[0] = true;
                    });

                    if (!checked[0]) {
                        String currentClusterId = "";
                        int cluster_index = 6;
                        String original_cluster_1 = cluster_1.split("_")[0];
                        String original_cluster_2 = cluster_2.split("_")[0];
                        if (topClusterList.contains(original_cluster_1)) {
                            cluster_index = topClusterList.indexOf(original_cluster_1);
                            currentClusterId = cluster_1;
                            if (topClusterList.contains(original_cluster_2)) {
                                int cluter_2_index_of_top = topClusterList.indexOf(original_cluster_2);
                                if (cluter_2_index_of_top < cluster_index) {
                                    cluster_index = cluter_2_index_of_top;
                                    currentClusterId = cluster_2;
                                }

                            }
                        }

                        HashSet<Integer> closeCluster_nodeSet = joined_clustering_node_result.get(currentClusterId) == null ? new HashSet<>() : joined_clustering_node_result.get(currentClusterId);

                        if (!currentClusterId.equals("")) {

                            if(!cluster_1.contains("_")) {
                                copy.remove(cluster_1);
                            }
                            if(!cluster_2.contains("_")) {
                                copy.remove(cluster_2);
                            }
                            final boolean[] existGroup = {false};
                            joined_clustering_result.forEach((k, v) -> {
                                if (v.contains(cluster_1)) {
                                    v.add(cluster_2);
                                    existGroup[0] =true;

                                    joined_clustering_node_result.put(k, closeCluster_nodeSet);
                                }else if(v.contains(cluster_2)){
                                    v.add(cluster_1);
                                    existGroup[0] =true;

                                    joined_clustering_node_result.put(k, closeCluster_nodeSet);
                                }
                            });

                            if(!existGroup[0]) {
                                HashSet<String> closeCluster_Set = joined_clustering_result.get(currentClusterId) == null ? new HashSet<>() : joined_clustering_result.get(currentClusterId);
                                closeCluster_Set.add(cluster_1);
                                closeCluster_Set.add(cluster_2);
                                joined_clustering_result.put(currentClusterId, closeCluster_Set);
                            }

                        }

                            closeCluster_nodeSet.addAll(current_clustering_result.get(cluster_1) != null ? current_clustering_result.get(cluster_1) : originalClusterMap.get(cluster_1));
                            closeCluster_nodeSet.addAll(current_clustering_result.get(cluster_2) != null ? current_clustering_result.get(cluster_2) : originalClusterMap.get(cluster_2));
                    }
                }
            }

            Iterator it = copy.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                String key = (String) pair.getKey();
                HashSet<Integer> value = (HashSet<Integer>) pair.getValue();
                joined_clustering_node_result.put(key, value);
            }

            Iterator it_join = joined_clustering_node_result.entrySet().iterator();
            while (it_join.hasNext()) {
                Map.Entry pair = (Map.Entry) it_join.next();
                HashSet<Integer> nodeSet = (HashSet<Integer>) pair.getValue();

               String clusterID = (String) pair.getKey();
               HashSet<String >  closeCluster_set = joined_clustering_result.get(clusterID);
               if(closeCluster_set!=null){
                   for(String s:closeCluster_set){
                           nodeSet.addAll(current_clustering_result.get(s)!=null?current_clustering_result.get(s):originalClusterMap.get(s));
                   }
               }

                ArrayList<Integer> list_int = new ArrayList<>(nodeSet);
                clusters_changed.put((String) pair.getKey(), list_int);

            }

            processingText.rewriteFile("", analysisDir + combination + "_joined_clusterTMP.txt");
            processingText.printMemebershipOfCurrentGraph(clusters_changed, combination + "_joined_clusterTMP.txt", false, analysisDir);
        }
        return joined_clustering_node_result;

    }


    private void generateToggleFileForEachCluster(HashMap<String, HashSet<Integer>> current_clustering_result, boolean isJoinCluster, int numberOfClusters) {
        String origin_togglejsPath = "toggle.js";
        String affix = "\n});\n";
        String isJoin = isJoinCluster ? "join-" : "";
        ProcessingText processText = new ProcessingText();
        final String[] toggleFile = {""};
        try {
            toggleFile[0] = processText.readResult(analysisDir + origin_togglejsPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final String[] newToggleFile = new String[1];
        final String[] nodeNeedToBeToggled = new String[1];

        current_clustering_result.forEach((k, v) -> {
            nodeNeedToBeToggled[0] = forkAddedNode;
            if (v.size() >= 9) {

                newToggleFile[0] = "\n$( \"#" + numberOfClusters + "-cluster-" + k + "\" ).click(function() {\n"
                        + "myFunction();\n";
                System.out.println(isJoin + numberOfClusters + "-cluster-" + k);
                v.forEach(nodeid -> {
                    String nodeLabel = nodeMap.get(nodeid);
                    nodeNeedToBeToggled[0] = nodeNeedToBeToggled[0].replace(nodeLabel + "\n", "");
                });

                String[] forkAddedNodeArray = nodeNeedToBeToggled[0].split("\n");
                for (String s : forkAddedNodeArray) {
                    newToggleFile[0] += "$(\"#" + s + "\").toggle()\n";
                }
                newToggleFile[0] += affix;
                processText.writeTofile(newToggleFile[0], analysisDir + isJoin + origin_togglejsPath);

            }
        });

    }

    /**
     * This function get joined clusters
     *
     * @param colorCode
     * @param numberOfCommunities
     * @param clusters
     * @param current_clustering_result
     * @return
     */
    private HashMap<String, HashSet<Integer>> getJoinedClusters(ColorCode colorCode, String numberOfCommunities, ArrayList<String> clusters, HashMap<String, HashSet<Integer>> current_clustering_result, int clusterSizeThreshold, ArrayList<HashSet<String>> closeClusters_list) {
//        /**  get list of clusterSet, each list represent a set of clusters that close to each other **/
//        ArrayList<HashSet<String>> closeClusters_list = colorCode.joiningCloseClusters(clusters, numberOfCommunities, clusterSizeThreshold);
        ArrayList<String> topClusterList = getTopClusterList();
        HashMap<String, HashSet<Integer>> joined_clusters = new HashMap<>();
        joined_clusters.putAll(current_clustering_result);
        System.out.println("#clusters:" + numberOfCommunities);
        int pre_topcluster_index = numberOfBiggestClusters + 1;
        for (HashSet<String> closeClusters : closeClusters_list) {
            HashSet<Integer> tmp = new HashSet<>();
            Iterator it = closeClusters.iterator();
            String current_index = "";
            String pre_index = "";

            while (it.hasNext()) {
                current_index = (it.next() + "").trim();
                System.out.println("current_index: " + current_index);
                if (topClusterList.contains(current_index)) {
                    int current_topcluster_index = topClusterList.indexOf(current_index);
                    if (current_topcluster_index < pre_topcluster_index) {
                        if (pre_topcluster_index < topClusterList.size()) {
                        }
                        pre_topcluster_index = current_topcluster_index;
                    }
                }

                if (current_clustering_result.get(current_index).size() < clusterSizeThreshold || (tmp.size()>0&& tmp.size() < clusterSizeThreshold)) {
                    tmp.addAll(current_clustering_result.get(current_index));
                    joined_clusters.remove(current_index);
                    pre_index = current_index;
                } else {
                    current_index = pre_index;

                }

            }
            if (current_index != "") {
                if (pre_topcluster_index < topClusterList.size()&&joined_clusters.get(topClusterList.get(pre_topcluster_index))!=null) {

                    joined_clusters.get(topClusterList.get(pre_topcluster_index)).addAll(tmp);
                } else {
                    joined_clusters.put(current_index, tmp);
                }
                pre_topcluster_index = numberOfBiggestClusters + 1;
            }
        }

        return joined_clusters;
    }

    /**
     * This function generates current clustering result map
     * #index  -> hashset of nodeid
     *
     * @param clusters a list of clusters
     * @return hash map of current_clustering_result
     */
    private HashMap<String, HashSet<Integer>> generateCurrentClusteringResultMap(ArrayList<String> clusters, String splitStep, boolean isOriginal, boolean isJoined) {
        HashMap<String, HashSet<Integer>> current_clustering_result = new HashMap<>();
        ArrayList<String> topClusters = getTopClusterList();
        String[] splitArray = new String[1];

            splitArray = splitStep.split("--");

        for (String cluster : splitArray) {
            String[] tmp = cluster.split("~");
            for (String cid : tmp) {
                String clusterID = cid;
                String s = "", index = "";
                if (clusters.size() > 2 ||(clusters.size()==2&&isJoined)||isOriginal||(clusters.size()==2&&!splitStep.contains("~"))) {
                    for (int i = 0; i < clusters.size(); i++) {
                        s = clusters.get(i);
                        index = s.substring(0, s.indexOf(")")).trim();
                        HashSet<Integer> cluster_nodeid_Set = getNodeIdSet4Cluster(s);
                        current_clustering_result.put(index, cluster_nodeid_Set);
                    }
                } else if (!isJoined&&clusters.size() == 2) {

                    for (int i = 0; i < clusters.size(); i++) {
                        s = clusters.get(i);
                        index = clusterID + "_" + (i + 1);
                        if (topClusters.contains(index.split("_")[0])) {
                            HashSet<Integer> cluster_nodeid_Set = getNodeIdSet4Cluster(s);
                            current_clustering_result.put(index, cluster_nodeid_Set);
                        }
                    }
                } else {
                    s = clusters.get(0);
                    index = clusterID;
                    HashSet<Integer> cluster_nodeid_Set = getNodeIdSet4Cluster(s);
                    current_clustering_result.put(index, cluster_nodeid_Set);
                    if(isJoined) {
                        return current_clustering_result;
                    }
                }
            }

        }
        return current_clustering_result;
    }

    private HashSet<Integer> getNodeIdSet4Cluster(String s) {
        String str = s.substring(s.indexOf("[") + 1).replace("]", "");
        String[] nodeList = str.split(",");
        HashSet<String> cluster_nodeSet = new HashSet<>(Arrays.asList(nodeList));
        HashSet<Integer> cluster_nodeid_Set = new HashSet<>();
        Iterator<String> it = cluster_nodeSet.iterator();
        while (it.hasNext()) {
            String istr = it.next().trim();
            if (istr.length() > 0) {
                cluster_nodeid_Set.add(Integer.valueOf(istr));
            }
        }
        return cluster_nodeid_Set;
    }

    /**
     * This function generate nodeMap from node list text, key -- node id; value -- node label
     */
    public void getNodeMap_id_to_label() {
        nodeMap = new HashMap<>();
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(analysisDir + nodeListTxt));
            while ((line = br.readLine()) != null) {
                // use comma as separator
                if (line.trim().split("---------").length > 1) {
                    nodeMap.put(Integer.valueOf(line.trim().split("---------")[0]), line.trim().split("---------")[1]);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function generates expect node Map
     *
     * @return
     */
    private String generatingExpectNodeMap() {
        ProcessingText iofunc = new ProcessingText();
        expectNodeMap = new HashMap<>();
        String expectNode = "";
        try {
            expectNode = iofunc.readResult(testCaseDir + expectTxt);
        } catch (IOException e) {
            e.printStackTrace();
        }
        expectNodeMap = new HashMap<>();
        String[] nodeCluster = expectNode.split("\n");
        for (int i = 0; i < nodeCluster.length; i++) {
            expectNodeMap.put(nodeCluster[i].split(" ")[0], nodeCluster[i].split(" ")[1]);
        }
        return expectNode;
    }


    /**
     * * This function calculats accuracy for each cutting result
     *
     * @param groundTruthClusters       ground truth clusters
     * @param current_clustering_result current clustering result clusters
     * @return int array, size = 5;
     * true_positive, false_positive, true_negtive, false_negtive, accuracy
     */

    public void calculatingAccuracy(HashMap<String, HashSet<Integer>> groundTruthClusters, HashMap<String, HashSet<Integer>> current_clustering_result, boolean isjoined, String combination) {
        int true_positive = 0;
        int false_positive = 0;
        int true_negtive = 0;
        int false_negtive = 0;
        ProcessingText processingText = new ProcessingText();
        HashSet<Integer> nodeIDSet = new HashSet<>();

        try {
            String forkAddedNodeIdList = processingText.readResult(testCaseDir + "forkAddedNodeID.txt").trim();
            String[] newNodeId = forkAddedNodeIdList.split(",");
            for (String nodeIdStr : newNodeId) {
                if (!newNodeId.equals("\n")) {
                    nodeIDSet.add(Integer.valueOf(nodeIdStr));
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        HashSet<HashSet<Integer>> nodePairSet = new GenerateCombination(analysisDir, max_numberOfCut).getAllPairs(nodeIDSet);
        Iterator nodePair_iterator = nodePairSet.iterator();


        while (nodePair_iterator.hasNext()) {
            HashSet<Integer> pair = (HashSet<Integer>) nodePair_iterator.next();
            /**   check ground truth **/
            boolean groundTruth_nodes_belong_Together = nodes_belong_together(groundTruthClusters, pair);

            /**check clustering result**/
            boolean clusteringResult_nodes_belong_Together = nodes_belong_together(current_clustering_result, pair);

            if (groundTruth_nodes_belong_Together == true && clusteringResult_nodes_belong_Together == true) {
                true_positive++;
            } else if (groundTruth_nodes_belong_Together == true && clusteringResult_nodes_belong_Together == false) {
                false_negtive++;
            } else if (groundTruth_nodes_belong_Together == false && clusteringResult_nodes_belong_Together == true) {
                false_positive++;
            } else if (groundTruth_nodes_belong_Together == false && clusteringResult_nodes_belong_Together == false) {
                true_negtive++;
            }
        }

        float accuracy = (float) (true_positive + true_negtive) / (true_positive + false_positive + false_negtive + true_negtive);

        String fileName = combination + "_accuracy.txt";
        if (isjoined) {
            fileName = combination + "_joined_accuracy.txt";
        }

        processingText.writeTofile(true_positive + "," + false_positive + "," + true_negtive + "," + false_negtive + "," + accuracy + "\n", analysisDir + fileName);
    }

    /**
     * This function check whether a pair of node belong to one cluster
     *
     * @param clusters several clusters
     * @param pair     a pair of node id
     * @return true- belong together; false - do not belong together
     */
    private boolean nodes_belong_together(HashMap<String, HashSet<Integer>> clusters, HashSet<Integer> pair) {
        List<Integer> list = new ArrayList<>(pair);
        int first = list.get(0);
        int second = list.get(1);

        boolean nodes_belong_Together = false;
        Iterator iterator = clusters.entrySet().iterator();
        while (iterator.hasNext()) {
            HashSet<Integer> currentCluster = (HashSet<Integer>) ((Map.Entry) iterator.next()).getValue();
            if (currentCluster.contains(first) && currentCluster.contains(second)) {
                nodes_belong_Together = true;
                break;
            } else if ((currentCluster.contains(first) && !currentCluster.contains(second)) || (!currentCluster.contains(first) && currentCluster.contains(second))) {
                nodes_belong_Together = false;
                break;
            } else {
                continue;
            }
        }
        return nodes_belong_Together;
    }

    /**
     * This function generates the ground truth cluster,
     * The output is a hashmap: key--- clusterID, value--- set of nodeid
     */
    public int[] generateGroundTruthMap() {
        int[] avg_max_size = new int[2];
        int avgFeatureSize = 0;
        groundTruthClusters = new HashMap<>();
        /**get node map  id-> label**/
        //todo :refactoring

        generatingExpectNodeMap();

        for (Integer nodeId : nodeMap.keySet()) {
            String nodeLabel = nodeMap.get(nodeId);
            HashSet<Integer> nodeSet;

            if (expectNodeMap.get(nodeLabel) != null) {

                String clusterNumber = expectNodeMap.get(nodeLabel);
                if (groundTruthClusters.get(clusterNumber) != null) {
                    nodeSet = groundTruthClusters.get(clusterNumber);
                } else {
                    nodeSet = new HashSet<>();
                }
                nodeSet.add(nodeId);
                groundTruthClusters.put(clusterNumber, nodeSet);
            }
        }
        Iterator it = groundTruthClusters.entrySet().iterator();
        ArrayList<Integer> sizeList = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            avgFeatureSize += ((HashSet<Integer>) pair.getValue()).size();
            sizeList.add(((HashSet<Integer>) pair.getValue()).size());

        }

        Collections.sort(sizeList);
        int maxSize = sizeList.get(sizeList.size() - 1);
        avgFeatureSize = avgFeatureSize / groundTruthClusters.size();

        return new int[]{avgFeatureSize, maxSize};
    }

    private void generateCuttingSummaryTable(int clusterSizeThreshold, boolean hasGroundTruth, String combination) {
        ProcessingText processingText = new ProcessingText();
        StringBuilder sb_csv = new StringBuilder();
        sb_csv.append("Clusters,RemovedEdges,modularity,latest betweenness,LOC Split,weight of cut edges,accuracy,Joined accuracy\n");
        String edgeCuttingRecord = "";
        String loc_splitting = "";
        String accuracyStr = "";
        String joined_accuracyStr = "";
        try {
            edgeCuttingRecord = processingText.readResult(analysisDir + combination + "_edgeCuttingRecord.txt");
            loc_splitting = processingText.readResult(analysisDir + combination + "_LOC_split.txt");
            if (hasGroundTruth) {
                accuracyStr = processingText.readResult(analysisDir + combination + "_accuracy.txt");
                joined_accuracyStr = processingText.readResult(analysisDir + combination + "_joined_accuracy.txt");

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] edgeCuttingArray = edgeCuttingRecord.split("\n");
        String[] loc_splittingArray = loc_splitting.split("\n");

        String[] accuracy_Array = null;
        String[] joined_accuracy_Array = null;
        if (hasGroundTruth) {
            accuracy_Array = accuracyStr.split("\n");
            joined_accuracy_Array = joined_accuracyStr.split("\n");
        }
        for (int i = 0; i < edgeCuttingArray.length; i++) {
            String cut = edgeCuttingArray[i];
            String[] cut_content = cut.split(",");

            String numOfClusters = cut_content[0];
            String numOfRemovedEdges = cut_content[1];
            String modularity = cut_content[2];
            String numOfLOCSplit = loc_splittingArray[i].split(",")[1];
            String weight_list = "0";
            if (cut_content.length > 3) {
                String weightList_origin = cut_content[3];
                if (weightList_origin.length() > 0) {
                    weight_list = weightList_origin.substring(0, weightList_origin.length() - 2);
                }
            }

            String betweenness = cut_content[4];

            /**  organizing accuracy result [0 - TRUE_positive,1 - false_positive, 2 - true_negtive, 3 - false_negtive, 4- accuracy]
             * (true_positive + true_negtive) / (true_positive + false_positive + false_negtive + true_negtive)
             * 0 + 2 / 0+1+3+2
             * **/
            String joined_accruacy = "", accruacy = "";
            if (hasGroundTruth) {

                String[] accuracy_oneCut = accuracy_Array[i].split(",");
//            int TP = Integer.parseInt(accuracy_oneCut[0]);
//            int FP = Integer.parseInt(accuracy_oneCut[1]);
//            int TN = Integer.parseInt(accuracy_oneCut[2]);
//            int FN = Integer.parseInt(accuracy_oneCut[3]);
                String AC = accuracy_oneCut[4];
                accruacy = AC;
//                    + " = " + TP + " + " + TN
//                    + " / "
//                    + TP + " + " + FP + " + " + FN + " + " + TN;


                if (!isMS_CLUSTERCHANGES) {
                    String[] joined_accuracy_oneCut = joined_accuracy_Array[i].split(",");

                    int joined_TP = Integer.parseInt(joined_accuracy_oneCut[0]);
                    int joined_FP = Integer.parseInt(joined_accuracy_oneCut[1]);
                    int joined_TN = Integer.parseInt(joined_accuracy_oneCut[2]);
                    int joined_FN = Integer.parseInt(joined_accuracy_oneCut[3]);
                    float joined_AC = Float.parseFloat(joined_accuracy_oneCut[4]);

                    joined_accruacy = joined_AC + "";
//                    + " = " + joined_TP + " + " + joined_TN
//                    + " / "
//                    + joined_TP + " + " + joined_FP + " + " + joined_FN + " + " + joined_TN;
                } else {
                    joined_accruacy = "MS_no_join";
                }
            }
            sb_csv.append(numOfClusters + "," + numOfRemovedEdges + "," + modularity + "," + betweenness + "," + numOfLOCSplit + "," + weight_list + "," + accruacy + "," + joined_accruacy + "\n");
        }
        processingText.rewriteFile(sb_csv.toString(), analysisDir + combination + "_resultTable_joinThreshold-" + clusterSizeThreshold + ".csv");

    }


    /**
     * This function get the number of communities and how many edges have been removed in current cluster result
     *
     * @param s cluster information
     * @return int array [0] --> num of communities
     * array[1] --> number of cut edges
     */

    private double[] getClusterInfo(String s, boolean isOriginalGraph) {
        double[] clusterInfo = new double[5];
        //get community number
        int start = s.indexOf("---");
        String number = s.substring(start + 3);
        clusterInfo[0] = Integer.valueOf(number.trim());

        if (isOriginalGraph) {
            //get weight
            int weight_from = s.indexOf("weight=") + 8;
            int weight_to = s.indexOf(")");
            String weight = s.substring(weight_from, weight_to);
            clusterInfo[3] = Double.parseDouble(weight);

            //get latest removed edge betweenness
            int betweenness = s.indexOf(" betweenness:") + 13;


            //get modularity
            int modularity_loc = s.indexOf("Modularity") + 11;


            String betweenness_str = s.substring(betweenness, modularity_loc - 11).trim();
            clusterInfo[4] = Double.parseDouble(betweenness_str);

            String modularity = s.substring(modularity_loc, start - 1);
            clusterInfo[2] = Double.parseDouble(modularity);


            // get number of cut edge
            int i = s.indexOf("**");
            int j = s.indexOf("edges");
            String numOfCutEdges = s.substring(i + 3, j);
            clusterInfo[1] = Integer.valueOf(numOfCutEdges.trim());
        }
        return clusterInfo;
    }

}