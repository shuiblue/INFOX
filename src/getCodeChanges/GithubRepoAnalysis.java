package getCodeChanges;

import Util.ProcessingText;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by shuruiz on 3/30/17.
 */
public class GithubRepoAnalysis {
    ProcessingText processingText = new ProcessingText();

    public HashMap<String, ArrayList<Integer>> getChangedCodeForGithubRepo(String diffFilePath) {
        ProcessingText processingText = new ProcessingText();
        HashMap<String, ArrayList<Integer>> changedFile_line_map = new HashMap<>();
        ArrayList<Integer> lineNumberList;
        try {
            String diffFileContent = processingText.readResult(diffFilePath);
            String[] changedFileDiffSet = diffFileContent.split("diff --git");
            String currentFileName = "";
            for (String changedFile : changedFileDiffSet) {
                if (!changedFile.equals("")) {
                    if (currentFileName.equals("")) {
                        String[] content = changedFile.split("\n");
                        currentFileName = content[0].split(" ")[1].replace("a/", "");

                        if (processingText.isCFile_general(currentFileName)) {
                            lineNumberList = new ArrayList<>();
                            changedFile_line_map.put(currentFileName, lineNumberList);

                            System.out.println(currentFileName);
                            String[] hunkArray = changedFile.split("\n@@ ");

                            for (String hunk : hunkArray) {
                                if (hunk.startsWith("-")) {
                                    int addIndex = hunk.indexOf("+") + 1;
                                    int endIndex = hunk.lastIndexOf("@@");
                                    String range = hunk.substring(addIndex, endIndex);
                                    String[] rangeDetail = range.split(",");
                                    String[] lineArray = hunk.split("\n");
                                    int startLineNum = Integer.valueOf(rangeDetail[0].trim());
                                    System.out.println(startLineNum);
                                    int i = 0;
                                    for (String line : lineArray) {
                                        if (!line.contains("@@")) {
                                            if (!line.startsWith("-")) {
                                                if (line.startsWith("+")) {
                                                    line = line.replace("+", "");
                                                    if (processingText.isCode(line)) {
                                                        lineNumberList.add(startLineNum + i);
                                                    }
                                                }

                                                i++;
                                            }

                                        }
                                    }

//                                    if (rangeDetail.length > 1) {
//                                        int i = 0;
//                                        for (String line : lineArray) {
//                                            if (line.startsWith("+")) {
//                                                line = line.replace("+", "");
//                                                if (processingText.isCode(line)) {
//                                                    lineNumberList.add(startLineNum + i);
//                                                }
//                                                i++;
//                                            }
//
//                                        }
//                                    } else {
//                                        for (String line : lineArray) {
//                                            if (line.startsWith("+")) {
//                                                if (processingText.isCode(line.substring(1))) {
//                                                    lineNumberList.add(startLineNum);
//                                                }
//                                            }
//                                        }
//
//                                    }
                                }
                            }
                        }

                    }
                }
                currentFileName = "";

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return changedFile_line_map;
    }

    public void generateForkAddedNodeFile(HashMap<String, ArrayList<Integer>> changedFile_line_map, String
            output) {
        StringBuilder sb = new StringBuilder();

        Iterator it = changedFile_line_map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String fileName = (String) pair.getKey();
            String newFileName = processingText.changeFileName(fileName);
            ArrayList<Integer> lineNumList = (ArrayList<Integer>) pair.getValue();
            for (Integer lineNum : lineNumList) {
                sb.append(newFileName + "-" + lineNum + "\n");
            }
        }
        processingText.rewriteFile(sb.toString(), output);


    }

    public static void main(String[] args) {
        String dir = "/Users/shuruiz/Work/MarlinRepo/MarlinForks/gralco_Marlin/";
        String diffFilePath = "diff.txt";
        String forkAddedNode_file = "forkAddedNode.txt";
        GithubRepoAnalysis githubRepoAnalysis = new GithubRepoAnalysis();
        HashMap<String, ArrayList<Integer>> changedFile_line_map = githubRepoAnalysis.getChangedCodeForGithubRepo(dir + diffFilePath);
        githubRepoAnalysis.generateForkAddedNodeFile(changedFile_line_map, dir + forkAddedNode_file);
    }


}
