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
    String analysisDir = "";
    String originalPage = "original.html";
    int max_numberOfCut;
    int numberOfBiggestClusters;

    HashMap<String, String> nodeId_to_lusterID_Map;
    String[] combination_list;
    HashMap<String, String> label_to_id;
    ArrayList<String> forkAddedNodeList;
    HashMap<Integer, HashMap<String, HashSet<Integer>>> clusterResultMap;

    public ParseHtml(int max_numberOfCut, int numberOfBiggestClusters) {
        this.max_numberOfCut = max_numberOfCut;
        this.numberOfBiggestClusters = numberOfBiggestClusters;
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


            String js = pt.readResult("/Users/shuruiz/Work/GithubProject/jsFile.txt");

            doc.getElementsByTag("html").last().after(js);

            Elements fileList_elements = doc.getElementsByClass("file-header");
            combination_list = GenerateCombination.getAllLists(max_numberOfCut, numberOfBiggestClusters);
            for (String splitStep : combination_list) {

                HashMap<String, String> nodeId_to_clusterID = genrate_NodeId_to_clusterIDList_Map(splitStep);
                HashMap<String, List<String>> cluster_keyword = new HashMap<>();
                String keyword[] = pt.readResult(analysisDir + splitStep + "_keyword.txt").split("\n");
                for (String kw : keyword) {
                    kw = kw.replace("[", "").replace("]", "");
                    String cid = kw.split(":")[0].trim();

                    List<String> list = Arrays.asList(kw.split(":")[1].split(","));
                    cluster_keyword.put(cid, list);
                }

                HashMap<String, String> cluster_color = generateClusterSummaryTable(splitStep, cluster_keyword);


                generateHtml(fileList_elements, nodeId_to_clusterID, splitStep, cluster_color, cluster_keyword);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, String> generateClusterSummaryTable(String splitStep, HashMap<String, List<String>> cluster_keyword) throws IOException {
        HashMap<String, String> cluster_color = new HashMap<>();

        ProcessingText pt = new ProcessingText();
        BackgroundColor cc = new BackgroundColor();

        ArrayList<ArrayList<String>> colorfamiliy_List = cc.getColorFamilyList();


        StringBuilder sb = new StringBuilder();
        sb.append("<style>\n" +
                "#cluster{\n" +
                "  background-color: #fff;\n" +
                "  border: 1px solid #000;\n" +
                " position:fixed;\n" +
                "  right: 0;\n" +
                "  left: 0;\n" +
                "  width: 980px;\n" +
                "  margin-right: auto;\n" +
                "  margin-left: auto;\n" +
                "  top:19px;\n" +
                "  /*right:461px;*/\n" +
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
                "       <th>Keywords</th>\n" +
                "       <th>LOC</th>\n" +
                "       <th>Split Cluster </th>\n" +
                "    </tr>\n");


        try {
            String[] topClusterID = pt.readResult(analysisDir + "topClusters.txt").split("\n");

            for (int i = 0; i < topClusterID.length; i++) {
                String clusterID = topClusterID[i];
                String cluster[] = clusterID.split("_");
                int clusterIndex = cluster.length == 1 ? 1 : Integer.parseInt(cluster[1]);


                String[] splitArray = splitStep.split("");
                String[] nextSplit = Arrays.copyOf(splitArray, splitArray.length);
                nextSplit[i] = String.valueOf(Integer.valueOf(splitArray[clusterIndex]) + 1);
                String nextStep = "";
                String nextStepStr="";
                if (Integer.valueOf(nextSplit[i]) <=max_numberOfCut) {
                    for (String s : nextSplit) {
                        nextStep += s;
                    }
                     nextStepStr = "       <td width=\"100\"><a href=\"./" + nextStep + ".html\" class=\"button\">split</a></td>\n";
                }else{
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
                        sb.append("<tr> \n" +
                                "       <td width=\"130\" style=\"cursor: pointer; background:" + color + "\" onclick='hide_cluster_rows(\"infox_" + current_clusterID + "\")'>" + keyword_suffix + "</td>\n" +
                                "        <td width=\"600\">" + keyword_long + "</td>\n" +
                                "       <td width=\"50\">" + clusterSize[0] + "</td>\n" +
                                nextStepStr +
                                "   </tr>");

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

                    sb.append("<tr> \n" +
                            "       <td width=\"130\" style=\"cursor: pointer; background:" + color + "\" onclick='hide_cluster_rows(\"infox_" + clusterID + "\")'>" + keyword_suffix + "</td>\n" +
                            "        <td width=\"600\">" + keyword_long + "</td>\n" +
                            "       <td width=\"50\">" + clusterSize[0] + "</td>\n" +
                            nextStepStr +
                            "   </tr>");


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

        nodeId_to_lusterID_Map = new HashMap<>();
        ProcessingText pt = new ProcessingText();
        AnalyzingCommunityDetectionResult acdr = new AnalyzingCommunityDetectionResult(analysisDir);

        label_to_id = pt.getNodeLabel_to_id_map(analysisDir + "nodeLable2IdMap.txt");

        DependencyGraph dp = new DependencyGraph();
        try {
            forkAddedNodeList = dp.getForkAddedNodeList(analysisDir + "forkAddedNode.txt");
            for (String nodeLabel : forkAddedNodeList) {
                String nodeID = label_to_id.get("\"" + nodeLabel + "\"");
                if (nodeID != null) {
                    nodeId_to_lusterID_Map.put(nodeID, "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean isOriginalGraph = false;
        if (splitStep.replace("1", "").equals("")) {
            isOriginalGraph = true;
        }
        clusterResultMap = acdr.getClusteringResultMap(splitStep, isOriginalGraph);

        clusterResultMap.forEach((k, v) -> {
            HashMap<String, HashSet<Integer>> currentClusterMap = v;
            currentClusterMap.forEach((clusterID, nodeSet) -> {
                nodeSet.forEach(p -> {
                            int nodeId = Integer.valueOf(p);
                            nodeId_to_lusterID_Map.put(nodeId + "", clusterID);
                        }
                );

            });
        });
        return nodeId_to_lusterID_Map;
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
                    String keywod_prefex = cluster_keyword.get(clusterid).get(0).substring(0, 6).trim();

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
                        lineElement.attr("class", "infox_" + clusterid.trim()).attr("style", styleStr).text(keywod_prefex);
                    } else {
                        System.out.println(fileName + " is bigger than the github diff limits..");


                    }
                }
            }
        }

        pt.rewriteFile(currentDoc.toString(), analysisDir + splitStep + ".html");


    }


    public String getDiffPageUrl(String forkName) {
        String forkUrl = github_api + forkName;
        JsonUtility jsonUtility = new JsonUtility();
        try {
            // get latest commit sha
            String[] todayTimeStamp = new Timestamp(System.currentTimeMillis()).toString().split(" ");
            String format_current_time = todayTimeStamp[0] + "T" + todayTimeStamp[1].split("\\.")[0] + "Z";
            JSONObject fork_commit_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + forkName + "/commits?until=") + format_current_time);
            String latestCommitSHA = fork_commit_jsonObj.getString("sha");

            //get general fork information
            JSONObject fork_jsonObj = new JSONObject(jsonUtility.readUrl(forkUrl));
            //get commit before fork was created
            String firstCommitTimeStamp = fork_jsonObj.getString("created_at");
            JSONObject upstreamInfo = (JSONObject) fork_jsonObj.get("parent");
            String upstreamName = upstreamInfo.getString("full_name");
            JSONObject upstream_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + upstreamName + "/commits?until=" + firstCommitTimeStamp));
            String SHA_beforeForkCreated = upstream_jsonObj.getString("sha");
            return github_page + upstreamName + "/compare/" + SHA_beforeForkCreated + "..." + forkName.split(FS)[0] + ":" + latestCommitSHA;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String[] args) {
        String forkName = "malx122/Marlin";

//        //get origin diff github page
//        ParseHtml parseHtml = new ParseHtml();
//        String diffPageUrl = parseHtml.getDiffPageUrl(forkName);
//
//        //git clone repo to local dir
//        JgitUtility jgitUtility = new JgitUtility();
//        String uri = github_page + forkName + ".git";
//        String localDirPath = "/Users/shuruiz/Work/GithubProject/" + forkName;
//        jgitUtility.cloneRepo(uri, localDirPath);


        // hack github page
//        parseHtml.generateMocGithubForkPage(diffPageUrl, forkName);
    }
}
