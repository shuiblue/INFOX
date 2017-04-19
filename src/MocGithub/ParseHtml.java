package MocGithub;


import ColorCode.BackgroundColor;
import CommunityDetection.AnalyzingCommunityDetectionResult;
import DependencyGraph.DependencyGraph;
import Util.GenerateCombination;
import Util.JsonUtility;
import Util.ProcessingText;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by shuruiz on 4/5/17.
 */
public class ParseHtml {
    static final String FS = File.separator;
    static String github_api = "https://api.github.com/repos/";
    static String github_page = "https://github.com/";
    Document doc, currentDoc;
    static String analysisDir = "";
    String originalPage = "original.html";
    int max_numberOfCut;
    int numberOfBiggestClusters;

    HashMap<String, String> nodeId_to_clusterID_Map;
    ArrayList<String> combination_list;
    HashMap<String, String> label_to_id;
    ArrayList<String> forkAddedNodeList;
    HashMap<Integer, HashMap<String, HashSet<Integer>>> clusterResultMap;
    static ArrayList<String> all_splitStep_list = new ArrayList<>();
    HashMap<String, Integer> usedColorIndex = new HashMap<>();
    HashMap<String, String> cluster_color = new HashMap<>();
    static HashMap<Integer, HashMap<Integer, HashMap<String, HashSet<Integer>>>> allSplittingResult;



    private String table_header = "<style>\n" +
            "#cluster{\n" +
            "  background-color: #fff;\n" +
            "  border: 1px solid #000;\n" +
            "  position:fixed;\n" +
            "  right: 0;\n" +
            "  left: 0;\n" +
            "  width: 980px;\n" +
            "  margin-right: auto;\n" +
            "  margin-left: auto;\n" +
            "  top:0px;\n" +
            "  z-index:99999;\n" +
            "  td, th {\n" +
            "  background-color: #fff;\n" +
            "  color: #000;\n" +
            "}\n" +
            "\n" +
            "}\n" +
            ".test_me:hover {\n" +
            "  max-width : initial;\n" +
            "  overflow: show;\n" +
            "}\n" +
            "</style>" +
            "<table id=\"cluster\"  border=1 frame=void rules=rows>\n" +
            "  <tr> \n" +
            "    <td> <button id=\"btn_hide_non_cluster_rows\" onclick=\"hide_non_cluster_rows()\">Hide non cluster code</button>\n" +
            "    </td> \n" +
            "  </tr>\n" +
            "  <tr>\n" +
            "       <th>Cluster</th>\n" +
            "       <th>Navigation</th>\n" +
            "       <th>Keywords</th>\n" +
            "       <th>LOC</th>\n" +
            "       <th>Split Cluster </th>\n" +
            "    </tr>\n";


    public ParseHtml(int max_numberOfCut, int numberOfBiggestClusters, String analysisDir) {
        this.max_numberOfCut = max_numberOfCut;
        this.numberOfBiggestClusters = numberOfBiggestClusters;
        this.analysisDir = analysisDir;


    }

    public void getOriginalDiffPage(String diffPageUrl, String localSourceCodeDirPath) {
        this.analysisDir = localSourceCodeDirPath + "INFOX_output/";
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        // turn off htmlunit warnings

        webClient.getOptions().setUseInsecureSSL(true); //ignore ssl certificate
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.setCssErrorHandler(new SilentCssErrorHandler());

        HtmlPage page = null;
        try {
            page = webClient.getPage(diffPageUrl + "#files_bucket");
        } catch (Exception e) {
            System.out.println("Get page error");
        }
        webClient.waitForBackgroundJavaScriptStartingBefore(200);
        webClient.waitForBackgroundJavaScript(50000);


        new ProcessingText().rewriteFile(page.asXml(), analysisDir + originalPage);
    }

