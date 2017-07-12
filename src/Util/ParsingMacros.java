package Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingInt;

/**
 * Created by shuruiz on 11/6/16.
 */
public class ParsingMacros {
    static final String FS = File.separator;
    static HashMap<String, ArrayList<String>> macro_to_locArray = new HashMap<>();
    static HashMap<String, ArrayList<String>> macro_to_interactedMacroList = new HashMap<>();
    static HashMap<String, HashSet<String>> file_to_MacroList = new HashMap<>();
    static ProcessingText iof = new ProcessingText();
    static String sourcecodeDir, testCaseDir;

    /**
     * This function aims to create a list of macros from the source code repository,
     * basically it calls {@link #findIndependentMacros(String)} function
     * to get changed code list.
     *
     * @return
     */
    public void createMacroList(String sourcecodeDir) {
        this.sourcecodeDir = sourcecodeDir;
        File dir1 = new File(sourcecodeDir);
        try {
            Files.walk(Paths.get(sourcecodeDir)).forEach(filePath -> {
                if (!new ProcessingText().isLib(filePath)) {
                    String fileName = String.valueOf(filePath.toString().replace(sourcecodeDir, ""));
                    if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".c")) {
                        findIndependentMacros(fileName);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }


        /**   print macro size **/
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ArrayList<String>> entry : macro_to_locArray.entrySet()) {
            sb.append(entry.getKey() + " : " + entry.getValue().size() + "\n");
        }
        iof.rewriteFile(sb.toString(), sourcecodeDir + "macroSize.txt");
    }


    /**
     * This function  randomly selects targetMacroList.
     * It first parse the source code to generate a list of independent macros( 'independent' means there is no interaction between those macros)
     * //todo in the future, we might handle feature interactions.
     * Second,
     *
     * @param sourcecodeDir
     * @param testCaseDir
     * @param numberOfTargetMacros
     * @return
     */

    public ArrayList<String> selectTargetMacros(String sourcecodeDir, String testCaseDir, int numberOfTargetMacros, int caseIndex, boolean macrosInOneFile) {
        List<String> keys;


        /**  randomly select targetMacroList   **/

        ArrayList<String> targetMacroList = new ArrayList<>();
        int num_bigSize_Macro = 0, num_smallSize_Macro = 0;
        HashSet<String> bigMacros = new HashSet<>();
        HashSet<String> smallMacros = new HashSet<>();

        int totalSize = 0;
        int sizeThreshold = 0;

        if (!macrosInOneFile) {
            // find threshold of big and small size of feature
            for (Map.Entry<String, ArrayList<String>> entry : macro_to_locArray.entrySet()) {
                totalSize += entry.getValue().size();
            }
            sizeThreshold = totalSize / macro_to_locArray.size();
        }


        if (caseIndex <= 3) {  //few big code block
            num_bigSize_Macro = numberOfTargetMacros / 3;
            num_smallSize_Macro = numberOfTargetMacros - num_bigSize_Macro;
        } else if (3 < caseIndex && caseIndex <= 6) {//few small code block
            num_smallSize_Macro = numberOfTargetMacros / 3;
            num_bigSize_Macro = numberOfTargetMacros - num_smallSize_Macro;
        }
        for (Map.Entry<String, ArrayList<String>> entry : macro_to_locArray.entrySet()) {
            while (targetMacroList.size() < numberOfTargetMacros) {
                Random random = new Random();
                if (!macrosInOneFile) {
                    keys = new ArrayList<String>(macro_to_locArray.keySet());
                } else {
                    List<String> values = file_to_MacroList
                            .entrySet()
                            .stream()
                            .sorted((left, right) ->
                                    Integer.compare(right.getValue().size(), left.getValue().size()))
                            .map(x -> x.getKey())
                            .collect(Collectors.toList());

                    String fileName = values.get(0);
                    keys = new ArrayList(file_to_MacroList.get(fileName));
                    for (String k : keys) {
                        totalSize += macro_to_locArray.get(k).size();
                    }

                    sizeThreshold = totalSize / keys.size();

                }
                String key = keys.get(random.nextInt(keys.size()));


                Pattern p = Pattern.compile("[^a-zA-Z0-9_]");
                boolean hasSpecialChar = p.matcher(key).find();
                if (hasSpecialChar) {
                    continue;
                }

                int size = macro_to_locArray.get(key).size();


                if (!key.startsWith("__AVR_AT") && !targetMacroList.contains(key)) {
                    if (size > sizeThreshold) {
                        if (bigMacros.size() == 0 || (bigMacros.size() < num_bigSize_Macro && noFeatureInteraction(targetMacroList, key))) {
                            bigMacros.add(key);
                            targetMacroList.add(key);
                            System.out.println("Item : " + key + " Count : " + macro_to_locArray.get(key).size());
                        }
                    } else if (size < sizeThreshold) {
                        if (smallMacros.size() == 0 || (smallMacros.size() < num_smallSize_Macro && noFeatureInteraction(targetMacroList, key))) {
                            smallMacros.add(key);
                            targetMacroList.add(key);
                            System.out.println("Item : " + key + " Count : " + macro_to_locArray.get(key).size());
                        }
                    }
                }

            }
        }

        /** store macro name into file, which helps to generate the final html **/
        StringBuffer sb_html = new StringBuffer();
        StringBuffer sb_featureList = new StringBuffer();
        for (int i = 1; i <= targetMacroList.size(); i++) {
            sb_html.append("<h3>" + i + ") " + targetMacroList.get(i - 1) + "</h3>\n");
            sb_featureList.append(targetMacroList.get(i - 1) + "\n");
        }
        iof.rewriteFile(sb_html.toString(), testCaseDir + "/testedMacros.txt");
        iof.rewriteFile(sb_featureList.toString(), testCaseDir + "/featureList.txt");


        /**--------- used for parsing #ifdef to generate ground truth---------------
         parsing source code to find LOC wrapped by those macros and generating forkAddedNode.txt file **/
        int clusterIndex = 1;
        StringBuilder sb = new StringBuilder();
        StringBuilder sb_cluster = new StringBuilder();
        for (String macro : targetMacroList) {
            for (String nodeLabel : macro_to_locArray.get(macro)) {
                if (nodeLabel.trim().length() > 0) {
                    sb.append(nodeLabel + "\n");
                    sb_cluster.append(nodeLabel + " " + clusterIndex + "\n");
                }
            }
            clusterIndex++;
        }
        iof.rewriteFile(sb.toString(), testCaseDir + "/forkAddedNode.txt");
        iof.rewriteFile(sb_cluster.toString(), testCaseDir + "/expectCluster.txt");


        return targetMacroList;
    }

    private boolean noFeatureInteraction(ArrayList<String> selectedMacros, String candidate_macro) {

        for (String macro : selectedMacros) {
            macro = "FAST_PWM_FAN";
            if (macro_to_interactedMacroList.get(candidate_macro) != null && macro_to_interactedMacroList.get(candidate_macro).contains(macro)) {
                return false;
            }
            if (macro_to_interactedMacroList.get(macro) != null) {
                if (macro_to_interactedMacroList.get(macro).contains(candidate_macro)) {
                    return false;
                }
            }
        }
        return true;
    }


    public static void findIndependentMacros(String fileName) {
        System.out.print(fileName + "\n");
        String newFileName = iof.changeFileName(fileName);
        HashSet<String> macroListInCurrentFile = new HashSet<>();
        int linenum = 1;
        ArrayList<ArrayList<String>> macroStack = new ArrayList<>();
        ArrayList<String> currentLevel_macroList = new ArrayList<>();
        File currentFile = new File(sourcecodeDir + "/" + fileName);
//        boolean headerguard = false;
        if (currentFile.isFile()) {
            if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".c") || fileName.endsWith(".pde")) {
                try {
                    BufferedReader result = new BufferedReader(new FileReader(sourcecodeDir + "/" + fileName));
                    String line;
                    while ((line = result.readLine()) != null) {


                        if (line.replace(" ", "").startsWith("#if") || line.replace(" ", "").startsWith("#elif")) {

//                            String clearLine = iof.removeComments(line);
                            line = iof.removeComments(line);
                            if (!(line.contains("||") && line.contains("&&")) && !line.replace(" ", "").contains("!define") && !line.contains("=") && !line.contains(">") && !line.contains("<")) {
                                if ((line.contains(" ENABLED(") || line.contains("defined(")) && !line.contains("#elif")) {
                                    currentLevel_macroList = new ArrayList<>();
                                    String[] conditions = line.split("\\|\\|");
                                    for (String c : conditions) {
                                        if (c.contains("ENABLED(") || c.contains("defined(")) {
                                            int leftPare = c.indexOf("(");
                                            int rightPare = c.indexOf(")");
                                            currentLevel_macroList.add(c.substring(leftPare + 1, rightPare).trim());
                                        } else if (c.contains("#if ")) {
                                            String macro = c.substring(4).trim();
                                            currentLevel_macroList.add(macro);
                                        }
                                    }
                                } else if (line.replace(" ", "").contains("#ifdef") && !line.contains("defined(")) {
                                    currentLevel_macroList = new ArrayList<>();
                                    currentLevel_macroList.add(iof.removeComments(line.replace(" ", "").substring(6)));
                                } else if (line.replace(" ", "").contains("#ifndef")) {
                                    currentLevel_macroList = new ArrayList<>();
                                    currentLevel_macroList.add("!" + line.substring(8).trim());
                                } else if (line.replace(" ", "").contains("#DISABLED(")) {

                                    int leftPare = line.indexOf("(");
                                    int rightPare = line.indexOf(")");
                                    currentLevel_macroList.add("!" + line.substring(leftPare + 1, rightPare).trim());
                                } else if (line.replace(" ", "").contains("#elif")) {
                                    currentLevel_macroList = new ArrayList<>();
                                    if (line.contains("ENABLED(")) {
                                        int leftPare = line.indexOf("(");
                                        int rightPare = line.indexOf(")");
                                        currentLevel_macroList.add(line.substring(leftPare + 1, rightPare).trim());
                                    } else {

                                        currentLevel_macroList.add(line.replace(" ", "").substring(5));
                                    }
                                    macroStack.remove(macroStack.get(macroStack.size() - 1));
                                } else if (line.replace(" ", "").contains("#if")) {
                                    System.out.print("");

                                    Pattern p = Pattern.compile("[a-zA-Z_]");
                                    boolean hasAlphabet = p.matcher(line.replace(" ", "").replace("#if", "")).find();
                                    if (hasAlphabet) {
                                        System.out.print("");
                                        currentLevel_macroList.add(line.replace(" ", "").substring(3));

                                    }

                                }

                                macroStack.add(currentLevel_macroList);

                                for (String macro : currentLevel_macroList) {
                                    Pattern p = Pattern.compile("[a-zA-Z_]");
                                    boolean hasAlphabet = p.matcher(macro).find();

                                    if (!macro.startsWith("!") && !macro.endsWith("_H") && hasAlphabet) {
                                        macroListInCurrentFile.add(macro);
                                        if (macroStack.size() > 1) {
                                            ArrayList<String> previousMacro = macroStack.get(macroStack.size() - 2);
                                            ArrayList<String> interactedMacroList = new ArrayList<>();
                                            for (String pre_macro : previousMacro) {
                                                if (macro_to_interactedMacroList.get(pre_macro) != null) {
                                                    interactedMacroList = macro_to_interactedMacroList.get(pre_macro);
                                                }
                                                interactedMacroList.add(macro);


                                                macro_to_interactedMacroList.put(pre_macro, interactedMacroList);

                                            }
                                        }

                                        if (macro_to_locArray.get(macro) == null) {
                                            ArrayList<String> wrappedCode = new ArrayList<>();
                                            macro_to_locArray.put(macro, wrappedCode);
                                        }
                                    }
                                }
                            } else {
                                currentLevel_macroList = new ArrayList<>();
                                currentLevel_macroList.add(line);
                                macroStack.add(currentLevel_macroList);
                            }

                        } else if (line.replace(" ", "").contains("#endif") || line.replace(" ", "").contains("#else")) {
                            if (macroStack.size() == 0) {
                                break;
                            }
                            ArrayList<String> removedMacro = macroStack.get(macroStack.size() - 1);
                            for (String rm : removedMacro) {
                                Pattern p = Pattern.compile("[^a-zA-Z0-9_]");
                                boolean hasSpecialChar = p.matcher(rm).find();

                                if (!rm.startsWith("!") && !rm.endsWith("_H") && !hasSpecialChar) {
                                    macro_to_locArray.get(rm).add(newFileName + "-" + linenum);
                                }
                            }
                            macroStack.remove(removedMacro);
                            if (macroStack.size() > 1) {
                                currentLevel_macroList = macroStack.get(macroStack.size() - 1);
                            } else {
                                currentLevel_macroList = new ArrayList<>();
                            }
                            if (line.replace(" ", "").contains("#else")) {
                                ArrayList<String> not_removedMacros = new ArrayList<>();
                                for (String rm : removedMacro) {
                                    not_removedMacros.add("!" + rm);
                                }
                                macroStack.add(not_removedMacros);
                            }
                        }

                        for (ArrayList<String> mList : macroStack) {
                            for (String m : mList) {
                                Pattern p = Pattern.compile("[^a-zA-Z0-9_]");
                                boolean hasSpecialChar = p.matcher(m).find();

                                if (!m.startsWith("!") && !m.endsWith("_H") && !hasSpecialChar && !m.contains("DEBUG") && !m.contains("TEST") && !m.contains("=") && !m.contains("#") && !m.contains("||")) {
                                    ArrayList<String> wrappedCode = macro_to_locArray.get(m);
                                    String edgeLabel = newFileName + "-" + linenum;
                                    if (!wrappedCode.contains(edgeLabel)) {
                                        wrappedCode.add(edgeLabel);
                                    }
                                    macro_to_locArray.put(m, wrappedCode);
                                }
                            }
                        }
                        linenum++;

                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (currentFile.isDirectory()) {
            String[] subNames = currentFile.list();
            for (String f : subNames) {
                findIndependentMacros(fileName + "/" + f);
            }
        }

        file_to_MacroList.put(newFileName, macroListInCurrentFile);

    }

    public static void main(String[] args) {

//        generatingTestCases_differentMacroCombination();
    }

    public static void generatingTestCases_differentMacroCombination(String testCasesDir) {
        String analysisDirName = "DPGraph";
        ParsingMacros parsingMacros = new ParsingMacros();
        try {
            Files.walk(Paths.get(testCasesDir), 1).forEach(filePath -> {

                if (Files.isDirectory(filePath) && !filePath.toString().equals(testCasesDir) && !filePath.toString().contains("DPGraph")) {
                    sourcecodeDir = filePath.toString() + FS;
                    parsingMacros.createMacroList(sourcecodeDir);

                    for (int numOfTargetMacro = 3; numOfTargetMacro <= 15; numOfTargetMacro++) {
                        for (int i = 1; i <= 6; i++) {
                            String testCaseDir = sourcecodeDir + analysisDirName + FS + numOfTargetMacro + "macros_oneFile" + FS + i + FS;
                            parsingMacros.selectTargetMacros(sourcecodeDir, testCaseDir, numOfTargetMacro, i, true);

                            testCaseDir = sourcecodeDir + analysisDirName + FS + numOfTargetMacro + "macros" + FS + i + FS;
                            parsingMacros.selectTargetMacros(sourcecodeDir, testCaseDir, numOfTargetMacro, i, false);
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[]  dirArray={"testINFOX","testMS","testMS_plus_CF_Hierachy","testINFOX_NO_DefUse", "testINFOX_NO_ControlF","testINFOX_NO_Hierarchy","testINFOX_NO_Consec","testMS_NO_Consec"};
        for(String target:dirArray){
            try {
                new ProcessingText().copyFolder(new File(sourcecodeDir+analysisDirName),new File(sourcecodeDir+target));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
