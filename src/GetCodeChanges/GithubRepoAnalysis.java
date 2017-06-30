package GetCodeChanges;

import MocGithub.ParseHtml;
import Util.ProcessingText;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by shuruiz on 3/30/17.
 */
public class GithubRepoAnalysis {
    static ProcessingText processingText = new ProcessingText();
    static final String FS = File.separator;

    public HashMap<String, ArrayList<Integer>> getChangedCodeForGithubRepo(String analysisDir) {
        System.out.println("analyzing diff.txt file...");
        ProcessingText processingText = new ProcessingText();
        String[] changedFileArray = {};
        try {
            changedFileArray = processingText.readResult(analysisDir + "/changedFileList.txt").replace("[", "").replace("]", "").split(",");
        } catch (IOException e) {
            e.printStackTrace();
        }
        HashMap<String, ArrayList<Integer>> changedFile_line_map = new HashMap<>();
        ArrayList<Integer> lineNumberList;
        try {
            String diffFilePath = analysisDir + "/diff.txt";
            String diffFileContent = processingText.readResult(diffFilePath);
//            String[] changedFileDiffSet = diffFileContent.split("diff --git");
            String[] changedFileDiffSet = diffFileContent.split("INFOX_DIFF_BLOCK\n");
            String currentFileName = "";
            for (int x =0; x < changedFileDiffSet.length; x++) {
                String changedFile = changedFileDiffSet[x];
                if (!changedFile.equals("")) {
                    if (currentFileName.equals("")) {
//                        String[] content = changedFile.split("\n");
//                        currentFileName = content[0].split(" ")[2].replace("b/", "");
                        currentFileName = changedFileArray[x-1].trim();

                        if (processingText.isCFile_general(currentFileName)) {
                            lineNumberList = new ArrayList<>();
                            changedFile_line_map.put(currentFileName, lineNumberList);

                            System.out.println(currentFileName);
                            String[] hunkArray = changedFile.split("\n@@ ");

                            for (String hunk : hunkArray) {
                                if (hunk.startsWith("-")||hunk.startsWith("@@")) {
                                    int addIndex = hunk.indexOf("+") + 1;
                                    int endIndex = hunk.lastIndexOf("@@");
                                    String range = hunk.substring(addIndex, endIndex);
                                    String[] rangeDetail = range.split(",");
                                    String[] lineArray = hunk.split("\n");
                                    int startLineNum = Integer.valueOf(rangeDetail[0].trim());
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
        System.out.println("generating fork added node list..");

        StringBuilder sb = new StringBuilder();

        Iterator it = changedFile_line_map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String fileName = (String) pair.getKey();

            if (!fileName.toString().contains("matlab")
                    && !fileName.toString().contains("example_configurations")
                    && !fileName.toString().contains("ArduinoAddons")
                    && !fileName.toString().contains("language")
                    ) {

                String newFileName = processingText.changeFileName(fileName);
                ArrayList<Integer> lineNumList = (ArrayList<Integer>) pair.getValue();
                for (Integer lineNum : lineNumList) {
                    sb.append(newFileName + "-" + lineNum + "\n");
                }
            }
        }
        processingText.rewriteFile(sb.toString(), output);


    }


    public void calculatingAvgSizeOfCodeChanges(String forkListFilePath, String publicToken, String root, String projectName) {
         String originalPage = "original.html";
        try {
            String[] forkListArray = processingText.readResult(forkListFilePath).split("\n");

            processingText.rewriteFile("", root + projectName + "_codeChangeSize.csv");
            for (String forkName : forkListArray) {
                String analysisDir = root + forkName + FS + "INFOX_output/";
                ParseHtml parseHtml = new ParseHtml(0, 0, analysisDir, publicToken);
//              String diffPageUrl = parseHtml.getDiffPageUrl(localSourceCodeDirPath,forkName,"3 month");
                String localSourceCodeDirPath = root + forkName + FS;
                String diffPageUrl = parseHtml.getDiffPageUrl(localSourceCodeDirPath, forkName, "withUpstream");

                ProcessingText processingText = new ProcessingText();
                processingText.getDiffText(forkName,analysisDir,originalPage, localSourceCodeDirPath + "INFOX_output/diff.txt");
//                processingText.readTextFromURL(diffPageUrl + ".diff", localSourceCodeDirPath + "INFOX_output/diff.txt");
//                /***   get fork added node, generate ForkAddedNode.txt file   ***/
//                GithubRepoAnalysis githubRepoAnalysis = new GithubRepoAnalysis();
//                HashMap<String, ArrayList<Integer>> changedFile_line_map = githubRepoAnalysis.getChangedCodeForGithubRepo(localSourceCodeDirPath + "INFOX_output/diff.txt");
//                githubRepoAnalysis.generateForkAddedNodeFile(changedFile_line_map, localSourceCodeDirPath + "INFOX_output/forkAddedNode.txt");
                /*** hack github page   ***/
                parseHtml.getOriginalDiffPage(diffPageUrl, localSourceCodeDirPath, forkName);

                int size = processingText.readResult(localSourceCodeDirPath + "INFOX_output/forkAddedNode.txt").split("\n").length;

                processingText.writeTofile(forkName + "," + size + "\n", root + projectName + "_codeChangeSize.csv");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        String dir = "/Users/shuruiz/Work/MarlinRepo/MarlinForks/gralco_Marlin/";
//        String diffFilePath = "diff.txt";
//        String forkAddedNode_file = "forkAddedNode.txt";
        GithubRepoAnalysis githubRepoAnalysis = new GithubRepoAnalysis();
        String projectName = "ofxGifEncoder";
        String folder = "check" + projectName + "ForkSize/";
        String root = "/Users/shuruiz/Work/checkProjectSize/";
        String token = null;
        try {
            token = processingText.readResult(root + "/token.txt").trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        githubRepoAnalysis.calculatingAvgSizeOfCodeChanges(root + "/" + folder + projectName + "ForkList.txt", token, root, projectName);


//        HashMap<String, ArrayList<Integer>> changedFile_line_map = githubRepoAnalysis.getChangedCodeForGithubRepo(dir + diffFilePath);
//        githubRepoAnalysis.generateForkAddedNodeFile(changedFile_line_map, dir + forkAddedNode_file);
    }

}
