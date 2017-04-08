import ClusteringCodeChanges.ClusterCodeChanges;
import MocGithub.ParseHtml;
import Util.JgitUtility;
import Util.ProcessingText;
import getCodeChanges.GithubRepoAnalysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by shuruiz on 4/7/17.
 */
public class INFOX_main {
    static final String FS = File.separator;
    static String github_api = "https://api.github.com/repos/";
    static String github_page = "https://github.com/";


    /**
     * Main method for testing the INFOX method
     *
     * @param args
     */
    public static void main(String[] args) {
        /*** user input***/
        String forkName = "malx122/Marlin";
        boolean hasGroundTruth = false;
        String localSourceCodeDirPath = "/Users/shuruiz/Work/GithubProject/" + forkName+FS;
//
//        /***  get origin diff github page  ***/
//        ParseHtml parseHtml = new ParseHtml();
//        String diffPageUrl = parseHtml.getDiffPageUrl(forkName);
//
//        ProcessingText processingText = new ProcessingText();
//        processingText.ReadTextFromURL(diffPageUrl+".diff",localSourceCodeDirPath+"INFOX_output/diff.txt");
////
//        /***git clone repo to local dir***/
//        JgitUtility jgitUtility = new JgitUtility();
//        String uri = github_page + forkName + ".git";
//          jgitUtility.cloneRepo(uri, localSourceCodeDirPath);

        /***   get fork added node, generate ForkAddedNode.txt file   ***/
//        GithubRepoAnalysis githubRepoAnalysis = new GithubRepoAnalysis();
//        HashMap<String, ArrayList<Integer>> changedFile_line_map = githubRepoAnalysis.getChangedCodeForGithubRepo(localSourceCodeDirPath+"INFOX_output/diff.txt");
//        githubRepoAnalysis.generateForkAddedNodeFile(changedFile_line_map,localSourceCodeDirPath+"INFOX_output/forkAddedNode.txt");



        /*** start clustering code  ***/
        ClusterCodeChanges clusterCodeChanges = new ClusterCodeChanges();
        clusterCodeChanges.clusteringChangedCodeFromFork(localSourceCodeDirPath, hasGroundTruth);


        /*** hack github page   ***/
//        parseHtml.generateMocGithubForkPage(diffPageUrl, forkName);


    }
}
