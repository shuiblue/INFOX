package DependencyGraph;

import NamingClusters.StopWords;
import Util.GenerateCombination;
import Util.ProcessingText;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

/**
 * Created by shuruiz on 12/10/15.
 * The DependencyGraph object corresponds to a source code file.
 * There are 3 kinds of dependency graph:
 * todo: rename dependencyGraph to forkAddedDependencyGraph
 * 1) dependencyGraph only contains fork added node
 * 2) completGraph: contains all the nodes from the file
 * 3) compactGraph: if the file has not been changed, then the whole file is one node
 */
public class DependencyGraph {

    static final public String CONTROLFLOW_LABEL = "<Control-Flow>";
    static final String FS = File.separator;
    /**
     * These two var are used for srcML
     **/
    String NAMESPACEURI, NAMESPACEURI_CPP, NAMESPACEURI_POSITION;

    public boolean HIERACHICAL = true;
    public boolean CONTROL_FLOW = true;
    public boolean DEF_USE = true;
    public boolean CONSECUTIVE = true;

    public String current_OS = System.getProperty("os.name").toLowerCase();

    ProcessingText processingText = new ProcessingText();

    /**
     * symbol Table stores all the declaration nodes.*
     */
    HashSet<Symbol> symbolTable = new HashSet<>();

    /**
     * lonelySymbolSet stores all the nodes that haven't find any node that could be point to.
     **/
    HashSet<Symbol> lonelySymbolSet = new HashSet<>();

    /**
     * This map stores nodes that have same name, used for search, in order to find dependencies effectively.
     **/
    HashMap<String, HashSet<Symbol>> sameNameMap = new HashMap<>();


    /**
     * ---------------------- dependency Graph ---------------------------------
     **/
    // key: node label
    HashMap<String, HashSet<String[]>> dependencyGraph = new HashMap<>();
    HashMap<String, HashSet<String[]>> completeGraph = new HashMap<>();
    ArrayList<String> forkaddedNodeList;
    // node id
    int id = 0;
    //node list stores  the id for the node, used for create graph file. HashMap<String, Integer>
    HashMap<String, Integer> nodeList = new HashMap<>();
    ArrayList<String> complete_nodeList = new ArrayList();

    //edge list stores all the edges, used for testing
    HashSet<String> edgeList = new HashSet<>();


    /**
     * ----------------------compact Graph ------------------------------
     * if the file is not changed, then the whole file is a node
     */
    HashSet<String> changedFiles;


    /**
     * used for Similarity Calculation, LD algorithm
     **/
    ArrayList<String> candidateStrings = new ArrayList<>();
    HashMap<String, String> sourceCodeLocMap = new HashMap<>();
    //map nodeID in graph -> stringID in candidateString list
    HashMap<Integer, Integer> idMap = new HashMap<>();

    /**
     * goto statement
     **/
    HashMap<String, String> gotoMap;
    String label_location = "";


    /**
     * This flag is used for checking whether the macro is a header guard or not
     **/
    boolean foundHeaderGuard = false;


    /**
     * ----------------  Specifying paths  -----------------------
     **/
    String Root_Dir = "";
    /**
     * analysis Dir stores the output of INFOX
     **/
    String analysisDir = "";
    /**
     * sourceCode Dir stores the source code to be analyzed
     **/
    String sourcecodeDir = "";
    String testCaseDir = "";
    String testDir = "";
    String tmpXmlPath = "tmpXMLFile" + FS;
    String edgeListTxt, parsedLineTxt, forkAddedNodeTxt;

    int stringID = 0;
    HashMap<String, String> label_to_id = new HashMap<>();


    /**
     * compared_method_parameter
     * 1 -- INFOX,
     * 2--MS,
     * 3--MS+CF+HIE (NO spliting, joining),
     * 4--INFOX-(DEF_USE)
     * 5--INFOX-(CONTROL_FLOW),
     * 6--INFOX-(HIERARCHY)
     * 7--INFOX-(Consecutive)
     * 8--MS-(Consecutive)
     **/
    public DependencyGraph(int compared_method_parameter) {
        if (compared_method_parameter == 2) {
            HIERACHICAL = false;
            CONTROL_FLOW = false;
        } else if (compared_method_parameter == 4) {
            DEF_USE = false;
        } else if (compared_method_parameter == 5) {
            CONTROL_FLOW = false;
        } else if (compared_method_parameter == 6) {
            HIERACHICAL = false;
        } else if (compared_method_parameter == 7) {
            CONSECUTIVE = false;
        } else if (compared_method_parameter == 8) {
            HIERACHICAL = false;
            CONTROL_FLOW = false;
            CONSECUTIVE = false;
        }

        System.out.println(" HIERACHICAL : " + HIERACHICAL);
        System.out.println("CONTROL_FLOW : " + CONTROL_FLOW);
        System.out.println("DEF_USE: " + DEF_USE);
        System.out.println(" CONSECUTIVE: " + CONSECUTIVE);

        if (current_OS.indexOf("mac") >= 0) {
            NAMESPACEURI_POSITION = "http://www.sdml.info/srcML/position";
            NAMESPACEURI = "http://www.sdml.info/srcML/src";
            NAMESPACEURI_CPP = "http://www.sdml.info/srcML/cpp";
        } else {
            NAMESPACEURI_POSITION = "http://www.srcML.org/srcML/position";
            NAMESPACEURI = "http://www.srcML.org/srcML/src";
            NAMESPACEURI_CPP = "http://www.srcML.org/srcML/cpp";
        }


    }

    public DependencyGraph() {
//        if (current_OS.indexOf("mac") >= 0) {
//            NAMESPACEURI_POSITION = "http://www.sdml.info/srcML/position";
//            NAMESPACEURI = "http://www.sdml.info/srcML/src";
//            NAMESPACEURI_CPP = "http://www.sdml.info/srcML/cpp";
//        } else {
        NAMESPACEURI_POSITION = "http://www.srcML.org/srcML/position";
        NAMESPACEURI = "http://www.srcML.org/srcML/src";
        NAMESPACEURI_CPP = "http://www.srcML.org/srcML/cpp";
//        }
    }

