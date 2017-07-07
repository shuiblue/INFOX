//import DependencyGraph.AnalyzingRepository;
//import Util.ProcessingText;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.junit.Before;
//import org.rosuda.JRI.Rengine;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashSet;
//
///**
// * Created by shuruiz on 9/4/16.
// */
//public class TestIfdefGroundTruth {
//    String projectPath = "/Users/shuruiz/Work/MarlinRepo/IfdefGroundTruth/";
//    static String analysisDirName = "DPGraph";
//    ArrayList<String> commitList = new ArrayList<>();
//    AnalyzingRepository amr = new AnalyzingRepository();
//    Rengine re;
//    static String sourcecodeDir = "", analysisDir = "";
//    static final String FS = File.separator;
//    static int numOfCuts = 10;
//    static AnalyzingRepository analyzeRepo = new AnalyzingRepository();
//
//    @Before
//    public void initR() {
//        re = new Rengine(new String[]{"--vanilla"}, false, null);
//        if (!re.waitForR()) {
//            System.out.println("Cannot load R");
//        }
//    }
//
//    public HashSet<String> create_graph(String repo, String test_dir) {
//        sourcecodeDir = projectPath + repo + FS;
//        analysisDir = sourcecodeDir + analysisDirName + FS + test_dir + FS;
//        new File(analysisDir).mkdir();
//        return analyzeRepo.analyzeRepository(sourcecodeDir, analysisDir, numOfCuts, re);
//    }
//
//
//    // independent features
////    @Ignore
//    @Test
//    public void test10() {
//        String repo = "Marlin";
//        String testdir = "test_10";
//        ArrayList<String> macroList = new ArrayList<>();
//        macroList.add("FILAMENT_SENSOR");
//        macroList.add("MESH_BED_LEVELING");
//        macroList.add("ADVANCE");
//        macroList.add("PREVENT_DANGEROUS_EXTRUDE");
//
//        StringBuffer sb = new StringBuffer();
//        for (int i = 1; i <= macroList.size(); i++) {
//            sb.append("<h3>" + i + ") " + macroList.get(i - 1) + "</h3>\n");
//        }
//
//        create_graph(repo, testdir);
//        ProcessingText iof = new ProcessingText();
//        iof.rewriteFile(sb.toString(), analysisDir + "/testedMacros.txt");
//    }
//
//
//    /**  temporary test cases **/
//        @Ignore
//    @Test
//    public void test00() {
//        String repo = "TEMP";
//        String testdir = "test";
//        create_graph(repo, testdir);
//    }
//
//}
