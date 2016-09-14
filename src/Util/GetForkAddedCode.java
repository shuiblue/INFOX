package Util;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by shuruiz on 8/29/16.
 *
 */
public class GetForkAddedCode {
    static String DIR = "C:\\Users\\shuruiz\\Documents\\components\\rel\\mcs.mpss\\test\\";

    String forkAddedNodeTxt = "forkAddedNode.txt";
    String expectTxt = "expectCluster.txt";
    static String sourcecodeDir, analysisDir;
    static ArrayList<String> commitSHAList,macroList;
    static ProcessingText iof = new ProcessingText();
    StringBuffer forkAddedNodeString = new StringBuffer();
    StringBuffer expectedClusterString = new StringBuffer();
    String FS = File.separator;

    public void identifyChangedCodeBySHA(String projectPath, String repo, ArrayList<String> commitSHAList) {
        String testDir = projectPath + repo;
        this.commitSHAList = commitSHAList;
        sourcecodeDir = testDir + "/Marlin/";
        analysisDir = testDir + "/DPGraph/";
        new File(analysisDir).mkdir();
        File dir = new File(sourcecodeDir);
        String[] names = dir.list();
        iof.rewriteFile("", analysisDir + forkAddedNodeTxt);
        iof.rewriteFile("", analysisDir + expectTxt);


        for (String fileName : names) {
//            if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".c")) {
            if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".c") || fileName.endsWith(".pde")) {
                try {
                    String cmd = "/usr/bin/git --git-dir=" + testDir + "/.git --work-tree=" + testDir + " blame -n --abbrev=6 " + sourcecodeDir + fileName;
                    Process p = Runtime.getRuntime().exec(cmd);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.length() > 0) {
                            checkSHA(line, fileName);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                iof.rewriteFile(sb.toString(), analysisDir + fileName + SHA);
            }
        }
    }

    private void checkSHA(String line, String fileName) {
        String suffix = fileName.split("\\.")[1];
        String newFileName = fileName.replace("." + suffix, suffix.toUpperCase()).replace("-", "~");
        ;

        String blameLines[] = line.split(" ");
        String sha = blameLines[0];

//        String blameLines[] = line.split(" ");
//        String sha = blameLines[0]
        String splitParenthesis = line.split(" \\(")[0];
        String lineNum = splitParenthesis.substring(splitParenthesis.lastIndexOf(" ") + 1, splitParenthesis.length());


        if (commitSHAList.contains(sha)) {
            String s = line.split("\\)")[0];
            String code = line.split("\\)")[1];

//            String lineNum = s.substring(s.lastIndexOf(" ") + 1, s.length());

            boolean comments = false;
            String cleanCode = code.trim();
            if (!cleanCode.equals("")) {
                if (!cleanCode.startsWith("//") && !comments) {
//                    System.out.println(newFileName + "-" + lineNum);
                    iof.writeTofile(newFileName + "-" + lineNum + " \n", analysisDir + forkAddedNodeTxt);
                    if (!cleanCode.startsWith("#")) {
                        iof.writeTofile(newFileName + "-" + lineNum + " 1\n", analysisDir + expectTxt);
                    }
                } else if (cleanCode.startsWith("/*")) {
                    comments = true;
                } else if (cleanCode.startsWith("*/") && comments) {
                    comments = false;
                }
            }


        }


    }


    public static void findIndependentMacros(String fileName) {
        int linenum = 1;
        ArrayList<String> macroStack = new ArrayList<>();
        String macro = "";
        File currentFile = new File(sourcecodeDir + "/" + fileName);
        boolean headerguard = false;
        int boundary;
        if (currentFile.isFile()) {
            if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".c") || fileName.endsWith(".pde")) {
                try {
                    BufferedReader result = new BufferedReader(new FileReader(sourcecodeDir + "/" + fileName));
                    String line;
                    while ((line = result.readLine()) != null) {
                        if (line.contains("#if") || line.contains("#elif")) {
                            if (line.contains("if ENABLED(")) {
                                String[] conditions = line.split("\\|\\|");
                                for (String c : conditions) {
                                    if (c.contains("ENABLED(")) {

                                        int leftPare = c.indexOf("(");
                                        int rightPare = c.indexOf(")");
                                        macro = c.substring(leftPare + 1, rightPare).trim();
                                    }
                                }
                            } else if (line.contains("#ifdef")) {
                                macro = line.trim().substring(7);
                            } else if (line.contains("#ifndef")) {
                                if (fileName.trim().endsWith(".h")) {
                                    String name = line.trim().substring(8);
                                    String[] tmp = fileName.split("/");

                                    String file = tmp[tmp.length - 1].trim();
                                    file = file.split("\\.")[0].toUpperCase() + "_" + file.split("\\.")[1].toUpperCase();
                                    if (name.equals(file)) {
                                        headerguard = true;
                                    }
                                }
                            }

                            if (!headerguard) {
                                macroStack.add(macro);
                            }
                        } else if (line.contains("#endif")) {
                            if (macroStack.size() == 0) {
                                break;
                            }

                            if (headerguard) {
                                boundary = 1;
                            } else {
                                boundary = 0;
                            }


                            String removedMacro = macroStack.get(macroStack.size() - 1);

                            macroStack.remove(macroStack.remove(macroStack.size() - 1));
                            if (macroStack.size() == boundary && !macroList.contains(removedMacro) && !removedMacro.equals("") && !removedMacro.startsWith("#")) {
                                macroList.add(removedMacro);
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
    }

    /**
     * This function aims to create a list of macros from the source code repository,
     * basically it calls {@link #findIndependentMacros(String)} function
     * to get changed code list.
     * @return
     */
    public  ArrayList<String> createMacroList(String sourcecodeDir,String analysisDir) {
        this.sourcecodeDir=sourcecodeDir;
        this.analysisDir = analysisDir;
        macroList = new ArrayList<>();
        File dir1 = new File(sourcecodeDir);
        File dir2 = new File(analysisDir);
        String[] names = dir1.list();
        dir2.mkdirs();

        for (String fileName : names) {
//            if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".c")) {
            findIndependentMacros(fileName);
        }
        return macroList;
    }


    /**
     * This function parses all the source code files and calls {@link #findIndependentMacros(String)} function
     * to get changed code list.
     * Output: 2 text file.
     * 1) a list of forkAddedNode
     * 2) a list of forkAddedNode + expectCluster --- Ground Truth
     * @param sourcecodeDir
     * @param analysisDir
     * @param macroList a list of macros that need to be analyzed. The code that is wrapped by those macros are the ForkAdded code.
     */
    public void identifyIfdefs(String sourcecodeDir, String analysisDir,  ArrayList<String> macroList) {
//    public void identifyIfdefs(String projectPath, String repo, int dirNum, ArrayList<String> macroList) {
//        String testDir = projectPath + repo;
//        sourcecodeDir = testDir + "/" + repo;
//        analysisDir = testDir + "/DPGraph/" + dirNum + "/";
        File dir1 = new File(sourcecodeDir);
        File dir2 = new File(analysisDir);
        dir2.mkdirs();
        String[] names = dir1.list();
        iof.rewriteFile("", analysisDir + forkAddedNodeTxt);
        iof.rewriteFile("", analysisDir + expectTxt);

        for (String fileName : names) {
            identifyChangedCodeFromFile(sourcecodeDir,fileName, macroList);
        }
        iof.writeTofile(forkAddedNodeString.toString(), analysisDir + forkAddedNodeTxt);
        iof.writeTofile(expectedClusterString.toString(), analysisDir + expectTxt);

    }

    StringBuffer sb = new StringBuffer();

    public void findForkAddedCode(String dirName) {
//        StringBuffer sb = new StringBuffer();
//        String s="";
        try {
            Files.walk(Paths.get(DIR + dirName)).forEach(filePath -> {
                if (filePath.toString().endsWith(".h") || filePath.toString().endsWith(".c")) {
                    if (Files.isRegularFile(filePath)) {
                        findNewCodeFromAFile(filePath.toString());
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        iof.rewriteFile(sb.toString(), DIR + dirName + FS + "DPGraph" + FS + "forkAddedNode.txt");
    }

    public String findNewCodeFromAFile(String file) {

        try {
            String fileContent = iof.readResult(file);
            String[] lines = fileContent.split(System.getProperty("line.separator"));
//            String[] lines = fileContent.split("\r\n");
//            String[] lines = fileContent.split("\r\n|\r|\n");
            String newFileName = iof.changeFileName(file.replace(DIR, ""));
            for (int i = lines.length; i > 0; i--) {
                String line = newFileName + "-" + i;
                if (newFileName.contains("ota")) {
                    line += " " + 1;
                } else {
                    line += " " + 2;
                }
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String switchCRsToGroundTruth(String file, int clusterNumber) {
        BufferedReader result_br = null;
        StringBuilder sb = new StringBuilder();
        try {
            String result = "";
            result_br = new BufferedReader(new FileReader(file));

            String line = result_br.readLine();
            while (line != null) {
                String prefix = "//components/rel/mcs.mpss/6.1/";
                String fileName = iof.changeFileName((line.split(" ")[0]).replace(prefix, ""));
                String lineNumber = line.split(" ")[1];

                sb.append(fileName + "-" + lineNumber + " " + clusterNumber);
                sb.append(System.lineSeparator());
                line = result_br.readLine();
            }
            result = sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                result_br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public void identifyChangedCodeFromFile(String sourcecodeDir,String fileName, ArrayList<String> macroList) {
        ArrayList<String> macroStack = new ArrayList<>();

        int startLine = 0;
        boolean withinIfdef = false;
        String newFileName = "";
        File currentFile = new File(sourcecodeDir + "/" + fileName);
        if (currentFile.isFile()) {
            if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".c") || fileName.endsWith(".pde")) {

                for (String targetMacro : macroList) {
                    try {
                        BufferedReader result = new BufferedReader(new FileReader(sourcecodeDir + "/" + fileName));
                        String line;
                        int lineNum = 1;
                        String macro = "";
                        int currentCommunityNum = -1;
                        while ((line = result.readLine()) != null) {
//                            System.out.println(fileName + "-" + lineNum);
                            if (line.contains("#if") || line.contains("#elif")) {
                                if (line.contains("if ENABLED(") || line.contains("elif")) {
                                    if (macroStack.size() > 0 && line.contains("elif")) {
                                        macroStack.remove(macroStack.size() - 1);
                                        if (macroStack.size() == 0) {
                                            withinIfdef = false;
                                            macro = "";
                                        }
                                    }
                                    String[] conditions = line.split("\\|\\|");
                                    for (String c : conditions) {
                                        if (c.contains("ENABLED(")) {

                                            int leftPare = c.indexOf("(");
                                            int rightPare = c.indexOf(")");
                                            macro = c.substring(leftPare + 1, rightPare).trim();
                                            if (targetMacro.equals(macro)) {
                                                break;
                                            }
                                        }
                                    }

                                } else if (line.contains("#ifdef")) {
                                    macro = line.trim().substring(7);
                                } else{
                                    macro = line.trim();
                                }
                                if (targetMacro.equals(macro)) {
                                    withinIfdef = true;
                                    startLine = lineNum + 1;
                                    macroStack.add(macro);


                                    currentCommunityNum = macroList.indexOf(macro) + 1;
                                    //Rewrite the file name for html purpose
                                    newFileName = iof.changeFileName(fileName);
                                } else if (macroStack.size() > 0) {
                                    macroStack.add(line);
                                }

                            } else if (macroStack.size() > 0 && (line.contains("#endif"))) {
                                macroStack.remove(macroStack.size() - 1);
                                if (macroStack.size() == 0) {
                                    withinIfdef = false;
                                    macro = "";
                                }
                            } else if (macroStack.size() == 1 && (line.contains("#else"))) {
                                macroStack.remove(macroStack.size() - 1);
                                if (macroStack.size() == 0) {
                                    withinIfdef = false;
                                    macro = "";
                                }
                            }

                            if (startLine <= lineNum && withinIfdef && currentCommunityNum != -1) {
                                String nodeId = newFileName + "-" + lineNum + " ";

                                if (!forkAddedNodeString.toString().contains(nodeId)) {
                                    forkAddedNodeString.append(nodeId + "\n");
                                    expectedClusterString.append(nodeId + currentCommunityNum + "\n");
                                } else {

                                    String tmp = expectedClusterString.toString();
                                    expectedClusterString = new StringBuffer();
                                    expectedClusterString.append(tmp.replace(nodeId, nodeId + currentCommunityNum + "/"));

                                }


                            }
                            lineNum++;
                        }
                        result.close();
                    } catch (IOException e) {
                        e.printStackTrace();

                    }

                }
            }
        } else if (currentFile.isDirectory()) {
            String[] subNames = currentFile.list();
            for (String f : subNames) {
                identifyChangedCodeFromFile(sourcecodeDir,fileName+"/"+f, macroList);
            }
        }
    }


    public static ArrayList<String> selectTargetMacros(int number) {
        ArrayList<String> targetMacros = new ArrayList<>();
        ArrayList<Integer> indexList = new ArrayList<>();
        while (indexList.size() < 5) {
            Random random = new Random();
            int index = random.ints(0, (macroList.size() - 1)).findFirst().getAsInt();
            if (!indexList.contains(index)) {

                indexList.add(index);
                System.out.println("macroList.size()" + macroList.size());
                System.out.println("index" + index);
                targetMacros.add(macroList.get(index));
            }
        }

        return targetMacros;

    }


    public static void main(String[] args) {
        String dir = "C:\\Users\\shuruiz\\Documents\\PRISM-CR\\MCS61_CR\\result";
        String outputfile = "C:\\Users\\shuruiz\\Documents\\PRISM-CR\\MCS61_CR\\forkAddedCode.txt";
        iof.rewriteFile("", outputfile);
        final int[] i = {1};
        try {
            Files.walk(Paths.get(dir)).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    if (filePath.toString().endsWith("txt")) {
                        iof.writeTofile(switchCRsToGroundTruth(filePath.toString(), i[0]), outputfile);
                        i[0]++;
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
