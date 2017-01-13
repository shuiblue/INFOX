package CommunityDetection;

import DependencyGraph.Graph;
import Util.ProcessingText;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import java.io.*;
import java.util.*;

/**
 * Created by shuruiz on 8/29/16.
 */
public class R_CommunityDetection {
    ArrayList<Double> modularityArray;
    HashSet<String> upstreamNode;
    HashSet<String> forkAddedNode;
    //    HashSet<String> upstreamEdge;
    HashMap<Integer, Boolean> checkedEdges;
    Graph originGraph;

    static String upstreamNodeTxt = "/upstreamNode.txt";
    static String forkAddedNodeTxt = "/forkAddedNode.txt";

    public ArrayList<Integer> cutSequence;
    ProcessingText ioFunc = new ProcessingText();
    HashMap<Integer, Double> modularityMap;
    static int bestCut;
    Rengine re = null;
    static final String FS = File.separator;
    //    double[][] shortestPathMatrix;
    static double[][] edgelist;
    static String[] nodelist;

    /**
     * This methods is detecting communities for changedCode.pajek.net, which is the graph for new code.
     *
     * @param testCaseDir
     * @param testDir
     * @param numOfcut
     * @param re
     * @return
     */
    public int detectingCommunitiesWithIgraph(String testCaseDir, String testDir, int numOfcut, Rengine re, boolean directedGraph) {
        modularityArray = new ArrayList<>();
        checkedEdges = new HashMap<>();
        cutSequence = new ArrayList<>();
//        upstreamNode = new HashSet<>();
        forkAddedNode = new HashSet<>();
        modularityMap = new HashMap<>();
        this.re = re;
        //------------------------WINDOWS------------------------
//        re.eval(".libPaths('C:/Users/shuruizDocuments/R/win-library/3.3/rJava/jri/x64)");
//        re.eval(".libPaths('C:/Users/shuruiz/Documents/R/win-library/3.3')");
        re.eval("library(igraph)");

        /*-------    calculating distance between clusters ----------
//        re.eval("completeGraph<-read.graph(\"" + fileDir + "/complete.pajek.net\", format=\"pajek\")");
//        re.eval("completeGraph<- simplify(comGraph)");
////        re.eval("completeGraph<-as.undirected(completeGraph)\n");
//        re.eval("E(completeGraph)$weight <- 1");
//        REXP shortestPath_R = re.eval("distMatrix <- shortest.paths(completeGraph, v=V(completeGraph), to=V(completeGraph))");
//        shortestPathMatrix = shortestPath_R.asMatrix();
        -----------*/
        String analysisDir = testCaseDir + testDir + FS;
        System.getProperty("java.library.path");


        System.out.println("oldg<-read_graph(\"" + testCaseDir + "changedCode.pajek.net\", format=\'pajek\')");
        re.eval("oldg<-read_graph(\"" + testCaseDir + "changedCode.pajek.net\", format=\'pajek\')");
        // removes the loop and/or multiple edges from a graph.
        re.eval("g<-simplify(oldg)");

        re.eval("originalg<-g");

        // get original graph
        REXP edgelist_R = re.eval("cbind( get.edgelist(g) , round( E(g)$weight, 3 ))", true);
        REXP nodelist_R = re.eval("get.vertex.attribute(g)$id", true);
        edgelist = edgelist_R.asDoubleMatrix();
        nodelist = (String[]) nodelist_R.getContent();
        originGraph = new Graph(nodelist, edgelist, null, 0);

        HashMap<Integer, String> nodeMap = originGraph.getNodelist();
        printOriginNodeList(nodeMap, analysisDir);

        File upstreamNodeFile = new File(analysisDir + upstreamNodeTxt);
        if (upstreamNodeFile.exists()) {
            //get nodes/edges belong to upstream
            storeNodes(analysisDir, upstreamNodeTxt);
        }
        //get fork added node
        File forkAddedFile = new File(testCaseDir + forkAddedNodeTxt);
        if (forkAddedFile.exists()) {
            storeNodes(testCaseDir, forkAddedNodeTxt);
        }

//        upstreamEdge = findUpstreamEdges(originGraph, fileDir);

        //print old edge
        ioFunc.rewriteFile("", analysisDir + "clusterTMP.txt");
        //initialize removedEdge Map, all the edges have not been removed, so the values are all false
        for (int i = 0; i < edgelist.length; i++) {
            checkedEdges.put(i + 1, false);
        }
        int cutNum = 1;
        while (checkedEdges.values().contains(false)) {
//            if (currentIteration <= numOfIteration) {
            if (listOfNumberOfCommunities.size() <= numOfcut) {
                //count betweenness for current graph
                calculateEachGraph(re, testCaseDir, testDir, cutNum, directedGraph);
                cutNum++;
            } else {
                break;
            }
        }

        findBestClusterResult(originGraph, cutSequence, analysisDir);
        writeToModularityCSV(analysisDir);
        re.end();
        System.out.println("\nBye.");
        return bestCut;
    }

