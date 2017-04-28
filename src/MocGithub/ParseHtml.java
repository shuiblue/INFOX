package MocGithub;

import ColorCode.BackgroundColor;
import CommunityDetection.AnalyzingCommunityDetectionResult;
import Util.JsonUtility;
import Util.ProcessingText;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;

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
    private String workingDir = System.getProperty("user.dir");
    HashMap<String, String> nodeId_to_clusterID_Map;
    ArrayList<String> combination_list = new ArrayList<>();
    HashMap<String, String> label_to_id;
    ArrayList<String> forkAddedNodeList;
    HashMap<Integer, HashMap<String, HashSet<Integer>>> clusterResultMap;
    static ArrayList<String> all_splitStep_list = new ArrayList<>();
    HashMap<String, Integer> usedColorIndex = new HashMap<>();
    HashMap<String, String> cluster_color = new HashMap<>();
    static HashMap<Integer, HashMap<Integer, HashMap<String, HashSet<Integer>>>> allSplittingResult;
    static int otherClusterSize = 0;


    private String table_header = "<table id=\"cluster\"  \n" +
            "  <tr> \n" +
            "    <td colspan=\"2\"> <button class=\"btn\" id=\"btn_hide_non_cluster_rows\" onclick=\"hide_non_cluster_rows()\">Hide non cluster code</button>\n" +
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
        System.out.println("get original diff page");
        this.analysisDir = localSourceCodeDirPath + "INFOX_output/";
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        // turn off htmlunit warnings
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
        webClient.getOptions().setUseInsecureSSL(true); //ignore ssl certificate
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.setCssErrorHandler(new SilentCssErrorHandler());

        HtmlPage page = null;
        HashMap<String, String> loadDiffId_to_content = new HashMap<>();
        try {
            page = webClient.getPage(diffPageUrl + "#files_bucket");
            webClient.waitForBackgroundJavaScriptStartingBefore(200);
            webClient.waitForBackgroundJavaScript(50000);


            ArrayList<HtmlDivision> buttonList = (ArrayList<HtmlDivision>) page.getByXPath("//div[@class='js-diff-load-container']");
            for (HtmlDivision hd : buttonList) {

                String loadDiffUrl = hd.getElementsByTagName("include-fragment").get(0).getAttribute("data-fragment-url");
                String diffID = loadDiffUrl.split("\\?")[0].split("/")[4];

                HtmlPage currentDiffPage = webClient.getPage("https://github.com" + loadDiffUrl);
                loadDiffId_to_content.put(diffID, currentDiffPage.asXml());

            }

        } catch (Exception e) {
            System.out.println("Get page error");
        }


        Document currentPage = Jsoup.parse(page.asXml());
        loadDiffId_to_content.forEach((diffID, xml) -> {
            Document xmldoc = Jsoup.parse(xml);
            Element currentDiff = currentPage.getElementById("diff-" + diffID).getElementsByClass("js-file-content Details-content--shown").first().append(String.valueOf(xmldoc.getElementsByClass("data highlight blob-wrapper")));
//            Element currentDiff= currentPage.getElementById("diff-"+diffID);
//            Element replaceBlock = currentDiff.getElementsByClass("js-file-content Details-content--shown").first();
//            replaceBlock.getElementsByTag("div").remove();

//           replaceBlock.append(String.valueOf(xmldoc.getElementsByClass("data highlight blob-wrapper")));
            currentDiff.getElementsByClass("js-diff-load-container").remove();
            System.out.println();
        });


        new ProcessingText().rewriteFile(currentPage.toString(), analysisDir + originalPage);
    }

    public void generateMocGithubForkPage(String diffPageUrl, String forkName, String localSourceCodeDirPath) {
        ProcessingText pt = new ProcessingText();
//        combination_list = generateAllCombineResult(analysisDir, max_numberOfCut);

        try {
            String[] splitSteps = pt.readResult(analysisDir + "splittingSteps.txt").split("\n");
            for (String s : splitSteps) {
                combination_list.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        this.analysisDir = localSourceCodeDirPath + "INFOX_output/";


        try {
            String page = pt.readResult(analysisDir + originalPage);
            doc = Jsoup.parse(page);
            //modify title-- code changes of fork : ...
            Element fork_title = doc.getElementsByClass("gh-header-title").first();
            fork_title.text(fork_title.text().replaceAll("Comparing changes", "Code Changes of fork: " + forkName));

            doc.getElementsByClass("range-editor text-gray js-range-editor is-cross-repo").remove();
            doc.getElementsByClass("gh-header-meta").first().text("Comparing..<h1>parent of the first commit</h1> to  <h1>the latest commit</h1>");


            //remove background color for line-number columns
            String css = pt.readResult(workingDir + "/src/files/stylesheet.css");
            doc.getElementsByTag("header").append(css);

//            String jqueryLink = "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/jquery/3.2.1/jquery.min.js\"></script>";
//            doc.getElementsByTag("header").append(jqueryLink);


            String js = pt.readResult(workingDir + "/src/files/jsFile.js");
            doc.getElementsByTag("html").append(js);
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

            System.out.println("get all splitting result map");
            allSplittingResult = new AnalyzingCommunityDetectionResult(analysisDir).getAllSplittingResult(max_numberOfCut, topClusterList, combination_list);

//            ArrayList<String> splitStepList = generateAllCombineResult(analysisDir, max_numberOfCut);
//            ArrayList<String> splitStepList = generateAllCombineResult(analysisDir, max_numberOfCut);


            for (String splitStep : combination_list) {
//            for (String splitStep : splitStepList) {

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


    private HashMap<String, String> generateClusterSummaryTable(String splitStep, HashMap<String, List<String>> cluster_keyword, List<String> stopSplitClusters) throws IOException {


        ProcessingText pt = new ProcessingText();


        StringBuilder sb = new StringBuilder();
        sb.append(table_header);


        String[] clusters = splitStep.split("--");

        for (int i = 0; i < clusters.length; i++) {
            String cid = clusters[i];
            /**   generate next step link **/
            String nextStep, nextStepStr;

            for (int j = 0; j < cid.split("~").length; j++) {
//            for (String clusterID : cid.split("~")) {
                String clusterID = cid.split("~")[j];
                if (!stopSplitClusters.contains(clusterID) && clusterID.split("_").length < max_numberOfCut + 1) {
                    nextStep = replaceCurrentStep(splitStep, cid, j);

                    nextStepStr = "       <td width=\"80\"><a href=\"./" + nextStep + ".html\" class=\"button\">split</a></td>\n";
                } else {
                    nextStepStr = "       <td width=\"80\">no more</td>\n";
                }
                generate_one_row_of_currentCluster(cluster_keyword, sb, i, nextStepStr, clusterID);
            }
        }

        sb.append("<tr> \n" +
                "       <td width=\"60\" style=\"cursor: pointer; background:grey\" onclick='hide_cluster_rows(\"infox_other\")'> other </td>\n" +
                "       <td width=\"90\"><button class=\"btn BtnGroup-item\" onclick=\"next_in_cluster(\'infox_other\')\" >Next</button><button class=\"btn BtnGroup-item\" onclick=\"prev_in_cluster(\'infox_other\')\">Prev</button></td>" +
                "       <td ><div class=\"long_td\"> other small clusters </div></td>\n" +
                "       <td width=\"50\">" + otherClusterSize + "</td>\n" +
                "       <td width=\"80\">no more</td>\n" +
                "   </tr>");
        cluster_color.put("other", "grey");

        sb.append("</table>");

        pt.rewriteFile(sb.toString(), analysisDir + splitStep + ".color");
        return cluster_color;

    }

    private String replaceCurrentStep(String splitStep, String cid, int j) {
        String[] currentIDArray = cid.split("~");
        String clusterID = currentIDArray[j];
        currentIDArray[j] = currentIDArray[j].replace(clusterID, clusterID + "_1~" + clusterID + "_2");
        String newStep = "";
        for (String s : currentIDArray) {
            newStep += "~"+s;
        }
        while (newStep.startsWith("~")) {
            newStep = newStep.substring(1);
        }

        return splitStep.replace(cid,newStep);

    }

    private String generate_one_row_of_currentCluster(HashMap<String, List<String>> cluster_keyword, StringBuilder sb, int i, String nextStepStr, String clusterID) {
        BackgroundColor cc = new BackgroundColor();
        ArrayList<ArrayList<String>> colorfamiliy_List = cc.getColorFamilyList();

        String originalClusterID = clusterID.split("_")[0];
        /**   color  **/
        String color;
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
        //todo: null pointer
        System.out.print(clusterID);
        if (cluster_keyword.get(clusterID) == null) {
            System.out.println();
        }
        /**  keyword **/
        String keyword_long = cluster_keyword.get(clusterID).toString();
        String keyword_prefix = keyword_long.trim().substring(0, 6).replace("[", "") + ".";


        int clusterSize = allSplittingResult.get(Integer.valueOf(originalClusterID)).get(clusterID.split("_").length - 1).get(clusterID).size();

        sb.append(generateRow(color, clusterID, keyword_prefix, keyword_long, clusterSize, nextStepStr));
        return sb.toString();
    }

    private String generateRow(String color, String current_clusterID, String keyword_suffix, String keyword_long, int clusterSize, String nextStepStr) {
        return "<tr> \n" +
                "       <td width=\"60\" style=\"cursor: pointer; background:" + color + "\" onclick='hide_cluster_rows(\"infox_" + current_clusterID + "\")'>" + keyword_suffix + "</td>\n" +
                "       <td width=\"90\"><button class=\"btn BtnGroup-item\" onclick=\"next_in_cluster(\'infox_" + current_clusterID + "\')\" >Next</button><button class=\"btn BtnGroup-item\" onclick=\"prev_in_cluster(\'infox_" + current_clusterID + "\')\">Prev</button></td>" +
                "        <td ><div class=\"long_td\">" + keyword_long + "</div></td>\n" +
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
                    nextStepStr = "       <td width=\"80\"><a href=\"./" + nextStep + ".html\" class=\"button\">split</a></td>\n";
                } else {
                    nextStepStr = "       <td width=\"80\"></td>\n";
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
                        sb.append(generateRow(color, current_clusterID, keyword_suffix, keyword_long, Integer.parseInt(clusterSize[0]), nextStepStr));

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

                    sb.append(generateRow(color, clusterID, keyword_suffix, keyword_long, Integer.parseInt(clusterSize[0]), nextStepStr));
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
        Set<String> topClusterSet = new HashSet<String>(Arrays.asList(splitStep.replaceAll("--", "~").split("~")));


        nodeId_to_clusterID_Map = new HashMap<>();
        ProcessingText pt = new ProcessingText();
        AnalyzingCommunityDetectionResult acdr = new AnalyzingCommunityDetectionResult(analysisDir);

        label_to_id = pt.getNodeLabel_to_id_map(analysisDir + "nodeLable2IdMap.txt");

        try {
            forkAddedNodeList = pt.getForkAddedNodeList(analysisDir + "forkAddedNode.txt");
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
                });

                if (!topClusterSet.contains(clusterID)) {
                    otherClusterSize += nodeSet.size();
                }

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


                ArrayList<String> topClusterList = pt.getListFromFile(analysisDir, "topClusters.txt");
                if (pt.isTopCluster(topClusterList, clusterid)) {
                    String keywod_prefix = cluster_keyword.get(clusterid).get(0).trim();
                    keywod_prefix = keywod_prefix.split("").length > 3 ? keywod_prefix.substring(0, 3).replace("[", "") + "." : keywod_prefix;

                    currentDoc.getElementsByAttributeValue("data-path", fileName);
                    Element currentFile = currentDoc.getElementsByAttributeValue("data-path", fileName).next().first();
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

                    String styleStr = "background-color:"
                            + cluster_color.get(clusterid)
                            + ";font-family:SFMono-Regular, Consolas, Liberation Mono, Menlo, Courier, monospace;\n" +
                            "  font-size:12px;\n" +
                            "  line-height:20px;\n" +
                            "  text-align:right;\"";

                    lineElement.attr("class", "infox_" + clusterid.trim()).attr("style", styleStr).text(keywod_prefix);

                } else {
                    String keywod_prefix = "other";
                    currentDoc.getElementsByAttributeValue("data-path", fileName);
                    Element currentFile = currentDoc.getElementsByAttributeValue("data-path", fileName).next().first();
                    if (currentFile == null) {
                        System.out.println(lineNumber);
                    }
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

                    String styleStr = "background-color:grey"
                            + ";font-family:SFMono-Regular, Consolas, Liberation Mono, Menlo, Courier, monospace;\n" +
                            "  font-size:12px;\n" +
                            "  line-height:20px;\n" +
                            "  text-align:right;\"";
                    lineElement.attr("class", "infox_other").attr("style", styleStr).text(keywod_prefix);

                }
            }
        }

        pt.rewriteFile(currentDoc.toString(), analysisDir + splitStep + ".html");


    }

    /**
     * This function get diff url of github fork
     *
     * @param forkName
     * @param timeWindowSize "begining" -- , "x months"
     * @return
     */
    public String getDiffPageUrl(String forkName, String timeWindowSize) {
        String forkUrl = github_api + forkName;
        JsonUtility jsonUtility = new JsonUtility();
        try {
            // get latest commit sha
            String[] todayTimeStamp = new Timestamp(System.currentTimeMillis()).toString().split(" ");
            String format_current_time = todayTimeStamp[0] + "T" + todayTimeStamp[1].split("\\.")[0] + "Z";
            JSONObject fork_commit_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + forkName + "/commits?until=" + format_current_time));
            String latestCommitSHA = fork_commit_jsonObj.getString("sha");
            String latestCommitDate = fork_commit_jsonObj.getJSONObject("commit").getJSONObject("author").get("date").toString();
            int latestMonth = Integer.parseInt(latestCommitDate.split("-")[1]);
            int latestYear = Integer.parseInt(latestCommitDate.split("-")[0]);

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


            } else if (timeWindowSize.contains("month")) {
                int[] monthArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
                int diffMonth = Integer.parseInt(timeWindowSize.split(" ")[0]);

                int delta = latestMonth - diffMonth;
                int previousMonth = delta > 0 ? delta : delta + 12;
                String previousMonthStr = (previousMonth + "").split("").length == 2 ? previousMonth + "" : "0" + previousMonth;

                int previousYear = delta > 0 ? latestYear : latestYear - 1;

                comparedFork = forkName;
                String previousTimeStamp = previousYear + "-" + previousMonthStr + "-" + todayTimeStamp[0].split("-")[2] + "T" + todayTimeStamp[1].split("\\.")[0] + "Z";
                // get latest commit sha
                fork_commit_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + comparedFork + "/commits?until=" + previousTimeStamp));
                previousCommitSHA = fork_commit_jsonObj.getString("sha");


            }

            String diffURL = github_page + comparedFork + "/compare/" + previousCommitSHA + "..." + forkName.split(FS)[0] + ":" + latestCommitSHA;
            System.out.println(diffURL);
            new ProcessingText().rewriteFile(diffURL, analysisDir + "diffurl.txt");
            System.out.println("diff url:" + diffURL);
            return diffURL;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    public static void main(String[] args) {

    }
}
