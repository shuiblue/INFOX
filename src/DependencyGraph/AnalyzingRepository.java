package DependencyGraph;

import ColorCode.ColorCode;
import CommunityDetection.AnalyzingCommunityDetectionResult;
import CommunityDetection.R_CommunityDetection;
import NamingClusters.GetCommitMsg;
import NamingClusters.IdentifyingKeyWordForCluster;
import NamingClusters.Tokenizer;
import Util.ProcessingText;
import org.rosuda.JRI.Rengine;

import java.io.File;
import java.io.IOException;
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


            if (analysisDir.contains("3macros/1/")||analysisDir.contains("3macros_oneFile/1/")) {
                /**  this function generate all the graph at the same time **/
                dependencyGraph.getDependencyGraphForProject(sourcecodeDir, testCaseDir, testDir);
                String[] approaches = {"testINFOX", "testMS", "testMS_plus_CF_Hierachy", "testINFOX_NO_DefUse", "testINFOX_NO_ControlF", "testINFOX_NO_Hierarchy", "testINFOX_NO_Consec", "testMS_NO_Consec"};

                System.out.println("copy complete graph to root..");
                for (String app : approaches) {
                    try {
                        new ProcessingText().copyFolder(new File(testCaseDir + "complete.pajek.net"), new File(sourcecodeDir + app + "/complete.pajek.net"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                /**  this function extract changed_code_dependency_graph from complete graph**/
                dependencyGraph.generateChangedDependencyGraphFromCompleteGraph(sourcecodeDir, analysisDirName, testCaseDir, testDir, re);

            }

        }

        /** Community Detection  **/
        boolean hasEdge = new R_CommunityDetection().detectingCommunitiesWithIgraph(sourcecodeDir, analysisDirName, testCaseDir, testDir, numOfCuts, re, directedGraph);

        if (hasEdge) {
            /** Generating html to visualize source code, set background and left side bar color for new code  **/
            AnalyzingCommunityDetectionResult analyzingCommunityDetectionResult = new AnalyzingCommunityDetectionResult(sourcecodeDir, testCaseDir, testDir, isMS_CLUSTERCHANGES);

            analyzingCommunityDetectionResult.generateGroundTruthMap();
            if (!isMS_CLUSTERCHANGES) {
                int clusterSizeThreshold = 50;
                System.out.println("clusterSizeThreshold: " + clusterSizeThreshold);
                analyzingCommunityDetectionResult.parseEachUsefulClusteringResult(clusterSizeThreshold);

            } else {
                analyzingCommunityDetectionResult.parseEachUsefulClusteringResult(0);
            }

        }
    }
}
