package DependencyGraph;

import ColorCode.ColorCode;
import CommunityDetection.R_CommunityDetection;
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

    public void analyzeRepository(String sourcecodeDir, String analysisDir, String projectPath, ArrayList<String> commitList, ArrayList<String> macroList, int numOfCuts, Rengine re) {
        DependencyGraph dependencyGraph = new DependencyGraph();
        /*
         ----------- used for parsing #ifdef to generate ground truth---------------
        boolean SHA = false;
        boolean IFDEF = true;
        IdentifyChangedCode icc = new IdentifyChangedCode();
        if (SHA) {
            icc.identifyChangedCodeBySHA(projectPath, repo, commitList);
        } else if (IFDEF) {
            icc.identifyIfdefs(projectPath, repo, dirNum, macroList);
        }
        */
        dependencyGraph.getDependencyGraphForProject(sourcecodeDir,analysisDir);
        /*
        ---------------------calculating similarity------------------------------------
        StringSimilarity strSim = new StringSimilarity();
        strSim.calculateStringSimilarity(projectPath, repo, dirNum,re);
*/

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

    public HashSet<String> analyzeRepository(String sourcecodeDir,  String analysisDir,int numOfCuts, Rengine re) {

        DependencyGraph dependencyGraph = new DependencyGraph();
        HashSet<String> edgelist = null;
        edgelist =
                dependencyGraph.getDependencyGraphForProject(sourcecodeDir,analysisDir);

//        String analysisDir = sourcecodeDir + analysisDirName + FS;
        R_CommunityDetection rCommunityDetection = new R_CommunityDetection();
        rCommunityDetection.detectingCommunitiesWithIgraph(analysisDir, numOfCuts, re);

        ColorCode colorCode = new ColorCode();
        colorCode.parseEachUsefulClusteringResult(sourcecodeDir, analysisDir);
        return edgelist;
//        return null;
    }


    public void analyzeRepository(String sourcecdeDir, String analysisDir, ArrayList<String> commitList, ArrayList<String> macroList, int numOfIteration, Rengine re) {
//        String DPGraphDir = "/DPGraph/";
//        String filePath = projectPath + repo;


        DependencyGraph dependencyGraph = new DependencyGraph();
//        boolean SHA = false;
        boolean IFDEF = true;
        GetForkAddedCode icc = new GetForkAddedCode();
//        if (SHA) {
//            icc.identifyChangedCodeBySHA(projectPath, repo, commitList);
//        } else


        if (IFDEF) {
            icc.identifyIfdefs(sourcecdeDir, analysisDir, macroList);
        }
        dependencyGraph.getDependencyGraphForProject(sourcecdeDir, analysisDir);
//        StringSimilarity strSim = new StringSimilarity();
//        strSim.calculateStringSimilarity(projectPath, repo, dirNum,re);


        R_CommunityDetection rCommunityDetection = new R_CommunityDetection();

        int bestCut = rCommunityDetection.detectingCommunitiesWithIgraph(analysisDir, numOfIteration, re);
        ColorCode colorCode = new ColorCode();
        colorCode.parseEachUsefulClusteringResult(sourcecdeDir, analysisDir);

        /* FOR EMAIL SYSTEM*/
//        colorCodeBlocks.parseEachUsefulClusteringResult(projectPath, repo, dirNum, 3);

    }
}
