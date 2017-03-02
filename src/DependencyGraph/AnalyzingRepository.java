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
    public void analyzeRepository(String sourcecodeDir, String analysisDirName, String testCaseDir, int[] parameters, Rengine re) {

        boolean isMS_CLUSTERCHANGES;

        /**   Set parameters   **/
        /** param[5]
         *  1 -- INFOX,
         *  2--MS,
         *  3--MS+CF+HIE (NO spliting, joining),
         *  4--INFOX-(DEF_USE)
         *  5--INFOX-(CONTROL_FLOW),
         *  6--INFOX-(HIERARCHY)
         *  7--INFOX-(Consecutive)
         *  8--MS-(Consecutive)
         *  **/
        int numOfCuts = parameters[1];
        if (parameters[5] == 2 || parameters[5] == 3 || parameters[5] == 8) {
            numOfCuts = 0;
            isMS_CLUSTERCHANGES = true;
        } else {
            isMS_CLUSTERCHANGES = false;
        }


        boolean directedGraph = parameters[4] == 1 ? true : false;
        String testDir = "";
        for (int index = 0; index <= 4; index++) {
            testDir += parameters[index];
        }

        String analysisDir = testCaseDir + testDir + FS;
        System.out.println("~~~~~~~current configuration: " + testDir + "~~");
        new File(analysisDir).mkdir();

        /**  Generating Dependency Graphs for current test case/project  **/
        if (!directedGraph) {
            DependencyGraph dependencyGraph = new DependencyGraph(parameters[5]);
            /**  this function extract changed_code_dependency_graph from complete graph**/
            dependencyGraph.generateChangedDependencyGraphFromCompleteGraph(sourcecodeDir, analysisDirName, testCaseDir, testDir, re);

            /**  this function generate all the graph at the same time **/
//            dependencyGraph.getDependencyGraphForProject(sourcecodeDir, testCaseDir, testDir);
        }
//
        /** Community Detection  **/
        boolean hasEdge = new R_CommunityDetection().detectingCommunitiesWithIgraph(sourcecodeDir, analysisDirName, testCaseDir, testDir, numOfCuts, re, directedGraph);

        if (hasEdge) {
//        if (hasEdge) {
            /** Generating html to visualize source code, set background and left side bar color for new code  **/
            AnalyzingCommunityDetectionResult analyzingCommunityDetectionResult = new AnalyzingCommunityDetectionResult(sourcecodeDir, testCaseDir, testDir, isMS_CLUSTERCHANGES);


            int[] avgFeatureSize_maxSize = analyzingCommunityDetectionResult.generateGroundTruthMap();
            HashMap<Integer, ArrayList<String>> clusterList;
            if (!isMS_CLUSTERCHANGES) {
//                int avgFeatureSize = avgFeatureSize_maxSize[0];
//                int maxFeatureSize = avgFeatureSize_maxSize[1];
                int maxFeatureSize = 100;
//                int clusterSizeThreshold = avgFeatureSize / 2;
                int clusterSizeThreshold = 50;
                System.out.println("clusterSizeThreshold: " + clusterSizeThreshold);
//                while (clusterSizeThreshold < maxFeatureSize) {
//
//                   clusterList = analyzingCommunityDetectionResult.parseEachUsefulClusteringResult(clusterSizeThreshold);
//                    clusterSizeThreshold += 10;
//
//                }
                clusterList = analyzingCommunityDetectionResult.parseEachUsefulClusteringResult(clusterSizeThreshold);

            } else {
                clusterList = analyzingCommunityDetectionResult.parseEachUsefulClusteringResult(0);
            }
//        new Tokenizer().tokenizeSourceCode(sourcecodeDir, testCaseDir);

        /** parse commit msg for each node **/
//        new GetCommitMsg(testCaseDir, testDir, clusterList,1);
//        new GetCommitMsg(testCaseDir, testDir, clusterList,2);


            /**  calculate tfidf  to identifing keywords from each cluster**/
//        IdentifyingKeyWordForCluster identifyingKeyWordForCluster = new IdentifyingKeyWordForCluster();
//
//        identifyingKeyWordForCluster.findKeyWordsForEachCut(testCaseDir, testDir, clusterList, 1);
//        identifyingKeyWordForCluster.findKeyWordsForEachCut(testCaseDir, testDir, clusterList, 2);

        }
    }
}
