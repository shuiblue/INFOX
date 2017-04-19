package NamingClusters;

import CommunityDetection.AnalyzingCommunityDetectionResult;
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


    public HashMap<String, ArrayList<String>> findKeyWordsFor_eachSplitStep(String testCaseDir, String testDir, HashMap<String, HashSet<Integer>> clusterList, int n_gram, String splitStep) {
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
                processingText.rewriteFile("", analysisDir + splitStep + "_one_keyWord.txt");

            } else if (n_gram == 2) {
                sourceCode = processingText.readResult(testCaseDir + "tokenizedSouceCode_twoGram.txt");
                processingText.rewriteFile("", analysisDir + splitStep + "_two_keyWord.txt");
            }
            String[] sourceCodeArray = sourceCode.split("\n");
            for (String s : sourceCodeArray) {
                sourceCodeLocMap.put(s.split(":")[0], s.split(":")[1]);
                doc.add(s.split(":")[1]);
            }

            /** get commit msg
             * other commit msg added into all
             * cluster -related commit add to cluster
             * **/

            docs.add(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }


        clusterList.forEach((clusterID, v) -> {
            for (Integer cl_int : v) {
                String cl = cl_int.toString();
                if (cl.length() > 0 && !cl.contains("-")) {
                    HashMap<String, String> clusterCommitMsg = new HashMap<>();
                    Set<String> clusterString = new HashSet<>();
                    Set<String> origin_clusterString = new HashSet<>();

                    v.forEach(nodeId -> {
                        String nodeLable = nodeIdMap.get(nodeId + "");

                        String nodeContent = sourceCodeLocMap.get(nodeIdMap.get(nodeId + ""));
                        if (nodeContent != null) {
                            for (String word : nodeContent.split(" ")) {
                                if (word.length() > 0) clusterString.add(word);
                            }
                        }
                    });


                }
            }

            /** add source code **/
            List<List<String>> newCode_docs = new ArrayList<>();
            List<String> clusterString = new ArrayList<>();
            newCode_docs.add(clusterString);


            /**get all the commit msg for changed code **/

            HashMap<String, ArrayList<String>> cluster_to_commit_map = new HashMap<>();

            String fileName_prefix = "";
            if (n_gram == 1) {
                fileName_prefix = "one";
            } else if (n_gram == 2) {
                fileName_prefix = "two";
            }

            try {
                ArrayList<String> commitForAllNewCode = null;
                String commitMsg = processingText.readResult(analysisDir + splitStep + "_" + fileName_prefix + "_commitMsgPerCluster.txt");

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
            words.forEach(w -> {
                if (w.trim().length() > 1) {
                    clusterString.add(w);
                }
            });

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
            sb.append(clusterID + ":  [");


            topTerms.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(10)
                    .forEach(item -> sb.append(item.getKey() + ", "));
            sb.append("]\n");


        });


        if (n_gram == 1) {
            processingText.writeTofile(sb.toString(), analysisDir + splitStep + "_one_keyWord.txt");
        } else if (n_gram == 2) {
            processingText.writeTofile(sb.toString(), analysisDir + splitStep + "_two_keyWord.txt");
        }
        return keyWordList;
    }


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
                if (cl.length() > 0 && !cl.contains("-")) {
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
                    words.forEach(w -> {
                        if (w.trim().length() > 1) {
                            clusterString.add(w);
                        }
                    });

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


    public void findKeyWordForEachSplit(String sourcecodeDir, String analysisDir, String testDir, String splitStep, String repoPath) {

        System.out.println("    list top X biggest cluster id");
        String[] topClusters = null;
        ArrayList<String> topClusterList = new ArrayList<>();
        try {
            topClusters = new ProcessingText().readResult(analysisDir + "topClusters.txt").split("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String tc : topClusters) {
            topClusterList.add(tc);
        }


        AnalyzingCommunityDetectionResult acdr = new AnalyzingCommunityDetectionResult(analysisDir);
        boolean isOriginalGraph = false;

        HashMap<Integer, HashMap<String, HashSet<Integer>>> clusterResultMap = acdr.getClusteringResultMapforClusterID(splitStep, isOriginalGraph);

        clusterResultMap.forEach((k, v) -> {
            HashMap<String, HashSet<Integer>> currentClusterMap = v;

            /**  tokenization **/
            System.out.println("        Tokenizing source code...");
//            new Tokenizer().tokenizeSourceCode(sourcecodeDir, analysisDir);
//
//            /** parse commit msg for each node **/
//            System.out.println("        getting commit messages for current split...");
//            System.out.println("        generating one gram term ...");
//            new GetCommitMsg().getCommitMsg_currentSplit(analysisDir, testDir, currentClusterMap, 1, repoPath, splitStep,topClusterList);
//            System.out.println("        generating two gram term ...");
//            new GetCommitMsg().getCommitMsg_currentSplit(analysisDir, testDir, currentClusterMap, 2, repoPath, splitStep,topClusterList);


            /**  calculate tfidf  to identifing keywords from each cluster**/
            System.out.println("        identifying keywords from one gram list...");
            findKeyWordsFor_eachSplitStep(analysisDir, testDir, currentClusterMap, 1,splitStep);
            System.out.println("        identifying keywords from two gram list...");
            findKeyWordsFor_eachSplitStep(analysisDir, testDir, currentClusterMap, 2,splitStep);

            /**  merging one-gram and two-gram result**/
            System.out.println("        merging one-gram and two-gram result.. removing redundancy...");
            mergeOne_two_gram(analysisDir, splitStep);

//            findKeyWordsForEachCut(analysisDir, testDir, currentClusterMap, 2);
        });


    }

    private void mergeOne_two_gram(String analysisDir, String splitStep) {


        ProcessingText pt = new ProcessingText();
        HashMap<String, HashSet<String>> clusterID_keyword = new HashMap<>();

        try {
            String[] topClusterID = pt.readResult(analysisDir + "topClusters.txt").split("\n");

            for (String cl:topClusterID) {
                String twoGramList[] = pt.readResult(analysisDir + splitStep + "_two_keyWord.txt").split("\n");
                HashSet<String> keywordSet = new HashSet<>();
                HashSet<String> one_wordSet = new HashSet<>();

                for (String two : twoGramList) {
                    if (two.startsWith(cl )) {
                        String clusterID = two.split(":")[0];
                        int start = two.indexOf("[");
                        int end = two.indexOf("]");
                        String[] keywords = two.substring(start + 1, end - 1).split(",");

                        for (String s : keywords) {
                            if (s.split("_").length > 1) {
                                one_wordSet.addAll(new HashSet<String>(Arrays.asList(s.split("_"))));
                            }else{
                                one_wordSet.add(s);
                            }
                        }

                        keywordSet = new HashSet<String>(Arrays.asList(keywords));
                        clusterID_keyword.put(clusterID, keywordSet);
                    }
                }

                String[] oneGramList = pt.readResult(analysisDir + splitStep + "_one_keyWord.txt").split("\n");
                for (String one : oneGramList) {
                    if (one.startsWith(cl)) {
                        int start = one.indexOf("[");
                        int end = one.indexOf("]");
                        String[] keywords = one.substring(start + 1, end - 1).split(",");
                        for (String oneKey : keywords) {
                            if (!one_wordSet.contains(oneKey)) {
                                keywordSet.add(oneKey);
                            }
                        }
                    }


                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuffer sb = new StringBuffer();
        clusterID_keyword.forEach((k, v) -> {

            sb.append(k + ": "+v.toString());

            sb.append("\n");
        });

        pt.rewriteFile(sb.toString(), analysisDir + splitStep + "_keyword.txt");
    }

}