    /**
     * This function stores the
     *
     * @param fileDir
     * @param filePath
     */
    private void storeNodes(String fileDir, String filePath) {
        String path = fileDir + filePath;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
//                if (filePath.contains("upstream")) {
//                    upstreamNode.add(line.trim());
//                } else {
//                    forkAddedNode.add(line.trim());
//                }
                forkAddedNode.add(line.split(" ")[0]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToModularityCSV(String fileDir) {
        StringBuffer csv = new StringBuffer();
        Iterator it = modularityMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry node = (Map.Entry) it.next();
            csv.append(node.getKey() + "," + node.getValue() + "\n");
        }
        ioFunc.rewriteFile(csv.toString(), fileDir + "modularity.csv");
//        ioFunc.rewriteFile(csv.toString(), fileDir + "/modularity.csv");

    }

    private void printOriginNodeList(HashMap<Integer, String> nodelist, String fileDir) {
        StringBuffer nodeList_print = new StringBuffer();
        Iterator it_e = nodelist.entrySet().iterator();
        while (it_e.hasNext()) {
            Map.Entry node = (Map.Entry) it_e.next();
            nodeList_print.append(node.getKey() + "---------" + node.getValue() + "\n");

        }
        ioFunc.rewriteFile(nodeList_print.toString(), fileDir + "NodeList.txt");
    }


//    private HashSet<String> findUpstreamEdges(Graph originGraph, String filePath) {
//        HashSet<String> upstreamEdge = new HashSet<>();
//        HashMap<Integer, String> allEdges = originGraph.getEdgelist();
//        HashMap<Integer, String> allNodes = originGraph.getNodelist();
//
//        //Print upstream Edge
//        StringBuffer print = new StringBuffer();
//        print.append("Upstream Edgelist:\n");
//
//        Iterator it_e = allEdges.entrySet().iterator();
//        while (it_e.hasNext()) {
//            Map.Entry edge = (Map.Entry) it_e.next();
//            int from = Integer.parseInt(edge.getValue().toString().split(",")[0]);
//            int to = Integer.parseInt(edge.getValue().toString().split(",")[1]);
//
//            String from_nodeLabel = allNodes.get(from);
//            String to_nodeLabel = allNodes.get(to);
//            if (upstreamNode.size() > 0) {
//                if (upstreamNode.contains(from_nodeLabel) && upstreamNode.contains(to_nodeLabel)) {
//                    upstreamEdge.add((String) edge.getValue());
//                    print.append(edge.getValue() + ": " + allNodes.get(from) + "->" + allNodes.get(to) + "\n");
//                }
//            } else if (forkAddedNode.size() > 0) {
//                if (!forkAddedNode.contains(from_nodeLabel) && !forkAddedNode.contains(to_nodeLabel)) {
//                    upstreamEdge.add((String) edge.getValue());
//                    print.append(edge.getValue() + ": " + allNodes.get(from) + "->" + allNodes.get(to) + "\n");
//                }
//            }
//        }
//        ioFunc.rewriteFile(print.toString(), filePath + "/upstreamEdge.txt");
////      System.out.print("upstreaEDGE"+upstreamEdge.size());
//        return upstreamEdge;
//
//    }


    int pre_numberOfCommunities = 0;
    int current_numberOfCommunities = 0;
    ArrayList<Integer> listOfNumberOfCommunities = new ArrayList<>();

    /**
     * This function is calling community detection algorithm through igraph lib
     *
     * @param re
     * @param testCaseDir
     * @param testDir
     * @param cutNum
     */
    public void calculateEachGraph(Rengine re, String testCaseDir, String testDir, int cutNum, boolean directedGraph) {
        String analysisDir = testCaseDir + testDir + FS;


        //get graph's edgeList and nodeList
//        REXP edgelist_R = re.eval("cbind( get.edgelist(g) , round( E(g)$weight, 3 ))", true);
//        REXP nodelist_R = re.eval("get.vertex.attribute(g)$id", true);
//        double[][] edgelist = edgelist_R.asMatrix();
//        String[] nodelist = (String[]) nodelist_R.getContent();
        ArrayList<Integer> nodeIdList = new ArrayList<>();
        for (int i = 0; i < nodelist.length; i++) {
            if (forkAddedNode.contains(nodelist[i])) {
                nodeIdList.add(i + 1);
            }
        }

        String command;
        if (!directedGraph) {
            command = "edge.betweenness(g,directed=FALSE)";
        } else {
            command = "edge.betweenness(g,directed=TRUE)";
        }

        //get betweenness for current graph
        REXP betweenness_R = re.eval(command, true);
        double[] betweenness = betweenness_R.asDoubleArray();

        //calculate modularty for current graph
        REXP membership_R = re.eval("cl<-clusters(g)$membership");
        double[] membership = membership_R.asDoubleArray();


        Graph currentGraph;

        HashMap<Integer, ArrayList<Integer>> clusters = getCurrentClusters(membership, nodeIdList);

        current_numberOfCommunities = clusters.keySet().size();
        if (!listOfNumberOfCommunities.contains(current_numberOfCommunities)) {
            listOfNumberOfCommunities.add(current_numberOfCommunities);

        }

        /**        calculating distance between clusters for joining purpose    **/
        if (pre_numberOfCommunities != current_numberOfCommunities) {
            calculateDistanceBetweenCommunities(clusters, testCaseDir, testDir, current_numberOfCommunities, directedGraph);
        }


        REXP modularity_R = re.eval("modularity(originalg,cl)");

        double modularity = modularity_R.asDoubleArray()[0];

        currentGraph = new Graph(nodelist, edgelist, betweenness, modularity);

//        minimizeUpstreamEdgeBetweenness(currentGraph);

        //modularity find removableEdge
        int removableEdgeID = findRemovableEdge(currentGraph);
//        if (pre_numberOfCommunities != current_numberOfCommunities) {
        printEdgeRemovingResult(currentGraph, analysisDir, cutNum);
        printMemebershipOfCurrentGraph(clusters, analysisDir);
//        }
        modularityMap.put(cutNum, modularity);
        modularityArray.add(modularity);

        //remove edge
        re.eval("g<-g-edge(\"" + removableEdgeID + "\")");
        pre_numberOfCommunities = current_numberOfCommunities;

    }

    /**
     * This function calculate the distance between communities
     *
     * @param clusters
     * @param testCaseDir
     * @param testDir
     * @param numOfClusters
     */
    private void calculateDistanceBetweenCommunities(HashMap<Integer, ArrayList<Integer>> clusters, String testCaseDir, String testDir, int numOfClusters, boolean directedGraph) {

        ArrayList<ArrayList<Integer>> combination = getPairsOfCommunities(clusters);
        HashMap<ArrayList<Integer>, Integer> distanceMatrix = new HashMap<>();
        StringBuffer sb = new StringBuffer();
        HashMap<Integer, double[]> shortestDistanceOfNodes = new HashMap<>();
        String filePath = ("comGraph<-read.graph(\"" + testCaseDir + "complete.pajek.net\", format=\"pajek\")").replace("\\", "/");
        re.eval(filePath);
        re.eval("scomGraph<- simplify(comGraph)");

        if (!directedGraph) {
            re.eval("completeGraph<-as.undirected(scomGraph)");
        } else {
            re.eval("completeGraph<-scomGraph");
        }

        /**   whether to calculate the distance with weight  **/
//        re.eval("E(completeGraph)$weight <- 1");
        for (ArrayList<Integer> pair : combination) {
//        for (ArrayList<Integer> pair : combination) {
            ArrayList<Integer> cluster_1 = clusters.get(pair.get(0));
            ArrayList<Integer> cluster_2 = clusters.get(pair.get(1));
            double shortestPath = 999999;
            for (Integer c1 : cluster_1) {
                double[] c1_array;
                if (shortestDistanceOfNodes.get(c1) == null) {

                    String c1_array_cmd = "distMatrixc1 <- shortest.paths(completeGraph, v=\"" + c1 + "\", to=V(completeGraph))";
//                    ioFunc.sleep();
                    REXP shortestPath_R_c1 = re.eval(c1_array_cmd);
                    c1_array = shortestPath_R_c1.asDoubleArray();
                    shortestDistanceOfNodes.put(c1, c1_array);
                } else {
                    c1_array = shortestDistanceOfNodes.get(c1);
//                    System.out.println("exist");
                }

                for (Integer cl2 : cluster_2) {
                    int c2 = cl2 - 1;
                    double c1_c2 = c1_array[c2];
//                    System.out.println(c1+"+"+c2+" " + c1_c2);

                    if (shortestPath > c1_c2) {
                        shortestPath = c1_c2;
                    }
                }
            }
            distanceMatrix.put(pair, (int) shortestPath);

            System.out.println("---" + pair.get(0) + "---" + pair.get(1) + "-------" + shortestPath);


        }

        StringBuffer clusterIDList = new StringBuffer();

        for (Integer index : clusters.keySet()) {
            ArrayList<Integer> nodelist = clusters.get(index);
            clusterIDList.append(index + ",");
        }

        //print distance
        for (ArrayList<Integer> s : distanceMatrix.keySet()) {

            for (Integer i : s) {
                sb.append(i + ",");
            }
            sb.append(distanceMatrix.get(s) + "\n");
        }

/*---------------mac----------path
        ioFunc.rewriteFile(sb.toString(), fileDir + "/" + numOfClusters + "_distanceBetweenCommunityies.txt");
        ioFunc.rewriteFile(clusterIDList.toString(), fileDir + "/" + numOfClusters + "_clusterIdList.txt");
*/
        String analysisDir = testCaseDir + testDir + FS;
        ioFunc.rewriteFile(sb.toString(), analysisDir + numOfClusters + "_distanceBetweenCommunityies.txt");
        ioFunc.rewriteFile(clusterIDList.toString(), analysisDir + numOfClusters + "_clusterIdList.txt");
    }

    /**
     * This function get pairs of communities, in order to calculate distance
     *
     * @param clusters
     * @return
     */
    private ArrayList<ArrayList<Integer>> getPairsOfCommunities(HashMap<Integer, ArrayList<Integer>> clusters) {

        ArrayList<HashSet<Integer>> combination_Set = new ArrayList<>();
        ArrayList<ArrayList<Integer>> combination_List = new ArrayList<>();
        Set<Integer> keyset = clusters.keySet();
        for (Integer s : keyset) {
            for (Integer c : keyset) {
                HashSet<Integer> pair = new HashSet<>();
                if (!s.equals(c)) {
                    pair.add(s);
                    pair.add(c);
                    if (!combination_Set.contains(pair)) {
                        ArrayList<Integer> pairList = new ArrayList<>();
                        pairList.addAll(pair);
                        combination_Set.add(pair);
                        combination_List.add(pairList);
                    }
                }
            }
        }
        return combination_List;
    }

    private HashMap<Integer, ArrayList<Integer>> getCurrentClusters(double[] membership, ArrayList<Integer> nodeIdList) {
        HashMap<Integer, ArrayList<Integer>> clusters = new HashMap<>();

        for (int i = 0; i < membership.length; i++) {
            if (nodeIdList.contains(i + 1)) {
                ArrayList<Integer> member = clusters.get((int) membership[i]);
                if (member == null) {
                    member = new ArrayList<>();
                }
                member.add(i + 1);
                clusters.put((int) membership[i], member);
            }
        }
        return clusters;
    }

    private void printMemebershipOfCurrentGraph(HashMap<Integer, ArrayList<Integer>> clusters, String fileDir) {
        //print
        StringBuffer membership_print = new StringBuffer();
        membership_print.append("\n---" + clusters.entrySet().size() + " communities\n");
        Iterator it = clusters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry cluster = (Map.Entry) it.next();
            ArrayList<Integer> mem = (ArrayList<Integer>) cluster.getValue();
            membership_print.append(cluster.getKey() + ") ");
            membership_print.append("[");
            for (Integer m : mem) {
                membership_print.append(m + " , ");
            }
            membership_print.append("]\n");
        }
        //print old edge
        ioFunc.writeTofile(membership_print.toString(), fileDir + "/clusterTMP.txt");


    }