    public void generateChangedDependencyGraphFromCompleteGraph(String sourcecodeDir, String analysisDirName, String testCaseDir, String testDir, Rengine re) {
        this.analysisDir = testCaseDir + testDir + FS;
        this.testCaseDir = testCaseDir;
        String graphPath = sourcecodeDir + analysisDirName + "/complete.pajek.net";
        try {
            String completeGraph = processingText.readResult(graphPath);

            //get node list
            StringBuilder stringBuilder = new StringBuilder();
            String nodeListString = completeGraph.split("\\*arcs")[0];
            String[] nodeList = nodeListString.split("\n");
            for (String line : nodeList) {
                if (!line.startsWith("*")) {
                    String label = line.split(" ")[1];
                    String id = line.split(" ")[0];
                    label_to_id.put(label, id);
                    stringBuilder.append(label + ":" + id + "\n");
                }
            }
            processingText.rewriteFile(stringBuilder.toString(), analysisDir + "nodeLable2IdMap.txt");

            //get edgelist
            String edgeListString = completeGraph.split("\\*arcs")[1];
            String[] edgeList = edgeListString.split("\n");


            /** Check whether forkAddedNode file exists **/
            changedFiles = new HashSet<>();

            forkAddedNodeTxt = "forkAddedNode.txt";
            if (new File(forkAddedNodeTxt).exists()) {
                try {
                    forkaddedNodeList = processingText.getForkAddedNodeList(forkAddedNodeTxt);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                System.out.println("file forkAddedNode.txt does not exist!");
                forkaddedNodeList = new ArrayList<>();
            }

            HashSet<String> forkaddedNodeID_Set = new HashSet<>();
            StringBuilder sb_forkAddedNode = new StringBuilder();
            StringBuilder sb_forkAddedNode_id = new StringBuilder();
            for (String s : forkaddedNodeList) {
                if (label_to_id.get("\"" + s + "\"") != null) {
                    forkaddedNodeID_Set.add(label_to_id.get("\"" + s + "\""));
                    sb_forkAddedNode.append("\"" + label_to_id.get("\"" + s + "\"") + "\",");
                    sb_forkAddedNode_id.append(label_to_id.get("\"" + s + "\"") + ",");
                }
            }

            //get subgraph
            re.eval("library(igraph)");
            System.getProperty("java.library.path");
            re.eval("oldg<-read_graph(\"" + graphPath + "\", format=\'pajek\')");
            re.eval("subv <- c(" + sb_forkAddedNode.toString().substring(0, sb_forkAddedNode.toString().length() - 1) + ")");
            re.eval("subg<-induced.subgraph(graph=oldg,vids=subv)");


            REXP edgelist_R = re.eval("cbind( get.edgelist(subg) , round( E(subg)$weight, 3 ))", true);
            REXP nodelist_R = re.eval("get.vertex.attribute(subg)$id", true);
            double[][] edgelist = edgelist_R.asDoubleMatrix();
            String[] old_nodelist = (String[]) nodelist_R.getContent();


            StringBuilder sub_edgelist_sb = new StringBuilder();
            for (double[] edge : edgelist) {
                String from = label_to_id.get("\"" + old_nodelist[(int) edge[0] - 1] + "\"");
                String to = label_to_id.get("\"" + old_nodelist[(int) edge[1] - 1] + "\"");

                sub_edgelist_sb.append(from + " " + to + " 5\n");

            }

            processingText.rewriteFile(nodeListString + "*Arcs\n" + sub_edgelist_sb.toString(), testCaseDir + "changedCode.pajek.net");
            processingText.rewriteFile(sb_forkAddedNode_id.toString(), testCaseDir + "forkAddedNodeID.txt");


/** generating edges for consecutive lines  **/
            if (CONSECUTIVE) {
                createNeighborEdges_reverse();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * This function get nodeid to label map
     *
     * @return hashmap  nodeid_label
     */
    public HashMap<Integer, String> getNodeid2LableMap(String analysisDir) {
        HashMap<Integer, String> nodeID_label = new HashMap<>();
        try {
            String[] nodeArray = processingText.readResult(analysisDir + "NodeList.txt").split("\n");
            for (String node : nodeArray) {
                String[] nodeInfo = node.split("---------");
                if (nodeInfo.length > 1) {
                    nodeID_label.put(Integer.valueOf(nodeInfo[0]), nodeInfo[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nodeID_label;
    }

    /**
     * This function just call the create Dependency Graph, used for cluster nodes.
     * TODO modify compareTwoGraphs return type
     *
     * @return dependency graph, no edge label stored.
     */
    public HashSet<String> getDependencyGraphForProject(String sourcecodeDir, String testCaseDir, String testDir) {
        if (testDir.equals("")) {
            this.analysisDir = testCaseDir;
        } else {
            this.analysisDir = testCaseDir + testDir + FS;
        }
        this.sourcecodeDir = sourcecodeDir;
        this.testCaseDir = testCaseDir;
        this.testDir = testDir;
        gotoMap = new HashMap<>();
        Root_Dir = processingText.getRootDir();

        /**------------ Specify paths --------------**/
        forkAddedNodeTxt = testCaseDir + "forkAddedNode.txt";
        edgeListTxt = analysisDir + "edgeList.txt";
        parsedLineTxt = analysisDir + "parsedLines.txt";

        /**------------ Preparing for writing into output files  --------------**/
        processingText.rewriteFile("", edgeListTxt);
        processingText.rewriteFile("", parsedLineTxt);

        changedFiles = new HashSet<>();

        /** Check whether forkAddedNode file exists **/
        if (new File(forkAddedNodeTxt).exists()) {
            try {
                forkaddedNodeList = new ProcessingText().getForkAddedNodeList(forkAddedNodeTxt);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("file forkAddedNode.txt does not exist!");
            forkaddedNodeList = new ArrayList<>();
        }

//        processingText.rewriteFile("", analysisDir + "memoryRecord.txt");
        //parse every header file in the project
        try {
            Files.walk(Paths.get(sourcecodeDir)).forEach(filePath -> {
                if (Files.isRegularFile(filePath) && processingText.isHeaderFile(filePath.toString())) {
                    if (!filePath.toString().contains("pixman-arm-neon-asm.h")
                            && !filePath.toString().contains("SkBitmapSamplerTemplate.h")
                            && !filePath.toString().contains("prstrms.h")
                            && !filePath.toString().contains("/js/")) {


                        parseSingleFile(filePath);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        //parse every .c / .cpp  file in the project
        try {
            Files.walk(Paths.get(sourcecodeDir)).forEach(filePath -> {
//                if (Files.isRegularFile(filePath) && processingText.isCFile(filePath.toString())) {
/**  need to parse pde file as well for real fork**/
                if (Files.isRegularFile(filePath) && new ProcessingText().isCLanguageFile(filePath)) {
                    if (!filePath.toString().contains("/matlab/")) {
                        parseSingleFile(filePath);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        /** generating edges cross files  **/
        addEdgesCrossFiles();

        /** generating edges for consecutive lines  **/
        if (CONSECUTIVE) {
            createNeighborEdges();
        }

        /****   Write dependency graphs into pajek files  ****/

        processingText.writeToPajekFile(completeGraph, nodeList, testCaseDir, testDir, "complete.pajek.net", forkaddedNodeList);

        processingText.writeToPajekFile(dependencyGraph, nodeList, testCaseDir, testDir, "changedCode.pajek.net", forkaddedNodeList);

        addAllNodeToChangedCodeGraphFile(testCaseDir);

        /*re-write source code to StringList.txt, remove all the symbols for similarity calculation
             ------ similarity calculation purpose---------     */
        writeStringsToFile(sourceCodeLocMap);


        // get forkAddedNodeId LIST
        String graphPath = testCaseDir + "/complete.pajek.net";
        String completeGraph = null;
        try {
            completeGraph = processingText.readResult(graphPath);

            //get node list
            StringBuilder stringBuilder = new StringBuilder();
            String nodeListString = completeGraph.split("\\*arcs")[0];
            String[] nodeList = nodeListString.split("\n");
            for (String line : nodeList) {
                if (!line.startsWith("*")) {
                    String label = line.split(" ")[1];
                    String id = line.split(" ")[0];
                    label_to_id.put(label, id);
                    stringBuilder.append(label + ":" + id + "\n");
                }
            }
            processingText.rewriteFile(stringBuilder.toString(), analysisDir + "nodeLable2IdMap.txt");

            //get edgelist
            String edgeListString = completeGraph.split("\\*arcs")[1];
            String[] edgeList = edgeListString.split("\n");


            StringBuilder sb_forkAddedNode_id = new StringBuilder();
            for (String s : forkaddedNodeList) {
                if (label_to_id.get("\"" + s + "\"") != null) {
                    sb_forkAddedNode_id.append(label_to_id.get("\"" + s + "\"") + ",");
                }
            }
            processingText.rewriteFile(sb_forkAddedNode_id.toString(), testCaseDir + "forkAddedNodeID.txt");

        } catch (IOException e) {
            e.printStackTrace();
        }


        return edgeList;
    }


    private static void addAllNodeToChangedCodeGraphFile(String analysisDir) {
        ProcessingText processingText = new ProcessingText();
//        String analysisDir =                "/Users/shuruiz/Work/GithubProject/malx122/Marlin/INFOX_output/";
//        String analysisDir =  testCaseDir;
        try {
            String completeNodeList = processingText.readResult(analysisDir + "complete.pajek.net").split("\\*arcs")[0];
            String changedCodeGraph[] = processingText.readResult(analysisDir + "changedCode.pajek.net").split("\\*arcs");

            String changedCodeGraph_edgeList = changedCodeGraph[1];
            String newChangeGraph = completeNodeList + "*arcs" + changedCodeGraph_edgeList;

            processingText.rewriteFile(newChangeGraph, analysisDir + "changedCode.pajek.net");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * This function parses one source code file
     *
     * @param filePath path of source file
     */
    public void parseSingleFile(Path filePath) {
        foundHeaderGuard = false;

        //get fileName
        String fileName = processingText.getFileNameFromDir(filePath.toString(), sourcecodeDir);


        /**   re-write source code file in case the misinterpretation of srcml   **/
        String tmpFilePath = Root_Dir + tmpXmlPath + filePath.toString().replace(Root_Dir, "");
//        if (fileName.endsWith(".h") || fileName.endsWith(".pde")) {  // src2srcml cannot parse  ' *.h' file correctly, so change the suffix '+.cpp'
//        if (fileName.endsWith(".h")) {  // src2srcml cannot parse  ' *.h' file correctly, so change the suffix '+.cpp'
        /**for real fork **/
        if (fileName.endsWith(".h") || fileName.endsWith(".pde") || fileName.endsWith(".ino")) {  // src2srcml cannot parse  ' *.h' file correctly, so change the suffix '+.cpp'
            tmpFilePath += ".cpp";
        }
        try {
            FileUtils.copyFile(filePath.toFile(), new File(tmpFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        /** replace text that cannot be correctly parsed by srcML  **/
        processingText.removeSrcmlCannotHandleText(tmpFilePath);

        /** generating xml file by src2srcml **/
        String xmlFilePath = processingText.getXmlFile(tmpFilePath);


        /** generating DOM tree by xmlParser (xom) **/
        Element root = processingText.getXmlDom(xmlFilePath).getRootElement();

        /** Rewrite the file name for the convenience of generating html files later **/
        String newFileName = processingText.changeFileName(fileName);

        /*  for Apache
                if (!newFileName.equals("server/util_expr_parseC")) {
        */

        /* for Marlin
                 if (!fileName.contains("pcre_globals")) {
        */
        if (!(sourcecodeDir.contains("Apache") && newFileName.equals("server~util_expr_parseC"))
                && !(sourcecodeDir.contains("Apache") && fileName.contains("pcre_globals"))) {
            String parentLocation = "";
            /** Generating dependency graph for  **/
            System.out.println("now parsing ----:" + newFileName);
            generatingDependencyGraphForSubTree(root, newFileName, 1, parentLocation);

        }

    }


    /**
     * This method is calling the generatingDependencyGraphForSubTree function by setting isinit = false
     *
     * @param subTreeRoot    dom tree
     * @param fileName       name of the file
     * @param scope          is the scope of the variable declaration, used for finding the correct def-use relation
     *                       scope = 1: global variable, defined in a class
     *                       scope = currentScope +1 , local variable , defined in a function declaration, if statement, etc.
     * @param parentLocation
     * @return
     */
    public ArrayList<String> generatingDependencyGraphForSubTree(Element subTreeRoot, String fileName, int scope, String parentLocation) {
        return generatingDependencyGraphForSubTree(subTreeRoot, fileName, scope, parentLocation, false);
    }

    /**
     * This function is parsing the dependency graph in a sub-tree
     *
     * @param subTreeRoot    root node for the sub tree
     * @param fileName       prefix of the graph node (filename+line#)
     * @param scope          is the level of the declaration node. (1 means the symbol is in the file level, 2 means the symbol is in a function level)
     * @param parentLocation parent node of the root, used for creating edge: root->parent
     * @return a list of statement locations of the subtree
     */
    public ArrayList<String> generatingDependencyGraphForSubTree(Element subTreeRoot, String fileName, int scope, String parentLocation, boolean isinit) {
        ArrayList<String> tmpStmtList = new ArrayList<>();
        Elements elements = subTreeRoot.getChildElements();
        boolean isFunctionDeclaration = false;
        boolean isGotoLabel = false;
        String tmpParentLocation = parentLocation;
        int tmpScope = scope;
        int size = elements.size();
        for (int i = 0; i < size; i++) {
            String line = "";
            String currentLocation = "";
            if (isinit) {
                line = getLineNumOfElement(subTreeRoot);
                currentLocation = getLocationOfElement(subTreeRoot, fileName);
            }
            Element ele = elements.get(i);


            if (isGotoLabel) {
                parentLocation = label_location;
                scope = scope + 1;
            }

            if (ele.getLocalName().equals("define")) {
                parseDefineNode(fileName, scope, ele);
                String location = getLocationOfElement(ele, fileName);
                storeStrings(location, ele.getValue());
            } else if (ele.getLocalName().equals("function") || ele.getLocalName().equals("constructor") || ele.getLocalName().equals("function_decl")) {
                parseFunctionNode(ele, fileName, scope);
            } else if (ele.getLocalName().equals("if") && !ele.getNamespacePrefix().equals("cpp")) {
                tmpStmtList.add(parseIfStmt(ele, fileName, scope, parentLocation));
            } else if (ele.getLocalName().equals("expr_stmt")) {

                String location = getLocationOfElement(ele, fileName);
                tmpStmtList.add(parseVariableInExpression(ele, location, scope, parentLocation, false));
                storeStrings(location, ele.getValue());
            } else if (ele.getLocalName().equals("empty_stmt")) {
                if (isGotoLabel) {
                    isGotoLabel = false;
                }
            } else if (ele.getLocalName().equals("decl_stmt")) {
                String location = getLocationOfElement(ele, fileName);
                tmpStmtList.add(parseDeclStmt(fileName, scope, parentLocation, ele));
                storeStrings(location, ele.getValue());
//            } else if (ele.getLocalName().equals("label") && (((Element) ele.getParent().getParent()).getLocalName().equals("block"))&&) {
            } else if (ele.getLocalName().equals("label")) {
                /*  160929   because of "goto label..", I modify this for parsing
                <label><name> </name><label>
                 */
                if ((((Element) ele.getParent()).getLocalName().equals("macro"))) {
                    /** this is specifically for struct definition include bitfiles for certain fields
                     e.g.   unsigned short icon : 8;
                     srcml  interprets it in a wrong way, so I hard code this case to parse the symbol
                     <macro><label><expr_stmt>  represent a field
                     **/
                    Symbol declSymbol = addDeclarationSymbol(ele, "decl_stmt", fileName, scope, parentLocation, "");
                    tmpStmtList.add(declSymbol.getLocation());
                } else if (ele.getValue().contains(":")) {
                    isGotoLabel = true;
                    Element name = ele.getFirstChildElement("name", NAMESPACEURI);
                    label_location = getLocationOfElement(ele, fileName);
                    String key = "";
                    for (Map.Entry<String, String> entry : gotoMap.entrySet()) {
                        if (entry.getValue().equals(name.getValue())) {
                            key = entry.getKey();
                            if (CONTROL_FLOW) {
                                addEdgesToFile(key, label_location, "<Control-Flow> goto");
                            }
                            break;
                        }

                    }
                    gotoMap.remove(key);
                }
            } else if (ele.getLocalName().equals("typedef")) {
                parseTypedef(ele, fileName, scope, parentLocation);
            } else if (ele.getLocalName().equals("return")) {
                Element returnContent = ele.getFirstChildElement("expr", NAMESPACEURI);
                if (returnContent != null) {
                    tmpStmtList.add(parseVariableInExpression(ele, "", scope, parentLocation, false));
                }
                String location = getLocationOfElement(ele, fileName);
                storeStrings(location, ele.getValue());
            } else if (ele.getLocalName().equals("for")) {
                tmpStmtList.add(parseForStmt(ele, fileName, scope, parentLocation));
            } else if (ele.getLocalName().equals("while") || ele.getLocalName().equals("switch")) {
                tmpStmtList.add(parseWhileStmt(ele, fileName, scope, parentLocation));
            } else if (ele.getLocalName().equals("case") || ele.getLocalName().equals("default")) {
                tmpStmtList.add(parseCase_Default_Stmt(ele, fileName, scope + 1));
            } else if (ele.getLocalName().equals("enum")) {
                parseEnum(ele, fileName, scope, "");
            } else if (ele.getLocalName().equals("macro")) {
                isFunctionDeclaration = parseMacros(ele, fileName, scope);
            } else if (ele.getLocalName().equals("block")) {
                if (isFunctionDeclaration) {
                    isFunctionDeclaration = false;
                    continue;
                } else {
                    tmpStmtList.addAll(generatingDependencyGraphForSubTree(ele, fileName, scope + 1, parentLocation, isinit));
                }
            } else if (ele.getLocalName().equals("extern")) {
                tmpStmtList.addAll(generatingDependencyGraphForSubTree(ele, fileName, scope, parentLocation, isinit));
                //todo : hierachy?
            } else if (ele.getLocalName().equals("do")) {
                tmpStmtList.add(parseDoWhile(ele, fileName, scope));
            } else if (ele.getLocalName().equals("using")) {
                Element nameEle = ele.getFirstChildElement("name", NAMESPACEURI);
                if (nameEle == null) {
                    Element namespace_Ele = ele.getFirstChildElement("namespace", NAMESPACEURI);
                    nameEle = namespace_Ele.getFirstChildElement("name", NAMESPACEURI);
                }
                String location = getLocationOfElement(nameEle, fileName);
                tmpStmtList.add(fileName + "-" + line);
                Symbol symbol = new Symbol(nameEle.getValue(), "", location, "using", scope);
                findVarDependency(symbol);
            } else if (ele.getLocalName().equals("expr")) {
                parseVariableInExpression(ele, currentLocation, scope, parentLocation, isinit);

            } else if (ele.getLocalName().equals("break") || ele.getLocalName().equals("continue")) {
                String breakLoc = fileName + "-" + getLineNumOfElement(ele);
                storeIntoNodeList(breakLoc);
                tmpStmtList.add(breakLoc);
            } else if (ele.getLocalName().equals("struct_decl")) {
                addDeclarationSymbol(ele, "struct_decl", fileName, scope, parentLocation, "");
            } else if (ele.getLocalName().equals("goto")) {

                Element name = ele.getFirstChildElement("name", NAMESPACEURI);
                String location = getLocationOfElement(ele, fileName);
                gotoMap.put(location, name.getValue());
            } else {
                if (ele.getLocalName().equals("struct")
                        || ele.getLocalName().equals("class")
                        || ele.getLocalName().equals("namespace")
                        || ele.getLocalName().equals("union")
                        ) {
                    parseStructOrClass(ele, fileName, scope, parentLocation, "", ele.getLocalName());
                }
            }

            //remove symbol, whose scope >1
            if (((Element) ele.getParent()).getLocalName().equals("unit") && symbolTable.size() > 0) {
                symbolTable = removeLocalSymbol(symbolTable);
                Util.ProcessingText pt = new ProcessingText();
                pt.writeSymboTableToFile(symbolTable, analysisDir);

            }

            int tmpStmtList_size = tmpStmtList.size();
            if (isGotoLabel && !ele.getLocalName().equals("label") && tmpStmtList_size > 0) {
                String location = tmpStmtList.get(tmpStmtList_size - 1);
                if (HIERACHICAL) {
                    addEdgesToFile(location, parentLocation, "<Hierarchy> goto_label");
                }
                parentLocation = tmpParentLocation;
                scope = tmpScope + 1;
                isGotoLabel = false;
                tmpStmtList.remove(tmpStmtList_size - 1);

            }


        }
        //remove empty s
        ArrayList<String> stmtList = new ArrayList<>();
        for (String s : tmpStmtList) {
            if (!s.equals("")) {
                stmtList.add(s);
            }
        }


        return stmtList;
    }

    /**
     * This method parses declaration statement <decl_stmt> node
     *
     * @param fileName
     * @param scope
     * @param parentLocation
     * @param ele            decl_stmt node
     * @return
     */
    private String parseDeclStmt(String fileName, int scope, String parentLocation, Element ele) {
        Element decl = ele.getFirstChildElement("decl", NAMESPACEURI);
        Symbol declSymbol = addDeclarationSymbol(decl, "decl_stmt", fileName, scope, parentLocation, "");

        //---TODO: following could be removed?----
        if (declSymbol != null) {
            return declSymbol.getLocation();
        }
        System.out.println("error: decl_stmt is null!");
        return "";
    }


    /**
     * This method parses 'define' node, including macro, parameter, function_declaration.
     * if the tag is "macro" , then the node could be macro or parameter node,
     * otherwise, it is a function_declaration node.
     *
     * @param fileName
     * @param scope
     * @param ele
     */
    private void parseDefineNode(String fileName, int scope, Element ele) {
        Element macroEle = ele.getFirstChildElement("macro", NAMESPACEURI_CPP);
        if (macroEle != null) {
            Element paramEle = macroEle.getFirstChildElement("parameter_list", NAMESPACEURI);
            String tag;
            if (paramEle == null & macroEle != null) {
                tag = "macro";
            } else {
                tag = "function_decl";
            }
            Element nameEle = macroEle.getFirstChildElement("name", NAMESPACEURI);
            String macroName = nameEle.getValue();
            boolean isHeaderGuard = false;

            /** Checking whether the macro is a header guard or not,
             *  if it is a header guard, it will not be stored into the symbol table   **/
            if (!foundHeaderGuard && fileName.endsWith("H") && isHeaderGuard(macroName, fileName)) {
                isHeaderGuard = true;
                foundHeaderGuard = true;
            }
            String location;
            if (!isHeaderGuard) {
                location = getLocationOfElement(nameEle, fileName);
                Symbol macro = new Symbol(macroName, "", location, tag, scope);
                storeIntoNodeList(location);
                ArrayList<Symbol> macros = new ArrayList<Symbol>();
                macros.add(macro);
                storeSymbols(macros);
                processingText.writeTofile(location + "\n", parsedLineTxt);

                /** check macro value <cpp:value> , if it is a word, that might be another macro **/
                if (!tag.contains("function_decl")) {
                    Element value_ele = ele.getFirstChildElement("value", NAMESPACEURI_CPP);
                    if (value_ele != null) {
                        String value = value_ele.getValue();
                        String[] element_of_value = value.split(" ");
                        for (String str : element_of_value) {
                            str = str.replaceAll("[(){},.;!?<>%]", "");
                            /**  check the value of macro is a word or not, if it is a word, then it might be another macro **/
                            if (could_be_a_word(str)) {
                                Symbol depend_macro = new Symbol(str, "", location, "cpp:value", scope);
                                findVarDependency(depend_macro);
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * This function parse
     * do{} while()
     *
     * @param ele      do-element
     * @param fileName current fileName
     * @param scope    scope for current node
     * @return list of variable location
     */
    private String parseDoWhile(Element ele, String fileName, int scope) {
        String parentLocation = getLocationOfElement(ele, fileName);
        ArrayList<String> stmtList = new ArrayList<>();
        storeIntoNodeList(parentLocation);

        stmtList.addAll(generatingDependencyGraphForSubTree(ele, fileName, scope + 1, parentLocation));

        //while <condition>
        Element conditionEle = ele.getFirstChildElement("condition", NAMESPACEURI);
        if (conditionEle != null) {
            String location = getLocationOfElement(conditionEle, fileName);
            parseVariableInExpression(conditionEle, location, scope + 1, parentLocation, false);
            stmtList.add(location);
        }

        if (CONTROL_FLOW) {
            addControlFlowDependency(parentLocation, stmtList, "do-while");
        }
        return parentLocation;
    }

    /**
     * This function parse Case stmt of 'switch case '
     *
     * @param ele
     * @param fileName
     * @param scope
     * @return
     */
    private String parseCase_Default_Stmt(Element ele, String fileName, int scope) {
        String caseLocation = fileName + "-" + getLineNumOfElement(ele);
        storeIntoNodeList(caseLocation);
        ArrayList<String> case_tmpStmtList = new ArrayList<>();
        case_tmpStmtList.addAll(generatingDependencyGraphForSubTree(ele, fileName, scope + 1, caseLocation));
        String label = "";
        if (ele.getLocalName().equals("case")) {
            label = "case-block";
        } else if (ele.getLocalName().equals("default")) {
            label = "default-block";
        }
        if (CONTROL_FLOW && case_tmpStmtList.size() > 0) {
            addControlFlowDependency(caseLocation, case_tmpStmtList, label);
        }
        return caseLocation;
    }

    /**
     * This function parse typedef
     *
     * @param ele
     * @param fileName
     * @param scope
     * @param parentLocation
     */
    private void parseTypedef(Element ele, String fileName, int scope, String parentLocation) {
        //typedef struct
        Element type_ele = ele.getFirstChildElement("type", NAMESPACEURI);
        Element name_ele = ele.getFirstChildElement("name", NAMESPACEURI);
        if (type_ele != null) {
            //struct definition
            Element structChild = type_ele.getFirstChildElement("struct", NAMESPACEURI);
            boolean structDef = (structChild != null);
            //struct declaration :" typedef struct name alias;"
            boolean structDecl = (type_ele.getChild(0) instanceof Text) && type_ele.getChild(0).getValue().trim().equals("struct");
            //struct definition
            Element enum_Child = type_ele.getFirstChildElement("enum", NAMESPACEURI);

            if (enum_Child != null) {
                int i = 0;

                while (name_ele == null) {
                    name_ele = enum_Child.getFirstChildElement("name", NAMESPACEURI);

                    if (name_ele == null) {
                        return;
                    }
                }

                String alias = "";
                if (name_ele != null) {
                    alias = name_ele.getValue();
                }
                if (structDef) {
                    if (structChild.getLocalName().equals("struct")) {
                        parseStructOrClass(structChild, fileName, scope, parentLocation, alias, "struct");
                    }
                } else if (structDecl) {
                    addDeclarationSymbol(type_ele, "struct", fileName, scope, parentLocation, alias);
                } else if (enum_Child != null) {
                    parseEnum(enum_Child, fileName, scope, name_ele.getValue());
                } else {
                    String location = getLocationOfElement(ele, fileName);
                    Symbol symbol = new Symbol(alias, type_ele.getValue(), location, "typedef", scope, "");
                    ArrayList<Symbol> newsymbol = new ArrayList<>();
                    newsymbol.add(symbol);
                    storeSymbols(newsymbol);

                    //save into nodeList
                    storeIntoNodeList(location);
                }


            }
            //typedef function
            Element funcDecl_ele = ele.getFirstChildElement("function_decl", NAMESPACEURI);
            if (funcDecl_ele != null) {
                parseFunctionNode(funcDecl_ele, fileName, scope);
            }
        }
        return;
    }


    /**
     * This function parse macro definition element
     *
     * @param ele
     * @param fileName
     * @param scope
     * @return if the macro is actually a FunctionDeclaration, then return true; otherwise, return false;
     */
    private boolean parseMacros(Element ele, String fileName, int scope) {
        Element nameEle = ele.getFirstChildElement("name", NAMESPACEURI);
        if (nameEle != null) {
            Element argumentListEle = ele.getFirstChildElement("argument_list", NAMESPACEURI);
            String macroName = nameEle.getValue();
            String location = getLocationOfElement(nameEle, fileName);

            /**  check whether current macro is a function declaration, because srcml cannot parse it correctly
             * example : Apache/module/arch/unix/mod_unixd.c  L322-L373
             * **/
            Elements siblings = ((Element) ele.getParent()).getChildElements();
            int siblings_size = siblings.size();
            for (int i = 0; i < siblings_size; i++) {
                if (siblings.get(i).getValue().toString().equals(ele.getValue().toString()) && (i + 1) < siblings_size) {
                    if (siblings.get(i + 1).getLocalName().equals("block")) {
                        Symbol func_decl = new Symbol(macroName, "", location, "function_decl", scope);
                        ArrayList<Symbol> newsymbol = new ArrayList<>();
                        newsymbol.add(func_decl);
                        storeSymbols(newsymbol);
                        storeIntoNodeList(location);

                        //check block
                        Element block = siblings.get(i + 1);
                        if (block != null) {
                            ArrayList<String> stmtInBlock = generatingDependencyGraphForSubTree(block, fileName, scope + 1, location);
                            if (HIERACHICAL) {
                                linkChildToParent(stmtInBlock, location, "<Hierarchy> function-block");
                            }
                        }
                        return true;
                    }
                }
            }


            String tag = "macro";
            if (argumentListEle != null) {
                tag = "call";
            }


            Symbol macro = new Symbol(macroName, "", location, tag, scope);
            storeIntoNodeList(location);

            symbolTable.add(macro);
            lonelySymbolSet.add(macro);
            processingText.writeTofile(location + "\n", parsedLineTxt);

            if (argumentListEle != null) {
                Elements arguments = argumentListEle.getChildElements();
                int arguments_size = arguments.size();
                for (int x = 0; x < arguments_size; x++) {

                    Element argument = arguments.get(x);
                    String argumentLocation = getLocationOfElement(argument, fileName);

                    //save into nodeList
                    storeIntoNodeList(argumentLocation);

                    String var = argument.getValue();
                    Symbol dependent = new Symbol(var, "", argumentLocation, "name", scope);
                    findVarDependency(dependent);
                }
            }

            return false;
        }
        System.out.println("macro name is null!----it is not a macro");
        return false;
    }

    /**
     * this function remove the symbols that in the 2 level , prepare for find edge cross files
     *
     * @param tmpSymbolList symbol table
     * @return symbol table only contains 1st level symbols
     */
    private HashSet<Symbol> removeLocalSymbol(HashSet<Symbol> tmpSymbolList) {
        HashSet<Symbol> finalSymbolList = new HashSet<>();
        for (Symbol s : tmpSymbolList) {
            if (s.getScope() == 1)
                finalSymbolList.add(s);
        }
        return finalSymbolList;
    }

    /**
     * This function create the edge from a set of child to parent (used for struct)
     *
     * @param childrenLocations statements' location
     * @param parentLocation    struct node's location
     */

    private void linkChildToParent(ArrayList<String> childrenLocations, String parentLocation, String label) {
        if (HIERACHICAL && !parentLocation.equals("")) {
            for (String child : childrenLocations) {
                addEdgesToFile(child, parentLocation, label);
            }
        }
    }

    /**
     * This function link children to parent, used for block -> function, then-> if, else->if
     *
     * @param childLocation
     * @param parentLocation
     */
    private void linkChildToParent(String childLocation, String parentLocation, Element element) {
        if (!parentLocation.equals("") && !childLocation.equals("") && !childLocation.equals(parentLocation)) {
            if (HIERACHICAL && isValideHierachy(element)) {
                addEdgesToFile(childLocation, parentLocation, element.getLocalName());
            }
        }
    }

    /**
     * This function check whether it is valid to add hierachical edge for current element
     *
     * @param element
     * @return true, if it is valid; otherwise, return false
     */
    private boolean isValideHierachy(Element element) {
        return element.getLocalName().equals("function")
                || element.getLocalName().equals("struct")
                || element.getLocalName().equals("class")
                || element.getLocalName().equals("namespace")
                || element.getLocalName().equals("enum");
    }

    /**
     * This function parses struct or class statement
     *
     * @param ele
     * @param fileName
     * @param scope
     * @param parentLocation
     * @param alias          struct may havs a alias.
     * @param tag
     */
    private void parseStructOrClass(Element ele, String fileName, int scope, String parentLocation, String alias, String tag) {
        String edgeLabel = "";
        if (tag.equals("struct")) {
            edgeLabel = "<belongToStruct>";
        } else if (tag.equals("class")) {
            edgeLabel = "<belongToClass>";
        } else if (tag.equals("namespace")) {
            edgeLabel = "<belongToNamespace>";
        } else if (tag.equals("union")) {
            edgeLabel = "<belongToUnion>";
        }

        //struct
        Symbol parent = addDeclarationSymbol(ele, tag, fileName, scope, parentLocation, alias);
        findVarDependency(parent);
        if (parent != null) {
            parentLocation = parent.getLocation();
        }

        //block
        Element block = ele.getFirstChildElement("block", NAMESPACEURI);
        if (block != null) {
            Elements block_childElements = block.getChildElements();

            int block_childElements_size = block_childElements.size();
            if (block_childElements_size > 0) {
                if (block_childElements.get(0).getLocalName().equals("public")
                        || block_childElements.get(0).getLocalName().equals("private")
                        || block_childElements.get(0).getLocalName().equals("protected")) {

                    //for common struct definition
                    for (int s = 0; s < block_childElements_size; s++) {
                        Element group = block_childElements.get(s);

                        //get children
                        ArrayList<String> children = generatingDependencyGraphForSubTree(group, fileName, scope, parentLocation);
                        boolean tmpHierarchy = true;
                        if (!HIERACHICAL) {
                            tmpHierarchy = HIERACHICAL;
                            HIERACHICAL = true;
                        }
                        //link children -> parent
                        linkChildToParent(children, parentLocation, edgeLabel);
                        if (!tmpHierarchy) {
                            HIERACHICAL = tmpHierarchy;
                        }
                    }

                } else {
                    //get children
                    ArrayList<String> children = generatingDependencyGraphForSubTree(block, fileName, scope, parentLocation);
                    boolean tmpHierarchy = true;
                    if (!HIERACHICAL) {
                        tmpHierarchy = HIERACHICAL;
                        HIERACHICAL = true;
                    }
                    //link children -> parent
                    linkChildToParent(children, parentLocation, edgeLabel);
                    if (!tmpHierarchy) {
                        HIERACHICAL = tmpHierarchy;
                    }
                }
            }
        }
    }

    /**
     * This function parses the function node.
     * First, create symbol for the function, scope is 1
     * Second, use {@link #generatingDependencyGraphForSubTree(Element, String, int, String)} function to parse the <block> subtree
     *
     * @param element  function element
     * @param fileName current filename, used for mark dependency graph's node name (lineNumber-fileName)
     * @param scope    function's scope is 1, symbol in block is 2
     */
    private void parseFunctionNode(Element element, String fileName, int scope) {

        String tag = "";
        if (element.getLocalName().equals("function") || element.getLocalName().equals("constructor")) {
            tag = "function";
        } else {
            tag = "function_decl";
        }

        //add function to symbol table
        Symbol functionSymbol = addDeclarationSymbol(element, tag, fileName, scope, "", "");
        String parentLocation = functionSymbol.getLocation();
        //check parameters
        Element parameter_list = element.getFirstChildElement("parameter_list", NAMESPACEURI);
        if (parameter_list != null && parameter_list.getChildElements() != null) {
            for (int i = 0; i < parameter_list.getChildElements("param", NAMESPACEURI).size(); i++) {
                Element paramNode = parameter_list.getChildElements("param", NAMESPACEURI).get(i);
                //add Parameter to symbol table
                if (paramNode.getChildElements().size() > 0) {
                    addDeclarationSymbol((Element) paramNode.getChild(0), "param", fileName, scope + 1, parentLocation, "");
                }
            }
            //store string for comparing
            String location = getLocationOfElement(parameter_list, fileName);
            storeStrings(location, functionSymbol.getType() + " " + functionSymbol.getName() + " " + parameter_list.getValue());
        }

        //check block
        Element block = element.getFirstChildElement("block", NAMESPACEURI);
        if (block != null) {
            ArrayList<String> stmtInBlock = generatingDependencyGraphForSubTree(block, fileName, scope + 1, parentLocation);
            if (HIERACHICAL) {
                linkChildToParent(stmtInBlock, parentLocation, "<Hierarchy> function-block");
            }
        }
    }

    /**
     * this function parse if statement
     *
     * @param ele      if  element
     * @param fileName fileName of this node
     * @param scope    level of the node
     * @return if condition location
     */
    private String parseIfStmt(Element ele, String fileName, int scope, String parentLocation) {
        //<if><condition><then>[<else>], else is optional
        Element condition = ele.getFirstChildElement("condition", NAMESPACEURI);
        String ifStmtLocation = getLocationOfElement(condition, fileName);

        //store string
        storeStrings(ifStmtLocation, condition.getValue());

        parseVariableInExpression(condition, ifStmtLocation, scope, parentLocation, false);
        //control flow analysis dependency
        if (!CONTROL_FLOW) {
            ifStmtLocation = "";
        }

        //<then> [<block>], block is optional
        Element then_Node = ele.getFirstChildElement("then", NAMESPACEURI);
        ArrayList<String> symbolsInThen;

        if (then_Node != null) {
            if (then_Node.getFirstChildElement("block", NAMESPACEURI) != null) {
                Element block = then_Node.getFirstChildElement("block", NAMESPACEURI);
                symbolsInThen = generatingDependencyGraphForSubTree(block, fileName, scope, ifStmtLocation);
            } else {
                symbolsInThen = generatingDependencyGraphForSubTree(then_Node, fileName, scope, ifStmtLocation);
            }

            //else is optional
            ArrayList<String> symbolsInElse = null;
            Element else_Node = ele.getFirstChildElement("else", NAMESPACEURI);
            if (else_Node != null) {
                if (else_Node.getFirstChildElement("block", NAMESPACEURI) != null) {
                    Element block = else_Node.getFirstChildElement("block", NAMESPACEURI);
                    symbolsInElse = generatingDependencyGraphForSubTree(block, fileName, scope, ifStmtLocation);
                } else {
                    symbolsInElse = generatingDependencyGraphForSubTree(else_Node, fileName, scope, ifStmtLocation);
                }
            }

            //add control flow dependency
            if (CONTROL_FLOW && symbolsInThen.size() > 0) {
                addControlFlowDependency(ifStmtLocation, symbolsInThen, "if-then");
                if (symbolsInElse != null && symbolsInElse.size() > 0) {
                    addControlFlowDependency(ifStmtLocation, symbolsInElse, "if-else");
                }
            }
        }
        return ifStmtLocation;
    }

    /**
     * this function parse if statement
     *
     * @param ele      if  element
     * @param fileName fileName of this node
     * @param scope    level of the node
     * @return list of statement location
     */
    private void parseEnum(Element ele, String fileName, int scope, String name) {
        ArrayList<Symbol> newsymbol = new ArrayList<>();
        String enumLocation = getLocationOfElement(ele, fileName);
        //name
        String alias = "";

        Element enumNameElement = ele.getFirstChildElement("name", NAMESPACEURI);
        if (enumNameElement != null) {
            name = enumNameElement.getValue();
        }

        Element aliaElement = ele.getFirstChildElement("decl", NAMESPACEURI);
        if (aliaElement != null) {
            alias = aliaElement.getValue();
            if (name.equals(""))
                name = alias;
        }

        Symbol enumSymbol = new Symbol(name, "", enumLocation, "enum", scope, alias);
        newsymbol.add(enumSymbol);
        storeIntoNodeList(enumLocation);

        //<block>
        Element block = ele.getFirstChildElement("block", NAMESPACEURI);
        if (block != null) {
            Elements elements = block.getChildElements();
            for (int i = 0; i < elements.size(); i++) {
                Element decl = elements.get(i).getFirstChildElement("name", NAMESPACEURI);
                if (decl != null) {
                    String declLocation = getLocationOfElement(decl, fileName);
                    Symbol declSymbol = new Symbol(decl.getValue(), "", declLocation, "decl", scope);
                    newsymbol.add(declSymbol);
                    storeIntoNodeList(declLocation);
                    linkChildToParent(declLocation, enumLocation, ele);
                }
            }
        }
        storeSymbols(newsymbol);
    }


    /**
     * this function parse while node.
     *
     * @param ele            while node , type is element
     * @param fileName       file name
     * @param scope          while node's scope
     * @param parentLocation parent's location
     * @return a list of statement location of 'while' node
     */
    private String parseWhileStmt(Element ele, String fileName, int scope, String parentLocation) {

        if (parentLocation.equals("")) {
            parentLocation = fileName + "-" + getLineNumOfElement(ele);
        }
        //<while><condition><block>
        Element condition = ele.getFirstChildElement("condition", NAMESPACEURI);
        ArrayList<String> stmtList = new ArrayList<>();
        String whileLocation = parseVariableInExpression(condition, "", scope, parentLocation, false);

        //store string
        storeStrings(whileLocation, condition.getValue());

        if (whileLocation.equals("")) {
            whileLocation = fileName + "-" + getLineNumOfElement(condition);
            //save into nodeList
            storeIntoNodeList(whileLocation);
        }
        //Block
        Element block = ele.getFirstChildElement("block", NAMESPACEURI);
        if (block != null) {
            stmtList.addAll(generatingDependencyGraphForSubTree(block, fileName, scope + 1, whileLocation));
        }
        Element expr = ele.getFirstChildElement("expr_stmt", NAMESPACEURI);
        if (expr != null) {
            stmtList.add(parseVariableInExpression(expr, "", scope, parentLocation, false));
        }

        //add control flow dependency
        if (ele.getLocalName().equals("while")) {
            if (CONTROL_FLOW && stmtList.size() > 0) {
                String label = ele.getLocalName();
                addControlFlowDependency(whileLocation, stmtList, label);
            }
        } else if (ele.getLocalName().equals("switch")) {
            linkChildToParent(stmtList, whileLocation, "<Control-Flow> switch-case");
        }
        return whileLocation;
    }

    /**
     * this function add control flow dependency for if-then-else, while, etc.
     *
     * @param headLocation condition location
     * @param stmtList     statement list in block
     * @param label        label of the edge
     */
    private void addControlFlowDependency(String headLocation, ArrayList<String> stmtList, String label) {
        if (CONTROL_FLOW) {
            boolean allBelongToNewCode = true;
            if (forkaddedNodeList.size() > 0) {
                boolean stmtIsNew = true;
                for (String stmtLoc : stmtList) {
                    stmtIsNew = stmtIsNew && forkaddedNodeList.contains(stmtLoc);
                    if (stmtIsNew == false) {
                        allBelongToNewCode = false;
                        break;
                    }
                }
            }
            if (!allBelongToNewCode) {
                linkChildToParent(stmtList, headLocation, "<Control-Flow>");
            } else {
                int stmtList_size = stmtList.size();
                if (stmtList_size > 0) {
                    addEdgesToFile(stmtList.get(0), headLocation, CONTROLFLOW_LABEL + " " + label);
                    for (int i = 0; i < stmtList_size - 1; i++) {
                        String pre_loc = stmtList.get(i);
                        String after_loc = stmtList.get(i + 1);
                        if (!pre_loc.equals(after_loc)) {
                            addEdgesToFile(after_loc, pre_loc, CONTROLFLOW_LABEL + " " + label);
                        }
                    }
                }
            }
        }
    }


    /**
     * This function parse for loop
     * <for>
     * +---<control>
     * +---<init>
     * +--<condition></condition>
     * +---<incr>
     * +---<block>
     *
     * @param ele            for node
     * @param fileName
     * @param scope
     * @param parentLocation
     * @return List of statement location
     */
    private String parseForStmt(Element ele, String fileName, int scope, String parentLocation) {
        ArrayList<String> tmpStmtList = new ArrayList<>();
        String forLocation = getLocationOfElement(ele, fileName);
        Element init = ele.getFirstChildElement("init", NAMESPACEURI);
        Element init_decl = null, init_expr = null;
        if (init.getFirstChildElement("decl", NAMESPACEURI) != null) {
            init_decl = init.getFirstChildElement("decl", NAMESPACEURI);
        } else if (init.getFirstChildElement("expr", NAMESPACEURI) != null) {
            init_expr = init.getFirstChildElement("expr", NAMESPACEURI);
        }
        Symbol initVarSymbol;
        if (init_decl != null) {
            initVarSymbol = addDeclarationSymbol(init_decl, "for", fileName, scope, parentLocation, "");
            tmpStmtList.add(initVarSymbol.getLocation());
        } else if (init_expr != null) {
            parseVariableInExpression(init, getLocationOfElement(init_expr, fileName), scope, parentLocation, false);
        }

        Element condition = ele.getFirstChildElement("condition", NAMESPACEURI);

        if (condition != null) {
            Element cond_exprNode = condition.getFirstChildElement("expr", NAMESPACEURI);
            if (cond_exprNode != null) {
                parseVariableInExpression(condition, getLocationOfElement(cond_exprNode, fileName), scope, parentLocation, false);
            }
        }
        storeIntoNodeList(forLocation);
        Element block = ele.getFirstChildElement("block", NAMESPACEURI);
        if (block != null && block.getChildElements().size() > 0) {
            tmpStmtList.addAll(generatingDependencyGraphForSubTree(block, fileName, scope + 1, forLocation));
            addControlFlowDependency(forLocation, tmpStmtList, "for-loop");
        }
        Element expr = ele.getFirstChildElement("expr_stmt", NAMESPACEURI);
        if (expr != null) {
            parseVariableInExpression(expr, "", scope, parentLocation, false);
        }
        return forLocation;
    }

    /**
     * This function adds symbol and add it to symbolTable
     * e.g.: <decl_stmt><decl><type><name> [<init>]
     * parameter definition
     * function
     * ...
     *
     * @param element  declaration element
     * @param tag      srcml tag
     * @param fileName current filename, used for mark dependency graph's node name (lineNumber-fileName)
     * @param scope    function's scope is 1, symbol in block is 2
     * @return new symbol
     */
    private Symbol addDeclarationSymbol(Element element, String tag, String fileName, int scope, String parentLocation, String alias) {
        String type;
        Element type_Node = element.getFirstChildElement("type", NAMESPACEURI);
        if (type_Node != null) {
            Element name_ele = type_Node.getFirstChildElement("name", NAMESPACEURI);
            if (name_ele != null) {
                type = name_ele.getValue();
            } else {
                type = type_Node.getValue();
            }
        } else {
            type = "";
        }

        Symbol symbol;
        Element nameElement = element.getFirstChildElement("name", NAMESPACEURI);
        Element decl_Element = element.getFirstChildElement("decl", NAMESPACEURI);
        if (decl_Element != null && nameElement == null) {
            nameElement = decl_Element;
        }
        String name;
        if (nameElement != null && alias.equals("")) {
            //for the case of array , e.g. a[3]  <name><name>a</name><index>3</index><name>
            Element subnameEle = nameElement.getFirstChildElement("name", NAMESPACEURI);
            if (subnameEle != null) {
                name = subnameEle.getValue();
            } else {
                name = nameElement.getValue();
            }
            if (name.contains("::")) {
                name = name.split("::")[1];
            }
        } else if (nameElement == null && !alias.equals("")) {
            name = alias;    //this is only used for typedef struct
        } else if (nameElement != null && !alias.equals("")) {
            name = nameElement.getValue();
        } else {
            name = tag;
        }


        String location;
        if (((Element) element.getParent()).getLocalName().endsWith("param")) {
            location = parentLocation;
        } else {
            if (nameElement != null) {
                location = getLocationOfElement(nameElement, fileName);
            } else {
                location = getLocationOfElement(element, fileName);
            }
        }
        symbol = new Symbol(name, type, location, tag, scope, alias);
        ArrayList<Symbol> newsymbol = new ArrayList<>();
        newsymbol.add(symbol);
        storeSymbols(newsymbol);

        //save into nodeList
        storeIntoNodeList(location);

        //store string for comparing
        if (element.getLocalName().equals("decl")) {
            if ((!((Element) (element.getParent().getParent())).getLocalName().equals("parameter_list"))
                    && (!((Element) (element.getParent())).getLocalName().equals("decl_stmt"))) {
                storeStrings(getLocationOfElement(element, fileName), type + " " + name);
            }
        } else if (!element.getLocalName().equals("function")
                && !element.getLocalName().equals("function_decl")
                && !element.getLocalName().equals("constructor")) {
            storeStrings(getLocationOfElement(element, fileName), type + " " + name);
        }

        //index is optional
        if (nameElement != null) {
            Element indexNode = nameElement.getFirstChildElement("index", NAMESPACEURI);
            if (indexNode != null) {
                parseVariableInExpression(indexNode, getLocationOfElement(indexNode, fileName), scope, parentLocation, true);
            }
        }

        // init is optional
        Element initNode = element.getFirstChildElement("init", NAMESPACEURI);
        if (initNode != null) {
            boolean init = true;
            parseVariableInExpression(initNode, location, scope, parentLocation, init);
        }

        //name def-use matching (find corresponding macros, if it exists)
        Symbol name_symbol = new Symbol(name, "", location, "decl_name", scope);
        findVarDependency(name_symbol);

        //type def-use matching
        if (is_user_defined_type(type)) {
            Symbol type_symbol = new Symbol(type, "type", location, "", scope);
            findVarDependency(type_symbol);
        }

        processingText.writeTofile(getLocationOfElement(element, fileName) + "\n", parsedLineTxt);
        return symbol;
    }


    /**
     * This function check whether the type is user defined or not.
     *
     * @param type name of the type
     * @return true, if it is user defined.
     */
    private boolean is_user_defined_type(String type) {
        if (type.contains("char") || type.contains("int") || type.contains("long") || type.contains("float") || type.contains("double")
                || type.contains("void") || type.contains("boolean") || type.contains("struct") || type.equals("")) {
            return false;
        }
        return true;
    }


    /**
     * * This function find variables exist in expression, and create edges if needed
     *
     * @param element        an element contains expression element
     * @param stmtLocation   filename-linenumber
     * @param scope          is used for mark the symbol's position
     * @param parentLocation parent node's location
     * @param isInit         whether this element is a initial (function call)
     * @return expression's Location
     */
//    private String parseVariableInExpression(Element element, String stmtLineNumber, String fileName, int scope, String parentLocation, boolean isInit) {
    private String parseVariableInExpression(Element element, String stmtLocation, int scope, String parentLocation, boolean isInit) {

        Element exprNode;

        String fileName;
        if (!parentLocation.equals("")) {
            fileName = parentLocation.split("-")[0];
        } else {
            fileName = stmtLocation.split("-")[0];
        }
        if (element.getLocalName().equals("expr")) {
            exprNode = element;
        } else {
            exprNode = element.getFirstChildElement("expr", NAMESPACEURI);
        }
        String exprLocation = "";
        if (exprNode != null && !exprNode.getValue().equals("int")) {

            //<expr><name> [<name1...><name2...>] </name>  </expr>
            Elements name_Elements = exprNode.getChildElements("name", NAMESPACEURI);
            int nameElements_size = name_Elements.size();
            if (nameElements_size > 0) {
                for (int i = 0; i < nameElements_size; i++) {
                    Elements nameList = name_Elements.get(i).getChildElements("name", NAMESPACEURI);
                    String var;
                    Symbol dependent;
                    int nameList_size = nameList.size();
                    if (nameList_size > 0) {
                        if (stmtLocation.equals("")) {
                            stmtLocation = getLocationOfElement(nameList.get(0), fileName);
                        }
                        exprLocation = stmtLocation;
                        //save into nodeList
                        if (!isInit) {
                            storeIntoNodeList(exprLocation);
                        }
                        for (int x = 0; x < nameList_size; x++) {
                            var = nameList.get(x).getValue();
                            dependent = new Symbol(var, "", stmtLocation, "name", scope);
                            findVarDependency(dependent);
                        }
                    } else {
                        var = name_Elements.get(i).getValue();
                        if (is_not_a_C_dataType(var)) {
                            if (stmtLocation.equals("")) {
                                stmtLocation = getLocationOfElement(name_Elements.get(i), fileName);
                            }
                            dependent = new Symbol(var, "", stmtLocation, "name", scope);
                            exprLocation = stmtLocation;
                            if (!isInit) {
                                //save into nodeList
                                storeIntoNodeList(exprLocation);
                            }
                            findVarDependency(dependent);
                        }
                    }
                    Element index_Element = name_Elements.get(i).getFirstChildElement("index", NAMESPACEURI);
                    if (index_Element != null) {
                        parseVariableInExpression(index_Element, stmtLocation, scope, parentLocation, false);
                    }
                }
            }

            //<expr><call>
            Elements callElementList = exprNode.getChildElements("call", NAMESPACEURI);
            int callElementList_size = callElementList.size();
            for (int i = 0; i < callElementList_size; i++) {
                Element callElement = callElementList.get(i);
                if (callElement != null) {
                    // <expr> <name> = <call>, call is initializing the name
                    if (!exprLocation.equals("")) {
                        isInit = true;
                    }
                    String callLocation = handleCallNode(callElement, stmtLocation, scope, parentLocation, isInit);
                    if (exprLocation.equals("")) {
                        exprLocation = callLocation;
                    }
                }
            }

            //<expr><sizeof>
            Elements sizeofElements = exprNode.getChildElements("sizeof", NAMESPACEURI);
            int sizeofElements_size = sizeofElements.size();
            if (sizeofElements_size > 0)
                for (int i = 0; i < sizeofElements_size; i++) {
                    Element sizeofElement = sizeofElements.get(i);
                    exprLocation = getLocationOfElement(sizeofElement, fileName);
                    //argument list
                    Element argumentList = sizeofElement.getFirstChildElement("argument_list", NAMESPACEURI);
                    if (argumentList != null) {
                        Elements argue_children = argumentList.getChildElements();
                        handleArgumentList(argue_children, stmtLocation, scope, parentLocation, isInit);
                    }
                }

            //return nodes has its own line Number
            if (element.getLocalName().equals("return")) {
                String line = getLineNumOfElement(element);
                exprLocation = fileName + "-" + line;
                //save into nodeList
                storeIntoNodeList(exprLocation);
            }

            //block node
            Element exprBlockEle = exprNode.getFirstChildElement("block", NAMESPACEURI);
            if (exprBlockEle != null) {
                if (!isInit && parentLocation.equals("") && !((Element) exprNode.getParent()).getLocalName().equals("unit")) {
                    parentLocation = fileName + "-" + getLineNumOfElement(exprBlockEle);
                    storeIntoNodeList(parentLocation);
                }
                ArrayList<String> tmpStmtList = generatingDependencyGraphForSubTree(exprBlockEle, fileName, scope + 1, parentLocation, isInit);
                linkChildToParent(tmpStmtList, parentLocation, "<init>");
            }

            //macro node  (hard code wrong output from src2srcml)
            Elements macroEleList = exprNode.getChildElements("macro", NAMESPACEURI);
            int macroElementList_size = macroEleList.size();
            for (int j = 0; j < macroElementList_size; j++) {
                Element macroEle = macroEleList.get(j);
                parseMacros(macroEle, fileName, scope);
            }

            linkChildToParent(exprLocation, parentLocation, element);
            processingText.writeTofile(exprLocation + "\n", parsedLineTxt);
        }

        storeStrings(exprLocation, element.getValue());
        return exprLocation;
    }

    /**
     * This function create edges between consecutive lines,
     * ignore comments
     */
    private void createNeighborEdges_reverse() {
        StringBuilder sb = new StringBuilder();


        String currentFile = "";
        int preLineNum = -1;
        int diff = 1;
        //todo: fork added node

/**   only generate consecutive edge for new code **/
        for (String s : forkaddedNodeList) {
            s = s.trim();
            if (!s.equals("")) {
                String[] nodelabel = s.trim().split("-");
                String fileName = nodelabel[0];
                int lineNum = Integer.valueOf(nodelabel[1]);
                if (fileName.equals(currentFile)) {
                    if (lineNum == preLineNum + diff) {
                        String preloc = fileName + "-" + preLineNum;
                        String a = label_to_id.get("\"" + s + "\"");
                        String b = label_to_id.get("\"" + preloc + "\"");
                        if (a != null && b != null) {
                            sb.append(a + " " + b + " 1\n");
                            diff = 1;
                            preLineNum = lineNum;
                        } else if (b == null && a != null) {
                            preLineNum = lineNum;
                        } else if (b != null && a == null) {
                            diff++;
                        } else {
                            preLineNum = lineNum - 1;
                        }
                    } else {
                        preLineNum = lineNum;
                        diff = 1;
                    }
                } else {
                    diff = 1;
                    preLineNum = lineNum;
                }
                if (preLineNum == -1) {
                    preLineNum = lineNum;
                }
                currentFile = fileName;
            }
        }

        processingText.writeTofile(sb.toString(), testCaseDir + "changedCode.pajek.net");

    }

    private boolean is_not_a_C_dataType(String var) {
        return !var.equals("void") && !var.equals("float") && !var.equals("uint32") && !var.equals("NULL");
    }

    private String getLineNumOfElement(Element element) {
        String lineNum = "-1";
        if (element.getAttributeCount() > 0) {
            lineNum = element.getAttribute("line", NAMESPACEURI_POSITION).getValue();
        } else {
            Elements childElements = element.getChildElements();
            int childElements_size = childElements.size();
            if (childElements_size > 0) {
                for (int i = 0; i < childElements_size; i++) {
                    lineNum = getLineNumOfElement(childElements.get(i));
                    if (!lineNum.equals("")) {
                        break;
                    }
                }
            } else {
                lineNum = getLineNumOfElement((Element) element.getParent());
            }
        }
        return lineNum;
    }

    private String getLocationOfElement(Element element, String fileName) {

        return fileName + "-" + getLineNumOfElement(element);
    }


    /**
     * This function store node in dependency graph and
     *
     * @param exprLocation
     */
    private void storeIntoNodeList(String exprLocation) {
        if (exprLocation.equals("")) {
            System.out.println("");
        }
        // -----------for dependency graph
        if (!nodeList.containsKey(exprLocation)) {
            id++;
            nodeList.put(exprLocation, id);
            if (forkaddedNodeList.contains(exprLocation)) {
                dependencyGraph.put(exprLocation, new HashSet<>());
            }
            completeGraph.put(exprLocation, new HashSet<>());

            if (!complete_nodeList.contains(exprLocation)) {
                complete_nodeList.add(exprLocation);
            }
        }
//        System.out.println(exprLocation);
        // --------for compact graph--------------
        String filename = exprLocation.split("-")[0];
        if (!changedFiles.contains(filename)) {
            exprLocation = filename + "-all";
        }


    }

    /**
     * * This function parse call element
     * <expr><call> <name> <argument_list> </call> </expr>
     *
     * @param element        call element
     * @param stmtLocation   (lineNumber-fileName)
     * @param scope          is used for mark the symbol's position
     * @param parentLocation
     * @param isInit         whether this call node is an isolated node or initialization for a variable.
     *                       if it is a initialization, isInit = true
     * @return call node Location
     */

    private String handleCallNode(Element element, String stmtLocation, int scope, String parentLocation, boolean isInit) {
        String fileName;
        if (!parentLocation.equals("")) {
            fileName = parentLocation.split("-")[0];
        } else {
            fileName = stmtLocation.split("-")[0];
        }
        //call node
        Element exprNode = element.getFirstChildElement("expr", NAMESPACEURI);
        Element callNode;
        if (exprNode != null) {
            callNode = exprNode.getFirstChildElement("call", NAMESPACEURI);
        } else {
            callNode = element;
        }
        Element callNameElement = callNode.getFirstChildElement("name", NAMESPACEURI);
        String location = "";

        if (callNameElement != null) {
            if (isInit && stmtLocation.length() > 0) {
                location = stmtLocation;
            } else {
                location = getLocationOfElement(callNameElement, fileName);
            }
            System.out.print(location + "\n");
            String callName = callNameElement.getValue();
            if (callName.contains(".")) {
                String obj = callName.split("\\.")[0];
                callName = callName.split("\\.")[1];
                Symbol dependent = new Symbol(obj, "", location, "name", scope);
                findVarDependency(dependent);
            }

//            nodeLabel = getLocationOfElement(callNameElement, fileName);
            //save into dependent Table
            //call's level is 1
            if (!callName.equals("defined")) {
                //save into nodeList
                if (!isInit) {
                    storeIntoNodeList(location);
                }
                Symbol call = new Symbol(callName, "", location, "call", 1);
                lonelySymbolSet.add(call);
                addFuncDependency(call);
            }
        }
        //argument list
        Element argumentListEle = callNode.getFirstChildElement("argument_list", NAMESPACEURI);
        if (argumentListEle != null) {
            Elements argumentList = argumentListEle.getChildElements("argument", NAMESPACEURI);
            if (location.equals("")) {
                location = getLocationOfElement(argumentListEle, fileName);
            }
            handleArgumentList(argumentList, location, scope, parentLocation, isInit);
        }
        return location;
    }

    /**
     * This function handle argumentList in <call> or in <sizeof>
     *
     * @param argument_List  argument list
     * @param stmtLocation   filename+linenumber
     * @param scope
     * @param parentLocation
     */
    public void handleArgumentList(Elements argument_List, String stmtLocation, int scope, String parentLocation, boolean isInit) {
        int argumentLsit_size = argument_List.size();
        for (int i = 0; i < argumentLsit_size; i++) {
            Element argument = argument_List.get(i);
            parseVariableInExpression(argument, stmtLocation, scope, parentLocation, isInit);
        }
    }

    /**
     * * This function finds the use of variable declaration, and create edge between 'def-use'
     *
     * @param variable 'use' variable symbol is looking for 'def' of variable
     */

    public void findVarDependency(Symbol variable) {
        if (DEF_USE) {
            String var_name = variable.getName();
            String var_alias = variable.getAlias();
            if (!var_alias.equals("unknowntype") && !var_name.equals("unknowntype")
                    && !var_name.equals("ret_error") && !var_name.equals("ret_ok") && !var_name.equals("ret")) {
//            if (!var_alias.equals("unknowntype") && !var_name.equals("unknowntype")) {
                String var = "";
                int scope = variable.getScope();
                String fileName = variable.getLocation().split("-")[0];
                String depenNodeLabel = variable.getLocation();
//        storeIntoNodeList(depenNodeLabel);
                int edgeNum = 0;
                // Key- scope, value--label
                ArrayList<Symbol> candidates = new ArrayList<>();

                for (Symbol s : symbolTable) {
                    if (s != null) {
                        if (!s.tag.contains("function")) {
                            boolean match = true;
//                    if (variable.getTag().contains("decl") && !s.getTag().equals("macro")) {
//                        match = false;
//                    }
                            if (variable.getTag().equals("struct") && !s.getTag().equals("struct_decl")) {
                                match = false;
                            }
                            if (match) {
                                //check local variable
                                if (((((s.getName().equals(var_name)) || (s.getName().equals(var_alias))) && !s.getName().equals(""))
                                        || ((s.getAlias().equals(var_name)) || (s.getAlias().equals(var_alias))) && !s.getAlias().equals(""))
                                        && scope == s.getScope()) {

                                    if ((s.getName().equals(var_name)) || (s.getName().equals(var_alias))) {
                                        var = s.getName();
                                    } else {
                                        var = s.getAlias();
                                    }
                                    String declLabel = s.getLocation();
                                    if (!declLabel.equals(depenNodeLabel)) {
                                        String edgeLable = "<Def-Use> " + var;
                                        addEdgesToFile(depenNodeLabel, s, edgeLable);
                                        edgeNum++;
//                                if (candidates.size() > 0) {
//                                    candidates.clear();
//                                }
//                                if (!s.getTag().equals("macro")) {
//                                    break;
//                                } else {
//                                    continue;
//                                }
                                    }
                                }
                                // if the variable is not local, then check global variable def
                                if (((((s.getName().equals(var_name)) || (s.getName().equals(var_alias))) && !s.getName().equals(""))
                                        || ((s.getAlias().equals(var_name)) || (s.getAlias().equals(var_alias))) && !s.getAlias().equals(""))
                                        && scope > s.getScope()) {
                                    if ((s.getName().equals(var_name)) || (s.getName().equals(var_alias))) {
                                        var = s.getName();
                                    } else {
                                        var = s.getAlias();
                                    }
                                    String edgeLable = "<Def-Use> " + var;
                                    addEdgesToFile(depenNodeLabel, s, edgeLable);
//                            candidates.add(s);
                                    edgeNum++;

                                }
                            }
                        }
                    }
                }
                /**   this following part is for adding one decl for dependency variable. however, we need to link all the possible decl to dependency, so remove this for now.**/
//        Symbol decl_symbol = null;
//        int min_scope = 99;
//        for (Symbol candidate : candidates) {
//            if (candidate.scope < min_scope) {
//                if (candidate.getLocation().split("-")[0].equals(fileName)) {
//                    decl_symbol = candidate;
//                    min_scope = candidate.scope;
//                } else if (decl_symbol == null) {
//                    decl_symbol = candidate;
//                }
//            }
//        }
//        if (decl_symbol != null) {
//            String edgeLable = "<Def-Use> " + var;
//            addEdgesToFile(depenNodeLabel, decl_symbol, edgeLable);
//        }


                if (edgeNum == 0) {
                    lonelySymbolSet.add(variable);
                }
            }
        }
    }

    /**
     * This function add the edge between call->function and call->function_declaration
     *
     * @param depend function node / call node
     */

    public void addFuncDependency(Symbol depend) {
        if (DEF_USE) {
            String funcName = depend.getName();
            String depen_position = depend.getLocation();
            String edgeLable = "";
            if (sameNameMap.containsKey(depend.getName())) {
                HashSet<Symbol> candidates = sameNameMap.get(depend.getName());
                for (Symbol s : candidates) {
                    if (depend.getTag().equals("call")) {
                        if (s.getTag().equals("function_decl") || s.getTag().equals("function")) {
                            edgeLable = "<Call> " + funcName;
                            addEdgesToFile(depen_position, s, edgeLable);

                        }
                    } else if (depend.getTag().equals("function")) {
                        if (s.getTag().equals("function_decl")) {
                            edgeLable = "<func_decl> " + funcName;
                            addEdgesToFile(depen_position, s, edgeLable);
                        }
                    }
                }
            }
        }
    }

    /**
     * This function write edge into graph file
     *
     * @param depen_position dependent node position (lineNumber_filename)
     * @param decl           declaration symbol
     * @param edgeLabel      edge label
     */
    public void addEdgesToFile(String depen_position, Symbol decl, String edgeLabel) {
        String decl_position = decl.getLocation();
        addEdgesToFile(depen_position, decl_position, edgeLabel);
    }

    /**
     * This function write edge into graph file
     *
     * @param depen_position dependent node position (lineNumber_filename)
     * @param decl_position  declaration node postion
     * @param edgeLabel      edge label
     */
    public void addEdgesToFile(String depen_position, String decl_position, String edgeLabel) {
//# reserve multipul edges between 2 nodes
        boolean addNewEdge = true;
        HashSet<String[]> dependencyNodes = completeGraph.get(decl_position);
//     HashSet<String[]> dependencyNodes = dependencyGraph.get(decl_position);
        if (dependencyNodes != null && dependencyNodes.size() > 0) {
            for (String[] sn : dependencyNodes) {
                if (sn[0].equals(depen_position)) {
                    // not allow : multipul edges between 2 nodes
//                    addNewEdge = false;
//                    break;
                    if (sn[1].contains(edgeLabel)) {
                        //  allow : same edge with different label appears twice
                        addNewEdge = false;
                        break;
                    }
                }
            }
        }

        if (addNewEdge) {
            if (nodeList.get(decl_position) == null) {
                storeIntoNodeList(decl_position);
            }
            if (nodeList.get(depen_position) == null) {
                storeIntoNodeList(depen_position);
            }

            int dependId = nodeList.get(depen_position);
            int declId = nodeList.get(decl_position);

            //  --------------for compact graph-------------------------
            String dependNode_file = depen_position.split("-")[0];
            String declNode_file = decl_position.split("-")[0];

            if (dependId != declId) {
                processingText.writeTofile(depen_position + "," + decl_position + "," + edgeLabel + "\n", edgeListTxt);
                edgeList.add(depen_position + "->" + decl_position + "," + edgeLabel + "");

                //add to dependency graph
                HashSet<String[]> dependNodes = dependencyGraph.get(decl_position);
                HashSet<String[]> complete_dependNodes = completeGraph.get(decl_position);
                if (dependNodes == null) {
                    //TODO: FORK ADDED NODE
                    if (forkaddedNodeList.contains(depen_position) && forkaddedNodeList.contains(decl_position)) {
                        dependNodes = new HashSet<>();
                    }
                } else if (complete_dependNodes == null) {
                    complete_dependNodes = new HashSet<>();
                }

                String[] tmpEdge;
                //--------------------weight-----------------------------------------
                //         edge between fork added node , weight is bigger
//                if (forkaddedNodeList.contains(depen_position + " ") && forkaddedNodeList.contains(decl_position + " ")) {
//                    tmpEdge = new String[]{depen_position, edgeLabel, "3"};
//                }else{
//                    tmpEdge = new String[]{depen_position, edgeLabel, "1"};
//                }

                //Another solution of weights
                if (edgeLabel.contains("neighbor")) {
                    tmpEdge = new String[]{depen_position, edgeLabel, "1"};
                } else {
                    tmpEdge = new String[]{depen_position, edgeLabel, "5"};
                }

                if (forkaddedNodeList.contains(depen_position) && forkaddedNodeList.contains(decl_position)) {
                    dependNodes.add(tmpEdge);
                    dependencyGraph.put(decl_position, dependNodes);
                }
                complete_dependNodes.add(tmpEdge);
                completeGraph.put(decl_position, complete_dependNodes);
            }
        }
    }

    private void writeStringsToFile(HashMap<String, String> sourceCodeLocMap) {
        StringBuffer sb = new StringBuffer();

        sourceCodeLocMap.forEach((k, v) -> sb.append(k + ":" + v + "\n"));

        processingText.rewriteFile(sb.toString(), testCaseDir + "/StringList.txt");
    }


    /**
     * This function store a set of symbols to Symbol table and nameMap.
     *
     * @param symbols ArrayList of symbols
     */
    private void storeSymbols(ArrayList<Symbol> symbols) {
        //add to symbol table
        symbolTable.addAll(symbols);

        //add to nameMap
        for (Symbol s : symbols) {
            if (s != null) {
                HashSet<Symbol> sameNameSymbols;
                if (!sameNameMap.containsKey(s.getName())) {
                    sameNameSymbols = new HashSet<>();
                    sameNameSymbols.add(s);
                    sameNameMap.put(s.getName(), sameNameSymbols);
                } else {
                    sameNameSymbols = sameNameMap.get(s.getName());
                    if (!sameNameSymbols.contains(s)) {
                        sameNameSymbols.add(s);
                    }
                }
            }
        }
    }


    /**
     * This function change source code statements into tokens
     *
     * @param location
     * @param content
     */
    private void storeStrings(String location, String content) {
        boolean removeUnderscore = true;
        if (forkaddedNodeList.contains(location)) {
            String strList;
            if (idMap.get(location) != null) {
//              strList.add(content);
                int strID = idMap.get(location);
                strList = candidateStrings.get(strID);
                strList += " " + content;
                candidateStrings.set(strID, strList);
            } else {
                strList = " " + content;
                idMap.put(nodeList.get(location), stringID);
                stringID++;
            }


//            //replace all symbols that are not alphabet except underscore
//            String newContent;
//
//            if (!removeUnderscore) {
//                newContent = new StopWords().removeStopWord(strList.replaceAll("[^a-zA-Z|^_]", " ").replace("\\n", " ").replace("\n", "").replace("%i", "").replaceAll("\\s+", " ").toLowerCase());
//            } else {
//                newContent = new StopWords().removeStopWord(strList.replace("\\n", " ").replace("\n", "").replace("%i", "").replaceAll("[^a-zA-Z ]", " ").replaceAll("\\s+", " ").toLowerCase());
//            }
//            candidateStrings.add(newContent);
//            sourceCodeLocMap.put(location, newContent);

        }
    }


    /**
     * this function add edges cross files
     * ind dependency for symbol in
     * 1. lonelySymbolSet
     * 2. symbolTable(func_decl)
     */
    private void addEdgesCrossFiles() {
        //add call-> function/func_decl
        for (Symbol dependent : lonelySymbolSet) {
            String tag = dependent.getTag();
            if (tag.equals("call")) {
                if (!dependent.getName().equals("printf")) {
                    addFuncDependency(dependent);
                }
            } else if (tag.equals("name")) {
                findVarDependency(dependent);
            }
        }
        //add function->func_decl
        for (Symbol dependent : symbolTable) {
            if (dependent != null) {
                String tag = dependent.getTag();
                if (tag.equals("function")) {
                    addFuncDependency(dependent);
                }
            }
        }
    }

    /**
     * This function check whther current macro is just a header guard
     *
     * @param macroName
     * @param fileName
     * @return true if the macro is a header guard, otherwise, return false
     */
    private boolean isHeaderGuard(String macroName, String fileName) {
        String macroName_capital = "_" + macroName.toUpperCase() + "_H_";
        fileName = fileName.split("~")[fileName.split("~").length - 1];
        String fileName_capital = "_" + fileName.substring(0, fileName.lastIndexOf("H")).toUpperCase() + "_" + "H_";
        return macroName_capital.contains(fileName_capital);
    }


    /**
     * This function check whether the str is a word or not
     *
     * @param str
     * @return true, if it might be a word; otherwise false
     */
    private boolean could_be_a_word(String str) {
        if (str.trim().equals("")) {
            return false;
        }
        return Pattern.compile("[a-zA-Z_]*").matcher(str).matches();
    }

    private void createNeighborEdges() {
        String currentFile = "";
        int preLineNum = -1;
        int diff = 1;
        //todo: fork added node

/**   only generate consecutive edge for new code **/
        for (String s : forkaddedNodeList) {
            s = s.trim();
            if (!s.equals("")) {
                String[] nodelabel = s.trim().split("-");
                String fileName = nodelabel[0];
                int lineNum = Integer.valueOf(nodelabel[1]);
                if (fileName.equals(currentFile)) {
                    if (lineNum == preLineNum + diff) {
                        String preloc = fileName + "-" + preLineNum;
                        if (dependencyGraph.get(s) != null && dependencyGraph.get(preloc) != null) {
                            addEdgesToFile(s, preloc, "<neighbor>");
                            diff = 1;
                            preLineNum = lineNum;
                        } else if (dependencyGraph.get(preloc) == null && dependencyGraph.get(s) != null) {
                            preLineNum = lineNum;
                        } else if (dependencyGraph.get(preloc) != null && dependencyGraph.get(s) == null) {
                            diff++;
                        } else {
                            preLineNum = lineNum - 1;
                        }
                    } else {
                        preLineNum = lineNum;
                        diff = 1;
                    }
                } else {
                    diff = 1;
                    preLineNum = lineNum;
                }
                if (preLineNum == -1) {
                    preLineNum = lineNum;
                }
                currentFile = fileName;
            }
        }

    }

    public static void main(String[] args) {
        ProcessingText processingText = new ProcessingText();
        /** generating DOM tree by xmlParser (xom) **/
        Element root = processingText.getXmlDom("/Users/shuruiz/Downloads/1.xml").getRootElement();

        /** Rewrite the file name for the convenience of generating html files later **/
        String newFileName = processingText.changeFileName("1.c");
        String parentLocation = "";
        /** Generating dependency graph for  **/
        System.out.println("now parsing ----:" + newFileName);
        new DependencyGraph().generatingDependencyGraphForSubTree(root, newFileName, 1, parentLocation);

    }

}
