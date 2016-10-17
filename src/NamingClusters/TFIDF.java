package NamingClusters;

import Util.ProcessingText;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by shuruiz on 10/11/16.
 */
public class TFIDF {

    final String FS = File.separator;

    /**
     * @param doc  list of strings
     * @param term String represents a term
     * @return term frequency of term in document
     */
    public double tf(List<String> doc, String term) {
        double result = 0;
        for (String word : doc) {
            if (term.equalsIgnoreCase(word))
                result++;
        }
        return result / doc.size();
    }

    /**
     * @param docs list of list of strings represents the dataset
     * @param term String represents a term
     * @return the inverse term frequency of term in documents
     */
    public double idf(List<List<String>> docs, String term) {
        double n = 0;
        for (List<String> doc : docs) {
            for (String word : doc) {
                if (term.equalsIgnoreCase(word)) {
                    n++;
                    break;
                }
            }
        }
        return Math.log(docs.size() / n);
    }

    /**
     * @param doc  a text document
     * @param docs all documents
     * @param term term
     * @return the TF-IDF of term
     */
    public double tfIdf(List<String> doc, List<List<String>> docs, String term) {
        return tf(doc, term) * idf(docs, term);

    }

    public HashMap<String, ArrayList<String>> findKeyWordsForEachCut(String testCaseDir, String testDir, HashMap<Integer, ArrayList<String>> clusterList) {
        String analysisDir = testCaseDir + testDir + FS;
        HashMap<String, ArrayList<String>> keyWordList = new HashMap<>();
        HashMap<String, String> nodeIdMap = new HashMap<>();
        ProcessingText processingText = new ProcessingText();
        HashMap<String, String> sourceCodeLocMap = new HashMap<>();
        List<List<String>> docs = new ArrayList<>();
        HashMap<Integer, String> clusterIndexToID = new HashMap<>();
        try {
            /**   get node id -  node location (label)**/
            String nodeIdList = processingText.readResult(analysisDir + "NodeList.txt");
            String[] nodeIdArray = nodeIdList.split("\n");
            for (String s : nodeIdArray) {
                if (s.split("---------")[0].equals("7389")) {
                    System.out.print("");
                }
                if (s.split("---------").length > 1) {
                    nodeIdMap.put(s.split("---------")[0], s.split("---------")[1]);
                }
            }

            /** get node label -- node content **/
            String sourceCode = processingText.readResult(testCaseDir + "StringList.txt");
            String[] sourceCodeArray = sourceCode.split("\n");
            for (String s : sourceCodeArray) {
                sourceCodeLocMap.put(s.split(":")[0], s.split(":")[1]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        processingText.rewriteFile("", analysisDir + "keyWord.com");
        clusterList.forEach((k, v) -> {
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
                    docs.add(clusterString);
                    clusterIndexToID.put(docs.size() - 1, clusterID);
                }
            }

            StringBuffer sb = new StringBuffer();


            sb.append(k +" clusters: \n");
            for (List<String> cluster : docs) {
                HashMap<String, Double> topTerms = new HashMap<>();
                HashSet<String> clusterSet = new HashSet<>(cluster);

                for (String term : clusterSet) {
                    double weight = tfIdf(cluster, docs, term);
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
                String clusterID = clusterIndexToID.get(docs.indexOf(cluster));
                sb.append(clusterID + ": \n [");


                topTerms.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .limit(10)
                        .forEach(item -> sb.append(item.getKey() + ", "));
                sb.append("]\n");

            }


            processingText.writeTofile(sb.toString(), analysisDir + "keyWord.com");

        });

        return keyWordList;
    }


    public static void main(String[] args) {

        List<String> doc1 = Arrays.asList("Lorem", "ipsum", "dolor", "ipsum", "sit", "ipsum");
        List<String> doc2 = Arrays.asList("Vituperata", "incorrupte", "at", "ipsum", "pro", "quo");
        List<String> doc3 = Arrays.asList("Has", "persius", "disputationi", "id", "simul");
        List<List<String>> documents = Arrays.asList(doc1, doc2, doc3);

        TFIDF calculator = new TFIDF();
        double tfidf = calculator.tfIdf(doc1, documents, "ipsum");
        System.out.println("TF-IDF (ipsum) = " + tfidf);


    }

}
