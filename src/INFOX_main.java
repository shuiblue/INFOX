import ClusteringCodeChanges.ClusterCodeChanges;
import GetCodeChanges.GithubRepoAnalysis;
import MocGithub.ParseHtml;
import Util.JgitUtility;
import Util.ProcessingText;
import org.apache.commons.io.FileUtils;
import org.rosuda.JRI.Rengine;

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

    static String OS = System.getProperty("os.name").toLowerCase();

    /**
     * Main method for testing the INFOX method
     * String args[]={
     * <p>
     * int max_numberOfCut = 3;
     * int numberOfBiggestClusters = 5;
     * }
     *
     * @param args
     */
    public static void main(String[] args) {
        String filePath = args[0];
        String[] experimentParameters = new String[7];
        try {
            experimentParameters = new ProcessingText().readResult("./testCases/" + filePath).split("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        int max_numberOfCut = Integer.valueOf(experimentParameters[0]);
        int numberOfBiggestClusters = Integer.valueOf(experimentParameters[1]);
        int minimumClusterSize = Integer.valueOf(experimentParameters[2]);
        String publicToken = experimentParameters[3];
        String forkListFile = experimentParameters[4];
        String timeWindow = experimentParameters[5];

        /*** user input***/
        String[] forkListArray = null;
        try {
            forkListArray = new ProcessingText().readResult("./testCases/" + forkListFile).split("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Rengine re = new Rengine(new String[]{"--vanilla"}, false, null);
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }
        for (String forkName : forkListArray) {
            boolean hasGroundTruth = false;
            String testCasesDir;

            if (OS.indexOf("mac") >= 0) {
                testCasesDir = "/Users/shuruiz/Work/GithubProject/";
            } else {
                testCasesDir = "/home/feature/INFOX_testCases/";
            }

            String localSourceCodeDirPath = testCasesDir + forkName + FS;
            String analysisDir = testCasesDir + forkName + FS + "INFOX_output/";


            /***git clone repo to local dir***/
            JgitUtility jgitUtility = new JgitUtility();
            String uri = github_page + forkName + ".git";
            System.out.println("Cloning repo from github: " + forkName + " to " + testCasesDir);
            jgitUtility.cloneRepo(uri, localSourceCodeDirPath);
            if (forkName.contains("Marlin")) {
                try {
                    FileUtils.deleteDirectory(new File(localSourceCodeDirPath + "ArduinoAddons"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /***  get origin diff github page  ***/
            ParseHtml parseHtml = new ParseHtml(max_numberOfCut, numberOfBiggestClusters, analysisDir, publicToken);
            String diffPageUrl = parseHtml.getDiffPageUrl(localSourceCodeDirPath, forkName, timeWindow);
//        String diffPageUrl = parseHtml.getDiffPageUrl(localSourceCodeDirPath, forkName, "3 month", isPublicRepo);
            System.out.println(diffPageUrl);
//          String diffPageUrl = "https://github.com/TsinghuaDatabaseGroup/VTree/compare/13556037a20baa1a3928224ff7c087102a5bba8c...TsinghuaDatabaseGroup:7e6516cac56ffdd3145b82c94db8c0f0ce48dca5";


            ProcessingText processingText = new ProcessingText();
            processingText.ReadTextFromURL(diffPageUrl + ".diff", localSourceCodeDirPath + "INFOX_output/diff.txt");

            /***   get fork added node, generate ForkAddedNode.txt file   ***/
            GithubRepoAnalysis githubRepoAnalysis = new GithubRepoAnalysis();
            HashMap<String, ArrayList<Integer>> changedFile_line_map = githubRepoAnalysis.getChangedCodeForGithubRepo(localSourceCodeDirPath + "INFOX_output/diff.txt");
            githubRepoAnalysis.generateForkAddedNodeFile(changedFile_line_map, localSourceCodeDirPath + "INFOX_output/forkAddedNode.txt");


            /*** start clustering code  ***/
            ClusterCodeChanges clusterCodeChanges = new ClusterCodeChanges(max_numberOfCut, numberOfBiggestClusters);
            clusterCodeChanges.clusteringChangedCodeFromFork(localSourceCodeDirPath, hasGroundTruth, re, minimumClusterSize);


            /*** hack github page   ***/
            parseHtml.getOriginalDiffPage(diffPageUrl, localSourceCodeDirPath);
            parseHtml.generateMocGithubForkPage(forkName, localSourceCodeDirPath);
        }

    }


}
