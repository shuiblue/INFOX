package DependencyGraph;

import ColorCode.ColorCode;
import CommunityDetection.AnalyzingCommunityDetectionResult;
import CommunityDetection.R_CommunityDetection;
import MocGithub.ParseHtml;
import NamingClusters.GetCommitMsg;
import NamingClusters.IdentifyingKeyWordForCluster;
import NamingClusters.Tokenizer;
import Util.GenerateCombination;
import org.rosuda.JRI.Rengine;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by shuruiz on 8/29/16.
 */
public class AnalyzingRepository {
    static final String FS = File.separator;
    String repoPath = "";
    String testDir = "";

    //todo user input
    int max_numberOfCut;
    int numberOfBiggestClusters;

    public void setNumberOfCut_numberOfBiggestClusters(int max_numberOfCut, int numberOfBiggestClusters) {
        this.max_numberOfCut = max_numberOfCut;
        this.numberOfBiggestClusters = numberOfBiggestClusters;
    }


    public void analyzeRepository(String sourcecodeDir, String analysisDirName, String testCaseDir, int approachIndex, Rengine re, boolean hasGroundTruth, String repoPath) {
        boolean isMS_CLUSTERCHANGES;

        boolean directedGraph = false;
        if (approachIndex == 2 || approachIndex == 3 || approachIndex == 8) {
            isMS_CLUSTERCHANGES = true;
        } else {
            isMS_CLUSTERCHANGES = false;
        }
        this.repoPath = repoPath;
        executeINFOX(sourcecodeDir, analysisDirName, testCaseDir, approachIndex, re, isMS_CLUSTERCHANGES, max_numberOfCut, directedGraph, testDir, hasGroundTruth);
    }

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
    public void analyzeRepository(String sourcecodeDir, String analysisDirName, String testCaseDir, int[] parameters, Rengine re, boolean hasGroundTruth) {

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
        executeINFOX(sourcecodeDir, analysisDirName, testCaseDir, parameters[5], re, isMS_CLUSTERCHANGES, numOfCuts, directedGraph, testDir, hasGroundTruth);
    }


    private void executeINFOX(String sourcecodeDir, String analysisDirName, String testCaseDir, int approachIndex, Rengine re, boolean isMS_CLUSTERCHANGES, int max_numberOfCut, boolean directedGraph, String testDir, boolean hasGroundTruth) {
//        if (!directedGraph) {
//            DependencyGraph dependencyGraph = new DependencyGraph(approachIndex);
//            /**  this function extract changed_code_dependency_graph from complete graph**/
////            dependencyGraph.generateChangedDependencyGraphFromCompleteGraph(sourcecodeDir, analysisDirName, testCaseDir, testDir, re);
//
//            /**  this function generate all the graph at the same time **/
//            dependencyGraph.getDependencyGraphForProject(sourcecodeDir, testCaseDir, testDir);
//        }
//
//
//        /** Community Detection  **/
//        R_CommunityDetection communityDetection = new R_CommunityDetection(sourcecodeDir, analysisDirName, testCaseDir, testDir, re,max_numberOfCut,numberOfBiggestClusters);
//        communityDetection.clustering_CodeChanges(testCaseDir, testDir, re, directedGraph,true,"original");
//
//        /** Generating html to visualize source code, set background and left side bar color for new code  **/
//        AnalyzingCommunityDetectionResult analyzingCommunityDetectionResult = new AnalyzingCommunityDetectionResult(sourcecodeDir, testCaseDir, testDir, isMS_CLUSTERCHANGES, max_numberOfCut, numberOfBiggestClusters);
//        int[] avgFeatureSize_maxSize = null;
//        /** get nodeMap: id -- lable  **/
//        analyzingCommunityDetectionResult.getNodeMap_id_to_label();
//
//        /**   if this test has ground Truth, then generate the ground Truth map for later comparing **/
//        if (hasGroundTruth) {
//            avgFeatureSize_maxSize = analyzingCommunityDetectionResult.generateGroundTruthMap();
//        }
//
//        HashMap<Integer, ArrayList<String>> clusterList = null;
//        String clusterFile = "clusterTMP.txt";
//        if (!isMS_CLUSTERCHANGES) {
//            //Marlin - 50
//            //Cherokee - 38
//            int clusterSizeThreshold = 50;
//            if (hasGroundTruth) {
//                int avgFeatureSize = avgFeatureSize_maxSize[0];
//                int maxFeatureSize = avgFeatureSize_maxSize[1];
//            }
//            /** starts to analyze each clustering result  ***/
//            // for original only one clustering result
////                clusterList = analyzingCommunityDetectionResult.parseEachUsefulClusteringResult(clusterSizeThreshold, hasGroundTruth,clusterFile);
//
//            /** for  *new INFOX*, --- splitting clusters one by one  **/
//            System.out.println("     starting to analyzing community detection result...");
//            analyzingCommunityDetectionResult.generateClusteringResult(max_numberOfCut, numberOfBiggestClusters);
//        } else { /**  MS approach**/
//            clusterList = analyzingCommunityDetectionResult.parseEachUsefulClusteringResult(0, hasGroundTruth, clusterFile, true);
//        }

        ArrayList<String> combination_list = new ParseHtml(max_numberOfCut, numberOfBiggestClusters, testCaseDir).generateAllCombineResult(testCaseDir, max_numberOfCut);

        for (String splitStep : combination_list) {
            System.out.println("identifying keywords for Splitting Steps ..: "+splitStep);
            IdentifyingKeyWordForCluster identifyingKeyWordForCluster = new IdentifyingKeyWordForCluster();
            identifyingKeyWordForCluster.findKeyWordForEachSplit(sourcecodeDir, testCaseDir, testDir, splitStep, repoPath);
        }


    }
}
