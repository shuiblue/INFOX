package DependencyGraph;

import ColorCode.ColorCode;
import CommunityDetection.R_CommunityDetection;
//import Similarity.StringSimilarity;
import Util.GetForkAddedCode;
import org.rosuda.JRI.Rengine;

/**
 * Created by shuruiz on 8/29/16.
 */
public class AnalyzingRepository {

    /**
     * This function analyzes the source code repository. There are several steps:
     * 1. Identify lines of code wrapped with target macros(identifyIfdefs}
     * 2. create dependency graph for the repository {(getDependencyGraphForProject}
     * 3. calculating similarity of statement
     * 4. community detection
     * 5. Generating html file to visualize code clusters.
     *
     * @param sourcecodeDir
     * @param analysisDir
     * @param parameters    1. numOfTargetMacro
     *                      2. numberOfCuts
     *                      3. Ground Truth : IFDEF / REAL  (1-- ifdef, 0 --- Real)
     *                      4. Consecutive lines: T/F  (1/0)
     *                      5. Directed Graph: T/F (1/0)
     * @param re
     */
    public void analyzeRepository(String sourcecodeDir, String analysisDir, int[] parameters, Rengine re) {
        /**   Set parameters   **/

        int numOfTargetMacro = parameters[0];
        int numOfCuts = parameters[1];
        String GroundTruth = parameters[2] == 1 ? "IFDEF" : "REAL";
        boolean createEdgeForConsecutiveLines = parameters[3] == 1 ? true : false;
        boolean directedGraph= parameters[4] == 1 ? true : false;

        if (GroundTruth.equals("IFDEF")) {
            /**  get targetMacroList  **/
            new GetForkAddedCode().selectTargetMacros(sourcecodeDir, analysisDir, numOfTargetMacro);
        } else if (GroundTruth.equals("REAL")) {
                                /* todo: for real feature related cases
                                targetFeatureList =...
                                */
        }


        /**  Generating Dependency Graphs for current test case/project  **/
        DependencyGraph dependencyGraph = new DependencyGraph();
        dependencyGraph.getDependencyGraphForProject(sourcecodeDir, analysisDir,createEdgeForConsecutiveLines);


        /*------------------calculating similarity--------------------------
        StringSimilarity strSim = new StringSimilarity();
        strSim.calculateStringSimilarityByR(sourcecodeDir, analysisDir, re);*/

        /** Community Detection  **/
        new R_CommunityDetection().detectingCommunitiesWithIgraph(analysisDir, numOfCuts, re,directedGraph);

        /** Generating html to visualize source code, set background and left side bar color for new code  **/
        new ColorCode().parseEachUsefulClusteringResult(sourcecodeDir, analysisDir);
        /* FOR EMAIL SYSTEM*/
//        colorCodeBlocks.parseEachUsefulClusteringResult(projectPath, repo, dirNum, 3);
    }
}
