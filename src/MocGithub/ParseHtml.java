package MocGithub;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * Created by shuruiz on 4/5/17.
 */
public class ParseHtml {

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


            System.out.print("");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        String forkName = "malx122/Marlin";

        ParseHtml parseHtml = new ParseHtml();
        parseHtml.generateMocGithubForkPage(forkName);
    }
}