    public void generateMocGithubForkPage(String diffPageUrl, String forkName, String localSourceCodeDirPath) {
        combination_list = generateAllCombineResult(analysisDir, max_numberOfCut);
        this.analysisDir = localSourceCodeDirPath + "INFOX_output/";
        ProcessingText pt = new ProcessingText();

        try {
            String page = pt.readResult(analysisDir + originalPage);
            doc = Jsoup.parse(page);
            //modify title-- code changes of fork : ...
            Element fork_title = doc.getElementsByClass("gh-header-title").first();
            fork_title.text(fork_title.text().replaceAll("Comparing changes", "Code Changes of fork: " + forkName));

            doc.getElementsByClass("range-editor text-gray js-range-editor is-cross-repo").remove();
            doc.getElementsByClass("gh-header-meta").first().text("Comparing..<h1>parent of the first commit</h1> to  <h1>the latest commit</h1>");


            //remove background color for line-number columns
            String css = "<style type=\"text/css\">\n" +
                    ".blob-num-deletion {\n" +
                    "  background-color:#fff;\n" +
                    "  border-color:#f1c0c0\n" +
                    "}\n" +
                    ".blob-num-addition {\n" +
                    "  background-color:#fff;\n" +
                    "  border-color:#f1c0c0\n" +
                    "}" +
                    "</style>\n";
            doc.getElementsByTag("header").append(css);

            String jqueryLink = "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/jquery/3.2.1/jquery.min.js\"></script>";
            doc.getElementsByTag("header").append(jqueryLink);

            String workingDir = System.getProperty("user.dir");
            String js = pt.readResult(workingDir+"/src/files/jsFile.txt");
            doc.getElementsByTag("html").last().after(js);
            Elements fileList_elements = doc.getElementsByClass("file-header");


            ArrayList<String> topClusterList = new ArrayList<>();
            String[] topClusters = null;
            try {
                topClusters = new ProcessingText().readResult(analysisDir + "topClusters.txt").split("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (String tc : topClusters) {
                topClusterList.add(tc);
                usedColorIndex.put(tc, 0);
            }


            allSplittingResult = new AnalyzingCommunityDetectionResult(analysisDir).getAllSplittingResult(max_numberOfCut, topClusterList, combination_list);

            ArrayList<String> splitStepList = generateAllCombineResult(analysisDir, max_numberOfCut);


            for (String splitStep : splitStepList) {

                HashMap<String, String> nodeId_to_clusterID = genrate_NodeId_to_clusterIDList_Map(splitStep);
                HashMap<String, List<String>> cluster_keyword = new HashMap<>();
                String keyword[] = pt.readResult(analysisDir + splitStep + "_keyword.txt").split("\n");
                for (String kw : keyword) {
                    kw = kw.replace("[", "").replace("]", "");
                    String cid = kw.split(":")[0].trim();

                    List<String> list = Arrays.asList(kw.split(":")[1].split(","));
                    cluster_keyword.put(cid, list);
                }


                List<String> stopSplitClusters = Arrays.asList(new ProcessingText().readResult(analysisDir + "noSplittingStepList.txt").split("\n"));


                HashMap<String, String> cluster_color = generateClusterSummaryTable(splitStep, cluster_keyword, stopSplitClusters);


                generateHtml(fileList_elements, nodeId_to_clusterID, splitStep, cluster_color, cluster_keyword);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param analysisDir
     * @param max_numberOfCut
     */
    public ArrayList<String> generateAllCombineResult(String analysisDir, int max_numberOfCut) {
        ProcessingText pt = new ProcessingText();
        ArrayList<String> topClusters = null;
        try {
            topClusters = new ArrayList<>(Arrays.asList(pt.readResult(analysisDir + "topClusters.txt").split("\n")));

        } catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<String, HashMap<Integer, ArrayList<String>>> allSplitSteps_map = new GenerateCombination(analysisDir).getAllSplitSteps(max_numberOfCut, (ArrayList<String>) topClusters);
        return generateCombineResult_4CurrentClusterList(allSplitSteps_map, topClusters, max_numberOfCut);

    }


    private ArrayList<String> generateCombineResult_4CurrentClusterList(HashMap<String, HashMap<Integer, ArrayList<String>>> allSplitSteps_map, ArrayList<String> topClusters, int max_numberOfCut) {
//        HashMap<String, ArrayList<String>> cluster_possibleSplitStep = getPossileSplitStepForeachCluster(allSplitSteps_map, topClusters, max_numberOfCut);
//
        List<List<String>> totalList = getAllCombination();
        return new GenerateCombination(analysisDir).printAllCases(totalList);

    }

    private static HashMap<String, ArrayList<String>> getPossileSplitStepForeachCluster(HashMap<String, HashMap<Integer, ArrayList<String>>> allSplitSteps_map, ArrayList<String> topClusters, int max_numberOfCut) {
        HashMap<String, ArrayList<String>> cluster_possibleSplitStep = new HashMap<>();

        for (String originalCluster : topClusters) {
            ArrayList<String> list = new ArrayList<>();
            list.add(originalCluster);
            cluster_possibleSplitStep.put(originalCluster, list);

            ArrayList<String> current_list = cluster_possibleSplitStep.get(originalCluster);

            String previousStep = originalCluster;
            for (int split = 1; split <= max_numberOfCut; split++) {
                ArrayList<String> nextSplit = allSplitSteps_map.get(originalCluster).get(split);
                String thisSplit = "";

                for (String parentCluster : allSplitSteps_map.get(originalCluster).get(split - 1)) {
                    if (nextSplit.size() > 0) {
                        String child1 = parentCluster + "_1";
                        String child2 = parentCluster + "_2";
                        if (nextSplit.contains(child1) && nextSplit.contains(child2)) {
                            thisSplit += child1 + "~" + child2;
                        } else {
                            thisSplit += "~" + parentCluster;
                        }
                    }
                }


                if (thisSplit.length() > 0) {
                    current_list.add(thisSplit);
                    previousStep = thisSplit;
                }
            }
            cluster_possibleSplitStep.put(originalCluster, current_list);
        }

        return cluster_possibleSplitStep;
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
        HashMap<String, HashMap<Integer, ArrayList<String>>> allSplitSteps_map = new GenerateCombination(analysisDir).getAllSplitSteps(max_numberOfCut, (ArrayList<String>) topClusters);

        for (String clusterID : topClusters) {
            HashSet<String> thisCombinelist = new HashSet<>();
            thisCombinelist.add(clusterID);
            String nextStr = "";
            String[] children = new String[]{clusterID};

            int stopCluster = 1;
            while (children.length > 0 && (stopCluster <= children.length || stopCluster == 1)) {

                for (String child : children) {

                    if (child.split("_").length > max_numberOfCut) {
                        nextStr += "~" + child;
                        stopCluster++;
                    } else {
                        if (!stopSplitClusters.contains(child)) {
                            nextStr += "~" + getChildren(child);

                        } else {
                            nextStr += "~" + child;
                            stopCluster++;
                        }


                    }

                }
                String next = nextStr.substring(1, nextStr.length());
                thisCombinelist.add(next);

                children = next.split("~");
                nextStr = "";


            }
            all_cluster_combineList.add(new ArrayList<String>(thisCombinelist));

        }
        return all_cluster_combineList;
    }

    private String getChildren(String cluster) {
        String nextSplit = "";
        String child1 = cluster + "_1";
        String child2 = cluster + "_2";

        return child1 + "~" + child2;
    }

    private HashMap<String, String> generateClusterSummaryTable(String splitStep, HashMap<String, List<String>> cluster_keyword, List<String> stopSplitClusters) throws IOException {


        ProcessingText pt = new ProcessingText();


        StringBuilder sb = new StringBuilder();
        sb.append(table_header);


        String[] clusters = splitStep.split("--");

        for (int i = 0; i < clusters.length; i++) {
            String cid = clusters[i];
            /**   generate next step link **/
            String nextStep, nextStepStr;

            for (String clusterID : cid.split("~")) {

                if (!stopSplitClusters.contains(clusterID) && clusterID.split("_").length < max_numberOfCut + 1) {
                    nextStep = splitStep.replace(clusterID, clusterID + "_1~" + clusterID + "_2");
                    nextStepStr = "       <td width=\"100\"><a href=\"./" + nextStep + ".html\" class=\"button\">split</a></td>\n";
                } else {
                    nextStepStr = "       <td width=\"100\">no more splitting</td>\n";
                }
                generate_one_row_of_currentCluster(cluster_keyword, sb, i, nextStepStr, clusterID);
            }

        }

        sb.append("</table>");

        pt.rewriteFile(sb.toString(), analysisDir + splitStep + ".color");
        return cluster_color;

    }

    private String generate_one_row_of_currentCluster(HashMap<String, List<String>> cluster_keyword, StringBuilder sb, int i, String nextStepStr, String clusterID) {
        BackgroundColor cc = new BackgroundColor();
        ArrayList<ArrayList<String>> colorfamiliy_List = cc.getColorFamilyList();

        String originalClusterID = clusterID.split("_")[0];
        /**   color  **/
        String color = "";
        if (clusterID.equals(originalClusterID)) {
            usedColorIndex.put(clusterID, 0);
            color = colorfamiliy_List.get(i).get(0);
        } else {
            if (cluster_color.get(clusterID) != null) {
                color = cluster_color.get(clusterID);
            } else {
                color = colorfamiliy_List.get(i).get(usedColorIndex.get(originalClusterID) + 1);
                cluster_color.put(clusterID, color);
                usedColorIndex.put(originalClusterID, usedColorIndex.get(originalClusterID) + 1);
            }
        }
        cluster_color.put(clusterID, color);

        /**  keyword **/
        String keyword_long = cluster_keyword.get(clusterID).toString();
        String keyword_prefix = keyword_long.trim().substring(0, 6).replace("[", "") + ".";


        int clusterSize = allSplittingResult.get(Integer.valueOf(originalClusterID)).get(clusterID.split("_").length - 1).get(clusterID).size();

        sb.append(generateRow(color,clusterID,keyword_prefix,keyword_long,clusterSize,nextStepStr));
        return sb.toString();
    }

    private String generateRow(String color, String current_clusterID, String keyword_suffix, String keyword_long, int clusterSize, String nextStepStr) {
       return "<tr> \n" +
                "       <td width=\"130\" style=\"cursor: pointer; background:" + color + "\" onclick='hide_cluster_rows(\"infox_" + current_clusterID + "\")'>" + keyword_suffix + "</td>\n" +
                "        <td width=\"600\">" + keyword_long + "</td>\n" +
                "       <td width=\"50\">" + clusterSize + "</td>\n" +
                nextStepStr +
                "   </tr>";
    }


    private HashMap<String, String> generateClusterSummaryTable_old(String splitStep, HashMap<String, List<String>> cluster_keyword, List<String> stopSplitClusters) throws IOException {
        HashMap<String, String> cluster_color = new HashMap<>();

        ProcessingText pt = new ProcessingText();
        BackgroundColor cc = new BackgroundColor();

        ArrayList<ArrayList<String>> colorfamiliy_List = cc.getColorFamilyList();


        StringBuilder sb = new StringBuilder();
        sb.append(table_header);


        try {
            String[] topClusterID = pt.readResult(analysisDir + "topClusters.txt").split("\n");

            for (int i = 0; i < topClusterID.length; i++) {
                String clusterID = topClusterID[i];
                String cluster[] = clusterID.split("_");
                int clusterIndex = cluster.length == 1 ? 1 : Integer.parseInt(cluster[1]);


                /**   generate next step link **/
                String[] splitArray = splitStep.split("");
                String[] nextSplit = Arrays.copyOf(splitArray, splitArray.length);
                nextSplit[i] = String.valueOf(Integer.valueOf(splitArray[clusterIndex]) + 1);
                String nextStep = "";
                String nextStepStr = "";
                if (Integer.valueOf(nextSplit[i]) <= max_numberOfCut) {
                    for (String s : nextSplit) {
                        nextStep += s;
                    }
                    nextStepStr = "       <td width=\"100\"><a href=\"./" + nextStep + ".html\" class=\"button\">split</a></td>\n";
                } else {
                    nextStepStr = "       <td width=\"100\"></td>\n";
                }


                int current_split = Integer.valueOf(splitArray[i]);
                if (current_split > 1) {
                    for (int s = 1; s <= current_split; s++) {
                        String current_clusterID = clusterID + "_" + s;
                        String keyword_long = cluster_keyword.get(current_clusterID).toString();
                        String keyword_suffix = keyword_long.trim().substring(0, 6).replace("[", "") + ".";
                        String color = colorfamiliy_List.get(i).get(s - 1);

                        final String[] clusterSize = {""};
                        clusterResultMap.forEach((k, v) -> {
                            HashMap<String, HashSet<Integer>> currentClusterMap = v;
                            clusterSize[0] = String.valueOf(currentClusterMap.get(current_clusterID).size());
                        });

                        cluster_color.put(current_clusterID, color);
                        sb.append(generateRow(color,current_clusterID,keyword_suffix,keyword_long,Integer.parseInt(clusterSize[0]),nextStepStr));

                    }
                } else {
                    String color = colorfamiliy_List.get(i).get(0);
                    String keyword_long = cluster_keyword.get(clusterID).toString();
                    String keyword_suffix = keyword_long.trim().substring(0, 6).replace("[", "") + ".";

                    final String[] clusterSize = {""};
                    clusterResultMap.forEach((k, v) -> {
                        HashMap<String, HashSet<Integer>> currentClusterMap = v;
                        clusterSize[0] = String.valueOf(currentClusterMap.get(clusterID).size());
                    });

                    sb.append(generateRow(color,clusterID,keyword_suffix,keyword_long,Integer.parseInt(clusterSize[0]),nextStepStr));
                    cluster_color.put(clusterID, color);
                }


            }


        } catch (IOException e) {
            e.printStackTrace();
        }


        sb.append("</table>");

        pt.rewriteFile(sb.toString(), analysisDir + splitStep + ".color");
        return cluster_color;

    }


    private HashMap<String, String> genrate_NodeId_to_clusterIDList_Map(String splitStep) {

        nodeId_to_clusterID_Map = new HashMap<>();
        ProcessingText pt = new ProcessingText();
        AnalyzingCommunityDetectionResult acdr = new AnalyzingCommunityDetectionResult(analysisDir);

        label_to_id = pt.getNodeLabel_to_id_map(analysisDir + "nodeLable2IdMap.txt");

        DependencyGraph dp = new DependencyGraph();
        try {
            forkAddedNodeList = dp.getForkAddedNodeList(analysisDir + "forkAddedNode.txt");
            for (String nodeLabel : forkAddedNodeList) {
                String nodeID = label_to_id.get("\"" + nodeLabel + "\"");
                if (nodeID != null) {
                    nodeId_to_clusterID_Map.put(nodeID, "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean isOriginalGraph = true;

        clusterResultMap = acdr.getClusteringResultMapforClusterID(splitStep, isOriginalGraph);

        clusterResultMap.forEach((k, v) -> {
            HashMap<String, HashSet<Integer>> currentClusterMap = v;
            currentClusterMap.forEach((clusterID, nodeSet) -> {
                nodeSet.forEach(nodeId -> {
                            nodeId_to_clusterID_Map.put(nodeId + "", clusterID);
                        }
                );

            });
        });
        return nodeId_to_clusterID_Map;
    }


    /**
     * This function generate html file based on clustering result, basically,
     * 1. modify the background color of changed code for different split step
     * 2. generate color table
     * 3. table should have  + to further split
     * <p>
     * input: combination set, changed code list, line number,
     *
     * @param fileList_elements
     */
    private void generateHtml(Elements fileList_elements, HashMap<String, String> nodeId_to_clusterID, String splitStep, HashMap<String, String> cluster_color, HashMap<String, List<String>> cluster_keyword) {


        currentDoc = doc.clone();
        ProcessingText pt = new ProcessingText();

        String summaryTable;
        try {
            summaryTable = pt.readResult(analysisDir + splitStep + ".color");
            currentDoc.getElementsByTag("html").first().children().first().before(summaryTable);
        } catch (IOException e) {
            e.printStackTrace();
        }


        for (String changedCodeLabel : forkAddedNodeList) {
            if (label_to_id.get("\"" + changedCodeLabel + "\"") != null) {
                String nodeId = label_to_id.get("\"" + changedCodeLabel + "\"");
                String clusterid = nodeId_to_clusterID.get(nodeId);


                String nodeLable[] = changedCodeLabel.split("-");
                String filename_tag = nodeLable[0];
                String fileName = pt.getOriginFileName(filename_tag);
                String lineNumber = nodeLable[1];

                String styleStr = "background-color:"
                        + cluster_color.get(clusterid)
                        + ";font-family:SFMono-Regular, Consolas, Liberation Mono, Menlo, Courier, monospace;\n" +
                        "  font-size:12px;\n" +
                        "  line-height:20px;\n" +
                        "  text-align:right;\"";

                boolean isTopCluster = cluster_keyword.get(clusterid) != null ? true : false;
                if (isTopCluster) {
                    String keywod_prefix = cluster_keyword.get(clusterid).get(0).trim().substring(0, 6).replace("[", "") + ".";

                    currentDoc.getElementsByAttributeValue("data-path", fileName);
                    Element currentFile = currentDoc.getElementsByAttributeValue("data-path", fileName).next().first();
                    if (!currentFile.toString().contains("Load diff")) {
                        Elements lineElements = currentFile.getElementsByAttributeValue("data-line-number", lineNumber);
                        Element lineElement;
                        if (lineElements.size() > 1) {
                            if (lineElements.get(0).toString().contains("addition")) {
                                lineElement = lineElements.get(0);
                            } else {
                                lineElement = lineElements.get(1);
                            }
                        } else {
                            lineElement = lineElements.first();
                        }
//todo keyword
                        lineElement.attr("class", "infox_" + clusterid.trim()).attr("style", styleStr).text(keywod_prefix);
                    } else {
                        System.out.println(fileName + " is bigger than the github diff limits..");


                    }
                }
            }
        }

        pt.rewriteFile(currentDoc.toString(), analysisDir + splitStep + ".html");


    }

    /**
     * This function get diff url of github fork
     * @param forkName
     * @param timeWindowSize "begining" -- , "x months"
     * @return
     */
    public String getDiffPageUrl(String forkName, String timeWindowSize) {

        HashMap<String,String> one_month_ago_map=new HashMap<>();
        one_month_ago_map.put("01","12");
        one_month_ago_map.put("02","01");
        one_month_ago_map.put("03","02");
        one_month_ago_map.put("04","03");
        one_month_ago_map.put("05","04");
        one_month_ago_map.put("06","05");
        one_month_ago_map.put("07","06");
        one_month_ago_map.put("08","07");
        one_month_ago_map.put("09","08");
        one_month_ago_map.put("10","09");
        one_month_ago_map.put("11","10");
        one_month_ago_map.put("12","11");


        String forkUrl = github_api + forkName;
        JsonUtility jsonUtility = new JsonUtility();
        try {
            // get latest commit sha
            String[] todayTimeStamp = new Timestamp(System.currentTimeMillis()).toString().split(" ");
            String format_current_time = todayTimeStamp[0] + "T" + todayTimeStamp[1].split("\\.")[0] + "Z";
            JSONObject fork_commit_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + forkName + "/commits?until=" + format_current_time));
            String latestCommitSHA = fork_commit_jsonObj.getString("sha");

            //get general fork information
            JSONObject fork_jsonObj = new JSONObject(jsonUtility.readUrl(forkUrl));


            //get commit before fork was created
            String comparedFork = "";
            String previousCommitSHA = "";

           if (timeWindowSize.equals("beginning")) {
                String firstCommitTimeStamp = fork_jsonObj.getString("created_at");
                JSONObject upstreamInfo = (JSONObject) fork_jsonObj.get("parent");
                comparedFork = upstreamInfo.getString("full_name");
                JSONObject upstream_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + comparedFork + "/commits?until=" + firstCommitTimeStamp));
                previousCommitSHA = upstream_jsonObj.getString("sha");



           }else if(timeWindowSize.equals("1 month")){
                comparedFork = forkName;
               String month= one_month_ago_map.get(todayTimeStamp[0].split("-")[1]);
                String previousTimeStamp =todayTimeStamp[0].split("-")[0]+"-"+month+"-"+todayTimeStamp[0].split("-")[2] + "T" + todayTimeStamp[1].split("\\.")[0] + "Z" ;
               // get latest commit sha
               fork_commit_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + comparedFork + "/commits?until=" + previousTimeStamp));
               previousCommitSHA = fork_commit_jsonObj.getString("sha");


            }else if(timeWindowSize.equals("1 year")){
               comparedFork = forkName;
               String month= one_month_ago_map.get(todayTimeStamp[0].split("-")[1]);
               String previousTimeStamp =(Integer.valueOf(todayTimeStamp[0].split("-")[0])-1)+"-"+todayTimeStamp[0].split("-")[1]+"-"+todayTimeStamp[0].split("-")[2] + "T" + todayTimeStamp[1].split("\\.")[0] + "Z" ;
               // get latest commit sha
               fork_commit_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + comparedFork + "/commits?until=" + previousTimeStamp));
               previousCommitSHA = fork_commit_jsonObj.getString("sha");

           }

            String diffURL=github_page + comparedFork + "/compare/" + previousCommitSHA + "..." + forkName.split(FS)[0] + ":" + latestCommitSHA;
            System.out.println(diffURL);
            new ProcessingText().rewriteFile(diffURL,analysisDir+"diffurl.txt");
            System.out.println("diff url:"+ diffURL);
            return  diffURL;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String[] args) {
        String forkName = "malx122/Marlin";
        for (int i = 0; i < 11; i++) {
            System.out.println(i + " - " + ((i - 3) < 0 ? (i + 12 - 3) : i - 3));
        }
    }
}
