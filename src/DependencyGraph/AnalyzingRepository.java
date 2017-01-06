package DependencyGraph;

import ColorCode.ColorCode;
import CommunityDetection.AnalyzingCommunityDetectionResult;
import CommunityDetection.R_CommunityDetection;
import NamingClusters.GetCommitMsg;
import NamingClusters.IdentifyingKeyWordForCluster;
import NamingClusters.Tokenizer;
import org.rosuda.JRI.Rengine;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by shuruiz on 8/29/16.
 */
public class AnalyzingRepository {
    static final String FS = File.separator;

    /**
     * This function analyzes the source code repository. There are several steps:
     * 1. Identify lines of code wrapped with target macros(identifyIfdefs}
     * 2. create dependency graph for the repository {(getDependencyGraphForProject}
     * 3. calculating similarity of statement
     * 4. community detection
     * 5. Generating html file to visualize code clusters.
     *
     * @param sourcecodeDir
     * @param testCaseDir
     * @param parameters    1. numOfTargetMacro
     *                      2. numberOfCuts
     *                      3. Ground Truth : IFDEF / REAL  (1-- ifdef, 0 --- Real)
     *                      4. Consecutive lines: T/F  (1/0)
     *                      5. Directed Graph: T/F (1/0)
     * @param re
     */
    public void analyzeRepository(String sourcecodeDir, String testCaseDir, int[] parameters, Rengine re) {
        /**   Set parameters   **/
        int numOfCuts = parameters[1];
        boolean createEdgeForConsecutiveLines = parameters[3] == 1 ? true : false;
        boolean directedGraph = parameters[4] == 1 ? true : false;


        String testDir = "";
        for (int index = 0; index <= 4; index++) {
            testDir += parameters[index];
        }

        String analysisDir = testCaseDir + testDir + FS;
        System.out.println("~~~~~~~current configuration: " + testDir + "~~");
        new File(analysisDir).mkdir();

        /**  Generating Dependency Graphs for current test case/project  **/
//        if (!directedGraph) {
//            DependencyGraph dependencyGraph = new DependencyGraph();
//            dependencyGraph.getDependencyGraphForProject(sourcecodeDir, testCaseDir, testDir, createEdgeForConsecutiveLines);
//        }
//
//        /** Community Detection  **/
//        new R_CommunityDetection().detectingCommunitiesWithIgraph(testCaseDir, testDir, numOfCuts, re, directedGraph);

        /** Generating html to visualize source code, set background and left side bar color for new code  **/
//        HashMap<Integer, ArrayList<String>> clusterList = new ColorCode().parseEachUsefulClusteringResult(sourcecodeDir, testCaseDir, testDir);
        HashMap<Integer, ArrayList<String>> clusterList = new AnalyzingCommunityDetectionResult().parseEachUsefulClusteringResult(sourcecodeDir, testCaseDir, testDir);
        new Tokenizer().tokenizeSourceCode(sourcecodeDir, testCaseDir);

        /** parse commit msg for each node **/
        new GetCommitMsg(testCaseDir, testDir, clusterList,1);
        new GetCommitMsg(testCaseDir, testDir, clusterList,2);



        /**  calculate tfidf  to identifing keywords from each cluster**/
        IdentifyingKeyWordForCluster identifyingKeyWordForCluster = new IdentifyingKeyWordForCluster();

        identifyingKeyWordForCluster.findKeyWordsForEachCut(testCaseDir, testDir, clusterList, 1);
        identifyingKeyWordForCluster.findKeyWordsForEachCut(testCaseDir, testDir, clusterList, 2);


    }
}
