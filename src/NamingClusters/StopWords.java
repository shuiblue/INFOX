package NamingClusters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by shuruiz on 10/14/16.
 */
public class StopWords {

    List<String> stopWordList = new ArrayList<>();

    public StopWords() {
        stopWordList.add("defines");
        stopWordList.add("define");
        stopWordList.add("ifndef");
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
        stopWordList.add("uint16_t");
        stopWordList.add("uint8_t");
        stopWordList.add("int8_t");
        stopWordList.add("int");
        stopWordList.add("bool");
        stopWordList.add("float");
        stopWordList.add("long");
        stopWordList.add("copyright");
        stopWordList.add("github");
        stopWordList.add("byte");
        stopWordList.add("unsigned");
        stopWordList.add("const");
        stopWordList.add("char");
        stopWordList.add("for");
        stopWordList.add("and");
        stopWordList.add("the");
        stopWordList.add("with");
        stopWordList.add("in");
        stopWordList.add("as");
        stopWordList.add("to");
        stopWordList.add("of");
        stopWordList.add("when");
        stopWordList.add("sizeof");
        stopWordList.add("final");
        stopWordList.add("is");
        stopWordList.add("it");
        stopWordList.add("at");
        stopWordList.add("there");
        stopWordList.add("are");
        stopWordList.add("fatal");
        stopWordList.add("could");
        stopWordList.add("ofxVideoRecorder");
        stopWordList.add("code");
        stopWordList.add("str");
        stopWordList.add("size_t");
        stopWordList.add("utf8");
        stopWordList.add("by");
        stopWordList.add("null");
        stopWordList.add("printf");
        stopWordList.add("this");
        stopWordList.add("string");
        stopWordList.add("public");
    }

    public String removeStopWord(String string) {
        for (String stw : stopWordList) {
            if(string.equals(stw)) {
                string = string.replace(stw, "");
            }
        }
        return string;
    }

}
