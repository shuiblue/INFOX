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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
    String publicToken;

    public String appendTableTitle(int colspan) {
        return "<table id=\"cluster\"  \n" +
                "  <tr> \n" +
                "    <td colspan=\"2\"> <button class=\"btn\" id=\"btn_hide_non_cluster_rows\" onclick=\"hide_non_cluster_rows()\">Hide non cluster code</button>\n" +
                "    </td> \n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "   <th colspan=\"" + colspan + "\">Level</th>\n" +
                "       <th>Cluster</th>\n" +
                "       <th>Navigation</th>\n" +
                "       <th>Keywords</th>\n" +
                "       <th>LOC</th>\n" +
                "       <th>Split Cluster </th>\n" +
                "    </tr>\n";
    }

    public ParseHtml(int max_numberOfCut, int numberOfBiggestClusters, String analysisDir, String publicToken) {
        this.max_numberOfCut = max_numberOfCut;
        this.numberOfBiggestClusters = numberOfBiggestClusters;
        this.analysisDir = analysisDir;
        this.publicToken = publicToken;

    }

    public void getOriginalDiffPage(String diffPageUrl, String localSourceCodeDirPath, String forkName) {
        ProcessingText processingText = new ProcessingText();
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
        Document currentPage = null;
        try {
            page = webClient.getPage(diffPageUrl + "?w=1");

            webClient.waitForBackgroundJavaScriptStartingBefore(200);
            webClient.waitForBackgroundJavaScript(5000);

            currentPage = Jsoup.parse(page.asXml());

            currentPage = getLoadDiffContentMap(webClient, currentPage);
            System.out.println("done with getting all the files");

            boolean isMarlin = localSourceCodeDirPath.contains("Marlin");
            currentPage = getLoadBigDiffContentMap(webClient, currentPage, isMarlin);
            System.out.println("done with clicking all the load diff button ");

            getDiffInfo(currentPage,localSourceCodeDirPath+"/INFOX_output/forkAddedNode.txt");

//            /** get commit list **/
//            Elements elements_commitID = currentPage.getElementsByClass("commit-id");
//            String firstCommit = elements_commitID.first().attr("href").split("/")[4];
//            String lastCommit = elements_commitID.last().attr("href").split("/")[4];
//            HtmlPage firstCommitPage = webClient.getPage("https://github.com/" + forkName + "/commit/" + firstCommit);
//            Document commitDoc = Jsoup.parse(firstCommitPage.asXml());
//            String parentCommit = commitDoc.getElementsByClass("sha").first().attr("href").split("/")[4];
//            processingText.rewriteFile(parentCommit + "," + lastCommit, analysisDir + "SHA.txt");
//            String parentRepo = commitDoc.getElementsByClass("fork-flag").text().replace("forked from", "").trim();
//            processingText.rewriteFile(parentRepo, analysisDir + "parent.txt");


        } catch (Exception e) {
            System.out.println("Get page error");
        }


        processingText.rewriteFile(currentPage.toString(), analysisDir + originalPage);
        System.out.println("done with getting original html file.");

    }

    private void getDiffInfo(Document currentPage,String outputFile) {
        System.out.println("getting diff info...");
        StringBuilder sb = new StringBuilder();
        ProcessingText processingText = new ProcessingText();
        /**  get changed file list **/
        Elements file_elements = currentPage.getElementsByClass("link-gray-dark");

        Elements diff_elements = currentPage.getElementsByClass("js-file-content");
        for (int i = 0; i < diff_elements.size(); i++) {
            String currentFile = file_elements.get(i).text();
            String newFileName = processingText.changeFileName(currentFile);
            Elements tr_list = diff_elements.get(i).getElementsByTag("tr");
            for (Element tr : tr_list) {
                Elements td_list = tr.getElementsByTag("td");
                if (td_list.size() == 3) {
                    String line = td_list.last().text();
                    if (line.trim().startsWith("+") && processingText.isCode(line.replaceAll("^[+]", ""))) {
                        String lineNumber = td_list.get(1).attr("data-line-number");
                        sb.append(newFileName + "-" + lineNumber + "\n");
                    }
                }
            }
        }
        processingText.rewriteFile(sb.toString(),outputFile);
        System.out.println("done with getting fork added node");
    }

    private Document getLoadBigDiffContentMap(WebClient webClient, Document currentPage, boolean isMarlin) throws IOException {
        Elements loadingElements = currentPage.getElementsByTag("include-fragment");
        HtmlPage currentDiffPage;
        int i = 1;
        for (Element ele : loadingElements) {
            String loadDiffUrl = ele.attr("data-fragment-url");
            boolean isMarlinFile = false;
            if (isMarlin) {
                isMarlinFile = ele.parent().parent().parent().getElementsByAttribute("data-path").first().toString().contains("data-path=\"Marlin");
            }
            if (!loadDiffUrl.equals("") && (!isMarlin || isMarlinFile)) {

                System.out.println(loadDiffUrl + "----" + i++);
                String diffID = loadDiffUrl.split("\\?")[0].split("/")[4];
                currentDiffPage = webClient.getPage("https://github.com" + loadDiffUrl + "&w=1");
                webClient.waitForBackgroundJavaScriptStartingBefore(200);
                webClient.waitForBackgroundJavaScript(5000);
                Document xmldoc = Jsoup.parse(currentDiffPage.asXml());
                Element currentDiff = currentPage.getElementById("diff-" + diffID).getElementsByClass("js-file-content Details-content--shown").first().append(String.valueOf(xmldoc.getElementsByClass("data highlight blob-wrapper")));
                currentDiff.getElementsByClass("js-diff-load-container").remove();
            }
        }
        return currentPage;
    }

    /**
     * This function iteratively load all the big diff files
     *
     * @param webClient   htmlunit web client, which is used for loading js file
     * @param currentPage currentPage, which contains "load diff" blocks and unload rest pages
     * @return renewed current page
     * @throws IOException
     */
    private Document getLoadDiffContentMap(WebClient webClient, Document currentPage) throws IOException {
        Elements loadingElements = currentPage.getElementsByTag("include-fragment");
        HtmlPage currentDiffPage;
        for (Element ele : loadingElements) {
            String loadDiffUrl = ele.attr("data-fragment-url");
            if (loadDiffUrl.equals("")) {
                loadDiffUrl = ele.attr("src");
                System.out.println(loadDiffUrl);
                currentDiffPage = webClient.getPage("https://github.com" + loadDiffUrl);
                Document currentDiffDoc = Jsoup.parse(currentDiffPage.asXml());
                Element bodyElement = currentDiffDoc.getElementsByTag("body").first();
                Element diffViewElement = currentPage.getElementsByClass("diff-view ").first();
                diffViewElement.append(bodyElement.toString().replace("<body>", "").replace("</body>", ""));
                ele.remove();
                currentPage = getLoadDiffContentMap(webClient, currentPage);
            }
        }
        return currentPage;
    }

    /**
     * This function generates all the html files for each splitting step
     *
     * @param forkName
     * @param localSourceCodeDirPath
     */
    public void generateMocGithubForkPage(String forkName, String localSourceCodeDirPath) {
        ProcessingText pt = new ProcessingText();
        this.analysisDir = localSourceCodeDirPath + "INFOX_output/";
        try {
            String[] splitSteps = pt.readResult(analysisDir + "splittingSteps.txt").split("\n");
            for (String s : splitSteps) {
                combination_list.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


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

            System.out.println("get all splitting result map, there are " + combination_list.size() + " html pages need to be generated..");
            allSplittingResult = new AnalyzingCommunityDetectionResult(analysisDir).getAllSplittingResult(max_numberOfCut, topClusterList, combination_list);

            for (String splitStep : combination_list) {

                List<String> stopSplitClusters = Arrays.asList(new ProcessingText().readResult(analysisDir + "noSplittingStepList.txt").split("\n"));

                HashMap<String, String> nodeId_to_clusterID = genrate_NodeId_to_clusterIDList_Map(splitStep);
                HashMap<String, List<String>> cluster_keyword = new HashMap<>();
                String keyword[] = pt.readResult(analysisDir + splitStep + "_keyword.txt").split("\n");
                for (String kw : keyword) {
                    kw = kw.replace("[", "").replace("]", "");
                    String cid = kw.split(":")[0].trim();

                    List<String> list = Arrays.asList(kw.split(":")[1].split(","));
                    cluster_keyword.put(cid, list);
                }


                System.out.println("generating Cluster Summary Table for current splitting step: " + splitStep);
                HashMap<String, String> cluster_color = generateClusterSummaryTable(splitStep, cluster_keyword, stopSplitClusters);

                generateHtml(fileList_elements, nodeId_to_clusterID, splitStep, cluster_color, cluster_keyword);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }


        new ProcessingText().rewriteFile("done", analysisDir + "done.txt");
    }


    private HashMap<String, String> generateClusterSummaryTable(String splitStep, HashMap<String, List<String>> cluster_keyword, List<String> stopSplitClusters) throws IOException {
        ProcessingText pt = new ProcessingText();
        StringBuilder sb = new StringBuilder();
        int colspan = new DrawTableHierarchy().getTreeHight(splitStep);
        sb.append(appendTableTitle(colspan));

        String[] clusters = splitStep.split("--");
        for (int i = 0; i < clusters.length; i++) {

            String cid = clusters[i];
            /**   generate next step link **/
            String nextStep, nextStepStr;
            String[] subClusterArray = cid.split("~");

            for (int j = 0; j < subClusterArray.length; j++) {
                String clusterID = subClusterArray[j];
                if (!stopSplitClusters.contains(clusterID) && clusterID.split("_").length < max_numberOfCut + 1) {
                    System.out.println(splitStep);
                    nextStep = replaceCurrentStep(splitStep, cid, j);
//                    System.out.println(nextStep+"\n----------");
                    nextStepStr = "       <td width=\"80\"><a href=\"./" + nextStep + ".html\" class=\"button\">split</a></td>\n";
                } else {
                    nextStepStr = "       <td width=\"80\">no more</td>\n";
                }

                boolean hasPair = false;
                if (j + 1 < subClusterArray.length) {
                    hasPair = hasPair(subClusterArray[j], subClusterArray[j + 1]);
                }

                DrawTableHierarchy drawTableHierarchy = new DrawTableHierarchy();
                drawTableHierarchy.calculatingArray(analysisDir, cid);
                HashMap<String, DrawTableHierarchy.Cluster> clusterTree = drawTableHierarchy.getClusterTree();
                System.out.print("get split step hierachy array ...");
                String[][] hierarchyArray = drawTableHierarchy.getHierachyStringFromText(analysisDir, cid);

                String parentStep = clusterID.contains("_") ? clusterID.replaceAll("[_][1|2]$", "") : clusterID;
                String joinStep = splitStep.replace(parentStep + "_1~" + parentStep + "_2", parentStep);

                generate_one_row_of_currentCluster(cluster_keyword, sb, i, nextStepStr, clusterID, hasPair, hierarchyArray[2 * j], hierarchyArray[2 * j + 1], joinStep, colspan, clusterTree);

            }
        }

        sb.append("<tr> \n" +
                "       <td colspan=\"" + colspan + "\"></td>\n" +
                "       <td width=\"60\" style=\"cursor: pointer; background:grey\" onclick='hide_cluster_rows(\"infox_other\")'> other </td>\n" +
                "       <td  width=\"90\"><button class=\"btn BtnGroup-item\" onclick=\"next_in_cluster(\'infox_other\')\" >Next</button><button class=\"btn BtnGroup-item\" onclick=\"prev_in_cluster(\'infox_other\')\">Prev</button></td>" +
                "       <td ><div class=\"long_td\"> other small clusters </div></td>\n" +
                "       <td width=\"50\">" + otherClusterSize + "</td>\n" +
                "       <td width=\"80\">no more</td>\n" +
                "   </tr>");
        cluster_color.put("other", "grey");

        sb.append("</table>");

        pt.rewriteFile(sb.toString(), analysisDir + splitStep + ".color");

        System.out.println("finished generating summary table");


        return cluster_color;

    }

    private String getHierachyStringForCurrentRow(String clusterID, boolean hasPair, String[] clusters, int index, int maxHeight) {
        String parentID = clusterID.substring(0, clusterID.lastIndexOf("_"));
        int middle = (2 * clusters.length) / 2;
        String str = "";
        System.out.println();
        for (int height = 0; height < maxHeight; height++) {
            if (hasPair) {
                if (clusterID.endsWith("_1")) {
                    if (index == 0 && height == 0) {
                        return "     <td style=\"border:none;\"> </td>\n" +
                                "       <td style=\"border:none;\"> </td>\n" +
                                "        <td style=\"border:none;\"> </td>\n" +
                                "       <td style=\"border:none;\"> </td>\n";
                    } else {
                        if (height == 0 & 2 * index + 1 == middle) {
                            str += "<td colspan=\"1\" id=\"cel_3\"><button>" + parentID + "</button></td>\n";
                        } else {
                            str += "     <td style=\"border:none;\"> </td>\n";
                        }

                        if (height == 1 & maxHeight - height > 1) {
                            str += "     <td style=\"border:none;\"> </td>\n";
                        } else {
                            str += "<td colspan=\"1\" id=\"cel_3\"><button>" + parentID + "</button></td>\n";
                        }
                    }
                } else if (clusterID.endsWith("_2")) {
                    if (index == clusters.length - 1) {
                        return "     <td style=\"border:none;\"> </td>\n" +
                                "       <td style=\"border:none;\"> </td>\n" +
                                "        <td style=\"border:none;\"> </td>\n" +
                                "       <td style=\"border:none;\"> </td>\n";
                    }

                    if (height == 1 & maxHeight - height > 1) {
                        str += "     <td style=\"border:none;\"> </td>\n";
                    } else {
                        str += "<td colspan=\"1\" id=\"cel_3\"><button>" + parentID + "</button></td>\n";
                    }

                } else {
                    return "     <td style=\"border:none;\"> </td>\n" +
                            "       <td style=\"border:none;\"> </td>\n" +
                            "        <td style=\"border:none;\"> </td>\n" +
                            "       <td style=\"border:none;\"> </td>\n";
                }
            } else {

            }

        }
        return str;
    }

    private boolean hasPair(String s, String s1) {

        return s.substring(0, s.lastIndexOf("_")).equals(s1.substring(0, s1.lastIndexOf("_")));
    }


    private String replaceCurrentStep(String splitStep, String cid, int j) {
        String[] currentIDArray = cid.split("~");
        String clusterID = currentIDArray[j];
        currentIDArray[j] = currentIDArray[j].replaceAll(clusterID, clusterID + "_1~" + clusterID + "_2");
        String newStep = "";
        for (String s : currentIDArray) {
            newStep += "~" + s;
        }
        while (newStep.startsWith("~")) {
            newStep = newStep.substring(1);
        }

        String nextStep = "";
        String[] array = splitStep.split("--");
        for (int i = 0; i < array.length; i++) {
            String tmpStr ;
            if (array[i].equals(cid)) {
                tmpStr = newStep;
            } else {
                tmpStr = array[i];
            }

            if (i == array.length - 1) {
                nextStep += tmpStr;
            } else if (i < array.length - 1) {
                nextStep += tmpStr + "--";
            }


        }

        return nextStep;

    }

    private String generate_one_row_of_currentCluster(HashMap<String, List<String>> cluster_keyword, StringBuilder sb, int i, String nextStepStr, String clusterID, boolean hasPair, String[] levelColumn_1, String[] levelColumn_2, String joinStep, int colspan, HashMap<String, DrawTableHierarchy.Cluster> clusterTree) {
        System.out.print("adding one row for current summary table of cluster : " + clusterID + " ...");

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

        /**  keyword **/
        String keyword_long = cluster_keyword.get(clusterID).toString();
        String keyword_prefix = keyword_long.trim().substring(0, 6).replace("[", "") + ".";


        int clusterSize = clusterResultMap.get(1).get(clusterID).size();
//        int clusterSize = allSplittingResult.get(Integer.valueOf(originalClusterID)).get(clusterID.split("_").length - 1).get(clusterID).size();


        sb.append(generateRow(color, clusterID, keyword_prefix, keyword_long, clusterSize, nextStepStr, levelColumn_1, levelColumn_2, joinStep, colspan, clusterTree));
        System.out.println("done");
        return sb.toString();
    }

    private String generateRow(String color, String current_clusterID, String keyword_suffix, String keyword_long, int clusterSize, String nextStepStr, String[] levelColumn_1, String[] levelColumn_2, String joinStep, int colspan, HashMap<String, DrawTableHierarchy.Cluster> clusterTree) {


        String row_1 = getLevelColumn(levelColumn_1, joinStep, colspan, current_clusterID, clusterTree);
        String row_2 = getLevelColumn(levelColumn_2, joinStep, colspan, current_clusterID, clusterTree);

        return row_1 +
                "       <td rowspan=\"2\" width=\"60\" style=\"cursor: pointer; background:" + color + "\"><button id= \"infox_" + current_clusterID + "_button\"   onclick='hide_cluster_rows(\"infox_" + current_clusterID + "_button\")'>hide</button>" + keyword_suffix + "</td>\n" +
                "       <td rowspan=\"2\" width=\"90\"><button class=\"btn BtnGroup-item\" onclick=\"next_in_cluster(\'infox_" + current_clusterID + "\')\" >Next</button><button class=\"btn BtnGroup-item\" onclick=\"prev_in_cluster(\'infox_" + current_clusterID + "\')\">Prev</button></td>" +
                "        <td rowspan=\"2\"><div class=\"long_td\">" + keyword_long + "</div></td>\n" +
                "       <td rowspan=\"2\" width=\"50\">" + clusterSize + "</td>\n" +
                nextStepStr +
                "   </tr>" +
                row_2;
    }

    private String getLevelColumn(String[] levelColumn_1, String joinStep, int colspan, String current_clusterID, HashMap<String, DrawTableHierarchy.Cluster> clusterTree) {
        String row_1 = "<tr>";
        for (int i = 0; i < colspan - levelColumn_1.length; i++) {
            row_1 += " <td  id=\"cel_none\"></td>\n";
        }
        for (int x = 0; x < levelColumn_1.length; x++) {
            String s = levelColumn_1[x];

            if (s.equals("bottomLeft") || s.equals("bottom")) {
                String join = "";
                if (x <= levelColumn_1.length - 2) {
                    if (levelColumn_1[x + 1].equals("topLeft")) {
                        System.out.print("");
                    }
                    join = levelColumn_1[x + 1].equals("topLeft") && ((x + 1 == levelColumn_1.length - 1) || ((x + 1 == levelColumn_1.length - 2) && levelColumn_1[x + 2].equals("none"))) ? "join " : "";
                }


                row_1 += " <td  id=\"cel_" + s + "\"><a href=\"./" + joinStep + ".html\" class=\"button\">" + join + "</a></td>\n";
            } else {
                row_1 += " <td  id=\"cel_" + s + "\"></td>\n";
            }
        }
        return row_1;
    }

    private String getJoinStep(String current_clusterID, int x) {

        return "";

    }


    private HashMap<String, String> genrate_NodeId_to_clusterIDList_Map(String splitStep) {
        otherClusterSize = 0;
        Set<String> topClusterSet = new HashSet<String>(Arrays.asList(splitStep.replaceAll("--", "~").split("~")));


        nodeId_to_clusterID_Map = new HashMap<>();
        ProcessingText pt = new ProcessingText();
        AnalyzingCommunityDetectionResult acdr = new AnalyzingCommunityDetectionResult(analysisDir);

        label_to_id = pt.getNodeLabel_to_id_map(analysisDir + "nodeLable2IdMap.txt");

        try {
            System.out.println("read fork added node list..");
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
        System.out.println("generating html file for step: " + splitStep + "...");

        currentDoc = doc.clone();
        ProcessingText pt = new ProcessingText();
        try {
            String summaryTable = pt.readResult(analysisDir + splitStep + ".color");
            currentDoc.getElementsByTag("html").first().children().first().before(summaryTable);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // could used forkaddednodelist id file instead of fork added not list todo
        int newCodeSize = forkAddedNodeList.size();
        System.out.println("start to modify each line of fork added code, there are " + newCodeSize + " loc...");
        int i = 1;
        for (String changedCodeLabel : forkAddedNodeList) {

            if (label_to_id.get("\"" + changedCodeLabel + "\"") != null) {
                System.out.println(i + "/" + newCodeSize);
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

                    Element currentFile = currentDoc.getElementsByAttributeValueMatching("data-path", fileName).next().first();
                    Elements lineElements = currentFile.getElementsByAttributeValue("data-line-number", lineNumber);
                    Element lineElement = getElement(lineElements);

                    if (lineElement != null) {
                        String styleStr = "background-color:"
                                + cluster_color.get(clusterid)
                                + ";font-family:SFMono-Regular, Consolas, Liberation Mono, Menlo, Courier, monospace;\n" +
                                "  font-size:12px;\n" +
                                "  line-height:20px;\n" +
                                "  text-align:right;\"";

                        lineElement.attr("class", "infox_" + clusterid.trim()).attr("style", styleStr).text(keywod_prefix);
                    }

                } else {
                    String keywod_prefix = "other";
                    Element currentFile = currentDoc.getElementsByAttributeValueMatching("data-path", fileName).next().first();

                    Elements lineElements = currentFile.getElementsByAttributeValue("data-line-number", lineNumber);

                    Element lineElement = getElement(lineElements);
                    if (lineElement != null) {
                        String styleStr = "background-color:grey"
                                + ";font-family:SFMono-Regular, Consolas, Liberation Mono, Menlo, Courier, monospace;\n" +
                                "  font-size:12px;\n" +
                                "  line-height:20px;\n" +
                                "  text-align:right;\"";
                        lineElement.attr("class", "infox_other").attr("style", styleStr).text(keywod_prefix);
                    }
                }
                i++;
            }
        }

        pt.rewriteFile(currentDoc.toString(), analysisDir + splitStep + ".html");
        System.out.println("done with current splitstep");

    }

    private Element getElement(Elements lineElements) {
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
        return lineElement;
    }

    /**
     * This function get diff url of github fork
     *
     * @param forkName
     * @param timeWindowSize "begining" -- , "x months"
     * @return
     */
    public String getDiffPageUrl(String localSourceCodeDirPath, String forkName, String timeWindowSize) {


        String forkUrl = github_api + forkName + "?access_token=" + publicToken;
        JsonUtility jsonUtility = new JsonUtility();
        try {
            // get latest commit sha
            String[] todayTimeStamp = new Timestamp(System.currentTimeMillis()).toString().split(" ");
            String format_current_time = todayTimeStamp[0] + "T" + todayTimeStamp[1].split("\\.")[0] + "Z";
            JSONObject fork_commit_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + forkName + "/commits?access_token=" + publicToken + "&until=" + format_current_time));
            String latestCommitSHA = fork_commit_jsonObj.getString("sha");
            String latestCommitDate = fork_commit_jsonObj.getJSONObject("commit").getJSONObject("author").get("date").toString();
            int latestMonth = Integer.parseInt(latestCommitDate.split("-")[1]);
            int latestYear = Integer.parseInt(latestCommitDate.split("-")[0]);

            //get general fork information
            JSONObject fork_jsonObj = new JSONObject(jsonUtility.readUrl(forkUrl));


            //get commit before fork was created
            String comparedFork = "";
            String previousCommitSHA = "";
            String defaultBranch = "", forkBranchName = "";
            if (timeWindowSize.equals("beginning")) {
                String firstCommitTimeStamp = fork_jsonObj.getString("created_at");
                if (fork_jsonObj.has("parent")) {
                    JSONObject upstreamInfo = (JSONObject) fork_jsonObj.get("parent");

                    comparedFork = upstreamInfo.getString("full_name");
                    JSONObject upstream_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + comparedFork + "/commits?access_token=" + publicToken + "&until=" + firstCommitTimeStamp));
                    previousCommitSHA = upstream_jsonObj.getString("sha");
                } else {
                    //git rev-list --max-parents=0 HEAD
                    ProcessBuilder pb = new ProcessBuilder("git", "rev-list", "--max-parents=0", "HEAD");
//                    ProcessBuilder pb = new ProcessBuilder("git","log");

                    Map<String, String> envs = pb.environment();
                    pb.directory(new File(localSourceCodeDirPath));
                    System.out.println("" + pb.directory());
                    Process p = pb.start();
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

                    while (true) {
                        previousCommitSHA = r.readLine();
                        break;
                    }
                    comparedFork = forkName;
                }

            } else if (timeWindowSize.contains("month")) {
                int diffMonth = Integer.parseInt(timeWindowSize.split(" ")[0]);

                int delta = latestMonth - diffMonth;
                int previousMonth = delta > 0 ? delta : delta + 12;
                String previousMonthStr = (previousMonth + "").split("").length == 2 ? previousMonth + "" : "0" + previousMonth;

                int previousYear = delta > 0 ? latestYear : latestYear - 1;

                comparedFork = forkName;
                String previousTimeStamp = previousYear + "-" + previousMonthStr + "-" + todayTimeStamp[0].split("-")[2] + "T" + todayTimeStamp[1].split("\\.")[0] + "Z";
                // get latest commit sha\
                String url = github_api + comparedFork + "/commits?until=" + previousTimeStamp + "access_token=" + publicToken;

                fork_commit_jsonObj = new JSONObject(jsonUtility.readUrl(url));

                previousCommitSHA = fork_commit_jsonObj.getString("sha");


            } else if (timeWindowSize.contains("withUpstream")) {

                JSONObject upstreamInfo = (JSONObject) fork_jsonObj.get("parent");
                comparedFork = upstreamInfo.getString("full_name");
                defaultBranch = upstreamInfo.get("default_branch").toString();
                forkBranchName = fork_jsonObj.get("default_branch").toString();

            }

            String diffURL;
            if (timeWindowSize.contains("withUpstream")) {
                diffURL = github_page + comparedFork + "/compare/" + defaultBranch + "..." + forkName.split(FS)[0] + ":" + forkBranchName;
            } else {
                diffURL = github_page + comparedFork + "/compare/" + previousCommitSHA + "..." + forkName.split(FS)[0] + ":" + latestCommitSHA;
            }
//            diffURL+="?w=1";
            System.out.println(diffURL);
            new ProcessingText().rewriteFile(diffURL, analysisDir + "diffurl.txt");
            System.out.println("diff url:" + diffURL);
            return diffURL;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