    /**
     * This function is finding next edge to be removed by finding the edge has largest betweenness
     *
     * @param g
     * @return
     */
    public int findRemovableEdge(Graph g) {
        int maxBetEdgeID = findMaxNumberLocation(g.getBetweenness()) + 1;
        int oldEdgeID = findCorrespondingEdgeInOriginGraph(maxBetEdgeID, g);
        checkedEdges.put(oldEdgeID, true);
        cutSequence.add(oldEdgeID);
        g.setRemovableEdgeLable(g.getEdgelist().get(maxBetEdgeID));
        return maxBetEdgeID;
    }

//    private void minimizeUpstreamEdgeBetweenness(Graph g) {
//        HashMap<Integer, String> edgelist = g.getEdgelist();
//
//        Iterator it_e = edgelist.entrySet().iterator();
//        while (it_e.hasNext()) {
//            Map.Entry edge = (Map.Entry) it_e.next();
//            if (upstreamEdge.contains(edge.getValue())) {
//                g.getBetweenness()[(int) edge.getKey() - 1] = -1000.0;
//
//                checkedEdges.put((Integer) edge.getKey(), true);
//            }
//
//        }
//
//    }

    public int findCorrespondingEdgeInOriginGraph(int maxBetEdgeID, Graph g) {
        HashMap<Integer, String> current_edgelist = g.getEdgelist();
        String edgeLabel = current_edgelist.get(maxBetEdgeID);
        int fromNodeID = Integer.parseInt(edgeLabel.split(",")[0]);
        int toNodeID = Integer.parseInt(edgeLabel.split(",")[1]);

        HashMap<String, Integer> reverseEdgelist = originGraph.getReverseEdgelist();

        return reverseEdgelist.get(fromNodeID + "," + toNodeID);
    }

