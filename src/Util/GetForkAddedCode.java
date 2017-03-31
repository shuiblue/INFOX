package Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by shuruiz on 8/29/16.
 */
public class GetForkAddedCode {
    //    static String DIR = "C:\\Users\\shuruiz\\Documents\\components\\rel\\mcs.mpss\\test\\";
    static String DIR = "/Users/shuruiz/Work/Open Source Project/Suricata/suricata";

    String forkAddedNodeTxt = "forkAddedNode.txt";
    String expectTxt = "expectCluster.txt";
    static String sourcecodeDir, testCaseDir;
    static ArrayList<String> commitSHAList;
    static ProcessingText iof = new ProcessingText();
    String FS = File.separator;
    StringBuilder sb = new StringBuilder();

    public void identifyChangedCodeBySHA(String projectPath, String changedFile, ArrayList<String> commitSHAList) {
        String testDir = projectPath;
        this.commitSHAList = commitSHAList;
        sourcecodeDir = testDir;
        testCaseDir = testDir + "/DPGraph/";
        new File(testCaseDir).mkdir();
        File dir = new File(sourcecodeDir);
        String[] names = dir.list();
//        iof.rewriteFile("", testCaseDir + forkAddedNodeTxt);
//        iof.rewriteFile("", testCaseDir + expectTxt);


        try {
            String cmd = "/usr/bin/git --git-dir=" + testDir + "/.git --work-tree=" + testDir + " blame -n --abbrev=6 " + sourcecodeDir + FS + changedFile +" > "+ sourcecodeDir + FS +"blame.txt";
//            String cmd = "/usr/bin/git --git-dir=" + testDir + "/.git --work-tree=" + testDir + " blame -n --abbrev=6 " + sourcecodeDir + FS + changedFile +" > "+ sourcecodeDir + FS +"blame.txt";
//            String cmd[] = {"git", "blame", "-n","--abbrev=6" ,sourcecodeDir + FS + changedFile, ">", sourcecodeDir + FS + "blame.txt"};
//            String cmd[] = {"/usr/bin/git", "--git-dir=" + testDir + "/.git", "--work-tree=" + testDir, "blame", sourcecodeDir + FS + changedFile, ">", sourcecodeDir + FS + "blame.txt"};
//            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
//            Process process = processBuilder.start();
//            process.waitFor();
//
//            BufferedReader reader =
//                    new BufferedReader(new InputStreamReader(process.getInputStream()));
//
//            String line = "";
//            while ((line = reader.readLine())!= null) {
//                sb.append(line + "\n");
//            }


        String blameTXT = iof.readResult(DIR + FS + "blame.txt");
        for (String line : blameTXT.split("\n")) {
            if (line.trim().length() > 0) {
                checkSHA(line, changedFile);
            }

        }

        } catch (IOException e) {
            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
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
                    iof.writeTofile(newFileName + "-" + lineNum + " \n", testCaseDir + forkAddedNodeTxt);
                    if (!cleanCode.startsWith("#")) {
                        iof.writeTofile(newFileName + "-" + lineNum + " "+(commitSHAList.indexOf(sha)+1)+"\n", testCaseDir + expectTxt);
                    }
                } else if (cleanCode.startsWith("/*")) {
                    comments = true;
                } else if (cleanCode.startsWith("*/") && comments) {
                    comments = false;
                }
            }


        }


    }

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

    public static void main(String[] args) {
//        String dir = "C:\\Users\\shuruiz\\Documents\\PRISM-CR\\MCS61_CR\\result";
//        String outputfile = "C:\\Users\\shuruiz\\Documents\\PRISM-CR\\MCS61_CR\\forkAddedCode.txt";
//        String dir = "/Users/shuruiz/Work/Open\\ Source\\ Project/Suricata/suricata";
        String dir = "/Users/shuruiz/Work/Open Source Project/Suricata/suricata";
        String outputfile = "/Users/shuruiz/Work/Open Source Project/Suricata/suricata/forkAddedCode.txt";
        iof.rewriteFile("", outputfile);

        ArrayList<String> commitSHAList = new ArrayList<>();
        commitSHAList.add("xxx");
        commitSHAList.add("81c8149");

        ArrayList<String> changedFileList = new ArrayList<>();
//        changedFileList.add("src/detect-engine-address.c");
//        changedFileList.add("src/detect-engine-address.h");
//        changedFileList.add("src/detect-engine-port.c");
//        changedFileList.add("src/detect-engine-port.h");
//        changedFileList.add("src/detect-engine-proto.c");
//        changedFileList.add("src/detect-engine-proto.h");
//        changedFileList.add("src/detect-pcre.c");
//        changedFileList.add("src/detect-recursive.c");
//        changedFileList.add("src/detect-recursive.h");
        changedFileList.add("src/detect.c");
//        changedFileList.add("src/detect.h");
//        changedFileList.add("src/suricata.c");
//        changedFileList.add("src/detect-engine-analyzer.c");
//        changedFileList.add("src/detect-engine-analyzer.h");

        for (String changedFile : changedFileList) {
            new GetForkAddedCode().identifyChangedCodeBySHA(dir, changedFile, commitSHAList);

        }

    }
}
