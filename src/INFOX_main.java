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


    /**
     * Main method for testing the INFOX method
     *
     * @param args
     */
    public static void main(String[] args) {
        /*** user input***/
        String[] forkListArray = null;
        try {
            forkListArray = new ProcessingText().readResult("/Users/shuruiz/Work/GithubProject/forklist.txt").split("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Rengine re = new Rengine(new String[]{"--vanilla"}, false, null);
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return ;
        }
        for (String forkName : forkListArray) {
//            String forkName = "TsinghuaDatabaseGroup/VTree";
//        String forkName = "AdeDZY/ShardFeature";
            boolean hasGroundTruth = false;
            String localSourceCodeDirPath = "/Users/shuruiz/Work/GithubProject/" + forkName + FS;
            String analysisDir = "/Users/shuruiz/Work/GithubProject/" + forkName + FS + "INFOX_output/";
            int max_numberOfCut = 3;
            int numberOfBiggestClusters = 5;

            /***git clone repo to local dir***/
            JgitUtility jgitUtility = new JgitUtility();
            String uri = github_page + forkName + ".git";
            System.out.println("Cloning repo from github: " + forkName);
            jgitUtility.cloneRepo(uri, localSourceCodeDirPath);
            if (forkName.contains("Marlin")) {
                try {
                    FileUtils.deleteDirectory(new File(localSourceCodeDirPath + "ArduinoAddons"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /***  get origin diff github page  ***/
            ParseHtml parseHtml = new ParseHtml(max_numberOfCut, numberOfBiggestClusters, analysisDir);
        String diffPageUrl = parseHtml.getDiffPageUrl(localSourceCodeDirPath,forkName,"withUpstream");
            System.out.println(diffPageUrl);
//        String diffPageUrl = "https://github.com/TsinghuaDatabaseGroup/VTree/compare/13556037a20baa1a3928224ff7c087102a5bba8c...TsinghuaDatabaseGroup:7e6516cac56ffdd3145b82c94db8c0f0ce48dca5";


            ProcessingText processingText = new ProcessingText();
            processingText.ReadTextFromURL(diffPageUrl + ".diff", localSourceCodeDirPath + "INFOX_output/diff.txt");

            /***   get fork added node, generate ForkAddedNode.txt file   ***/
            GithubRepoAnalysis githubRepoAnalysis = new GithubRepoAnalysis();
            HashMap<String, ArrayList<Integer>> changedFile_line_map = githubRepoAnalysis.getChangedCodeForGithubRepo(localSourceCodeDirPath + "INFOX_output/diff.txt");
            githubRepoAnalysis.generateForkAddedNodeFile(changedFile_line_map, localSourceCodeDirPath + "INFOX_output/forkAddedNode.txt");



            /*** start clustering code  ***/
            ClusterCodeChanges clusterCodeChanges = new ClusterCodeChanges(max_numberOfCut, numberOfBiggestClusters);
            clusterCodeChanges.clusteringChangedCodeFromFork(localSourceCodeDirPath, hasGroundTruth,re);


            /*** hack github page   ***/
            parseHtml.getOriginalDiffPage(diffPageUrl, localSourceCodeDirPath);
            parseHtml.generateMocGithubForkPage(forkName, localSourceCodeDirPath);
        }

    }
}