    /**
     * This function find the largest number in the double array
     *
     * @param doubleArray
     * @return location of the number
     */
    public int findMaxNumberLocation(double[] doubleArray) {
        int loc = 0;
        double max = doubleArray[0];
        for (int counter = 1; counter < doubleArray.length; counter++) {
            if (doubleArray[counter] > max) {
                max = doubleArray[counter];
                loc = counter;
            }
        }
        return loc;
    }

    /**
     * This function find the largest number in the double array
     *
     * @param doubleArray
     * @return location of the number
     */
    public int findMaxNumberLocation(ArrayList<Double> doubleArray) {
        int loc = 0;
        double max = doubleArray.get(0);
        for (int counter = 1; counter < doubleArray.size(); counter++) {
            if (doubleArray.get(counter) > max) {
                max = doubleArray.get(counter);
                loc = counter;
            }
        }
        return loc;
    }

    /**
     * This function print the clustering result when removing an edge that has the highest betweenness
     *
     * @param g
     * @param filePath
     * @param cutNum
     */
    public void printEdgeRemovingResult(Graph g, String filePath, int cutNum) {
        StringBuffer print = new StringBuffer();
        print.append("\n--------Graph-------\n");
        print.append("** " + cutNum + "edges has been removed **\n");
        String removableEdge = g.getRemovableEdgeLable();
        int index_lastComma = removableEdge.lastIndexOf(",");
        String removableEdge_from_to = removableEdge.substring(0, index_lastComma);

        int edgeId = originGraph.getReverseEdgelist().get(removableEdge_from_to);
        int from = Integer.parseInt(removableEdge.split(",")[0]);
        int to = Integer.parseInt(removableEdge.split(",")[1]);
        int weight = Integer.parseInt(removableEdge.split(",")[2]);
        String edgeNodes = originGraph.getNodelist().get(from) + "->" + originGraph.getNodelist().get(to);
        print.append("max between edge id:" + edgeId + "-" + removableEdge + "(" + edgeNodes + " weight= " + weight + ")");
        double modularity = g.getModularity();
        print.append("\nModularity: " + modularity);
        ioFunc.writeTofile(print.toString(), filePath + "/clusterTMP.txt");

    }

    /**
     * This function find the best clustering result by finding the result has the highest modularity
     *
     * @param g
     * @param cutSequence
     * @param filePath
     * @return
     */
    public String findBestClusterResult(Graph g, ArrayList<Integer> cutSequence, String filePath) {
        StringBuffer result = new StringBuffer();
        bestCut = findMaxNumberLocation(modularityArray) + 1;
        for (int i = 0; i < bestCut; i++) {
            result.append(cutSequence.get(i) + ",");
        }
        result.append("\nbestcut:" + (bestCut) + "\n");
        result.append("\n\nCut Sequence(" + cutSequence.size() + " steps): ");
        ioFunc.rewriteFile(result.toString(), filePath + "cutSequence.txt");
        return result.toString();
    }


}
