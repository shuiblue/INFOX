package ColorCode;

import Util.ProcessingText;

import java.awt.*;
import java.io.*;
import java.util.*;

/**
 * Created by shuruiz on 8/29/16.
 */
public class ColorCode {
    static ProcessingText iofunc = new ProcessingText();
    static final String CSS = ".css";
    static String expectTxt = "expectCluster.txt";
    static String sourceCodeTxt = "sourceCode.txt";
    static String nodeListTxt = "/NodeList.txt";
    static String upstreamNodeTxt = "upstreamNode.txt";
    static String forkAddedNodeTxt = "forkAddedNode.txt";
    static String jsFileHeader = "/jshead.txt";
    static HashMap<Integer, String> nodeMap;
    HashMap<Integer, String> colorMap = new HashMap<>();
    HashSet<String> bigSizeClusterList = new HashSet<>();
    static StringBuffer jsContent = new StringBuffer();
    static String forkAddedNode = "";

    String sourcecodeDir;
    String analysisDir;
    String testCaseDir;
    boolean print = false;
    StringBuilder sb = new StringBuilder();
    ProcessingText processingText = new ProcessingText();
    HashMap<Integer, ArrayList<String>> clusterResultMap = new HashMap<>();
    ArrayList<HashSet<String>> closeClusterList = new ArrayList<>();
    HashMap<HashSet<String>, String> closeClusters_ColorMap = new HashMap<>();
    HashMap<String, String> expectNodeMap = new HashMap<>();
    HashMap<Integer, HashSet<Integer>> groundTruthClusters = new HashMap<>();
    int initialNumOfClusters = 0;

