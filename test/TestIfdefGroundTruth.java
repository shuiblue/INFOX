import DependencyGraph.AnalyzingRepository;
import Util.ProcessingText;
import org.junit.Ignore;
import org.junit.Test;
import org.rosuda.JRI.Rengine;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by shuruiz on 9/4/16.
 */
public class TestIfdefGroundTruth {
    String projectPath = "/Users/shuruiz/Work/MarlinRepo/IfdefGroundTruth/";
    ArrayList<String> commitList = new ArrayList<>();
    AnalyzingRepository amr = new AnalyzingRepository();

    // independent features
    @Ignore
    @Test
    public void test10() {
        String repo= "Marlin";
        //start import R library
        System.out.println("Creating Rengine (with arguments)");
        //If not started with --vanilla, funny things may happen in this R shell.

        String[] Rargs = {"--vanilla"};
        Rengine re = new Rengine(Rargs, false, null);
        System.out.println("Rengine created, waiting for R");
        // the engine creates R is a new thread, so we should wait until it's
        // ready
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }

        int dirNum = 888;
        ArrayList<String> macroList = new ArrayList<>();
        macroList.add("FILAMENT_SENSOR");
        macroList.add("MESH_BED_LEVELING");
        macroList.add("ADVANCE");
//        macroList.add("HAS_BUZZER");
        macroList.add("PREVENT_DANGEROUS_EXTRUDE");

        StringBuffer sb = new StringBuffer();
        for (int i = 1; i <= macroList.size(); i++) {
            sb.append("<h3>" + i + ") " + macroList.get(i - 1) + "</h3>\n");
        }

        String analysisDir = projectPath + repo + "/DPGraph/";
        new File(analysisDir + dirNum).mkdir();
        ProcessingText iof=new ProcessingText();
        iof.rewriteFile(sb.toString(), analysisDir + dirNum + "/testedMacros.txt");


        commitList = new ArrayList<>();
        int numOfIteration = 5;
        amr.analyzeRepository(repo, dirNum, projectPath, commitList, macroList, numOfIteration, re);
    }





}
