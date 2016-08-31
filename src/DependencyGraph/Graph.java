package DependencyGraph;

/**
 * Created by shuruiz on 6/2/2016.
 *
 * The graph object contains the nodelist and edgelist.
 * The reverseEdgeList is in the inverted sequence of edgelist, for ___ purpose.
 *
 */

import java.util.HashMap;

public class Graph {
    HashMap<Integer, String> nodelist;
    HashMap<Integer, String> edgelist;
    HashMap<String, Integer> reverseEdgelist;

    double[] betweenness;
    double modularity;

    String removableEdgeLable;


    public Graph(String[] nlist, double[][] elist, double[] betweenness, double modularity) {

        nodelist = new HashMap<>();
        edgelist = new HashMap<>();
        reverseEdgelist = new HashMap<>();

        int node_id = 1;
        for (String node : nlist) {
            nodelist.put(node_id++, node);
        }
        for (int j = 1; j <= elist.length; j++) {
            edgelist.put(j, (int) elist[j - 1][0] + "," + (int) elist[j - 1][1]);
            reverseEdgelist.put((int) elist[j - 1][0] + "," + (int) elist[j - 1][1], j);
        }
        this.betweenness = betweenness;
        this.modularity = modularity;
    }

    /***  Getters ***/
    public HashMap<Integer, String> getNodelist() {
        return nodelist;
    }

    public HashMap<Integer, String> getEdgelist() {
        return edgelist;
    }

    public String getRemovableEdgeLable() {
        return removableEdgeLable;
    }

    public double[] getBetweenness() {
        return betweenness;
    }

    public double getModularity() {
        return modularity;
    }

    public HashMap<String, Integer> getReverseEdgelist() {
        return reverseEdgelist;
    }

    /***  Setters ***/
    public void setRemovableEdgeLable(String removableEdgeLable) {
        this.removableEdgeLable = removableEdgeLable;
    }
}
