package MocGithub;


import Util.JsonUtility;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;

/**
 * Created by shuruiz on 4/5/17.
 */
public class ParseHtml {
    static final String FS = File.separator;
    static String github_api = "https://api.github.com/repos/";
    static String github_page = "https://github.com/";

    public void generateMocGithubForkPage(String forkname) {
        Document doc_head, doc_body, doc_newPage;
        try {
            doc_head = Jsoup.connect("https://github.com/" + forkname).get();
            Element fork_title = doc_head.getElementsByClass("container repohead-details-container").first();

            doc_body = Jsoup.connect("https://github.com/MarlinFirmware/Marlin/pull/6245/files").get();
            //fork title , mentions fork which upstream
            doc_body.getElementsByClass("container repohead-details-container").first().replaceWith(fork_title);
            //remove
            Element sourceCodeBucket = doc_body.getElementById("files_bucket");
            sourceCodeBucket.getElementById("partial-discussion-header").remove();
            sourceCodeBucket.getElementsByClass("octicon octicon-comment-discussion").remove();

            //copy pr_tag and generate code change tag
            Elements tags = doc_body.getElementsByClass("js-selected-navigation-item selected reponav-item");
            Element pr_tag = tags.first();
            pr_tag.getElementsByTag("svg").remove();
            pr_tag.getElementsByTag("path").remove();
            pr_tag.getElementsByClass("counter").remove();
            String codeChangeElement = pr_tag.toString().replaceFirst("Pull\\srequests", "Code changes");
            tags.append(codeChangeElement);
            doc_body.getElementsByClass("tabnav-tabs").get(0).getElementsByClass("tabnav-tab  js-pjax-history-navigate").get(0).remove();
            doc_body.getElementsByClass("BtnGroup").remove();

            System.out.print("");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getDiffPage(String forkName) {
        String forkUrl = github_api + forkName;
        JsonUtility jsonUtility = new JsonUtility();
        try {
            // get latest commit sha
            String[] todayTimeStamp = new Timestamp(System.currentTimeMillis()).toString().split(" ");
            String format_current_time = todayTimeStamp[0] + "T" + todayTimeStamp[1].split("\\.")[0] + "Z";
            JSONObject fork_commit_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + forkName + "/commits?until=") + todayTimeStamp);
            String latestCommitSHA = fork_commit_jsonObj.getString("sha");

            //get general fork information
            JSONObject fork_jsonObj = new JSONObject(jsonUtility.readUrl(forkUrl));
            //get commit before fork was created
            String firstCommitTimeStamp = fork_jsonObj.getString("created_at");
            JSONObject upstreamInfo = (JSONObject) fork_jsonObj.get("parent");
            String upstreamName = upstreamInfo.getString("full_name");
            JSONObject upstream_jsonObj = new JSONObject(jsonUtility.readUrl(github_api + upstreamName + "/commits?until=" + firstCommitTimeStamp));
            String SHA_beforeForkCreated = upstream_jsonObj.getString("sha");
            System.out.println(firstCommitTimeStamp);

            String diff_url = github_page + upstreamName + "/compare/" + SHA_beforeForkCreated + "..." + forkName.split(FS)[0] + ":" + latestCommitSHA;

            System.out.print("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String forkName = "malx122/Marlin";

        ParseHtml parseHtml = new ParseHtml();
        parseHtml.getDiffPage(forkName);

//        parseHtml.generateMocGithubForkPage(forkName);
    }
}
