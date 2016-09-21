import DependencyGraph.AnalyzingRepository;
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
    static String sourcecodeDir, analysisDir;
    static String analysisDirName = "DPGraph";
    static String testCasesDir = "/Users/shuruiz/Work/MarlinRepo/IfdefGroundTruth";
    static final String FS = File.separator;

    /**
     * set by developer
     **/
    static int numOfTargetMacro = 5;
    static int numberOfCuts = 4;
    static int groundTruth = 1;  // (1-- ifdef, 0 --- Real)


    /**
     * Main method for testing the INFOX method
     * @param args
     */
    public static void main(String[] args) {
        /**     Initialize Rengine to call igraph R library.      **/
        Rengine re = new Rengine(new String[]{"--vanilla"}, false, null);
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }

        /** generating the parameters for creating dependency graph  **/
        ArrayList<int[]> parameterArray = getParameterSetting(numOfTargetMacro, numberOfCuts, groundTruth);

        /**  parse different repositories under testCasesDir  **/
        try {
            Files.walk(Paths.get(testCasesDir), 1).forEach(filePath -> {
                if (Files.isDirectory(filePath) && !filePath.toString().equals(testCasesDir)) {

                    /**  testCase specifys the repository that need to be parsed.  **/
                    sourcecodeDir = filePath.toString() + FS;
                    //TODO: set subdir name for multiple tests
                    for (int[] param : parameterArray) {
                        String testDir = "";
                        for (int index = 0; index <= 4; index++) {
                            testDir += param[index];
                        }
                        analysisDir = sourcecodeDir + analysisDirName + FS + testDir + FS;
                        new File(analysisDir).mkdir();
                        analyzeRepo.analyzeRepository(sourcecodeDir, analysisDir, param, re);
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
     * @param numberOfCuts     integer (this number specifies how many more clusters should be detect after generating the initial dependency graph )
     * @return parameterArray
     */
    private static ArrayList<int[]> getParameterSetting(int numOfTargetMacro, int numberOfCuts, int groundTruth) {
        ArrayList<int[]> parameterArray = new ArrayList<>();
        int[] param = new int[5];
        param[0] = numOfTargetMacro;
        param[1] = numberOfCuts;
        param[2] = groundTruth;
        for (int i = 0; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                param[3] = i;
                param[4] = j;
                parameterArray.add(param);
            }
        }
        return parameterArray;
    }
}
