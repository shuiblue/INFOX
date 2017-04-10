package MocGithub;


import CommunityDetection.AnalyzingCommunityDetectionResult;
import Util.JgitUtility;
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
import java.util.ArrayList;

/**
 * Created by shuruiz on 4/5/17.
 */
public class ParseHtml {
    static final String FS = File.separator;
    static String github_api = "https://api.github.com/repos/";
    static String github_page = "https://github.com/";
    Document doc, currentDoc;
    String analysisDir = "";
    String originalPage = "zzz.html";

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
            page = webClient.getPage(diffPageUrl);
        } catch (Exception e) {
            System.out.println("Get page error");
        }
        webClient.waitForBackgroundJavaScriptStartingBefore(200);
        webClient.waitForBackgroundJavaScript(50000);


        new ProcessingText().rewriteFile(page.asXml(), analysisDir + originalPage);
        new ProcessingText().rewriteFile(page.getWebResponse().getContentAsString(), analysisDir + "html.txt");
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
            Elements fileList_elements = doc.getElementsByClass("file-header");
            generateHtml(fileList_elements);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    Jsoup.parse( new WebClient(BrowserVersion.CHROME).getPage("http://www.manning.com")
//                        .asInstanceOf[HtmlPage].asXml ).select("div.dotdbox").text


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
    private void generateHtml(Elements fileList_elements) {
        currentDoc = doc;
        ProcessingText pt = new ProcessingText();
        ArrayList<ArrayList<Integer>> combinationArrayList = new AnalyzingCommunityDetectionResult().getCombinations();
        //todo ,+space



        for (ArrayList<Integer> com : combinationArrayList) {
            String splitStep = com.toString().replace("[", "").replace("]", "").replace(", ", "_");
            if (!splitStep.equals("1_1")) {
                try {
                    String[] sourceCode_to_color_array = pt.readResult(analysisDir + splitStep + "_colorTable.txt").split("\n");
                    modifyEachLine(sourceCode_to_color_array, com);
                    pt.rewriteFile(currentDoc.toString(), analysisDir + splitStep + ".html");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    /**
     * This function modify each line, add id
     *
     * @param sourceCode_to_color_array
     * @param com
     */
    private void modifyEachLine(String[] sourceCode_to_color_array, ArrayList<Integer> com) {
        ProcessingText pt = new ProcessingText();
        for (String str : sourceCode_to_color_array) {
            String[] map = str.split(",");
            String tmpstr[] = map[0].split("-");

            String fileName = pt.getOriginFileName(map[0]);
            String lineNumber = tmpstr[1];
            String color = map[1];

            System.out.println(str);
            doc.getElementsByAttributeValue("data-path", fileName);
            Element currentFile = doc.getElementsByAttributeValue("data-path", fileName).next().first();
            if (!currentFile.toString().contains("Load diff")) {
                currentFile.getElementsByClass("blob-num blob-num-empty empty-cell").remove();
                currentFile.getElementsByClass("blob-code blob-code-empty empty-cell").remove();
                Elements lineElements = currentFile.getElementsByAttributeValue("data-line-number", lineNumber);
                Element lineElement;
                if (lineElements.size() > 1) {
                    lineElement = lineElements.get(1);
                } else {
                    lineElement = lineElements.first();
                }
                lineElement.siblingElements().get(1).attr("bgcolor", "#" + color).attr("class", map[0]);
            } else {
                System.out.println(fileName + " is bigger than the github diff limits..");


            }

        }


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
            return github_page + upstreamName + "/compare/" + SHA_beforeForkCreated + "..." + forkName.split(FS)[0] + ":" + latestCommitSHA + "#files_bucket";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void main(String[] args) {
        String forkName = "malx122/Marlin";

        //get origin diff github page
        ParseHtml parseHtml = new ParseHtml();
        String diffPageUrl = parseHtml.getDiffPageUrl(forkName);

        //git clone repo to local dir
        JgitUtility jgitUtility = new JgitUtility();
        String uri = github_page + forkName + ".git";
        String localDirPath = "/Users/shuruiz/Work/GithubProject/" + forkName;
        jgitUtility.cloneRepo(uri, localDirPath);


        // hack github page
//        parseHtml.generateMocGithubForkPage(diffPageUrl, forkName);
    }
}
