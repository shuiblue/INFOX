import ClusteringCodeChanges.ClusterCodeChanges;
import GetCodeChanges.GithubRepoAnalysis;
import MocGithub.ParseHtml;
import Util.JgitUtility;
import Util.ProcessingText;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
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
        String forkName = "AdeDZY/ShardFeature";
//        String forkName = "malx122/Marlin";
//        String forkName = "Ultimaker/Marlin";
        boolean hasGroundTruth = false;
        String localSourceCodeDirPath = "/Users/shuruiz/Work/GithubProject/" + forkName+FS;
        String analysisDir = "/Users/shuruiz/Work/GithubProject/" + forkName+FS+"INFOX_output/";
        int max_numberOfCut = 4;
        int numberOfBiggestClusters = 5;

        /***git clone repo to local dir***/
//        JgitUtility jgitUtility = new JgitUtility();
//        String uri = github_page + forkName + ".git";
//        jgitUtility.cloneRepo(uri, localSourceCodeDirPath);
//        if(forkName.contains("Marlin")) {
//            try {
//                FileUtils.deleteDirectory(new File(localSourceCodeDirPath + "ArduinoAddons"));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        /***  get origin diff github page  ***/
        ParseHtml parseHtml = new ParseHtml(max_numberOfCut,numberOfBiggestClusters,analysisDir);
//        String diffPageUrl = parseHtml.getDiffPageUrl(forkName,"12 month");
       String diffPageUrl ="https://github.com/AdeDZY/ShardFeature/compare/9dc9cb7aa043010c89cef06d1031c834aef4825a...AdeDZY:3ff9d4d7404cb93a5e75e5cfe8b55336214c0bf9";
//
//        ProcessingText processingText = new ProcessingText();
//        processingText.ReadTextFromURL(diffPageUrl+".diff",localSourceCodeDirPath+"INFOX_output/diff.txt");
//
//        /***   get fork added node, generate ForkAddedNode.txt file   ***/
//        GithubRepoAnalysis githubRepoAnalysis = new GithubRepoAnalysis();
//        HashMap<String, ArrayList<Integer>> changedFile_line_map = githubRepoAnalysis.getChangedCodeForGithubRepo(localSourceCodeDirPath+"INFOX_output/diff.txt");
//        githubRepoAnalysis.generateForkAddedNodeFile(changedFile_line_map,localSourceCodeDirPath+"INFOX_output/forkAddedNode.txt");
//
//
//
//        /*** start clustering code  ***/
//        ClusterCodeChanges clusterCodeChanges = new ClusterCodeChanges(max_numberOfCut,numberOfBiggestClusters);
//        clusterCodeChanges.clusteringChangedCodeFromFork(localSourceCodeDirPath, hasGroundTruth);


        /*** hack github page   ***/
        parseHtml.getOriginalDiffPage(diffPageUrl,localSourceCodeDirPath);
        parseHtml.generateMocGithubForkPage(diffPageUrl, forkName,localSourceCodeDirPath);


    }
}
