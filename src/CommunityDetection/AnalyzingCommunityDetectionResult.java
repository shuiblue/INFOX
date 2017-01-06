package CommunityDetection;

import ColorCode.BackgroundColor;
import ColorCode.ColorCode;
import Util.ProcessingText;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by shuruiz on 8/29/16.
 */
public class AnalyzingCommunityDetectionResult {
    ProcessingText processingText = new ProcessingText();
    String analysisDir, testCaseDir, sourcecodeDir;

    static String forkAddedNodeTxt = "forkAddedNode.txt";
    static String forkAddedNode = "";
    int initialNumOfClusters = 0;
    HashMap<Integer, ArrayList<String>> clusterResultMap = new HashMap<>();
    static HashMap<Integer, String> nodeMap = new HashMap<>();


    static HashMap<String, String> expectNodeMap = new HashMap<>();
    HashMap<Integer, HashSet<Integer>> groundTruthClusters = new HashMap<>();
    List<String> bgcolorList = BackgroundColor.getExpectColorList();
    static final String FS = File.separator;

    static ProcessingText iofunc = new ProcessingText();

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
    public HashMap<Integer, ArrayList<String>> parseEachUsefulClusteringResult(String sourcecodeDir, String testCaseDir, String testDir) {


        this.sourcecodeDir = sourcecodeDir;
        this.analysisDir = testCaseDir + testDir + FS;
        this.testCaseDir = testCaseDir;
        ColorCode colorCode = new ColorCode(sourcecodeDir, testCaseDir, testDir);

        String clusterFilePath = analysisDir + "clusterTMP.txt";
        String clusterResultListString = "";
        processingText.rewriteFile("", analysisDir + "edgeCuttingRecord.txt");
        processingText.rewriteFile("", analysisDir + "LOC_split.txt");
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

                    colorCode.writeClusterToCSS(clusters, numberOfCommunities, clusterResultMap);


                    generateGroundTruthMap();
                    calculatingAccuracy(clusters);


                    AnalyzingCommunityDetectionResult analyzeCDResult = new AnalyzingCommunityDetectionResult();

                    analyzeCDResult.generatingClusteringTable(testCaseDir, testDir, numberOfCommunities, false);
                    analyzeCDResult.generatingClusteringTable(testCaseDir, testDir, numberOfCommunities, true);

                    colorCode.combineFiles(numberOfCommunities);
                    pre_numberOfCommunites = numberOfCommunities;

                    int numberOfCutEdges = (int) clusterInfo[1] - 1;
                    double modularity = clusterInfo[2];


                    processingText.writeTofile(numberOfCommunities + "," + (numberOfCutEdges - previous_cutted_edge_num) + "," + modularity + "," + weight_List + "\n", analysisDir + "edgeCuttingRecord.txt");
                    weight_List = weight + "-";
                    previous_cutted_edge_num = numberOfCutEdges;
                } else {
                    weight_List += weight + "-";

                }


            }
        }
        generateCuttingSummaryTable();
        return clusterResultMap;
    }

    /**
     * This function calculats accuracy for each cutting result
     *
     * @param clusters
     */
    private void calculatingAccuracy(ArrayList<String> clusters) {
        int index = 1;

        HashMap<Integer, HashSet<Integer>> current_clustering_result = new HashMap<>();
        for (String s : clusters) {
            if (!s.equals("")) {
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
                current_clustering_result.put(index++, cluster_nodeid_Set);
            }
        }

        System.out.print("");


    }

    /**
     * This function generates the ground truth cluster,
     * The output is a hashmap: key--- clusterID, value--- set of nodeid
     */
    private void generateGroundTruthMap() {
        for (Integer nodeId : nodeMap.keySet()) {
            String nodeLabel = nodeMap.get(nodeId);
            HashSet<Integer> nodeSet;

            Integer clusterNumber = Integer.valueOf(expectNodeMap.get(nodeLabel));
            if (groundTruthClusters.get(clusterNumber) != null) {
                nodeSet = groundTruthClusters.get(clusterNumber);
            } else {
                nodeSet = new HashSet<>();
            }
            nodeSet.add(nodeId);
            groundTruthClusters.put(clusterNumber, nodeSet);
        }
        System.out.print("");
    }

    private void generateCuttingSummaryTable() {
        String edgeCuttingRecord = "";
        String loc_splitting = "";
        try {
            edgeCuttingRecord = processingText.readResult(analysisDir + "edgeCuttingRecord.txt");
            loc_splitting = processingText.readResult(analysisDir + "LOC_split.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<style>\n" +
                "table, th, td {\n" +
                "    border: 1px solid black;\n" +
                "}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<h2>" + testCaseDir + "\n" +
                "\n" +
                "<table>\n" +
                "  <tr>\n" +
                "    <th>#Clusters</th>\n" +
                "    <th>#RemovedEdges</th>\n" +
                "    <th>modularity</th>\n" +
                "    <th>#LOC Split</th>\n" +
                "    <th>#weight of cut edges</th>\n" +
                "  </tr>");


        String[] edgeCuttingArray = edgeCuttingRecord.split("\n");
        String[] loc_splittingArray = loc_splitting.split("\n");
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
                weight_list = weightList_origin.substring(0, weightList_origin.length() - 1);
            }
            sb.append("<tr>\n" +
                    "    <td>" + numOfClusters + "</td>\n" +
                    "    <td>" + numOfRemovedEdges + "</td>\n" +
                    "    <td>" + modularity + "</td>\n" +
                    "    <td>" + numOfLOCSplit + "</td>\n" +
                    "    <td>" + weight_list + "</td>\n" +
                    "  </tr>");

        }
        sb.append("</table>\n" +
                "\n" +
                "</body>\n" +
                "</html>\n");


        processingText.rewriteFile(sb.toString(), analysisDir + "resultTable.html");

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

    public static void setNodeMap(HashMap<Integer, String> nodeMap) {
        AnalyzingCommunityDetectionResult.nodeMap = nodeMap;
    }

    public static void setExpectNodeMap(HashMap<String, String> expectNodeMap) {
        AnalyzingCommunityDetectionResult.expectNodeMap = expectNodeMap;
    }
}