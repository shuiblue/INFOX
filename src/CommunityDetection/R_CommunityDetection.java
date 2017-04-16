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
    String sourcecodeDir = "", analysisDir = "";
    String upstreamNodeTxt = "/upstreamNode.txt";
    String forkAddedNodeTxt = "/forkAddedNode.txt";

    public ArrayList<Integer> cutSequence;
    ProcessingText ioFunc = new ProcessingText();
    HashMap<Integer, double[]> modularityMap;
    //    int bestCut;
    Rengine re = null;
    static final String FS = File.separator;
    //    double[][] shortestPathMatrix;
    double[][] edgelist;
    double[][] current_edgelist;
    double[][] previous_edgelist;
    String[] nodelist;

    int pre_numberOfCommunities = 0;
    int current_numberOfCommunities = 0;
    ArrayList<Integer> listOfNumberOfCommunities = new ArrayList<>();
    int max_numberOfCut;
    int numberOfBiggestClusters;
    int current_numberOfCut = 0;
    HashMap<String, ArrayList<Integer>> new_clusters = new HashMap<>();
    StringBuilder noSplitting_step = new StringBuilder();

    //todo: user input
    int minimumClusterSize = 5;

    public R_CommunityDetection(String sourcecodeDir, String analysisDirName, String testCaseDir, String testDir, Rengine re, int max_numberOfCut, int numberOfBiggestClusters) {
        this.sourcecodeDir = sourcecodeDir;
        if (testDir.equals("")) {
            this.analysisDir = testCaseDir;
        } else {
            this.analysisDir = testCaseDir + testDir + FS;
        }
        modularityArray = new ArrayList<>();
        checkedEdges = new HashMap<>();
        cutSequence = new ArrayList<>();
        forkAddedNode = new HashSet<>();
        modularityMap = new HashMap<>();
        this.re = re;
        this.max_numberOfCut = max_numberOfCut;
        this.numberOfBiggestClusters = numberOfBiggestClusters;
    }

    public R_CommunityDetection(String analysisDir) {
        this.analysisDir = analysisDir;
    }

    /**
     * This functions read pajek file and generates changed code graph by igraph lib
     *
     * @param testCaseDir
     * @param testDir
     * @param directedGraph
     * @return true, if this graph has edge; false, if this graph does not have edge.
     */
    public boolean getCodeChangeGraph(String testCaseDir, String testDir, boolean directedGraph, String inputFile) {

        System.out.println("oldg<-read_graph(\"" + testCaseDir + inputFile + "\", format=\'pajek\')");
        re.eval("oldg<-read_graph(\"" + testCaseDir + inputFile + "\", format=\'pajek\')");
        // removes the loop and/or multiple edges from a graph.
        re.eval("g<-simplify(oldg)");
        re.eval("originalg<-g");


        return prepareEdgeList_NodeList_forClustering(testCaseDir);
    }

    private void getCompleteGraph(boolean directedGraph) {
        re.eval("library(igraph)");
        //------------------------WINDOWS------------------------
//        re.eval(".libPaths('C:/Users/shuruizDocuments/R/win-library/3.3/rJava/jri/x64)");
//        re.eval(".libPaths('C:/Users/shuruiz/Documents/R/win-library/3.3')");

        System.getProperty("java.library.path");


        //get complete graph
        String filePath = ("comGraph<-read.graph(\"" + analysisDir + "/complete.pajek.net\", format=\"pajek\")").replace("\\", "/");
        re.eval(filePath);
        re.eval("scomGraph<- simplify(comGraph)");
        if (!directedGraph) {
            REXP g = re.eval("completeGraph<-as.undirected(scomGraph)");
        } else {
            REXP g = re.eval("completeGraph<-scomGraph");
        }
        re.eval("origin_completeGraph<-scomGraph");
    }


    public boolean prepareEdgeList_NodeList_forClustering(String testCaseDir) {
        // get original graph
        REXP edgelist_R = re.eval("cbind( get.edgelist(g) , round( E(g)$weight, 3 ))", true);
        REXP nodelist_R = re.eval("get.vertex.attribute(g)$id", true);

        if (edgelist_R != null) {
            edgelist = edgelist_R.asDoubleMatrix();
            nodelist = (String[]) nodelist_R.getContent();
            if (originGraph == null) {
                originGraph = new Graph(nodelist, edgelist, null, 0);
            }

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

            //initialize removedEdge Map, all the edges have not been removed, so the values are all false
            for (int i = 0; i < edgelist.length; i++) {
                checkedEdges.put(i + 1, false);
            }
            re.end();
            System.out.println("\n ------finished reading changed code graph------");
            return true;
        }
        re.end();
        System.out.println("no edge");
        return false;

    }

    /**
     * This methods is detecting communities for changedCode.pajek.net, which is the graph for new code.
     *
     * @param testCaseDir
     * @param testDir
     * @param re
     * @return
     */
    public boolean clustering_CodeChanges(String testCaseDir, String testDir, Rengine re, boolean directedGraph, boolean isOriginalGraph, String fileprefix) {

        getCompleteGraph(directedGraph);

        String outputFile = fileprefix + "_clusterTMP.txt";
        String inputFile = "changedCode.pajek.net";
        boolean hasEdge = getCodeChangeGraph(testCaseDir, testDir, directedGraph, inputFile);

        if (hasEdge) {
            int cutNum = 1;
            while (checkedEdges.values().contains(false)) {
                if (listOfNumberOfCommunities.size() <= max_numberOfCut) {
                    //count betweenness for current graph
//                    calculateEachGraph(re, testCaseDir, testDir, cutNum, directedGraph, numOfcut, isOriginGraph, outputFile);


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


                    /** get current clusters **/
                    HashMap<String, ArrayList<Integer>> clusters = getCurrentClusters(membership, nodeIdList);
                    REXP modularity_R = re.eval("modularity(originalg,cl)");
                    double modularity = modularity_R.asDoubleArray()[0];

                    Graph currentGraph;
                    if (current_edgelist == null) {
                        currentGraph = new Graph(nodelist, edgelist, betweenness, modularity);
                    } else {
                        currentGraph = new Graph(nodelist, current_edgelist, betweenness, modularity);
                    }

                    if (currentGraph.getEdgelist().size() > 0) {
//        minimizeUpstreamEdgeBetweenness(currentGraph);

                        //modularity find removableEdge
                        String[] edgeID_maxBetweenness = findRemovableEdge(currentGraph);
                        if (isOriginalGraph) {
                            calculateDistanceBetweenCommunities(clusters, testCaseDir, testDir, fileprefix, directedGraph);
                        }
                        ioFunc.rewriteFile("", analysisDir + outputFile);
                        printEdgeRemovingResult(currentGraph, analysisDir, cutNum, edgeID_maxBetweenness[1], outputFile);
                        printMemebershipOfCurrentGraph(clusters, outputFile, true);

                        /** split sub-clusters step by step  **/
                        clusterToplusters(clusters, re, testCaseDir, testDir, cutNum, directedGraph);

                        cutNum++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        } else {
            return false;
        }

        /** find best modularity but not fit for INFOX ***
         findBestClusterResult(originGraph, cutSequence, analysisDir);
         writeToModularity_Betweenness_CSV(analysisDir);
         **/
        new ProcessingText().rewriteFile(noSplitting_step.toString(),analysisDir+"noSplittingStepList.txt");

        return true;
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

    private void writeToModularity_Betweenness_CSV(String fileDir) {
        StringBuffer csv = new StringBuffer();
        Iterator it = modularityMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry node = (Map.Entry) it.next();
            double[] modularity_betweenness = (double[]) node.getValue();
            csv.append(node.getKey() + "," + modularity_betweenness[0] + "," + modularity_betweenness[1] + "\n");
        }
        ioFunc.rewriteFile(csv.toString(), fileDir + "modularity_betweenness.csv");
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


    /**
     * This function is calling community detection algorithm through igraph lib
     *
     * @param cutNum
     */
    public int calculateEachGraph(Rengine re, int cutNum, boolean directedGraph, String outputFile, int current_numofCut) {

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

        /** get current clusters **/
        HashMap<String, ArrayList<Integer>> clusters = getCurrentClusters(membership, nodeIdList);

        current_numberOfCommunities = clusters.keySet().size();
        if (!listOfNumberOfCommunities.contains(current_numberOfCommunities)) {
            listOfNumberOfCommunities.add(current_numberOfCommunities);

        }
        if (pre_numberOfCommunities < current_numberOfCommunities && pre_numberOfCommunities > 0) {
            current_numofCut++;
        }
        //todo
        /** calculating distance between clusters for joining purpose
         * if numofcut == 0 , don't join clusters
         **/
//            if (pre_numberOfCommunities != current_numberOfCommunities && numofCut > 0 && clusters.size() > 1) {
//                calculateDistanceBetweenCommunities(clusters, testCaseDir, testDir, current_numberOfCommunities, directedGraph);
//            }
        REXP modularity_R = re.eval("modularity(originalg,cl)");
        double modularity = modularity_R.asDoubleArray()[0];


        if (current_edgelist == null) {
            currentGraph = new Graph(nodelist, edgelist, betweenness, modularity);
        } else {
            currentGraph = new Graph(nodelist, current_edgelist, betweenness, modularity);
        }

//        minimizeUpstreamEdgeBetweenness(currentGraph);

        //modularity find removableEdge
        String[] edgeID_maxBetweenness = findRemovableEdge(currentGraph);
//        if (pre_numberOfCommunities != current_numberOfCommunities) {
        printEdgeRemovingResult(currentGraph, analysisDir, cutNum, edgeID_maxBetweenness[1], outputFile);

        /**  print clusterTMP.txt file  **/
        printMemebershipOfCurrentGraph(clusters, outputFile, false);
//        }
        modularityMap.put(cutNum, new double[]{modularity, Double.parseDouble(edgeID_maxBetweenness[1])});
        modularityArray.add(modularity);

        int edgeID = Integer.valueOf(edgeID_maxBetweenness[0]);
        String edge_from_to;
        if (previous_edgelist != null) {
            edge_from_to = (int) previous_edgelist[edgeID - 1][0] + "%--%" + (int) previous_edgelist[edgeID - 1][1];
        } else {
            edge_from_to = (int) edgelist[edgeID - 1][0] + "%--%" + (int) edgelist[edgeID - 1][1];

        }
        //remove edge
        re.eval("g<-g-E(g)[" + edge_from_to + "]");

        REXP edgelist_R = re.eval("cbind( get.edgelist(g) , round( E(g)$weight, 3 ))", true);
        current_edgelist = edgelist_R.asDoubleMatrix();


//            String remove_completeGraph_edge = "completeGraph<-completeGraph-E(completeGraph)[" + edge_from_to + "]";
//            System.out.println(remove_completeGraph_edge);
//            re.eval(remove_completeGraph_edge);
//
//            REXP com_edgelist_r = re.eval("cbind( get.edgelist(completeGraph) , round( E(completeGraph)$weight, 3 ))", true);
//            double[][] edgelist_com = com_edgelist_r.asDoubleMatrix();
//
//            System.out.println("current number of edge: " + edgelist_com.length);
//            System.out.println("changed current number of edge: " + current_edgelist.length);


        pre_numberOfCommunities = current_numberOfCommunities;
        previous_edgelist = current_edgelist;
        return current_numofCut;
    }

    /**
     * This function clusters each cluster separately
     *
     * @param clusters
     */
    private void clusterToplusters(HashMap<String, ArrayList<Integer>> clusters, Rengine re, String testCaseDir, String testDir, int cutNum, boolean directedGraph) {
        ProcessingText processText = new ProcessingText();
        HashMap<String, Integer> clusterID_size = new HashMap<>();
        clusters.forEach((k, v) -> {
            clusterID_size.put(k, v.size());
        });


        /** get top clusters: sorting clusters by size**/

        Map<String, Integer> result = new LinkedHashMap<>();
        clusterID_size.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(numberOfBiggestClusters)
                .forEachOrdered(x -> {
                    result.put(x.getKey(), x.getValue());

                });

        final Iterator<String> cursor = result.keySet().iterator();
        StringBuilder sb_topClusters = new StringBuilder();

        while (cursor.hasNext()) {
            final String clusterID = cursor.next();
            sb_topClusters.append(clusterID + "\n");

            String inputFile = "changedCode.pajek.net";
           getCodeChangeGraph(testCaseDir, testDir, directedGraph, inputFile);

            if (current_numberOfCut <= max_numberOfCut) {

                ArrayList<Integer> clusterNodeList = clusters.get(clusterID);
                clusterSubGraphs(clusterNodeList, re, testCaseDir, testDir, cutNum, directedGraph, clusterID);
            }
        }
        processText.rewriteFile(sb_topClusters.toString(), analysisDir + "topClusters.txt");
    }

    /**
     * This function split one of the top cluster step by step, each step only has 1 cut
     *
     * @param clusterNodeList,
     * @param re
     * @param testCaseDir
     * @param testDir
     * @param cutNum
     * @param directedGraph
     * @param clusterID
     */
    private void clusterSubGraphs(ArrayList<Integer> clusterNodeList, Rengine re, String testCaseDir, String testDir, int cutNum, boolean directedGraph, String clusterID) {
        if (clusterNodeList.size() > minimumClusterSize) {
            ProcessingText pt = new ProcessingText();
            int current_numofCut = 0;
            System.out.println(clusterID);
            HashMap<String, HashSet<Integer>> current_clusters = generateCurrentSplittingResult(clusterID, clusterNodeList, testCaseDir, testDir, directedGraph, re, cutNum);


            current_clusters.forEach((k, v) -> {
                System.out.println("v size:"+v.size());

                final int[] splitNum = {1};
                if (k.split("_").length < max_numberOfCut + 1) {
                    String current_clusterID = k + "_" + (splitNum[0]++);
                    ArrayList<Integer> new_clusterNodeList = new_clusters.get(k);
                    if (new_clusterNodeList.size() > minimumClusterSize) {
                        generateCurrentSplittingResult(k, new_clusterNodeList, testCaseDir, testDir, directedGraph, re, cutNum);
                        clusterSubGraphs(new_clusters.get(current_clusterID.split("_")[0]), re, testCaseDir, testDir, cutNum, false, current_clusterID);
                    } else {
                        noSplitting_step.append(k + "\n");
                    }
                }

            });
        }else {
            noSplitting_step.append(clusterID + "\n");
        }
    }




    private HashMap<String, HashSet<Integer>> generateCurrentSplittingResult(String clusterID, ArrayList<Integer> clusterNodeList, String testCaseDir, String testDir, boolean directedGraph, Rengine re, int cutNum) {
        ProcessingText pt = new ProcessingText();

        String outputFile = clusterID + "_clusterTMP.txt";
        ioFunc.rewriteFile("", analysisDir + outputFile);

        String currentGraphPath = clusterID + "_changedCode.pajek.net";


        re.eval("subg<-induced.subgraph(graph=oldg,vids=c(" + clusterNodeList.toString().replace("[", "").replace("]", "" + "))"));

        REXP edgelist_R = re.eval("cbind( get.edgelist(subg) , round( E(subg)$weight, 3 ))", true);
        REXP nodelist_R = re.eval("get.vertex.attribute(subg)$id", true);
        double[][] edgelist = edgelist_R.asDoubleMatrix();
        if (edgelist.length > 1) {
            String[] old_nodelist = (String[]) nodelist_R.getContent();
            StringBuilder sub_edgelist_sb = new StringBuilder();
            HashMap<String, String> label_to_id = new ProcessingText().getNodeLabel_to_id_map(analysisDir + "nodeLable2IdMap.txt");
            for (double[] edge : edgelist) {
                String from = label_to_id.get("\"" + old_nodelist[(int) edge[0] - 1] + "\"");
                String to = label_to_id.get("\"" + old_nodelist[(int) edge[1] - 1] + "\"");
                sub_edgelist_sb.append(from + " " + to + " 5\n");
            }

            String completeGraph = null;
            try {
                completeGraph = pt.readResult(analysisDir + "/complete.pajek.net");
            } catch (IOException e) {
                e.printStackTrace();
            }
            //get node list
            String nodeListString = completeGraph.split("\\*arcs")[0];
            pt.rewriteFile(nodeListString + "*Arcs\n" + sub_edgelist_sb.toString(), testCaseDir + currentGraphPath);

            getCodeChangeGraph(testCaseDir, testDir, directedGraph, currentGraphPath);
            int current_numofCut = 0;
            while (current_numofCut == 0) {
                current_numofCut = calculateEachGraph(re, cutNum, directedGraph, outputFile, current_numofCut);
            }
            pre_numberOfCommunities = 0;

        }

        HashMap<String, HashSet<Integer>> current_clusters = getCurrentClusters(analysisDir,clusterID).get(2);


        current_clusters.forEach((k, v) -> {

            ArrayList<Integer> list = new ArrayList<Integer>();
            v.forEach(str -> {
                list.add(Integer.valueOf(str));
            });
            new_clusters.put(k, list);
        });
        return current_clusters;
    }


    private HashMap<Integer, HashMap<String, HashSet<Integer>>> getCurrentClusters(String analysisDir,String clusterID) {
        return new AnalyzingCommunityDetectionResult(analysisDir).getClusteringResultMap(clusterID, false);

    }


    /**
     * This function calculate the distance between communities
     *
     * @param clusters
     * @param testCaseDir
     * @param testDir
     * @param numOfClusters
     */
    private void calculateDistanceBetweenCommunities(HashMap<String, ArrayList<Integer>> clusters, String testCaseDir, String testDir, String numOfClusters, boolean directedGraph) {

        ArrayList<ArrayList<String>> combination = getPairsOfCommunities(clusters);
        HashMap<ArrayList<String>, Integer> distanceMatrix = new HashMap<>();
        StringBuffer sb = new StringBuffer();
        StringBuffer sb_shortestPath_node = new StringBuffer();
        HashMap<Integer, double[]> shortestDistanceOfNodes = new HashMap<>();
//        String filePath = ("comGraph<-read.graph(\"" + sourcecodeDir + analysisDirName + "/complete.pajek.net\", format=\"pajek\")").replace("\\", "/");
//        re.eval(filePath);
//        re.eval("scomGraph<- simplify(comGraph)");
//
//        if (!directedGraph) {
//            REXP g = re.eval("completeGraph<-as.undirected(scomGraph)");
//        } else {
//            REXP g = re.eval("completeGraph<-scomGraph");
//        }

        for (ArrayList<String> pair : combination) {
            ArrayList<Integer> cluster_1 = clusters.get(pair.get(0));
            ArrayList<Integer> cluster_2 = clusters.get(pair.get(1));
            double shortestPath = 999999;
            for (Integer c1 : cluster_1) {
                double[] c1_array;
                if (shortestDistanceOfNodes.get(c1) == null) {

                    String c1_array_cmd = "distMatrixc1 <- shortest.paths(completeGraph, v=\"" + c1 + "\", to=V(completeGraph))";
                    re.eval(c1_array_cmd);
                    REXP shortestPath_R_c1 = re.eval("distMatrixc1");

                    c1_array = shortestPath_R_c1.asDoubleArray();
                    shortestDistanceOfNodes.put(c1, c1_array);
                } else {
                    c1_array = shortestDistanceOfNodes.get(c1);
                }

                for (Integer cl2 : cluster_2) {
//                    int c2 = cl2 ;
                    int c2 = cl2 - 1;

                    if (c2 < c1_array.length) {
                        double c1_c2 = c1_array[c2];

                        if (shortestPath > c1_c2) {
                            shortestPath = c1_c2;
                            if (shortestPath == 10) {

                                sb_shortestPath_node.append("c1: " + nodelist[c1 - 1] + " , c2: " + nodelist[c2] + " shortestPath: " + c1_c2 + "\n");
                            }
                        }
                    }
                }
            }
            distanceMatrix.put(pair, (int) shortestPath);

//            System.out.println("---" + pair.get(0) + "---" + pair.get(1) + "-------" + shortestPath);


        }

        StringBuffer clusterIDList = new StringBuffer();

        for (String index : clusters.keySet()) {
            ArrayList<Integer> nodelist = clusters.get(index);
            clusterIDList.append(index + ",");
        }

        //print distance
        for (ArrayList<String> s : distanceMatrix.keySet()) {

            for (String i : s) {
                sb.append(i + ",");
            }
            sb.append(distanceMatrix.get(s) + "\n");
        }

/*---------------mac----------path
        ioFunc.rewriteFile(sb.toString(), fileDir + "/" + numOfClusters + "_distanceBetweenCommunityies.txt");
        ioFunc.rewriteFile(clusterIDList.toString(), fileDir + "/" + numOfClusters + "_clusterIdList.txt");
*/
//        String analysisDir = testCaseDir + testDir + FS;
        ioFunc.rewriteFile(sb.toString(), analysisDir + numOfClusters + "_distanceBetweenCommunityies.txt");
        ioFunc.rewriteFile(clusterIDList.toString(), analysisDir + numOfClusters + "_clusterIdList.txt");
//        ioFunc.rewriteFile(sb_shortestPath_node.toString(), analysisDir + numOfClusters + "_shortestPath.txt");
    }

    /**
     * This function get pairs of communities, in order to calculate distance
     *
     * @param clusters
     * @return
     */
    private ArrayList<ArrayList<String>> getPairsOfCommunities(HashMap<String, ArrayList<Integer>> clusters) {

        ArrayList<HashSet<String>> combination_Set = new ArrayList<>();
        ArrayList<ArrayList<String>> combination_List = new ArrayList<>();
        Set<String> keyset = clusters.keySet();
        for (String s : keyset) {
            for (String c : keyset) {
                HashSet<String> pair = new HashSet<>();
                if (!s.equals(c)) {
                    pair.add(s);
                    pair.add(c);
                    if (!combination_Set.contains(pair)) {
                        ArrayList<String> pairList = new ArrayList<>();
                        pairList.addAll(pair);
                        combination_Set.add(pair);
                        combination_List.add(pairList);
                    }
                }
            }
        }
        return combination_List;
    }

    private HashMap<String, ArrayList<Integer>> getCurrentClusters(double[] membership, ArrayList<Integer> nodeIdList) {
        HashMap<String, ArrayList<Integer>> clusters = new HashMap<>();

        for (int i = 0; i < membership.length; i++) {
            if (nodeIdList.contains(i + 1)) {
                ArrayList<Integer> member = clusters.get(((int) membership[i]) + "");
                if (member == null) {
                    member = new ArrayList<>();
                }
                member.add(i + 1);
                clusters.put(((int) membership[i]) + "", member);
            }
        }
        return clusters;
    }

    private void printMemebershipOfCurrentGraph(HashMap<String, ArrayList<Integer>> clusters, String outputFile, boolean isOriginalGraph) {
        //print
        StringBuffer membership_print = new StringBuffer();

        membership_print.append("\n---" + clusters.entrySet().size() + " communities\n");
        Iterator it = clusters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry cluster = (Map.Entry) it.next();
            ArrayList<Integer> cluster_content = (ArrayList<Integer>) cluster.getValue();
            if (isOriginalGraph || (!isOriginalGraph && cluster_content.size() > 1)) {
                ArrayList<Integer> mem = (ArrayList<Integer>) cluster.getValue();
                membership_print.append(cluster.getKey() + ") ");
                membership_print.append("[");
                for (Integer m : mem) {
                    membership_print.append(m + " , ");
                }
                membership_print.append("]\n");
            }
        }
        //print old edge
        ioFunc.writeTofile(membership_print.toString(), analysisDir + outputFile);
    }


    public void printMemebershipOfCurrentGraph_new(HashMap<String, HashSet<Integer>> clusters, String outputFile) {
        //print
        StringBuffer membership_print = new StringBuffer();

        membership_print.append("\n---" + clusters.entrySet().size() + " communities\n");
        Iterator it = clusters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry cluster = (Map.Entry) it.next();
            HashSet<Integer> cluster_content = (HashSet<Integer>) cluster.getValue();
            if (outputFile.equals("clusterTMP.txt") || cluster_content.size() > 1) {

                HashSet<Integer> mem = (HashSet<Integer>) cluster.getValue();
                membership_print.append(cluster.getKey() + ") ");
                membership_print.append("[");
                for (Integer m : mem) {
                    membership_print.append(m + " , ");
                }
                membership_print.append("]\n");
            }
        }
        //print old edge
        ioFunc.rewriteFile(membership_print.toString(), analysisDir + outputFile);
    }


    /**
     * This function is finding next edge to be removed by finding the edge has largest betweenness
     *
     * @param g
     * @return
     */
    public String[] findRemovableEdge(Graph g) {

        String[] edgeID_maxBetweenness = new String[2];
        edgeID_maxBetweenness = findMaxNumberLocation(g.getBetweenness());
//        int maxBetEdgeID = Integer.parseInt(edgeID_maxBetweenness[0]) + 1;
        int maxBetEdgeID = Integer.parseInt(edgeID_maxBetweenness[0]);

        int oldEdgeID = findCorrespondingEdgeInOriginGraph(maxBetEdgeID, g);
        checkedEdges.put(oldEdgeID, true);
        cutSequence.add(oldEdgeID);
        g.setRemovableEdgeLable(g.getEdgelist().get(maxBetEdgeID));
        return edgeID_maxBetweenness;
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
    public String[] findMaxNumberLocation(double[] doubleArray) {
        String[] edgeID_maxBetweenness = new String[2];
        int loc = 0;
        double max = doubleArray[0];
        for (int counter = 1; counter < doubleArray.length; counter++) {
            if (doubleArray[counter] > max) {
                max = doubleArray[counter];
                loc = counter;
            }
        }
        return new String[]{String.valueOf(loc + 1), String.valueOf(max)};
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
    public void printEdgeRemovingResult(Graph g, String filePath, int cutNum, String lastRemovedEdgeBetweenness, String outputFile) {
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
        print.append("\nlatest removed edge betweenness:" + lastRemovedEdgeBetweenness);
        double modularity = g.getModularity();
        print.append("\nModularity: " + modularity);
        ioFunc.writeTofile(print.toString(), filePath + outputFile);

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
        int bestCut = findMaxNumberLocation(modularityArray) + 1;
        for (int i = 0; i < bestCut; i++) {
            result.append(cutSequence.get(i) + ",");
        }
        result.append("\nbestcut:" + (bestCut) + "\n");
        result.append("\n\nCut Sequence(" + cutSequence.size() + " steps): ");
        ioFunc.rewriteFile(result.toString(), filePath + "cutSequence.txt");
        return result.toString();
    }


}
