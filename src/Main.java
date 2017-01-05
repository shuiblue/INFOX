import DependencyGraph.AnalyzingRepository;
import Util.ParsingMacros;
import org.rosuda.JRI.Rengine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;


/**
 * Created by shuruiz on 6/2/2016.
 */
public class Main {
    static AnalyzingRepository analyzeRepo = new AnalyzingRepository();
    static String sourcecodeDir;
    static String analysisDirName = "DPGraph";
    static String testCasesDir = "/Users/shuruiz/Work/MarlinRepo/IfdefGroundTruth";
    static final String FS = File.separator;
    static ArrayList<int[]> parameterArray = new ArrayList<>();
    /**
     * set by developer
     **/


    static int groundTruth = 1;  // (1-- ifdef, 0 --- Real)


    /**
     * Main method for testing the INFOX method
     *
     * @param args
     */
    public static void main(String[] args) {
        /**     Initialize Rengine to call igraph R library.      **/
        Rengine re = new Rengine(new String[]{"--vanilla"}, false, null);
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }

        ParsingMacros parsingMacros = new ParsingMacros();

        /** generating the parameters for creating dependency graph  **/

        /**  parse different repositories under testCasesDir  **/
        try {
            Files.walk(Paths.get(testCasesDir), 1).forEach(filePath -> {
                if (Files.isDirectory(filePath) && !filePath.toString().equals(testCasesDir)) {
                    sourcecodeDir = filePath.toString() + FS;

                    for (int numOfTargetMacro = 2; numOfTargetMacro <=2; numOfTargetMacro++) {
                        parameterArray = getParameterSetting(numOfTargetMacro, groundTruth);
                        /**  testCase specifys the repository that need to be parsed.  **/

                        //TODO: set subdir name for multiple tests
                        for (int i = 1; i <= 1; i++) {
                            String testCaseDir_1 = sourcecodeDir + analysisDirName + FS + numOfTargetMacro + "macros" + FS + i + FS;
                            String testCaseDir_2 = sourcecodeDir + analysisDirName + FS + numOfTargetMacro + "macros_oneFile" + FS + i + FS;
                            System.out.println("~~~~~~~current con1figuration: " + i + "~~");
                            for (int[] param : parameterArray) {
                                analyzeRepo.analyzeRepository(sourcecodeDir, testCaseDir_1, param, re);
                                analyzeRepo.analyzeRepository(sourcecodeDir, testCaseDir_2, param, re);
                            }
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This function generates all the possible combination of the parameters for generating different dependency graphs.
     * the parameters array stores different parameters for creating dependency graphs.
     * 1. numOfTargetMacro
     * 2. numberOfCuts
     * 3. groundTruth : IFDEF / REAL  (1-- ifdef, 0 --- Real)
     * 4. Consecutive lines: T/F  (1/0)
     * 5. Directed Edge: T/F (1/0)
     *
     * @param numOfTargetMacro int (the number of macros randomly selected from marco list, equals to size of targetMacroList)
     * @return parameterArray
     */
    private static ArrayList<int[]> getParameterSetting(int numOfTargetMacro, int groundTruth) {
        ArrayList<int[]> parameterArray = new ArrayList<>();
        int[] param = new int[5];
        param[0] = numOfTargetMacro;
        param[1] = numOfTargetMacro + 3;  // int numberOfCuts = numOfTargetMacro + 3;
        param[2] = groundTruth;
        param[3] = 1;
        param[4] = 0;
        parameterArray.add(param);
        return parameterArray;
    }


}
