package NamingClusters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shuruiz on 10/14/16.
 */
public class StopWords {

    List<String> stopWordList = new ArrayList<>();

    public StopWords() {
        stopWordList.add("define");
        stopWordList.add("ifdef");
        stopWordList.add("elif");
        stopWordList.add("endif");
        stopWordList.add("do");
        stopWordList.add("while");
        stopWordList.add("switch");
        stopWordList.add("case");
        stopWordList.add("goto");
        stopWordList.add("if");
        stopWordList.add("else");
        stopWordList.add("void");
        stopWordList.add("static");
        stopWordList.add("return");
        stopWordList.add("break");
        stopWordList.add("false");
        stopWordList.add("true");
        stopWordList.add("int");
        stopWordList.add("bool");


    }

    public String removeStopWord(String string) {
        for (String stw : stopWordList) {
            string = string.replace(stw, "");
        }
        return string;
    }

}
