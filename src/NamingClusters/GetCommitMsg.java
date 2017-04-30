package NamingClusters;

import Util.ProcessingText;

import java.io.*;
import java.util.*;

/**
 * Created by shuruiz on 10/22/16.
 */
public class GetCommitMsg {
    static final String FS = File.separator;

    static HashMap<String, String> label_to_id = null;

    public GetCommitMsg() {

    }


    public void getCommitMsg_currentSplit(String testCaseDir, HashMap<String, HashSet<Integer>> clusterList, int n_gram, String repoPath, String splitStep, ArrayList<String> topClusterList) {
        String analysisDir = testCaseDir;
        HashMap<String, String> nodeIdMap = new HashMap<>();
        ProcessingText processingText = new ProcessingText();
        processingText.rewriteFile("", analysisDir + "commitMsgPerCluster.txt");

        String nodeIdList = null;
        try {
            nodeIdList = processingText.readResult(analysisDir + "NodeList.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] nodeIdArray = nodeIdList.split("\n");
        for (String s : nodeIdArray) {
            if (s.split("---------").length > 1) {
                nodeIdMap.put(s.split("---------")[0], s.split("---------")[1]);
            }
        }
        int clusterSize = clusterList.size();
        StringBuffer sb = new StringBuffer();
        StringBuffer sb_origin = new StringBuffer();
        sb.append(clusterSize + " clusters: \n");
        sb_origin.append(clusterSize + " clusters: \n");
        clusterList.forEach((clusterID, v) -> {
            if (topClusterList.contains(clusterID.split("_")[0])) {
                sb.append("\n[" + clusterID + "]");
                sb_origin.append("\n[" + clusterID + "]");
                System.out.println("cluster id: " + clusterID);
                for (Integer cl_int : v) {
                    String cl = cl_int.toString();
                    if (cl.length() > 0 && !cl.contains("-")) {
                        HashMap<String, String> clusterCommitMsg = new HashMap<>();
                        Set<String> clusterString = new HashSet<>();
                        Set<String> origin_clusterString = new HashSet<>();

                        v.forEach(nodeId -> {
                            String nodeLable = nodeIdMap.get(nodeId + "");

                            if (nodeLable != null) {
                                String fileName = processingText.getOriginFileName(nodeLable);
                                String lineNumber = nodeLable.split("-")[1];
                                String commit[] = getCommitMsgForEachNode(fileName, lineNumber, n_gram, repoPath);
                                if (!clusterCommitMsg.keySet().contains(commit[0])) {
                                    clusterCommitMsg.put(commit[0], commit[1]);
                                    clusterString.add(commit[1]);
                                    origin_clusterString.add(commit[2]);
                                }

                            }

                        });

                        // create an iterator
                        Iterator iterator = clusterString.iterator();
                        Iterator iterator_origin = origin_clusterString.iterator();

                        // check values
                        while (iterator.hasNext()) {
                            String str = iterator.next().toString();
                            sb.append(str + " ");
//                        System.out.println("str:" + str);
                        }
                        while (iterator_origin.hasNext()) {
                            String str = iterator_origin.next().toString();
                            sb_origin.append(str + " ");
                        }

                    }
                }

            }
        });
        /** generate file name based on n-gram's n **/
        String fileName_prefix = "";
        if (n_gram == 1) {
            fileName_prefix = "_one";
        } else if (n_gram == 2) {
            fileName_prefix = "_two";
        }
        processingText.rewriteFile(sb.toString(), analysisDir + splitStep + fileName_prefix + "_commitMsgPerCluster.txt");
        processingText.rewriteFile(sb_origin.toString(), analysisDir + splitStep + fileName_prefix + "_commitMsgPerCluster_originCommit.txt");
        System.out.println("writing to file commitMsgPerCluster.txt , done.");

    }


    public GetCommitMsg(String testCaseDir, String testDir, HashMap<Integer, ArrayList<String>> clusterList, int n_gram, String repoPath) {
        String analysisDir = testCaseDir + testDir + FS;
        HashMap<String, String> nodeIdMap = new HashMap<>();
        ProcessingText processingText = new ProcessingText();
        processingText.rewriteFile("", analysisDir + "commitMsgPerCluster.txt");

        String nodeIdList = null;
        try {
            nodeIdList = processingText.readResult(analysisDir + "NodeList.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] nodeIdArray = nodeIdList.split("\n");
        for (String s : nodeIdArray) {
            if (s.split("---------").length > 1) {
                nodeIdMap.put(s.split("---------")[0], s.split("---------")[1]);
            }
        }


        clusterList.forEach((k, v) -> {

            StringBuffer sb = new StringBuffer();
            StringBuffer sb_origin = new StringBuffer();
            sb.append(k + " clusters: \n");
            sb_origin.append(k + " clusters: \n");
            for (String cl : v) {
                if (cl.length() > 0 && !cl.contains("-")) {
                    HashMap<String, String> clusterCommitMsg = new HashMap<String, String>();
                    Set<String> clusterString = new HashSet<>();
                    Set<String> origin_clusterString = new HashSet<>();
                    String clusterID = cl.substring(0, cl.trim().indexOf(")"));

                    sb.append("\n[" + clusterID + "]");
                    sb_origin.append("\n[" + clusterID + "]");
                    String[] elementList = cl.trim().split(",");
                    int length = elementList.length;
                    for (int j = 0; j < length; j++) {
                        String nodeIdStr = elementList[j].replace("[", "").replace("]", "").replace(clusterID + ")", "").trim();
                        if (nodeIdStr.length() > 0) {
                            String nodeLable = nodeIdMap.get(nodeIdStr);
                            if (nodeLable != null) {
                                String fileName = processingText.getOriginFileName(nodeLable);
//                                int index = fileName.lastIndexOf("/");
//                                fileName = fileName.substring(index-8);
                                String lineNumber = nodeLable.split("-")[1];

                                String commit[] = getCommitMsgForEachNode(fileName, lineNumber, n_gram, repoPath);


                                if (!clusterCommitMsg.keySet().contains(commit[0])) {
                                    clusterCommitMsg.put(commit[0], commit[1]);
                                    clusterString.add(commit[1]);
                                    origin_clusterString.add(commit[2]);
                                }


                            }
                        }
                    }

                    // create an iterator
                    Iterator iterator = clusterString.iterator();
                    Iterator iterator_origin = origin_clusterString.iterator();

                    // check values
                    while (iterator.hasNext()) {
                        String str = iterator.next().toString();
                        sb.append(str + " ");
                    }
                    while (iterator_origin.hasNext()) {
                        String str = iterator_origin.next().toString();
                        sb_origin.append(str + " ");
                    }
                }
            }

            /** generate file name based on n-gram's n **/
            String fileName_prefix = "";
            if (n_gram == 1) {
                fileName_prefix = "one_";
            } else if (n_gram == 2) {
                fileName_prefix = "two_";
            }
            processingText.rewriteFile(sb.toString(), analysisDir + fileName_prefix + k + "_commitMsgPerCluster.txt");
            processingText.rewriteFile(sb_origin.toString(), analysisDir + fileName_prefix + k + "_commitMsgPerCluster_originCommit.txt");

        });
    }



    public HashMap<String, HashMap<String, String>> getCommitMsgForChangedCode(String analysisDir, String repoPath) {
        System.out.println("        getting commit messages for current split...");
        label_to_id = new ProcessingText().getNodeLabel_to_id_map(analysisDir + "nodeLable2IdMap.txt");
        HashMap<String, HashMap<String, String>> allKindsOf_unifiedCommits = new HashMap<>();

        HashMap<String, String> original_nodeid_to_commitMessage = new HashMap();
        HashMap<String, String> one_gram_nodeid_to_commitMessage = new HashMap();
        HashMap<String, String> two_gram_nodeid_to_commitMessage = new HashMap();
        HashMap<String, String> nodeid_to_SHA = new HashMap();
        ProcessingText processingText = new ProcessingText();

        String forkAddedNodeTxt = "forkAddedNode.txt";
        ArrayList<String> forkaddedNodeList = null;
        if (new File(analysisDir + forkAddedNodeTxt).exists()) {
            try {

                forkaddedNodeList = processingText.getForkAddedNodeList(analysisDir + forkAddedNodeTxt);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("file forkAddedNode.txt does not exist!");
            forkaddedNodeList = new ArrayList<>();
        }

        for (String nodeLabel : forkaddedNodeList) {
            String fileName = nodeLabel.split("-")[0];
            String originFileName = processingText.getOriginFileName(nodeLabel);
            String lineNumber = nodeLabel.split("-")[1];

            //commit[] ={commitSHA, one_gram, two_gram, originCommit}
            String one_commit[] = getCommitMsgForEachNode(originFileName, lineNumber, 1, repoPath);
            String two_commit[] = getCommitMsgForEachNode(originFileName, lineNumber, 2, repoPath);
            String nodeId = label_to_id.get("\"" + nodeLabel + "\"");
            if (nodeId != null) {
                nodeid_to_SHA.put(nodeId, one_commit[0]);
                original_nodeid_to_commitMessage.put(nodeId, one_commit[2]);
                one_gram_nodeid_to_commitMessage.put(nodeId, one_commit[1]);
                two_gram_nodeid_to_commitMessage.put(nodeId, two_commit[1]);
            }
        }

        System.out.println("        generating one gram term ...");
        allKindsOf_unifiedCommits.put("one_gram", one_gram_nodeid_to_commitMessage);

        System.out.println("        generating two gram term ...");
        allKindsOf_unifiedCommits.put("two_gram", two_gram_nodeid_to_commitMessage);
        allKindsOf_unifiedCommits.put("origin", original_nodeid_to_commitMessage);
        allKindsOf_unifiedCommits.put("nodeSHA", nodeid_to_SHA);
        return allKindsOf_unifiedCommits;
    }


    /**
     * This function parses commit msg for each line of code based on filename and linenumber by git blame cmd
     *
     * @param fileName
     * @param lineNumber
     * @return commit msg
     */
    public static String[] getCommitMsgForEachNode(String fileName, String lineNumber, int n_gram, String repoPath) {
        String lineInfo = lineNumber + "," + lineNumber + ":" + fileName;
        ProcessBuilder processBuilder = new ProcessBuilder("git", "log", "--pretty=medium", "-L", lineInfo);
        processBuilder.directory(new File(repoPath).getParentFile()); // this is where you set the root folder for the executable to run with
        processBuilder.redirectErrorStream(true);
        Process process ;
        InputStream inputStream = null;
        try {
            process = processBuilder.start();
            inputStream = process.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String commitSHA = "";
        BufferedReader br ;
        String line ;
        String originCommit = null;
        String normalized_line = "";
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
                    originCommit = line;
                    String newline = line.toLowerCase().replaceAll("[^a-zA-Z0-9_]", " ");

                    for (String word : newline.split(" ")) {
                        word = word.trim();
                        if (word.length() > 0) {


                            if (word.contains("_") && n_gram == 2) {
                                for (String s : new Tokenizer().generateNgrams(n_gram, word)) {
                                    normalized_line += s + " ";
                                }
                            } else {
                                if (!word.matches(("[0-9]*"))){
                                    word = new StopWords().removeStopWord(word);
                                    if (word.trim().length() > 0) {
                                        normalized_line += new Stemmer().stemmingAWord(word) + " ";
                                    }
                                }
                            }
                        }
                    }
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

        String[] commit = new String[]{commitSHA, normalized_line, originCommit};

        return commit;
    }


    public static void main(String[] args) {

//        getCommitMsgForEachNode("Marlin/Marlin_main.cpp", "482");

    }


}
