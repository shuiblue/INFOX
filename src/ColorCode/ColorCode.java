package ColorCode;

import CommunityDetection.AnalyzingCommunityDetectionResult;
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
    static String sourceCodeTxt = "sourceCode.txt";

    static String expectTxt = "expectCluster.txt";
    static String upstreamNodeTxt = "upstreamNode.txt";
    static String jsFileHeader = "/jshead.txt";

    static HashMap<Integer, String> nodeMap;

    HashMap<Integer, String> colorMap = new HashMap<>();
    HashSet<String> bigSizeClusterList = new HashSet<>();
    static StringBuffer jsContent = new StringBuffer();
    static String forkAddedNode = "";
    static final String FS = File.separator;
    String analysisDir, testCaseDir, sourcecodeDir;
    boolean print = false;
    StringBuilder sb = new StringBuilder();
    ProcessingText processingText = new ProcessingText();
    HashMap<Integer, ArrayList<String>> clusterResultMap = new HashMap<>();
    ArrayList<HashSet<String>> closeClusterList = new ArrayList<>();
    HashMap<HashSet<String>, String> closeClusters_ColorMap = new HashMap<>();
    HashMap<String, String> expectNodeMap = new HashMap<>();
    static int initialNumOfClusters = 0;

    static boolean isMS_CLUSTERCHANGES = false;

    public ColorCode(String sourcecodeDir, String testCaseDir, String testDir, String forkAddedNode, boolean isMS_CLUSTERCHANGES) {
        this.sourcecodeDir = sourcecodeDir;
        this.analysisDir = testCaseDir + testDir + FS;
        this.testCaseDir = testCaseDir;
        this.forkAddedNode = forkAddedNode;
        this.isMS_CLUSTERCHANGES = isMS_CLUSTERCHANGES;
    }

    public void parseSourceCodeFromFile(String fileName) {
        File currentFile = new File(sourcecodeDir + fileName);
        //            if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".c")) {
        if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".c") || fileName.endsWith(".pde")) {
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
                        if (old_lable.contains("utilC-17")) {
                            System.out.print("");
                        }
                        if (!forkAddedNode.contains(old_lable) || line.trim().startsWith("//") || line.trim().startsWith("/*") || line.trim().startsWith("*")) {
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

    /**
     * This function generates css file based on the clustering result
     *
     * @param clusters
     * @param numberOfClusters
     * @param clusterResultMap
     * @param nodeMap
     * @param expectNodeMap
     * @param clusterSizeThreshold
     * @param joined_clusters
     * @param hasGroundTruth
     */
    public void writeClusterToCSS(ArrayList<String> clusters, int numberOfClusters, HashMap<Integer, ArrayList<String>> clusterResultMap, HashMap<Integer, String> nodeMap, HashMap<String, String> expectNodeMap, int clusterSizeThreshold, HashMap<Integer, HashSet<Integer>> joined_clusters, boolean hasGroundTruth) {
//    public void writeClusterToCSS(ArrayList<String> clusters, int numberOfClusters, HashMap<Integer, ArrayList<String>> clusterResultMap, HashMap<Integer, String> nodeMap, HashMap<String, String> expectNodeMap, int clusterSizeThreshold) {

        BackgroundColor bgcolor = new BackgroundColor();

        HashMap<String, String> clusterColorMap = new HashMap<>();
        HashMap<String, String> join_clusterColorMap = new HashMap<>();


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

        String expectNode = "";
        if (hasGroundTruth) {
            try {
                expectNode = iofunc.readResult(testCaseDir + expectTxt);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                                    if (clusterResultMap.size() > 1) {

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
                                    } else {
                                        initialNumOfClusters = numberOfClusters;
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
                            if (hasGroundTruth) {
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
//                                   /*for print
                                        if (print) {
                                            leftSidebarColor = "White";
                                        } else {
                                            leftSidebarColor = "Gray";
                                        }

                                    }
                                } else if (!forkAddedNode.equals("")) {
                                    if (!forkAddedNode.contains(nodeLabel)) {
//                                   /*for print
                                        if (print) {
                                            leftSidebarColor = "White";
                                        } else {
                                            leftSidebarColor = "Gray";
                                        }

                                    }
                                }
                            }
                            sb.append("\tborder-style: solid;\n\tborder-width: thin thick thin thick;" +
                                    "\n\tborder-color: white " + rightSidebarColor + " white " + leftSidebarColor + ";\n" + "}\n");
                            joining_sb.append("\tborder-style: solid;\n\tborder-width: thin thick thin thick;" +
                                    "\n\tborder-color: white " + rightSidebarColor + " white " + leftSidebarColor + ";\n" + "}\n");

                            //write to file for calculate color table
                            //--------------------------expect Node---------------------------
                            if (forkAddedNode.contains(nodeLabel)) {
                                colorTable.append(nodeLabel + "," + current_color + "," + leftSidebarColor + "\n");
                                joining_colorTable.append(nodeLabel + "," + afterJoining_color + "," + leftSidebarColor + "\n");
                                if (!rightSidebarColor.equals("White")) {
                                    colorTable.append(nodeLabel + "," + current_color + "," + rightSidebarColor + "\n");
                                    joining_colorTable.append(nodeLabel + "," + afterJoining_color + "," + rightSidebarColor + "\n");
                                }

                            }
//                            /**--------------------------expect Node---------------------------*
                            if (!previous_Color.equals(current_color)) {
                                clusterSB.append("," + current_color + "," + leftSidebarColor + "\n");
                                joining_clusterSB.append("," + afterJoining_color + "," + leftSidebarColor + "\n");
                            }
                            previous_Color = current_color;

                        }

                        if (j == 0) {
//                            /**      joining color table
                            clusterColorMap.put(clusterID, current_color);
                            for (HashSet<String> clusterList : closeClusterList) {
                                if (clusterList.contains(clusterID)) {
                                    if (closeClusters_ColorMap.get(clusterList).equals("")) {
                                        closeClusters_ColorMap.put(clusterList, current_color);
                                        join_clusterColorMap.put(clusterID, current_color);
                                    } else {
                                        join_clusterColorMap.put(clusterID, afterJoining_color);
                                    }
                                }
                            }

                        }

                    }


                }
            }
        }

        iofunc.rewriteFile(sb.toString(), analysisDir + numberOfClusters + CSS);
        iofunc.rewriteFile(colorTable.toString(), analysisDir + numberOfClusters + "_colorTable.txt");
        iofunc.rewriteFile(clusterSB.toString(), analysisDir + numberOfClusters + "_clusterColor.txt");

        iofunc.rewriteFile(joining_sb.toString(), analysisDir + numberOfClusters + "_join_bigSize-" + clusterSizeThreshold + CSS);
        iofunc.rewriteFile(joining_colorTable.toString(), analysisDir + numberOfClusters + "_colorTable_join_bigSize-" + clusterSizeThreshold + ".txt");
        iofunc.rewriteFile(joining_clusterSB.toString(), analysisDir + numberOfClusters + "_clusterColor_join_bigSize-" + clusterSizeThreshold + ".txt");

    }


    /**
     * This function generates joining table, if distance is 2, then join two clusters
     * //todo: other conditions?
     *
     * @param clusters
     * @param numberOfClusters
     */
    public ArrayList<HashSet<String>> joiningCloseClusters(ArrayList<String> clusters, int numberOfClusters, int clusterSizeThreshold) {
        bigSizeClusterList = new HashSet<>();
        ArrayList<String> distanceArray;
        String distanceFile = analysisDir + numberOfClusters + "_distanceBetweenCommunityies.txt";
        closeClusterList = new ArrayList<>();
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

                if (length_cluster_1 > clusterSizeThreshold && !bigSizeClusterList.contains(cluster_1)) {
                    bigSizeClusterList.add(cluster_1);
                } else if (length_cluster_2 > clusterSizeThreshold && !bigSizeClusterList.contains(cluster_2)) {
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
            closeClusters_ColorMap = new HashMap<>();
            for (HashSet<String> arrayList : closeClusterList) {
                if (arrayList.size() > 0) {
                    closeClusters_ColorMap.put(arrayList, "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return closeClusterList;
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

    public void combineFiles(int numberOfClusters, int clusterSizeThreshold) {
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

            if (!isMS_CLUSTERCHANGES) {
                //write color table
                iofunc.rewriteFile(iofunc.readResult(analysisDir + numberOfClusters + ".distanceTable"), analysisDir + numberOfClusters + "_joiningTable_bigSize-" + clusterSizeThreshold + ".html");
                iofunc.writeTofile("<h3> ----------------Before Joining-----------------", analysisDir + numberOfClusters + "_joiningTable_bigSize-" + clusterSizeThreshold + ".html");
                iofunc.writeTofile(iofunc.readResult(analysisDir + numberOfClusters + ".color"), analysisDir + numberOfClusters + "_joiningTable_bigSize-" + clusterSizeThreshold + ".html");
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
                iofunc.writeTofile(sb.toString(), analysisDir + numberOfClusters + "_joiningTable_bigSize-" + clusterSizeThreshold + ".html");
                iofunc.writeTofile("<h3> ----------------After Joining-----------------", analysisDir + numberOfClusters + "_joiningTable_bigSize-" + clusterSizeThreshold + ".html");
                iofunc.writeTofile(iofunc.readResult(analysisDir + numberOfClusters + "_join_bigSize-" + clusterSizeThreshold + ".color"), analysisDir + numberOfClusters + "_joiningTable_bigSize-" + clusterSizeThreshold + ".html");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

//        visualizeGraph(false, testDir);
    }

}
