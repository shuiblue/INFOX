package Similarity;

import NamingClusters.Levenshtein;
import Util.ProcessingText;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Created by shuruiz on 9/12/16.
 */
public class StringSimilarity {


    public void calculateStringSimilarityByR(String sourcecodeDir, String analysisDir, Rengine re) {
        String strList = "";
        ProcessingText iof = new ProcessingText();
        try {
            strList = iof.readResult(analysisDir + "/StringList.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        int lastComma = strList.lastIndexOf(",");
        strList = strList.substring(0, lastComma);
        re.eval("str<- c(" + strList + ")");
        re.eval(" d  <- adist(str)");
        re.eval("rownames(d) <- str");
        re.eval("matrix<-as.dist(d)");
        re.eval("hc <- hclust(as.dist(d))");
        re.eval("pdf(\"" + analysisDir + "plots.pdf\")");
        re.eval("plot(hc)");
        re.eval("dev.off()");
        for (int i = 2; i <= 10; i++) {
            re.eval("df <- data.frame(str,cutree(hc,k=" + i + "))");
            REXP cluster_R = re.eval("df$cutree.hc..k..." + i + ".");
            int[] cluster = cluster_R.asIntArray();
        }


    }

    public ArrayList<Double[]> levenshtein_Method(String sourcecodeDir, String analysisDir) {
        StringBuffer sb = new StringBuffer();
        ArrayList<Double[]> levenshtein_matrix = new ArrayList<>();

        Levenshtein levenshtein = new Levenshtein();
//        NormalizedLevenshtein normalizedLevenshtein = new NormalizedLevenshtein();

//        NGram nGram = new NGram();
        String strList = "";
        ProcessingText iof = new ProcessingText();
        try {
            strList = iof.readResult(analysisDir + "/StringList.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        int lastComma = strList.lastIndexOf(",");
        strList = strList.substring(0, lastComma);
        ArrayList<String> loc;
        loc = new ArrayList<>(Arrays.asList(strList.split(",")));

        for (int i = 0; i < loc.size(); i++) {
            for (int j = 0; j < loc.size(); j++) {
                if (i != j && i < j) {
                    double distance = levenshtein.distance(loc.get(i), loc.get(j));
//                    double distance = normalizedLevenshtein.distance(loc.get(i), loc.get(j));
//                    double distance = nGram.distance(loc.get(i), loc.get(j));
                    if (distance <= 0.0) {
                        sb.append("[" + i + "," + j + "] " + loc.get(i) + "-" + loc.get(j) + " : " + distance + "\n");
//                        System.out.println(i + "---" + j + "---" + loc.get(i) + "---" + loc.get(j) + "---" + distance);

                    }
                    if (levenshtein_matrix.size() > 0) {
                        if (levenshtein_matrix.get(i) != null) {
                            levenshtein_matrix.get(i)[j] = distance;
                        } else {
                            ArrayList<Double> distance_array = new ArrayList<>();
                            distance_array.add(distance);
                        }
                    } else {
                        Double[] distance_array = new Double[loc.size()];
                        distance_array[j] = distance;
                        levenshtein_matrix.add(i, distance_array);
                    }

                } else if (i == j) {
                    Double[] distance_array = new Double[loc.size()];
                    distance_array[j] = 0.0;
                    levenshtein_matrix.add(i, distance_array);
                }
            }

        }

        iof.rewriteFile(sb.toString(), analysisDir + "LevenshteinDistance.txt");
        return levenshtein_matrix;

    }


}
