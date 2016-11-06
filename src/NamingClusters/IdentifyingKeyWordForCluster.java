package NamingClusters;

import Util.ProcessingText;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by shuruiz on 10/23/16.
 */
public class IdentifyingKeyWordForCluster {

    TFIDF tfidf = new TFIDF();
    static final String FS = File.separator;

    public HashMap<String, ArrayList<String>> findKeyWordsForEachCut(String testCaseDir, String testDir, HashMap<Integer, ArrayList<String>> clusterList, int n_gram) {
        String analysisDir = testCaseDir + testDir + FS;
        HashMap<String, ArrayList<String>> keyWordList = new HashMap<>();
        HashMap<String, String> nodeIdMap = new HashMap<>();
        ProcessingText processingText = new ProcessingText();
        HashMap<String, String> sourceCodeLocMap = new HashMap<>();
        List<List<String>> docs = new ArrayList<>();
        HashMap<Integer, String> clusterIndexToID = new HashMap<>();
        StringBuffer sb = new StringBuffer();
        try {
            /**   get node id -  node location (label)**/
            String nodeIdList = processingText.readResult(analysisDir + "NodeList.txt");
            String[] nodeIdArray = nodeIdList.split("\n");
            for (String s : nodeIdArray) {
                if (s.split("---------").length > 1) {
                    nodeIdMap.put(s.split("---------")[0], s.split("---------")[1]);
                }
            }
            String sourceCode = "";
            List<String> doc = new ArrayList<>();
            /** get node label -- node content **/
            if (n_gram == 1) {
                sourceCode = processingText.readResult(testCaseDir + "tokenizedSouceCode_oneGram.txt");
                processingText.rewriteFile("", analysisDir + "one_keyWord.txt");

            } else if (n_gram == 2) {
                sourceCode = processingText.readResult(testCaseDir + "tokenizedSouceCode_twoGram.txt");
                processingText.rewriteFile("", analysisDir + "two_keyWord.txt");
            }
            String[] sourceCodeArray = sourceCode.split("\n");
            for (String s : sourceCodeArray) {
                sourceCodeLocMap.put(s.split(":")[0], s.split(":")[1]);
                doc.add(s.split(":")[1]);
            }

            System.out.print("");
            /** get commit msg
             * other commit msg added into all
             * cluster -related commit add to cluster
             * **/


            docs.add(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }



        clusterList.forEach((k, v) -> {
            sb.append(k + " clusters: \n");

            for (String cl : v) {
                if (cl.length() > 0) {
                    List<String> clusterString = new ArrayList<>();
                    String clusterID = cl.substring(0, cl.trim().indexOf(")"));
                    String[] elementList = cl.trim().split(",");
                    int length = elementList.length;
                    for (int j = 0; j < length; j++) {
                        String nodeIdStr = elementList[j].replace("[", "").replace("]", "").replace(clusterID + ")", "").trim();
                        if (nodeIdStr.length() > 0) {
                            String nodeContent = sourceCodeLocMap.get(nodeIdMap.get(nodeIdStr));
                            if (nodeContent != null) {
                                for (String word : nodeContent.split(" ")) {
                                    if (word.length() > 0) clusterString.add(word);
                                }
                            }
                        }
                    }
                    /** add source code **/
                    List<List<String>> newCode_docs = new ArrayList<>();
                    newCode_docs.add(clusterString);


                    /**get all the commit msg for changed code **/

                    HashMap<String, ArrayList<String>> cluster_to_commit_map = new HashMap<>();

                    String fileName_prefix = "";
                    if (n_gram == 1) {
                        fileName_prefix = "one_";
                    } else if (n_gram == 2) {
                        fileName_prefix = "two_";
                    }


                    try {
                        ArrayList<String> commitForAllNewCode = null;
                        String commitMsg = processingText.readResult(analysisDir + fileName_prefix + k + "_commitMsgPerCluster.txt");

                        String[] clusterCommitArray = commitMsg.split("\n");
                        for (String s : clusterCommitArray) {
                            if (s.trim().startsWith("[")) {
                                int index = s.indexOf("]");
                                String clusterNumber = s.trim().substring(1, index);
                                String msg = s.substring(index + 1);
                                commitForAllNewCode = new ArrayList<String>();
                                commitForAllNewCode.addAll(new ArrayList<String>(Arrays.asList(msg.split(" "))));
                                cluster_to_commit_map.put(clusterNumber, commitForAllNewCode);
                                newCode_docs.add(commitForAllNewCode);
                            }
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    /**  add commit msg of changed code **/

                    ArrayList<String> words = cluster_to_commit_map.get(clusterID);
                    words.forEach(w ->
                            clusterString.add(w));

                    clusterIndexToID.put(docs.size() - 1, clusterID);

                    HashMap<String, Double> topTerms = new HashMap<>();
                    HashSet<String> clusterSet = new HashSet<>(clusterString);

                    for (String term : clusterSet) {
                        List<List<String>> all_docs = new ArrayList<List<String>>();
                        all_docs.addAll(docs);
                        all_docs.addAll(newCode_docs);

                        double weight = tfidf.calculateTfIdf(clusterString, all_docs, term);
                        final double[] maxDiff = new double[1];
                        final String[] minWeightTerm = new String[1];
                        if (!topTerms.keySet().contains(term)) {
                            if (topTerms.keySet().size() <= 9) {
                                topTerms.put(term, weight);
                            } else {
                                topTerms.forEach((a, b) -> {
                                    boolean findSmallerWeightTerm = (weight - b) > maxDiff[0];
                                    if (findSmallerWeightTerm) {
                                        maxDiff[0] = weight - b;
                                        minWeightTerm[0] = a;
                                    }
                                });
                                if (minWeightTerm[0] != null) {
                                    topTerms.remove(minWeightTerm[0]);
                                    topTerms.put(term, weight);
                                }
                            }

                        }
                    }
                    sb.append(clusterID + ": \n [");


                    topTerms.entrySet().stream()
                            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                            .limit(10)
                            .forEach(item -> sb.append(item.getKey() + ", "));
                    sb.append("]\n");


                }
            }


        });

        if (n_gram == 1) {
            processingText.writeTofile(sb.toString(), analysisDir + "one_keyWord.txt");
        } else if (n_gram == 2) {
            processingText.writeTofile(sb.toString(), analysisDir + "two_keyWord.txt");
        }
        return keyWordList;
    }


}
