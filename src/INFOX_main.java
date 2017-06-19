import ClusteringCodeChanges.ClusterCodeChanges;
import GetCodeChanges.GithubRepoAnalysis;
import MocGithub.ParseHtml;
import Util.JgitUtility;
import Util.ProcessingText;
import org.apache.commons.io.FileUtils;
import org.rosuda.JRI.Rengine;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by shuruiz on 4/7/17.
 */
public class INFOX_main {
    static final String FS = File.separator;
    static String github_api = "https://api.github.com/repos/";
    static String github_page = "https://github.com/";

    static String current_OS = System.getProperty("os.name").toLowerCase();
    static String tmpXmlPath = "tmpXMLFile" + FS;
    static String Root_Dir = "";

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
//        String filePath = args[0];
        String filePath = "test_param.txt";
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
//        String forkName = "infinit/grpc";
//        String forkName = "MarlinFirmware/Marlin";
        String branchName = "";
        boolean hasGroundTruth = false;
        String testCasesDir;

        if (current_OS.indexOf("mac") >= 0) {
            testCasesDir = "/Users/shuruiz/Work/GithubProject/";
        } else {
            testCasesDir = "/home/feature/shuruiz/INFOX_testCases/";
        }

        File dir = new File(testCasesDir + "/" + forkName);
        if (dir.exists()) {
            continue;
        }


        Root_Dir = new ProcessingText().getRootDir();
        String localSourceCodeDirPath = testCasesDir + forkName + FS;
        String analysisDir = testCasesDir + forkName + FS + "INFOX_output/";

        File finishFile = new File(analysisDir + "done.txt");

        if (!finishFile.exists()) {

//            File file = new File(localSourceCodeDirPath);
//
//            if (!file.exists()) {
//
//                /***git clone repo to local dir***/
//                JgitUtility jgitUtility = new JgitUtility();
//                String uri = github_page + forkName + ".git";
//                System.out.println("Cloning repo from github: " + forkName + " to " + testCasesDir);
//
//                jgitUtility.cloneRepo(uri, localSourceCodeDirPath, branchName);
//                if (forkName.contains("Marlin")) {
//                    try {
//                        FileUtils.deleteDirectory(new File(localSourceCodeDirPath + "ArduinoAddons"));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            /***  get origin diff github page  ***/
//            ParseHtml parseHtml = new ParseHtml(max_numberOfCut, numberOfBiggestClusters, analysisDir, publicToken);
//            String diffPageUrl = parseHtml.getDiffPageUrl(localSourceCodeDirPath, forkName, timeWindow);
////            String diffPageUrl = "https://github.com/grpc/grpc/compare/master...infinit:master";
////            String diffPageUrl = "https://github.com/cruwaller/Marlin/compare/max318xx_dev...MarlinFirmware:1.1.x";
//            System.out.println(diffPageUrl);
//
//            ProcessingText processingText = new ProcessingText();
//            processingText.ReadTextFromURL(diffPageUrl + ".diff?w=1", localSourceCodeDirPath + "INFOX_output/diff.txt");
//
//            /***   get fork added node, generate ForkAddedNode.txt file   ***/
//            GithubRepoAnalysis githubRepoAnalysis = new GithubRepoAnalysis();
//            HashMap<String, ArrayList<Integer>> changedFile_line_map = githubRepoAnalysis.getChangedCodeForGithubRepo(localSourceCodeDirPath + "INFOX_output/diff.txt");
//            githubRepoAnalysis.generateForkAddedNodeFile(changedFile_line_map, localSourceCodeDirPath + "INFOX_output/forkAddedNode.txt");
//
//
//            /*** start clustering code  ***/
            ClusterCodeChanges clusterCodeChanges = new ClusterCodeChanges(max_numberOfCut, numberOfBiggestClusters);
            clusterCodeChanges.clusteringChangedCodeFromFork(localSourceCodeDirPath, hasGroundTruth, re, minimumClusterSize);
//            try {
//                processingText.deleteDir(new File(Root_Dir + tmpXmlPath));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//
//            /*** hack github page   ***/
//            parseHtml.getOriginalDiffPage(diffPageUrl, localSourceCodeDirPath);
//            parseHtml.generateMocGithubForkPage(forkName, localSourceCodeDirPath);
        }
    }

}


}
