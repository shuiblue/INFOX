package NamingClusters;

import java.util.*;

/**
 * Created by shuruiz on 10/11/16.
 */
public class TFIDF {


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

    public double idf_new(HashMap<String, HashMap<String, Integer>> clusterID_term_count, String term) {
        final double[] n = {0};
        clusterID_term_count.forEach((clusterID, map) -> {
            if (map.keySet().contains(term)) {
                n[0]++;
            }
        });

        return Math.log(clusterID_term_count.keySet().size() / n[0]);
    }


    /**
     * @param clusterID_term_count a map  clusterID-  term and times it appears
     * @return the TF-IDF of term
     */
    public String calculateTfIdf_new(HashMap<String, HashMap<String, Integer>> clusterID_term_count) {
        StringBuilder sb = new StringBuilder();
        clusterID_term_count.forEach((clusterid, map) -> {

            HashMap<String, Double> term_tfidf = new HashMap<>();
            int total = 0;
            for (int count : map.values()) {
                total += count;
            }
            int finalTotal = total;
            map.forEach((term, count) -> {
                double tf = (double)count / finalTotal;
                double idf = idf_new(clusterID_term_count, term);
                term_tfidf.put(term, tf * idf);
            });

            sb.append(clusterid + ": [");
            term_tfidf.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(10)
                    .forEach(item -> sb.append(item.getKey() + ", "));
            sb.append("]\n");


        });
        return sb.toString();
    }


    public double calculateTfIdf(List<String> doc, List<List<String>> docs, String term) {
        return tf(doc, term) * idf(docs, term);

    }

    public static void main(String[] args) {

        List<String> doc1 = Arrays.asList("Lorem", "ipsum", "dolor", "ipsum", "sit", "ipsum");
        List<String> doc2 = Arrays.asList("Vituperata", "incorrupte", "at", "ipsum", "pro", "quo");
        List<String> doc3 = Arrays.asList("Has", "persius", "disputationi", "id", "simul");
        List<List<String>> documents = Arrays.asList(doc1, doc2, doc3);

        TFIDF calculator = new TFIDF();
        double tfidf = calculator.calculateTfIdf(doc1, documents, "ipsum");
        System.out.println("TF-IDF (ipsum) = " + tfidf);


    }

}
