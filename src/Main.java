import DependencyGraph.AnalyzingRepository;
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

    //-------------------Changable Variables------------------------
    static String DIR = "C:\\Users\\shuruiz\\Documents\\components\\rel\\mcs.mpss\\";
    static GroundTruth GROUNDTRUTH = REAL;
    static int numOfCuts = 10;

    /**
     * Main method for testing the INFOX method
     * @param args
     */
    public static void main(String[] args) {
        Rengine re = new Rengine(new String[]{"--vanilla"}, false, null);
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }
        String testCase = "test\\Email";
//        String testCase = "6.1";
        sourcecodeDir = DIR + testCase + FS;
        analysisDir = sourcecodeDir + analysisDirName + FS;

        if (GROUNDTRUTH == IFDEF) {
            for (int dirNum = 1; dirNum < 2; dirNum++) {
//                ArrayList<String> macroList = selectTargetMacros(numOfTargetMacro);
//                ArrayList<String> macroList = selectApacheMacros(numOfTargetMacro);
                ArrayList<String> macroList = new ArrayList<>();
//                    macroList.add("CL_DEBUG");
//                    StringBuffer sb = new StringBuffer();
//                    for (int i = 1; i <= macroList.size(); i++) {
//                        sb.append("<h3>" + i + ") " + macroList.get(i - 1) + "</h3>\n");
//                    }
//                    iof.rewriteFile(sb.toString(), analysisDir + dirNum + "/testedMacros.txt");
                new File(analysisDir + dirNum).mkdir();
                commitList = new ArrayList<>();
                analyzeRepo.analyzeRepository(sourcecodeDir, analysisDir + dirNum, DIR, commitList, macroList, numOfCuts, re);
            }
        } else if (GROUNDTRUTH == REAL) {
            new File(analysisDir).mkdir();
            analyzeRepo.analyzeRepository(sourcecodeDir, numOfCuts, re);
        }
    }
}
