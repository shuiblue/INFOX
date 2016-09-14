package DependencyGraph;

import ColorCode.ColorCode;
import CommunityDetection.R_CommunityDetection;
import Similarity.StringSimilarity;
import Util.GetForkAddedCode;
import org.rosuda.JRI.Rengine;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by shuruiz on 8/29/16.
 */
public class AnalyzingRepository {
    static String analysisDirName = "DPGraph";
    static final String FS = File.separator;

    /**
     * This function analyzes the source code repository. There are several steps:
     * 1. Identify lines of code wrapped with target macros(identifyIfdefs}
     * 2. create dependency graph for the repository {(getDependencyGraphForProject}
     * 3. calculating similarity of statement
     * 4. community detection
     * 5. Generating html file to visualize code clusters.
     * @param sourcecodeDir
     * @param analysisDir
     * @param commitList
     * @param macroList
     * @param numOfCuts
     * @param re
     */
    public void analyzeRepository(String sourcecodeDir, String analysisDir, ArrayList<String> commitList, ArrayList<String> macroList, int numOfCuts, Rengine re) {
        DependencyGraph dependencyGraph = new DependencyGraph();
        /**--------- used for parsing #ifdef to generate ground truth---------------**/
        boolean SHA = false;
        boolean IFDEF = true;
        GetForkAddedCode icc = new GetForkAddedCode();
        if (SHA) {
//            icc.identifyChangedCodeBySHA(projectPath, repo, commitList);
        } else if (IFDEF) {
            icc.identifyIfdefs(sourcecodeDir, analysisDir, macroList);
        }

        dependencyGraph.getDependencyGraphForProject(sourcecodeDir,analysisDir);
        /**------------------calculating similarity------------------------------------**/
        StringSimilarity strSim = new StringSimilarity();
        strSim.calculateStringSimilarityByR(sourcecodeDir,analysisDir,re);


        R_CommunityDetection rCommunityDetection = new R_CommunityDetection();
        /*------------include dirNum----------------------
        int bestCut = rCommunityDetection.detectingCommunitiesWithIgraph(filePath + DPGraphDir + dirNum, numOfCuts, re);
        -----------include dirNum----------------------*/
        int bestCut = rCommunityDetection.detectingCommunitiesWithIgraph(analysisDir, numOfCuts, re);

        ColorCode colorCode = new ColorCode();
          /*------------include dirNum----------------------
        colorCode.parseEachUsefulClusteringResult(projectPath, repo, dirNum, macroList);
           -----------include dirNum----------------------*/
        colorCode.parseEachUsefulClusteringResult(sourcecodeDir, analysisDir);
        /* FOR EMAIL SYSTEM*/
//        colorCodeBlocks.parseEachUsefulClusteringResult(projectPath, repo, dirNum, 3);

    }

    /**
     * used for test cases
     * @param sourcecodeDir
     * @param analysisDir
     * @param numOfCuts
     * @param re
     * @return
     */
    public HashSet<String> analyzeRepository(String sourcecodeDir,  String analysisDir,int numOfCuts, Rengine re) {

        DependencyGraph dependencyGraph = new DependencyGraph();
        HashSet<String> edgelist = null;
        edgelist =
                dependencyGraph.getDependencyGraphForProject(sourcecodeDir,analysisDir);
//        /**------------------calculating similarity------------------------------------**/
//        StringSimilarity strSim = new StringSimilarity();
//                strSim.calculateStringSimilarityByR(sourcecodeDir,analysisDir,re);
//        ArrayList<Double[]> levenshtein_matrix =  strSim.levenshtein_Method(sourcecodeDir,analysisDir);

        R_CommunityDetection rCommunityDetection = new R_CommunityDetection();
        /*-----------include dirNum----------------------
        int bestCut = rCommunityDetection.detectingCommunitiesWithIgraph(filePath + DPGraphDir + dirNum, numOfCuts, re);
        -----------include dirNum----------------------*/

        int bestCut = rCommunityDetection.detectingCommunitiesWithIgraph(analysisDir, numOfCuts, re);

        ColorCode colorCode = new ColorCode();
          /*------------include dirNum----------------------
        colorCode.parseEachUsefulClusteringResult(projectPath, repo, dirNum, macroList);
           -----------include dirNum----------------------*/
        colorCode.parseEachUsefulClusteringResult(sourcecodeDir, analysisDir);
        return edgelist;
//        return null;
    }


    public HashSet<String> analyzeRepository(String sourcecodeDir, String analysisDir,  ArrayList<String> macroList, int numOfIteration, Rengine re) {
//        String DPGraphDir = "/DPGraph/";
//        String filePath = projectPath + repo;


//        DependencyGraph dependencyGraph = new DependencyGraph();
////        boolean SHA = false;
//        boolean IFDEF = true;
//        GetForkAddedCode icc = new GetForkAddedCode();
////        if (SHA) {
////            icc.identifyChangedCodeBySHA(projectPath, repo, commitList);
////        } else
//
//
//        if (IFDEF) {
//            icc.identifyIfdefs(sourcecodeDir, analysisDir, macroList);
//        }
        HashSet<String>  edgelist  =null;
//                edgelist  = dependencyGraph.getDependencyGraphForProject(sourcecodeDir, analysisDir);
//        StringSimilarity strSim = new StringSimilarity();
//        strSim.calculateStringSimilarity(projectPath, repo, dirNum,re);


//        R_CommunityDetection rCommunityDetection = new R_CommunityDetection();
//
//        int bestCut = rCommunityDetection.detectingCommunitiesWithIgraph(analysisDir, numOfIteration, re);
        ColorCode colorCode = new ColorCode();
        colorCode.parseEachUsefulClusteringResult(sourcecodeDir, analysisDir);

        /* FOR EMAIL SYSTEM*/
//        colorCodeBlocks.parseEachUsefulClusteringResult(projectPath, repo, dirNum, 3);
        return edgelist;
    }
}
