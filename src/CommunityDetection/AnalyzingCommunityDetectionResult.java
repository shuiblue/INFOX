package CommunityDetection;

import ColorCode.BackgroundColor;
import ColorCode.ColorCode;
import Util.GenerateCombination;
import Util.ProcessingText;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by shuruiz on 8/29/16.
 */
public class AnalyzingCommunityDetectionResult {
    ProcessingText processingText = new ProcessingText();
    String analysisDir, testCaseDir, sourcecodeDir, testDir;
    static String nodeListTxt = "/NodeList.txt";
    static String forkAddedNodeTxt = "forkAddedNode.txt";

    static String expectTxt = "expectCluster.txt";
    static String forkAddedNode = "";
    int initialNumOfClusters = 0;
    HashMap<Integer, ArrayList<String>> clusterResultMap = new HashMap<>();
    static HashMap<Integer, String> nodeMap = new HashMap<>();


    static HashMap<String, String> expectNodeMap = new HashMap<>();
    HashMap<Integer, HashSet<Integer>> groundTruthClusters = new HashMap<>();
    List<String> bgcolorList = BackgroundColor.getExpectColorList();
    static final String FS = File.separator;

    static ProcessingText iofunc = new ProcessingText();
    static int BIG_SIZE = 50;


    public AnalyzingCommunityDetectionResult(String sourcecodeDir, String testCaseDir, String testDir) {
        this.sourcecodeDir = sourcecodeDir;
        this.analysisDir = testCaseDir + testDir + FS;
        this.testCaseDir = testCaseDir;
        this.testDir = testDir;
    }