    public void parseSourceCodeFromFile(String fileName) {
        File currentFile = new File(sourcecodeDir + fileName);
        //            if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".c")) {
        if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".c") || fileName.endsWith(".pde")) {
//            System.out.print(fileName + "\n");
            String newFileName;
            newFileName = iofunc.changeFileName(fileName);
            if (forkAddedNode.contains(newFileName)) {
                String fileName_forHTML = "";
                fileName_forHTML = newFileName.replace("~", "-");
                sb.append("<h1 id=\"" + newFileName + "title\" >" + fileName + "</h1>\n<pre id=\"" + newFileName + "\"  class=\"prettyprint linenums\">");
                BufferedReader result_br = null;
                int lineNumber = 1;
                try {
                    result_br = new BufferedReader(new FileReader(sourcecodeDir + fileName));

                    String line;
                    while ((line = result_br.readLine()) != null) {

                        String lable = fileName_forHTML + "-" + lineNumber;
                        String old_lable = newFileName + "-" + lineNumber;
                        sb.append("<front id=\"" + lable + "\">");

                            /* for print
                            if(line.trim().startsWith("#if")) {
                                sb.append("#ifdef ??\n");
                            }else{
                                sb.append(line.replace("<", "&lt;").replace(">", "&gt;"));

                            }
*/
                        sb.append(line.replace("<", "&lt;").replace(">", "&gt;"));
                        sb.append("</front>\n");
                        if (!forkAddedNode.contains(old_lable + " ") || line.trim().startsWith("//") || line.trim().startsWith("/*") || line.trim().startsWith("*")) {
                            jsContent.append("$(\"#" + lable + "\").toggle()\n");
                        }

                        lineNumber++;
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        result_br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                sb.append(" </pre>\n");
//                }

            }


        }
        if (currentFile.isDirectory()) {
            String[] subNames = currentFile.list();
            for (String f : subNames) {
                parseSourceCodeFromFile(fileName + "/" + f);
            }

        }
    }

    public void createSourceFileHtml() throws IOException {
        File dir = new File(sourcecodeDir);
        String[] names = dir.list();

        for (String fileName : names) {
            parseSourceCodeFromFile(fileName);
            iofunc.rewriteFile(sb.toString(), analysisDir + sourceCodeTxt);
        }
    }

    public void writeClusterToCSS(ArrayList<String> clusters, int numberOfClusters) {
        BufferedReader br;
        String line;
        nodeMap = new HashMap<>();
        ArrayList<String> distanceArray;


        BackgroundColor bgcolor = new BackgroundColor();

//        HashMap<String, String> clusterColorMap = new HashMap<>();
//        HashMap<String, String> join_clusterColorMap = new HashMap<>();


        try {
            br = new BufferedReader(new FileReader(analysisDir + nodeListTxt));
            while ((line = br.readLine()) != null) {
                // use comma as separator
                System.out.println(line);
                if (line.trim().split("---------").length > 1) {
                    nodeMap.put(Integer.valueOf(line.trim().split("---------")[0]), line.trim().split("---------")[1]);
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //get upstream Node
        String upstreamNode = "";

        File upstreamNodeFile = new File(analysisDir + upstreamNodeTxt);
        if (upstreamNodeFile.exists()) {
            try {
                upstreamNode = iofunc.readResult(analysisDir + upstreamNodeTxt);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //generating joining table, if distance is 2, then join two clusters
        //todo: other conditions?
        String distanceFile = analysisDir + numberOfClusters + "_distanceBetweenCommunityies.txt";
        try {
            String distanceString = processingText.readResult(distanceFile);
            distanceArray = new ArrayList(Arrays.asList(distanceString.split("\n")));
            for (String str : distanceArray) {
                String distanceBetweenTwoClusters = str.split(",")[2];
                String cluster_1 = str.split(",")[0];
                String cluster_2 = str.split(",")[1];

                int length_cluster_1 = 0, length_cluster_2 = 0;
                for (String cl : clusters) {
                    if (cl.startsWith(cluster_1 + ")")) {
                        length_cluster_1 = cl.split(",").length - 1;
                    }
                    if (cl.startsWith(cluster_2 + ")")) {
                        length_cluster_2 = cl.split(",").length - 1;
                    }
                }

                if (length_cluster_1 > 50 && !bigSizeClusterList.contains(cluster_1)) {
                    bigSizeClusterList.add(cluster_1);
                } else if (length_cluster_2 > 50 && !bigSizeClusterList.contains(cluster_2)) {
                    bigSizeClusterList.add(cluster_2);
                }


                /**  condition for joining two clusters is the distance between them is 10--weighted  , 2-- unweighted**/
                if (distanceBetweenTwoClusters.equals("10")) {
                    boolean existEdge = false;
                    ArrayList<Integer> redundantClusterListIndex = new ArrayList<>();
                    if (closeClusterList.size() > 0) {
                        for (HashSet<String> clusterlist : closeClusterList) {
                            if (!existEdge) {
                                if (clusterlist.contains(str.split(",")[0])) {
                                    clusterlist.add(str.split(",")[1]);
                                    existEdge = true;
                                } else if (clusterlist.contains(str.split(",")[1])) {
                                    clusterlist.add(str.split(",")[0]);
                                    existEdge = true;
                                }
                            } else {
                                if (clusterlist.contains(str.split(",")[0]) || clusterlist.contains(str.split(",")[1])) {
                                    redundantClusterListIndex.add(closeClusterList.indexOf(clusterlist));
                                }
                            }
                        }
                    }

                    for (int index : redundantClusterListIndex) {
                        closeClusterList.set(index, new HashSet<>());
                    }

                    if (!existEdge) {
                        HashSet<String> list = new HashSet<>();
                        list.add(cluster_1);
                        list.add(cluster_2);
                        closeClusterList.add(list);
                    }
                }


            }
            for (HashSet<String> arrayList : closeClusterList) {
                if (arrayList.size() > 0) {
                    closeClusters_ColorMap.put(arrayList, "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        //--------------------------expect Node---------------------------
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
//            expectNodeMap.put(nodeCluster[i].split(" ")[0], Integer.valueOf(nodeCluster[i].split(" ")[1]));
        }

        StringBuilder sb = new StringBuilder();
        StringBuilder joining_sb = new StringBuilder();
        StringBuilder colorTable = new StringBuilder();
        StringBuilder joining_colorTable = new StringBuilder();

        String css_head = "td {\n" +
                "    width: 5px;\n" +
                "    border:1px solid black;\n" +
                "    text-align:center;\n" +
                "}\n" +
                "\n" +
                "hr {\n" +
                "     -moz-transform: rotate(45deg);  \n" +
                "       -o-transform: rotate(45deg);  \n" +
                "  -webkit-transform: rotate(45deg);  \n" +
                "      -ms-transform: rotate(45deg);  \n" +
                "          transform: rotate(45deg);  \n" +
                "             width:150%;\n" +
                "    margin-left:-15px;\n" +
                "}\n" +
                "\n" +
                "#distance{\n" +
                "\tposition:fixed;\n" +
                "\ttop:250px;\n" +
                "\tright:50px;\n" +
                "}\n" +
                "#cluster{\n" +
                "\tposition:fixed;\n" +
                "\ttop:50px;\n" +
                "\tright:50px;\n" +
                "}\n";

        sb.append(css_head);
        joining_sb.append(css_head);


        //record cluster id and  colors
        StringBuffer clusterSB = new StringBuffer();
        StringBuffer joining_clusterSB = new StringBuffer();
        for (int i = 0; i < clusters.size(); i++) {

            if (clusters.get(i).length() > 0) {
                String current_color = "";
                String afterJoining_color = null;
//                if (colorList.size() > i - 1) {
//                    current_color = colorList.get(i - 1);
//                } else {
//                    current_color = randomColor();
//                    colorList.add(current_color);
//
//                }


                String cluster = clusters.get(i);
                if (cluster.trim().length() > 0) {
                    String clusterID = cluster.substring(0, clusters.get(i).trim().indexOf(")"));
                    clusterSB.append(clusterID);
                    joining_clusterSB.append(clusterID);
                    String[] elementList = clusters.get(i).trim().split(",");
                    int length = elementList.length;
                    String previous_Color = "";
                    for (int j = 0; j < length; j++) {
                        String nodeIdStr = elementList[j].replace("[", "").replace("]", "").replace(clusterID + ")", "").trim();
                        if (nodeIdStr.length() > 0) {
                            int nodeID = Integer.parseInt(nodeIdStr);
                            String nodeLabel = nodeMap.get(nodeID);
                            //only set the color when parsing the first node of this cluster
                            if (j == 0) {
                                String color = colorMap.get(nodeID);
                                if (color != null) {
                                    current_color = colorMap.get(nodeID);
                                } else {
                                    current_color = randomColor();
                                    colorMap.put(nodeID, current_color);

//                                    for (Map.Entry<Integer, ArrayList<String>> entry : clusterResultMap.entrySet()) {
                                    if (initialNumOfClusters < numberOfClusters) {
                                        ArrayList<String> currentClusters = clusterResultMap.get(numberOfClusters - 1);
                                        for (String clusterStr : currentClusters) {
                                            if (clusterStr.contains(nodeID + " ,")) {
                                                int currentNumberOfNodes = cluster.split(",").length - 1;
                                                int previousNumberOfNodes = clusterStr.split(",").length - 1;

                                                int diff = previousNumberOfNodes - currentNumberOfNodes;
                                                processingText.writeTofile(numberOfClusters + "," + previousNumberOfNodes + " - " + currentNumberOfNodes + " = " + diff + "\n", analysisDir + "LOC_split.txt");

                                            }
                                        }
//                                        }

                                    }


                                }
                                afterJoining_color = current_color;
                                /**  check whether current cluster need to be join with others, and whether the afterJoningColor has been set **/
                                for (Map.Entry<HashSet<String>, String> entry : closeClusters_ColorMap.entrySet()) {
                                    if (entry.getKey().contains(clusterID) && !entry.getValue().equals("") && !bigSizeClusterList.contains(clusterID)) {
                                        afterJoining_color = entry.getValue();
                                        break;
                                    }
                                }
                            }




                        /*for printing purpose*/
                            if (print) {
                                sb.append("#" + nodeLabel + "{\n\tbackground-color:;\n");
                                joining_sb.append("#" + nodeLabel + "{\n\tbackground-color:;\n");
                            } else {
                                String htmlLabel = nodeLabel.replace("~", "-");
                                if (forkAddedNode.contains(nodeLabel)) {

                                    sb.append("#" + htmlLabel + "{\n\tbackground-color:#" + current_color + ";\n");
                                    joining_sb.append("#" + htmlLabel + "{\n\tbackground-color:#" + afterJoining_color + ";\n");
                                } else {
                                    sb.append("#" + htmlLabel + "{\n\tbackground-color:White;\n");
                                    joining_sb.append("#" + htmlLabel + "{\n\tbackground-color:White;\n");
                                }
                            }

                            String leftSidebarColor = "";
                            String rightSidebarColor = "White";
                            //--------------------------expect Node---------------------------
                            if (expectNodeMap.get(nodeLabel) != null) {

//                            ----------------------------------for print------------------
                                if (print) {
                                    leftSidebarColor = "Black";
                                } else {

                                    String expectCommunity = expectNodeMap.get(nodeLabel);
                                    if (expectCommunity.contains("/")) {
                                        String[] each = expectCommunity.split("/");
                                        leftSidebarColor = bgcolor.getExpectColorList().get(Integer.valueOf(each[0]) - 1);
                                        rightSidebarColor = bgcolor.getExpectColorList().get(Integer.valueOf(each[1]) - 1);
                                    } else {
                                        leftSidebarColor = bgcolor.getExpectColorList().get(Integer.valueOf(expectCommunity.trim()) - 1);
                                        rightSidebarColor = "White";

                                    }

                                }

                            }
//                        --------------------------expect Node---------------------------
                            if (!upstreamNode.equals("")) {
                                if (upstreamNode.contains(nodeLabel)) {
                                   /*for print */
                                    if (print) {
                                        leftSidebarColor = "White";
                                    } else {
                                        leftSidebarColor = "Gray";
                                    }

                                }
                            } else if (!forkAddedNode.equals("")) {
                                if (!forkAddedNode.contains(nodeLabel)) {
                                   /*for print */
                                    if (print) {
                                        leftSidebarColor = "White";
                                    } else {
                                        leftSidebarColor = "Gray";
                                    }

                                }
                            }
                            sb.append("\tborder-style: solid;\n\tborder-width: thin thick thin thick;" +
                                    "\n\tborder-color: white " + rightSidebarColor + " white " + leftSidebarColor + ";\n" + "}\n");
                            joining_sb.append("\tborder-style: solid;\n\tborder-width: thin thick thin thick;" +
                                    "\n\tborder-color: white " + rightSidebarColor + " white " + leftSidebarColor + ";\n" + "}\n");


                            //write to file for calculate color table
                            //--------------------------expect Node---------------------------
                            if (expectNode.contains(nodeLabel + " ")) {
                                colorTable.append(nodeLabel + "," + current_color + "," + leftSidebarColor + "\n");
                                joining_colorTable.append(nodeLabel + "," + afterJoining_color + "," + leftSidebarColor + "\n");
                                if (!rightSidebarColor.equals("White")) {
                                    colorTable.append(nodeLabel + "," + current_color + "," + rightSidebarColor + "\n");
                                    joining_colorTable.append(nodeLabel + "," + afterJoining_color + "," + rightSidebarColor + "\n");
                                }

                            }
                            /**--------------------------expect Node---------------------------**/
                            if (!previous_Color.equals(current_color)) {
                                clusterSB.append("," + current_color + "," + leftSidebarColor + "\n");
                                joining_clusterSB.append("," + afterJoining_color + "," + leftSidebarColor + "\n");
                            }
                            previous_Color = current_color;

                        }


                        if (j == 0) {
                            /**      joining color table   **/
//                            clusterColorMap.put(clusterID, current_color);
                            for (HashSet<String> clusterList : closeClusterList) {
                                if (clusterList.contains(clusterID)) {
                                    if (closeClusters_ColorMap.get(clusterList).equals("")) {
                                        closeClusters_ColorMap.put(clusterList, current_color);
//                                       join_clusterColorMap.put(clusterID,current_color);
                                    }
//                                   else {
////                                       join_clusterColorMap.put(clusterID,afterJoining_color);
//                                   }
                                }
                            }

                        }
                    }


                }
            }
        }

        iofunc.rewriteFile(sb.toString(), analysisDir + numberOfClusters + CSS);
        iofunc.rewriteFile(joining_sb.toString(), analysisDir + numberOfClusters + "_join" + CSS);
        iofunc.rewriteFile(colorTable.toString(), analysisDir + numberOfClusters + "_colorTable.txt");
        iofunc.rewriteFile(joining_colorTable.toString(), analysisDir + numberOfClusters + "_colorTable_join.txt");
        iofunc.rewriteFile(clusterSB.toString(), analysisDir + numberOfClusters + "_clusterColor.txt");
        iofunc.rewriteFile(joining_clusterSB.toString(), analysisDir + numberOfClusters + "_clusterColor_join.txt");
    }

    private String randomColor() {
        Random rand = new Random();
        float r = (float) (rand.nextFloat() / 2f + 0.5);
        float g = (float) (rand.nextFloat() / 2f + 0.5);
        float b = (float) (rand.nextFloat() / 2f + 0.5);
        Color randomColor = new Color(r, g, b);
        String color = Integer.toHexString(randomColor.getRGB() & 0x00ffffff);

        if (!colorMap.values().contains(color)) {
            return color;
        } else {
            return randomColor();
        }

    }

    public void combineFiles(int numberOfClusters) {
        String htmlfilePath = "/Users/shuruiz/Work/MarlinRepo/visualizeHtml/";
//        String htmlfilePath = "C:\\Users\\shuruiz\\Documents\\visualizeHtml\\";
        String headtxt = "head.txt";
        String bodyPreTxt = "body_pre.txt";
        String endtxt = "end.txt";
        String togglejsPath = "/toggle.js";
        String html = numberOfClusters + ".html";
        try {
            //write code.html
            iofunc.rewriteFile(iofunc.readResult(htmlfilePath + headtxt).replace("style.css", numberOfClusters + ".css"), analysisDir + html);
            iofunc.writeTofile(iofunc.readResult(analysisDir + numberOfClusters + ".color"), analysisDir + html);

            iofunc.writeTofile(iofunc.readResult(htmlfilePath + bodyPreTxt), analysisDir + html);
            iofunc.writeTofile(iofunc.readResult(analysisDir + sourceCodeTxt), analysisDir + html);
            iofunc.writeTofile(iofunc.readResult(htmlfilePath + endtxt), analysisDir + html);

            //toggle js
            iofunc.rewriteFile(iofunc.readResult(htmlfilePath + jsFileHeader), analysisDir + togglejsPath);
            iofunc.writeTofile(jsContent.toString() + "\n});", analysisDir + togglejsPath);


            //write color table
            iofunc.rewriteFile(iofunc.readResult(analysisDir + numberOfClusters + ".distanceTable"), analysisDir + numberOfClusters + "_joiningTable.html");
            iofunc.writeTofile("<h3> ----------------Before Joining-----------------", analysisDir + numberOfClusters + "_joiningTable.html");
            iofunc.writeTofile(iofunc.readResult(analysisDir + numberOfClusters + ".color"), analysisDir + numberOfClusters + "_joiningTable.html");
            StringBuffer sb = new StringBuffer();
            sb.append("<h3> ---------clusters could be joined: <br>");
            for (HashSet<String> list : closeClusterList) {
                if (list.size() > 0) {
                    sb.append("[");
                    for (String s : list) {
                        sb.append(s + " , ");
                    }
                    sb.append("] <br>");
                }
            }
            sb.append("<h3> ---------big size cluster (#node>50) will not be joined : [ ");
            for (String bsc : bigSizeClusterList) {
                sb.append(bsc + " , ");
            }
            sb.append("]");
            iofunc.writeTofile(sb.toString(), analysisDir + numberOfClusters + "_joiningTable.html");
            iofunc.writeTofile("<h3> ----------------After Joining-----------------", analysisDir + numberOfClusters + "_joiningTable.html");
            iofunc.writeTofile(iofunc.readResult(analysisDir + numberOfClusters + "_join.color"), analysisDir + numberOfClusters + "_joiningTable.html");

        } catch (IOException e) {
            e.printStackTrace();
        }
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

        final String FS = File.separator;
        this.sourcecodeDir = sourcecodeDir;
        this.analysisDir = testCaseDir + testDir + FS;
        this.testCaseDir = testCaseDir;


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
            createSourceFileHtml();
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
                    writeClusterToCSS(clusters, numberOfCommunities);


                    calculatingAccuracy(clusters);


                    AnalyzingCommunityDetectionResult analyzeCDResult = new AnalyzingCommunityDetectionResult();

                    analyzeCDResult.generatingClusteringTable(testCaseDir, testDir, numberOfCommunities, false);
                    analyzeCDResult.generatingClusteringTable(testCaseDir, testDir, numberOfCommunities, true);

                    combineFiles(numberOfCommunities);
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

        generateGroundTruthMap();


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
            }else{
                nodeSet = new HashSet<>();
            }
            nodeSet.add(nodeId);
            groundTruthClusters.put(clusterNumber, nodeSet);
        }

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

    public static void main(String[] args) {

//        visualizeGraph(false, testDir);
    }

}
