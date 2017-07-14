package NamingClusters;

import CommunityDetection.AnalyzingCommunityDetectionResult;
import Util.ProcessingText;

import java.io.*;
import java.util.*;

/**
 * Created by shuruiz on 10/23/16.
 */
public class IdentifyingKeyWordForCluster {

    TFIDF tfidf = new TFIDF();
    static final String FS = File.separator;
    static HashMap<String, ArrayList<String>> commitInfoMap;
    static ArrayList<String> topClusterList = new ArrayList<>();
    static HashMap<String, String> nodeIdMap = new HashMap<>();
    HashMap<String, String> oneGram_sourceCodeLocMap = new HashMap<>();
    HashMap<String, String> twoGram_sourceCodeLocMap = new HashMap<>();
    String isJoined_str = "";

    public IdentifyingKeyWordForCluster(String sourcecodeDir, String analysisDir, HashMap<String, ArrayList<String>> commitInfoMap) {
        /**  tokenization **/
        System.out.println("        Tokenizing source code...");
        ProcessingText processingText = new ProcessingText();

        boolean hasTokenizedSourceCode = false;
        try {
            if (new File(analysisDir + "tokenizedSouceCode_oneGram.txt").exists()) {
                String tokenizedSourceCode = processingText.readResult(analysisDir + "tokenizedSouceCode_oneGram.txt");
                if (tokenizedSourceCode.trim().length() > 0) {
                    hasTokenizedSourceCode = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!hasTokenizedSourceCode) {
            new Tokenizer().tokenizeSourceCode(sourcecodeDir, analysisDir);
        }
        this.commitInfoMap = commitInfoMap;


        System.out.println("    list top X biggest cluster id");
        String[] topClusters = null;
        try {
            topClusters = new ProcessingText().readResult(analysisDir + "topClusters.txt").split("\n");
            nodeIdMap = processingText.getNodeIdMap(analysisDir, processingText);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String tc : topClusters) {
            topClusterList.add(tc);
        }

        oneGram_sourceCodeLocMap = processingText.getnodeToSourceCodeMap(1, analysisDir);
        twoGram_sourceCodeLocMap = processingText.getnodeToSourceCodeMap(2, analysisDir);

    }


    public HashMap<String, ArrayList<String>> findKeyWordsFor_eachSplitStep(String testCaseDir, String testDir, HashMap<String, HashSet<Integer>> clusterList, int n_gram, String splitStep) {
        String analysisDir = testCaseDir + testDir + FS;
        HashMap<String, ArrayList<String>> keyWordList = new HashMap<>();
        ProcessingText processingText = new ProcessingText();
        StringBuffer sb = new StringBuffer();

        HashMap<String, String> sourceCodeLocMap = n_gram == 1 ? oneGram_sourceCodeLocMap : twoGram_sourceCodeLocMap;
        // doc stores all the documents
        List<String> all_sourceCode_doc = new ArrayList<>();
        /** get node label -- node content **/
        if (clusterList.size() == 1) {
            if (n_gram == 1) {
                processingText.rewriteFile("", analysisDir + splitStep + isJoined_str + "_one_keyWord.txt");
                all_sourceCode_doc.addAll(oneGram_sourceCodeLocMap.values());
            } else if (n_gram == 2) {
                processingText.rewriteFile("", analysisDir + splitStep + isJoined_str + "_two_keyWord.txt");
                all_sourceCode_doc.addAll(twoGram_sourceCodeLocMap.values());
            }
        }

        /** get commit msg
         * other commit msg added into all
         * cluster -related commit add to cluster
         * **/
        ArrayList<String> topClusterList = new ProcessingText().getListFromFile(analysisDir, "topClusters.txt");
        HashMap<String, HashMap<String, Integer>> clusterID_termFrequency = new HashMap<>();

        clusterList.forEach((clusterID, v) -> {
            if (processingText.isTopCluster(topClusterList, clusterID)) {
                System.out.println("parsing cluster: " + clusterID + " 's keyword..");
                /** add source code **/
               final HashMap<String, Integer>[] term_count = new HashMap[]{new HashMap<>()};

                for (Integer cl_int : v) {
                    String cl = cl_int.toString();
                    if (cl.length() > 0 && !cl.contains("-")) {
                        v.forEach(nodeId -> {
                            String nodeContent = sourceCodeLocMap.get(nodeIdMap.get(nodeId + ""));
                            if (nodeContent != null) {
                                term_count[0] = getTermCountMap(nodeContent, term_count[0]);

                                int n_gram_index = n_gram == 1 ? 2 : 3;
                                String commit = commitInfoMap.get(nodeId + "").get(n_gram_index);
                                term_count[0] = getTermCountMap(commit, term_count[0]);
                            }
                        });
                    }
                }
                clusterID_termFrequency.put(clusterID, term_count[0]);
            }
        });
        sb.append(tfidf.calculateTfIdf_new(clusterID_termFrequency));

        if (n_gram == 1) {
            processingText.rewriteFile(sb.toString(), analysisDir + splitStep + isJoined_str + "_one_keyWord.txt");
        } else if (n_gram == 2) {
            processingText.rewriteFile(sb.toString(), analysisDir + splitStep + isJoined_str + "_two_keyWord.txt");
        }
        return keyWordList;
    }

    private HashMap<String, Integer> getTermCountMap(String nodeContent, HashMap<String, Integer> term_count) {
        for (String word : nodeContent.split(" ")) {
            if (word.length() > 0) {

                if (term_count.get(word) != null) {
                    int count = term_count.get(word);
                    term_count.put(word, count + 1);
                } else {
                    term_count.put(word, 1);
                }
            }
        }
        return term_count;
    }


    public void findKeyWordForEachSplit(String sourcecodeDir, String analysisDir, String testDir, String splitStep, String repoPath, boolean isJoined) {
        this.isJoined_str = isJoined ? "_joined" : "";
        System.out.println("    list top X biggest cluster id");


        AnalyzingCommunityDetectionResult acdr = new AnalyzingCommunityDetectionResult(analysisDir);
        boolean isOriginalGraph = false;

        HashMap<Integer, HashMap<String, HashSet<Integer>>> clusterResultMap = acdr.getClusteringResultMapforClusterID(splitStep, isOriginalGraph, isJoined);

        clusterResultMap.forEach((k, v) -> {
            HashMap<String, HashSet<Integer>> currentClusterMap = v;
            /**  calculate tfidf  to identifing keywords from each cluster**/
            System.out.println("        identifying keywords from one gram list...");
            findKeyWordsFor_eachSplitStep(analysisDir, testDir, currentClusterMap, 1, splitStep);
            System.out.println("        identifying keywords from two gram list...");
            findKeyWordsFor_eachSplitStep(analysisDir, testDir, currentClusterMap, 2, splitStep);

            /**  merging one-gram and two-gram result**/
            System.out.println("        merging one-gram and two-gram result.. removing redundancy...");
            mergeOne_two_gram(analysisDir, splitStep);

        });


    }

    private void mergeOne_two_gram(String analysisDir, String splitStep) {
        ProcessingText pt = new ProcessingText();
        HashMap<String, HashSet<String>> clusterID_keyword = new HashMap<>();
        try {
            String[] topClusterID = pt.readResult(analysisDir + "topClusters.txt").split("\n");
            for (String cl : topClusterID) {
                String twoGramList[] = pt.readResult(analysisDir + splitStep + isJoined_str + "_two_keyWord.txt").split("\n");
                HashSet<String> keywordSet = new HashSet<>();
                HashSet<String> one_wordSet = new HashSet<>();

                for (String two : twoGramList) {
                    String clusterID = two.split(":")[0];
                    if (two.startsWith(cl)) {

                        if (two.replace(cl, "").replace(":", "").replace("[", "").replace("]", "").trim().length() > 0) {

                            int start = two.indexOf("[");
                            int end = two.indexOf("]");
                            String[] keywords = two.substring(start + 1, end - 1).split(",");

                            for (String s : keywords) {
                                if (s.split("_").length > 1) {
                                    one_wordSet.addAll(new HashSet<String>(Arrays.asList(s.split("_"))));
                                } else {
                                    one_wordSet.add(s);
                                }
                            }

                            keywordSet = new HashSet<String>(Arrays.asList(keywords));
                            clusterID_keyword.put(clusterID, keywordSet);
                        } else {
                            keywordSet = new HashSet<String>();
                            keywordSet.add("no-meaningful-keyword");
                            clusterID_keyword.put(clusterID, keywordSet);
                        }
                    }
                }

                String[] oneGramList = pt.readResult(analysisDir + splitStep + isJoined_str + "_one_keyWord.txt").split("\n");
                for (String one : oneGramList) {
                    if (one.startsWith(cl) && one.replace(cl, "").replace(":", "").replace("[", "").replace("]", "").trim().length() > 0) {

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
        } catch (
                IOException e)

        {
            e.printStackTrace();
        }

        StringBuffer sb = new StringBuffer();
        clusterID_keyword.forEach((k, v) ->

        {

            sb.append(k + ": " + v.toString());

            sb.append("\n");
        });

        pt.rewriteFile(sb.toString(), analysisDir + splitStep + isJoined_str + "_keyword.txt");
        System.out.println("done with generating keyword for "+splitStep);
    }

}
