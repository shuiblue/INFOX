package NamingClusters;

import Util.ProcessingText;

import java.io.*;
import java.util.*;

/**
 * Created by shuruiz on 10/22/16.
 */
public class GetCommitMsg {
    static final String FS = File.separator;

    public GetCommitMsg( String testCaseDir, String testDir, HashMap<Integer, ArrayList<String>> clusterList, int n_gram,String repoPath) {


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
                if (cl.length() > 0) {

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

                                String commit[] = getCommitMsgForEachNode(fileName, lineNumber, n_gram,repoPath);


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

    /**
     * This function parses commit msg for each line of code based on filename and linenumber by git blame cmd
     *
     * @param fileName
     * @param lineNumber
     * @return commit msg
     */
    public static String[] getCommitMsgForEachNode(String fileName, String lineNumber, int n_gram,String repoPath) {
        String dir = "Marlin/";
        Stemmer stemmer = new Stemmer();
        Tokenizer tokenizer = new Tokenizer();
        String lineInfo = lineNumber + "," + lineNumber + ":"+dir + fileName;
        ProcessBuilder processBuilder = new ProcessBuilder("git", "log", "--pretty=medium", "-L", lineInfo);
        processBuilder.directory(new File(repoPath).getParentFile()); // this is where you set the root folder for the executable to run with

        processBuilder.redirectErrorStream(true);
        Process process = null;
        InputStream inputStream = null;
        try {
            process = processBuilder.start();
//            process.waitFor();
            inputStream = process.getInputStream();


        } catch (IOException e) {
            e.printStackTrace();
        }
        String commitSHA = "";
        BufferedReader br = null;
        String line = null;
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
                                for (String s : tokenizer.generateNgrams(n_gram, word)) {
                                    normalized_line += s + " ";
                                }
                            } else {
                               word = new StopWords().removeStopWord(word);
                                if(word.trim().length()>0) {
                                    normalized_line += stemmer.stemmingAWord(word) + " ";
                                }
                            }
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String[] commit = new String[]{commitSHA, normalized_line,originCommit};

        return commit;
    }


    public static void main(String[] args) {

//        getCommitMsgForEachNode("Marlin/Marlin_main.cpp", "482");

    }


}
