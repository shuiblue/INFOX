import DependencyGraph.AnalyzingRepository;
import Util.GetForkAddedCode;
import Util.GroundTruth;
import Util.ProcessingText;
import org.rosuda.JRI.Rengine;

import java.io.File;
import java.util.ArrayList;

import static Util.GroundTruth.*;

/**
 * Created by shuruiz on 6/2/2016.
 */
public class Main {


    static ArrayList<String> commitList = new ArrayList<>();
    static AnalyzingRepository analyzeRepo = new AnalyzingRepository();
    static ProcessingText iof = new ProcessingText();
    static ArrayList<String> macroList;
    static String sourcecodeDir, analysisDir;
    static String analysisDirName = "DPGraph";
    static final String FS = File.separator;

    /*-------------------Changable Variables------------------------
    * DIR : source code directory
    * GROUNDTRUTH: REAL / IFDEF
    * numOfCuts: This is the number of edge-cutting of Community Detection algorithm.
    * */
//    static String DIR = "C:\\Users\\shuruiz\\Documents\\components\\rel\\mcs.mpss\\";

    static String projectPath = "/Users/shuruiz/Work/MarlinRepo/IfdefGroundTruth/";
    static String repo = "Marlin";
    static GroundTruth GROUNDTRUTH = IFDEF;

//    static GroundTruth GROUNDTRUTH = REAL;
//    static String repo = "Email";

    //= REAL;
    static int numOfCuts = 10;


    // need to be set by developer
    static int numOfTargetMacro = 2;

    /**
     * Main method for testing the INFOX method
     *
     * @param args
     */
    public static void main(String[] args) {
        /*
        Initialize Rengine to call igraph R library.
         */
        Rengine re = new Rengine(new String[]{"--vanilla"}, false, null);
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }

        /* testCase specifys the repository that need to be parsed.      */

        sourcecodeDir = projectPath + repo + FS;


        if (GROUNDTRUTH == IFDEF) {
            for (int dirNum = 1; dirNum < 2; dirNum++) {
                analysisDir = sourcecodeDir + analysisDirName + FS + dirNum + FS;

                GetForkAddedCode getForkAddedCode = new GetForkAddedCode();

                macroList = getForkAddedCode.createMacroList(sourcecodeDir, analysisDir);

                ArrayList<String> targetMacroList = getForkAddedCode.selectTargetMacros(numOfTargetMacro);
//                ArrayList<String> macroList = selectApacheMacros(numOfTargetMacro);
//                  ArrayList<String> macroList = new ArrayList<>();
//                macroList.add("CL_DEBUG");
//                StringBuffer sb = new StringBuffer();
//                for (int i = 1; i <= macroList.size(); i++) {
//                    sb.append("<h3>" + i + ") " + macroList.get(i - 1) + "</h3>\n");
//                }
//                    iof.rewriteFile(sb.toString(), analysisDir + dirNum + "/testedMacros.txt");
                new File(analysisDir).mkdir();
                commitList = new ArrayList<>();
                analyzeRepo.analyzeRepository(sourcecodeDir, analysisDir, commitList, targetMacroList, numOfCuts, re);
            }
        } else if (GROUNDTRUTH == REAL) {
            analysisDir = sourcecodeDir + analysisDirName + FS ;
            new File(analysisDir).mkdir();
            analyzeRepo.analyzeRepository(sourcecodeDir, analysisDir, numOfCuts, re);
        }
    }
}
