package NamingClusters;

import Util.ProcessingText;

import java.io.*;
import java.util.*;

/**
 * Created by shuruiz on 10/22/16.
 */
public class GetCommitMsg {
    static HashMap<String, String> label_to_id = null;
    /**
     * This function generates a mapping between nodeID to commit msg arraylist, which contains {sha, original commit msg, one gram, two gram}
     * @param analysisDir
     * @param repoPath
     * @return
     */
    public HashMap<String, ArrayList<String>> getCommitMsgForChangedCode(String analysisDir, String repoPath) {
        StringBuilder sb = new StringBuilder();
        System.out.println("        getting commit messages for current split...");
        label_to_id = new ProcessingText().getNodeLabel_to_id_map(analysisDir + "nodeLable2IdMap.txt");
        HashMap<String, ArrayList<String>> nodeID_commitMsg_ngram_List = new HashMap<>();

        ProcessingText processingText = new ProcessingText();

        ArrayList<String> forkaddedNodeList = null;
        try {
            forkaddedNodeList = processingText.getForkAddedNodeList(analysisDir + "forkAddedNode.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Get commit msg for each added line ...");
        for (String nodeLabel : forkaddedNodeList) {
            String originFileName = processingText.getOriginFileName(nodeLabel);
            String lineNumber = nodeLabel.split("-")[1];

            //commit[] ={commitSHA, originCommit}
            String originalCommit[] = getCommitMsgForEachNode(originFileName, lineNumber, repoPath);
            String commitMsg = originalCommit[1];
            String one_commit = getCommitMsgForEachNode(commitMsg, 1);
            if(one_commit  .length()==0){
                one_commit="not_english";
            }
            String two_commit = getCommitMsgForEachNode(commitMsg, 2);
            if(two_commit.length()==0){
                two_commit="not_english";
            }
            String nodeId = label_to_id.get("\"" + nodeLabel + "\"");
            if (nodeId != null) {
                ArrayList<String> commitInfoList = new ArrayList<>();
                //SHA
                commitInfoList.add(originalCommit[0]);
                // Original commit
                commitInfoList.add(originalCommit[1]);
                commitInfoList.add(one_commit);
                commitInfoList.add(two_commit);
                nodeID_commitMsg_ngram_List.put(nodeId, commitInfoList);

                sb.append(nodeId + "," + originalCommit[0] + "," + originalCommit[1] + "," + one_commit + "," + two_commit + "\n");
            }
        }
        processingText.rewriteFile(sb.toString(), analysisDir + "nodeid_commit.txt");
        return nodeID_commitMsg_ngram_List;
    }

    public HashMap<String, ArrayList<String>> getCommitInfoMap(String analysisDir) {
        System.out.println("get nodeid to commit msg map..");
        HashMap<String, ArrayList<String>> commitInfoMap = new HashMap<>();
        try {
            String commitInfo[] = new ProcessingText().readResult(analysisDir + "nodeid_commit.txt").split("\n");
            for (String line : commitInfo) {
                String[] infoArray = line.split(",");
                String nodeID = infoArray[0];
                ArrayList<String> infoList = new ArrayList<>();

                infoList.add(infoArray[1]);
                infoList.add(infoArray[2]);
                infoList.add(infoArray[3]);
                infoList.add(infoArray[4]);
                commitInfoMap.put(nodeID, infoList);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return commitInfoMap;
    }

    /**
     * This function parses commit msg for each line of code based on filename and linenumber by git blame cmd
     *
     * @param fileName
     * @param lineNumber
     * @return String[]{commitSHA, line};
     */
    public static String[] getCommitMsgForEachNode(String fileName, String lineNumber, String repoPath) {

        String lineInfo = lineNumber + "," + lineNumber + ":" + fileName;
        ProcessBuilder processBuilder = new ProcessBuilder("git", "log", "--pretty=medium", "-L", lineInfo);
        processBuilder.directory(new File(repoPath).getParentFile()); // this is where you set the root folder for the executable to run with
        processBuilder.redirectErrorStream(true);
        Process process;
        InputStream inputStream = null;
        try {
            process = processBuilder.start();
            inputStream = process.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String commitSHA = "";
        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = br.readLine()) != null) {
                if (line.startsWith("diff")) {
                    break;
                }
                if (line.startsWith("commit")) {
                    commitSHA = line.substring(7);
                }
                if (!line.startsWith("commit") && !line.startsWith("Author") && !line.startsWith("Date") && line.trim().length() > 0) {
                    return new String[]{commitSHA, line};
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    /**
     * This function generates n-gram terms for commit msg
     *
     * @param commitMsg original commit msg
     * @param n_gram    int n  1 or 2
     * @return normalized_line
     */
    public static String getCommitMsgForEachNode(String commitMsg, int n_gram) {
        String normalized_line = "";
        String newline = commitMsg.toLowerCase().replaceAll("[^a-zA-Z0-9_]", " ");
        for (String word : newline.split(" ")) {
            word = word.trim();
            if (word.length() > 0) {
                if (word.contains("_") ) {
//                if (word.contains("_") && n_gram == 2) {
                    for (String s : new Tokenizer().generateNgrams(n_gram, word)) {
                        normalized_line += s + " ";
                    }
                } else {
                    if (!word.matches(("[0-9]*"))) {
                        word = new StopWords().removeStopWord(word);
                        if (word.trim().length() > 0) {
                            normalized_line += new Stemmer().stemmingAWord(word) + " ";
                        }
                    }
                }
            }
        }
        return normalized_line;
    }


    public static void main(String[] args) {

    }


}