    //    public void generatingClusteringTable(String analysisDir, int numberOfCommunities, ArrayList<String> macroList) {
    public void generatingClusteringTable(String testCaseDir, String testDir, int numberOfCommunities, boolean isJoiningTable) {
        this.analysisDir = testCaseDir + testDir + FS;
        this.testCaseDir = testCaseDir;
        HashMap<String, HashMap<String, Integer>> resultTable = new HashMap<>();

//        for (int i = 0; i < macroList.size(); i++) {
//            resultTable.put(bgcolorList.get(i), new HashMap<>());
//        }


        String filePath;
        if (!isJoiningTable) {
            filePath = analysisDir + numberOfCommunities + "_colorTable.txt";
        } else {
            filePath = analysisDir + numberOfCommunities + "_colorTable_join.txt";
        }
        try {

            Thread.sleep(1000);
            String cssString = processingText.readResult(filePath);
            String[] colorArray = cssString.split("\n");
            ArrayList<String> nodeColorList = new ArrayList(Arrays.asList(colorArray));
            ArrayList<String> communityColorList = new ArrayList<>();
            for (String node : nodeColorList) {
                if (node.length() > 0) {

                    String[] nodeInfo = node.split(",");
                    String id = nodeInfo[0];
                    String bgColor = nodeInfo[1];
                    String expectColor = nodeInfo[2];
                    if (!communityColorList.contains(bgColor)) {
                        communityColorList.add(bgColor);
                    }

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

                }
            }

            printResultTable(resultTable, communityColorList, numberOfCommunities, isJoiningTable);
//            printResultTable(resultTable, communityColorList, numberOfCommunities, macroList);

            printClusterDistanceTable(numberOfCommunities);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void printClusterDistanceTable(int numberOfCommunities) {
        StringBuffer sb = new StringBuffer();
        HashMap<String, String[]> distanceTable = new HashMap<>();

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
    private void printResultTable(HashMap<String, HashMap<String, Integer>> resultTable, ArrayList<String> communityColorList, int numberOfCommunities, boolean isJoiningTable) {

        StringBuffer sb = new StringBuffer();
        Iterator it = resultTable.keySet().iterator();

        ArrayList<String> featureList = getFeatureList();


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


//                new ArrayList<>();
//        macroList.add("ots_firewall");
//        macroList.add("stm2");
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
            processingText.rewriteFile(sb.toString(), analysisDir + numberOfCommunities + "_join.color");
        }
    }

    private HashMap<String, String> getIdToColorMap(int numberOfCommunities, boolean isJoiningTable) {

        String[] colorList = new String[0];
        try {
            colorList = processingText.readResult(analysisDir + numberOfCommunities + "_clusterColor.txt").split("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<String, String> colorTable = new HashMap();
        for (String c : colorList) {
            //[0] id  [1] current color  [2] expect color
            String[] content = c.split(",");
            String id = content[0];
            String current_color = content[1];
            String expect_color = content[2];

            colorTable.put(id, current_color);
        }
        return colorTable;
    }

    private ArrayList<String> getFeatureList() {
        try {
            String[] featureArray = processingText.readResult(testCaseDir + "featureList.txt").split("\n");
            return new ArrayList<>(Arrays.asList(featureArray));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This function parse the cluster.txt file, to analyze each clustering result after removing a bridge
     */
         /*------------include dirNum----------------------
         * Used for marlin

    public void parseEachUsefulClusteringResult(String sourcecodeDir, String analysisDir, ArrayList<String> macroList) {
        //----for Marlin repo structure----
      */
    public HashMap<Integer, ArrayList<String>> parseEachUsefulClusteringResult() {


        String clusterFilePath = analysisDir + "clusterTMP.txt";
        String clusterResultListString = "";
        processingText.rewriteFile("", analysisDir + "edgeCuttingRecord.txt");
        processingText.rewriteFile("", analysisDir + "LOC_split.txt");
        processingText.rewriteFile("", analysisDir + "accuracy.txt");
        processingText.rewriteFile("", analysisDir + "joined_accuracy.txt");
        //get fork added node
        File forkAddedFile = new File(testCaseDir + forkAddedNodeTxt);
        if (forkAddedFile.exists()) {
            try {
                forkAddedNode = iofunc.readResult(testCaseDir + forkAddedNodeTxt);
//                clusterResultListString = iofunc.readResult(clusterFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ColorCode colorCode = new ColorCode(sourcecodeDir, testCaseDir, testDir, forkAddedNode);
        try {
            colorCode.createSourceFileHtml();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            clusterResultListString = iofunc.readResult(clusterFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int pre_numberOfCommunites = 0;
        int previous_cutted_edge_num = 0;
        String weight_List = "";
        String[] resultArray = clusterResultListString.split("--------Graph-------");

        for (int i = 0; i < resultArray.length; i++) {
            String result = resultArray[i];


            if (result.contains("communities")) {
                /**     calculating cluster info, such as modularity, #edge cut ..  **/
                double[] clusterInfo = getClusterInfo(result.split("communities")[0]);

                int numberOfCommunities = (int) clusterInfo[0];
                if (numberOfCommunities > 1) {
                    int weight = (int) clusterInfo[3];
//                int numOfCutEdge = clusterInfo[1];
//                if (numOfCutEdge <= bestcut) {

                    if (pre_numberOfCommunites != numberOfCommunities) {
                        result = result.split("communities")[1];
                        String[] clusterArray = result.split("\n");

                        ArrayList<String> clusters = new ArrayList(Arrays.asList(clusterArray));

                        if (clusterResultMap.size() == 0) {
                            initialNumOfClusters = numberOfCommunities;
                            processingText.writeTofile(initialNumOfClusters + ",0\n", analysisDir + "LOC_split.txt");
                        }
                        clusterResultMap.put(numberOfCommunities, clusters);
                        /**   write to css file        **/

                        HashMap<Integer, HashSet<Integer>> current_clustering_result = generateCurrentClusteringResultMap(clusters);


                        /**   get joined clusters   **/
                        HashMap<Integer, HashSet<Integer>> joined_clusters = getJoinedClusters(colorCode, numberOfCommunities, clusters, current_clustering_result);


                        /** calculating accuracy for clustering result
                         * 0 - true_positive,1 - false_positive, 2 - true_negtive, 3 - false_negtive, 4- accuracy
                         * **/
                        calculatingAccuracy(groundTruthClusters, current_clustering_result, false);
                        calculatingAccuracy(groundTruthClusters, joined_clusters, true);


                        generatingClusteringTable(testCaseDir, testDir, numberOfCommunities, false);
                        generatingClusteringTable(testCaseDir, testDir, numberOfCommunities, true);


                        colorCode.writeClusterToCSS(clusters, numberOfCommunities, clusterResultMap, nodeMap, expectNodeMap);

                        colorCode.combineFiles(numberOfCommunities);
                        pre_numberOfCommunites = numberOfCommunities;

                        int numberOfCutEdges = (int) clusterInfo[1] - 1;
                        double modularity = clusterInfo[2];


                        processingText.writeTofile(numberOfCommunities + "," + (numberOfCutEdges - previous_cutted_edge_num) + "," + modularity + "," + weight_List + "\n", analysisDir + "edgeCuttingRecord.txt");
                        weight_List = weight + "->";
                        previous_cutted_edge_num = numberOfCutEdges;
                    } else {
                        weight_List += weight + "->";

                    }


                }
            }
        }
        generateCuttingSummaryTable();
        return clusterResultMap;
    }

    private HashMap<Integer, HashSet<Integer>> getJoinedClusters(ColorCode colorCode, int numberOfCommunities, ArrayList<String> clusters, HashMap<Integer, HashSet<Integer>> current_clustering_result) {
        /**  get joining result  **/
        ArrayList<HashSet<String>> closeClusters_list = colorCode.joiningCloseClusters(clusters, numberOfCommunities);
        HashMap<Integer, HashSet<Integer>> joined_clusters = new HashMap<>();
        joined_clusters.putAll(current_clustering_result);
        System.out.println("#clusters:" + numberOfCommunities);

        for (HashSet<String> closeClusters : closeClusters_list) {
            HashSet<Integer> tmp = new HashSet<>();
            Iterator it = closeClusters.iterator();
            int index = 0;
            while (it.hasNext()) {
                index = Integer.valueOf((String) it.next());

                if (current_clustering_result.get(index).size() < BIG_SIZE) {
                    tmp.addAll(current_clustering_result.get(index));
                    joined_clusters.remove(index);
                }

            }
            joined_clusters.put(index, tmp);
        }

        return joined_clusters;
    }


    /**
     * This function generate nodeMap from node list text, key -- node id; value -- node label
     */
    private void getNodeMap_id_to_label() {
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
     * This function generates current clustering result map
     * #index  -> hashset of nodeid
     *
     * @param clusters a list of clusters
     * @return hash map of current_clustering_result
     */
    private HashMap<Integer, HashSet<Integer>> generateCurrentClusteringResultMap(ArrayList<String> clusters) {

        HashMap<Integer, HashSet<Integer>> current_clustering_result = new HashMap<>();
        for (String s : clusters) {
            if (!s.equals("")) {

                int index = Integer.valueOf(s.substring(0, s.indexOf(")")));
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
                current_clustering_result.put(index, cluster_nodeid_Set);
            }
        }
        return current_clustering_result;
    }

    /**
     * * This function calculats accuracy for each cutting result
     *
     * @param groundTruthClusters       ground truth clusters
     * @param current_clustering_result current clustering result clusters
     * @return int array, size = 5;
     * true_positive, false_positive, true_negtive, false_negtive, accuracy
     */

    public void calculatingAccuracy(HashMap<Integer, HashSet<Integer>> groundTruthClusters, HashMap<Integer, HashSet<Integer>> current_clustering_result, boolean isjoined) {
        int true_positive = 0;
        int false_positive = 0;
        int true_negtive = 0;
        int false_negtive = 0;

        HashSet<Integer> nodeIDSet = new HashSet<>();

        try {
            String forkAddedNodeIdList = processingText.readResult(testCaseDir + "forkAddedNodeID.txt").trim();
            String[] newNodeId = forkAddedNodeIdList.split(",");
            for (String nodeIdStr : newNodeId) {
                if (!newNodeId.equals("\n")) {
                    nodeIDSet.add(Integer.valueOf(nodeIdStr));
                }

            }

            System.out.print("");
        } catch (IOException e) {
            e.printStackTrace();
        }


//        Iterator node_iter = nodeMap.entrySet().iterator();
        Iterator node_iter = nodeIDSet.iterator();
//        Iterator node_iter = expectNodeMap.entrySet().iterator();

        HashSet<HashSet<Integer>> nodePairSet = new GenerateCombination().getAllPairs(nodeIDSet);
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

        String fileName = "accuracy.txt";
        if (isjoined) {
            fileName = "joined_accuracy.txt";
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
    private boolean nodes_belong_together(HashMap<Integer, HashSet<Integer>> clusters, HashSet<Integer> pair) {
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
    public void generateGroundTruthMap() {
        /**get node map  id-> label**/
        //todo :refactoring
        getNodeMap_id_to_label();
        generatingExpectNodeMap();

        for (Integer nodeId : nodeMap.keySet()) {
            String nodeLabel = nodeMap.get(nodeId);
            HashSet<Integer> nodeSet;

            if (expectNodeMap.get(nodeLabel) != null) {

                Integer clusterNumber = Integer.valueOf(expectNodeMap.get(nodeLabel));
                if (groundTruthClusters.get(clusterNumber) != null) {
                    nodeSet = groundTruthClusters.get(clusterNumber);
                } else {
                    nodeSet = new HashSet<>();
                }
                nodeSet.add(nodeId);
                groundTruthClusters.put(clusterNumber, nodeSet);
            }
        }
    }

    private void generateCuttingSummaryTable() {
        StringBuilder sb_csv = new StringBuilder();
        sb_csv.append("#Clusters,#RemovedEdges,modularity,#LOC Split,#weight of cut edges,accuracy,Joined accuracy\n");
        String edgeCuttingRecord = "";
        String loc_splitting = "";
        String accuracyStr = "";
        String joined_accuracyStr = "";
        try {
            edgeCuttingRecord = processingText.readResult(analysisDir + "edgeCuttingRecord.txt");
            loc_splitting = processingText.readResult(analysisDir + "LOC_split.txt");
            accuracyStr = processingText.readResult(analysisDir + "accuracy.txt");
            joined_accuracyStr = processingText.readResult(analysisDir + "joined_accuracy.txt");

        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] edgeCuttingArray = edgeCuttingRecord.split("\n");
        String[] loc_splittingArray = loc_splitting.split("\n");
        String[] accuracy_Array = accuracyStr.split("\n");
        String[] joined_accuracy_Array = joined_accuracyStr.split("\n");
        for (int i = 0; i < edgeCuttingArray.length; i++) {
            String cut = edgeCuttingArray[i];
            String[] cut_content = cut.split(",");

            String numOfClusters = cut_content[0];
            String numOfRemovedEdges = cut_content[1];
            String modularity = cut_content[2];
            String numOfLOCSplit = loc_splittingArray[i].split(",")[1];
            String weight_list = "";
            if (cut_content.length > 3) {
                String weightList_origin = cut_content[3];
                weight_list = weightList_origin.substring(0, weightList_origin.length() - 2);
            }

            /**  organizing accuracy result [0 - TRUE_positive,1 - false_positive, 2 - true_negtive, 3 - false_negtive, 4- accuracy]
             * (true_positive + true_negtive) / (true_positive + false_positive + false_negtive + true_negtive)
             * 0 + 2 / 0+1+3+2
             * **/


            String[] accuracy_oneCut = accuracy_Array[i].split(",");
//            int TP = Integer.parseInt(accuracy_oneCut[0]);
//            int FP = Integer.parseInt(accuracy_oneCut[1]);
//            int TN = Integer.parseInt(accuracy_oneCut[2]);
//            int FN = Integer.parseInt(accuracy_oneCut[3]);
            String AC = accuracy_oneCut[4];
            String accruacy = AC;
//                    + " = " + TP + " + " + TN
//                    + " / "
//                    + TP + " + " + FP + " + " + FN + " + " + TN;

            String[] joined_accuracy_oneCut = joined_accuracy_Array[i].split(",");

            int joined_TP = Integer.parseInt(joined_accuracy_oneCut[0]);
            int joined_FP = Integer.parseInt(joined_accuracy_oneCut[1]);
            int joined_TN = Integer.parseInt(joined_accuracy_oneCut[2]);
            int joined_FN = Integer.parseInt(joined_accuracy_oneCut[3]);
            float joined_AC = Float.parseFloat(joined_accuracy_oneCut[4]);

            String joined_accruacy = joined_AC+"";
//                    + " = " + joined_TP + " + " + joined_TN
//                    + " / "
//                    + joined_TP + " + " + joined_FP + " + " + joined_FN + " + " + joined_TN;

            sb_csv.append(numOfClusters + "," + numOfRemovedEdges + "," + modularity + "," + numOfLOCSplit + "," + weight_list + "," + accruacy + "," + joined_accruacy + "\n");
        }
        processingText.rewriteFile(sb_csv.toString(), analysisDir + "resultTable.csv");

    }


    /**
     * This function get the number of communities and how many edges have been removed in current cluster result
     *
     * @param s cluster information
     * @return int array [0] --> num of communities
     * array[1] --> number of cut edges
     */

    private double[] getClusterInfo(String s) {
        double[] clusterInfo = new double[4];
        //get weight
        int weight_from = s.indexOf("weight=") + 8;
        int weight_to = s.indexOf(")");
        String weight = s.substring(weight_from, weight_to);
        clusterInfo[3] = Double.parseDouble(weight);


        //get modularity
        int modularity_loc = s.indexOf("Modularity") + 11;
        //get community number
        int start = s.indexOf("---");

        String modularity = s.substring(modularity_loc, start - 1);
        clusterInfo[2] = Double.parseDouble(modularity);

        String number = s.substring(start + 3);
        clusterInfo[0] = Integer.valueOf(number.trim());

        // get number of cut edge
        int i = s.indexOf("**");
        int j = s.indexOf("edges");
        String numOfCutEdges = s.substring(i + 3, j);
        clusterInfo[1] = Integer.valueOf(numOfCutEdges.trim());

        return clusterInfo;
    }

}