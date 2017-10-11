package DependencyGraph;

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

/**
 * Created by shuruiz on 12/10/15.
 */
public class DependencyGraph {
    public boolean HIERACHICAL = true;
    public boolean CONSECUTIVE = true;
    static final public boolean CONTROL_FLOW = true;
    static final public String CONTROLFLOW_LABEL = "<Control-Flow>";

    ProcessingText ProcessingText = new ProcessingText();
    //symbol Table stores all the declaration nodes.
    HashSet<Symbol> symbolTable = new HashSet<>();

    // lonelySymbolSet stores all the nodes that haven't find any node that could be point to.
    HashSet<Symbol> lonelySymbolSet = new HashSet<>();

    // this map stores nodes that have same name, used for search, in order to find dependencies effectively.
    HashMap<String, HashSet<Symbol>> sameNameMap = new HashMap<>();


    //---------------------- dependency Graph ---------------------------------
    ArrayList<String> forkaddedNodeList;
    // node id
    int id = 0;
    // key: node label
    HashMap<String, HashSet<String[]>> dependencyGraph = new HashMap<>();
    HashMap<String, HashSet<String[]>> completeGraph = new HashMap<>();
    //node list stores  the id for the node, used for create graph file. HashMap<String, Integer>
    HashMap<String, Integer> nodeList = new HashMap<>();
    //edge list stores all the edges, used for testing
    HashSet<String> edgeList = new HashSet<>();


    /*----------------------compact Graph ------------------------------
    *    if the file is not changed in 6.1 vs 6.0, then the whole file is a node
    */
    static HashSet<String> changedFiles;
    static int compact_graph_node_id = 0;
    HashMap<String, HashSet<String[]>> compactGraph = new HashMap<>();
    //node list stores  the id for the node, used for create graph file. HashMap<String, Integer>
    HashMap<String, Integer> compact_nodeList = new HashMap<>();
    //edge list stores all the edges, used for testing
    HashSet<String> compact_edgeList = new HashSet<>();


    //used for similarity calculation, LD algorithm
    ArrayList<String> candidateStrings = new ArrayList<>();
    //map nodeID in graph -> stringID in candidateString list
    HashMap<Integer, Integer> idMap = new HashMap<>();

    boolean foundHeaderGuard = false;

    static final String FS = File.separator;
    // used for srcml
    static final public String NAMESPACEURI = "http://www.sdml.info/srcML/src";
    static final public String NAMESPACEURI_CPP = "http://www.sdml.info/srcML/cpp";

    String analysisDir = "";
    String sourcecodeDir = "";
    static String WIN_Root_Dir = "C:\\Users\\shuruiz\\Documents\\";
    static String MAC_Root_Dir = " /Users/shuruiz/Work/";
    static String tmpXmlPath = "tmpXMLFile" + FS;
    static String analysisDirName = "DPGraph";
    String edgeListTxt, parsedLineTxt, forkAddedNodeTxt, compact_graph_edgeList_txt;

    /**
     * This function just call the create Dependency Graph, used for cluster nodes.
     * TODO modify compareTwoGraphs return type
     *
     * @return dependency graph, no edge label stored.
     */
    public HashSet<String> getDependencyGraphForProject(String sourcecodeDir) {
        this.analysisDir = sourcecodeDir + analysisDirName + FS;
        this.sourcecodeDir = sourcecodeDir;

        forkAddedNodeTxt = analysisDir + "forkAddedNode.txt";
        edgeListTxt = analysisDir + "edgeList.txt";
        compact_graph_edgeList_txt = analysisDir + "compact_edgeList.txt";
        parsedLineTxt = analysisDir + "parsedLines.txt";
        changedFiles = new HashSet<>();

        ProcessingText.rewriteFile("", edgeListTxt);
        ProcessingText.rewriteFile("", compact_graph_edgeList_txt);
        ProcessingText.rewriteFile("", parsedLineTxt);

        if (new File(forkAddedNodeTxt).exists()) {
            try {
                forkaddedNodeList = getForkAddedNodeList();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("file forkAddedNode.txt does not exist!");
            forkaddedNodeList = new ArrayList<>();
        }
        //parse every source code file in the project
        try {
            Files.walk(Paths.get(sourcecodeDir)).forEach(filePath -> {
                if (Files.isRegularFile(filePath) && isCFile(filePath.toString())) {
                    parseSingleFile(filePath);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        //create edges cross files
        addEdgesCrossFiles();

        //generate edge about consecutive lines
        if (CONSECUTIVE) {
            createNeighborEdges();
        }
        ProcessingText.writeToPajekFile(dependencyGraph, nodeList, analysisDir + "changedCode.pajek.net");
        ProcessingText.writeToPajekFile(completeGraph, nodeList, analysisDir + "complete.pajek.net");
        ProcessingText.writeToPajekFile(compactGraph, compact_nodeList, analysisDir + "compact.pajek.net");
//        writeStringsToFile(candidateStrings);
        return edgeList;
    }

    private ArrayList getForkAddedNodeList() throws IOException {
        String[] lines = ProcessingText.readResult(forkAddedNodeTxt).split("\n");
        ArrayList<String> forkAddedNodeList = new ArrayList<>();
        for (String s : lines) {
            String node = s.split(" ")[0];
            forkAddedNodeList.add(node);
            String filename = node.split("-")[0];
            if (!changedFiles.contains(filename)) {
                changedFiles.add(filename);
            }
        }

        return forkAddedNodeList;
    }

    private boolean isCFile(String filePath) {
        return filePath.endsWith(".cpp") || filePath.endsWith(".h") || filePath.endsWith(".c") || filePath.endsWith(".pde");
    }

    /**
     * This function parse each  source code files
     *
     * @param filePath
     */
    public void parseSingleFile(Path filePath) {
        String fileName = diffString(filePath.toString(), sourcecodeDir);
        foundHeaderGuard = false;

        String tmpFilePath = WIN_Root_Dir + tmpXmlPath + filePath.toString().replace(WIN_Root_Dir, "");
        if (fileName.endsWith(".h") || fileName.endsWith(".pde")) {  // src2srcml cannot parse  ' *.h' file correctly, so change the suffix '+.cpp'
            tmpFilePath += ".cpp";
        }
        try {
            FileUtils.copyFile(filePath.toFile(), new File(tmpFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //preprocess file in case the misinterpretation of srcml
        ProcessingText.preprocessFile(tmpFilePath);

        // get xml file using src2srcml
        String xmlFilePath = ProcessingText.getXmlFile(tmpFilePath);

        //parse dependency graph in each file
        Element root = ProcessingText.getXmlDom(xmlFilePath).getRootElement();


        //Rewrite the file name for html purpose
        String newFileName = ProcessingText.changeFileName(fileName);

		/*  for Apache
		 *  if (!newFileName.equals("server/util_expr_parseC")) {
 		 */

      	/* for Marlin
		 * if (!fileName.contains("pcre_globals")) {*/
        String parentLocation = "";
        parseDependencyForSubTree(root, newFileName, 1, parentLocation);

    }

    public static String diffString(String str1, String str2) {
        int index = str1.lastIndexOf(str2);
        if (index > -1) {
            return str1.substring(str2.length());
        }
        return str1;
    }

    public ArrayList<String> parseDependencyForSubTree(Element subTreeRoot, String fileName, int scope, String parentLocation) {
        return parseDependencyForSubTree(subTreeRoot, fileName, scope, parentLocation, false);
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
    public ArrayList<String> parseDependencyForSubTree(Element subTreeRoot, String fileName, int scope, String parentLocation, boolean isinit) {
        ArrayList<String> tmpStmtList = new ArrayList<>();
        Elements elements = subTreeRoot.getChildElements();
        for (int i = 0; i < elements.size(); i++) {
            String line = "";
            String currentLocation = "";
            if (isinit) {
                line = getLineNumOfElement(subTreeRoot);
                currentLocation = getLocationOfElement(subTreeRoot, fileName);
            }
            Element ele = elements.get(i);

            if (ele.getLocalName().equals("define")) {
                parseDefine(fileName, scope, ele);
            } else if (ele.getLocalName().equals("function") || ele.getLocalName().equals("constructor") || ele.getLocalName().equals("function_decl")) {
                parseFunctionNode(ele, fileName, scope);
            } else if (ele.getLocalName().equals("if") && !ele.getNamespacePrefix().equals("cpp")) {
                tmpStmtList.add(parseIfStmt(ele, fileName, scope, parentLocation));
            } else if (ele.getLocalName().equals("expr_stmt")) {
                tmpStmtList.add(parseVariableInExpression(ele, getLocationOfElement(ele, fileName), scope, parentLocation, false));
            } else if (ele.getLocalName().equals("decl_stmt")) {
                tmpStmtList.add(parseDeclStmt(fileName, scope, parentLocation, ele));
            } else if (ele.getLocalName().equals("label") && (((Element) ele.getParent().getParent()).getLocalName().equals("block"))) {
                // this is specifically for struct definition include bitfiles for certain fields
                //e.g.   unsigned short icon : 8;
                // srcml  interprets it in a wrong way, so I hard code this case to parse the symbol
                // <macro><label><expr_stmt>  represent a field
                Symbol declSymbol = addDeclarationSymbol(ele, "decl_stmt", fileName, scope, parentLocation, "");
                tmpStmtList.add(declSymbol.getLocation());
            } else if (ele.getLocalName().equals("typedef")) {
                parseTypedef(ele, fileName, scope, parentLocation);
            } else if (ele.getLocalName().equals("return")) {
                Element returnContent = ele.getFirstChildElement("expr", NAMESPACEURI);
                if (returnContent != null) {
                    tmpStmtList.add(parseVariableInExpression(ele, "", scope, parentLocation, false));
                }
            } else if (ele.getLocalName().equals("for")) {
                tmpStmtList.add(parseForStmt(ele, fileName, scope, parentLocation));
            } else if (ele.getLocalName().equals("while") || ele.getLocalName().equals("switch")) {
                tmpStmtList.add(parseWhileStmt(ele, fileName, scope, parentLocation));
            } else if (ele.getLocalName().equals("case") || ele.getLocalName().equals("default")) {
                tmpStmtList.add(parseCase_Default_Stmt(ele, fileName, scope + 1));
            } else if (ele.getLocalName().equals("enum")) {
                parseEnum(ele, fileName, scope, "");
            } else if (ele.getLocalName().equals("macro")) {
                parseMacros(ele, fileName, scope);
            } else if (ele.getLocalName().equals("block")) {
                tmpStmtList.addAll(parseDependencyForSubTree(ele, fileName, scope + 1, parentLocation, isinit));
            } else if (ele.getLocalName().equals("extern")) {
                tmpStmtList.addAll(parseDependencyForSubTree(ele, fileName, scope, parentLocation, isinit));
                //todo : hierachy?
            } else if (ele.getLocalName().equals("do")) {
                tmpStmtList.add(parseDoWhile(ele, fileName, scope));
            } else if (ele.getLocalName().equals("using")) {
                Element nameEle = ele.getFirstChildElement("name", NAMESPACEURI);
                String location = getLocationOfElement(nameEle, fileName);
                tmpStmtList.add(fileName + "-" + line);
                Symbol symbol = new Symbol(nameEle.getValue(), "", location, "using", scope);
                findVarDependency(symbol);
            } else if (ele.getLocalName().equals("expr")) {
                parseVariableInExpression(ele, currentLocation, scope, parentLocation, isinit);
//                parseVariableInExpression(subTreeRoot, line, fileName, scope, parentLocation, isinit);
            } else if (ele.getLocalName().equals("break") || ele.getLocalName().equals("continue")) {
                String breakLoc = fileName + "-" + getLineNumOfElement(ele);
                storeIntoNodeList(breakLoc);
                tmpStmtList.add(breakLoc);
            } else if (ele.getLocalName().equals("struct_decl")) {
                addDeclarationSymbol(ele, "struct_decl", fileName, scope, parentLocation, "");
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
        }
        return tmpStmtList;
    }


    private String parseDeclStmt(String fileName, int scope, String parentLocation, Element ele) {
        Element decl = ele.getFirstChildElement("decl", NAMESPACEURI);
        Symbol declSymbol = addDeclarationSymbol(decl, "decl_stmt", fileName, scope, parentLocation, "");
        if (declSymbol != null) {
            return declSymbol.getLocation();
        }
        System.out.println("error: decl_stmt is null!");
        return "";
    }

    private void parseDefine(String fileName, int scope, Element ele) {
        Element macroEle = ele.getFirstChildElement("macro", NAMESPACEURI_CPP);
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

        if (!foundHeaderGuard && fileName.endsWith("H") && isHeaderGuard(macroName, fileName)) {
            isHeaderGuard = true;
            foundHeaderGuard = true;
        }
        String location = "";
        if (!isHeaderGuard) {
            location = getLocationOfElement(nameEle, fileName);
            Symbol macro = new Symbol(macroName, "", location, tag, scope);
            storeIntoNodeList(location);
            ArrayList<Symbol> macros = new ArrayList<Symbol>();
            macros.add(macro);
            storeSymbols(macros);
            ProcessingText.writeTofile(location + "\n", parsedLineTxt);


            // macro value <cpp:value>
            if (!tag.contains("function_decl")) {
                Element value_ele = ele.getFirstChildElement("value", NAMESPACEURI_CPP);
                if (value_ele != null) {
                    String value = value_ele.getValue();
                    String[] element_of_value = value.split(" ");
                    for (String str : element_of_value) {
                        str = str.replaceAll("[(){},.;!?<>%]", "");
                        if (could_be_a_word(str)) {
                            Symbol depend_macro = new Symbol(str, "", location, "cpp:value", scope);
                            findVarDependency(depend_macro);
                        }
                    }
                }
            }
        }
    }

    private boolean could_be_a_word(String str) {
        if (str.trim().equals("")) {
            return false;
        }

        return Pattern.compile("[a-zA-Z_]*").matcher(str).matches();
    }

    private boolean isHeaderGuard(String macroName, String fileName) {
        String macroName_capital = "_" + macroName.toUpperCase() + "_H_";
        fileName = fileName.split("~")[fileName.split("~").length - 1];
        String fileName_capital = "_" + fileName.substring(0, fileName.lastIndexOf("H")).toUpperCase() + "_" + "H_";
        return macroName_capital.contains(fileName_capital);
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

        stmtList.addAll(parseDependencyForSubTree(ele, fileName, scope + 1, parentLocation));

        //while <condition>
        Element conditionEle = ele.getFirstChildElement("condition", NAMESPACEURI);
        if (conditionEle != null) {
//            String line = getLineNumOfElement(conditionEle);
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
        case_tmpStmtList.addAll(parseDependencyForSubTree(ele, fileName, scope + 1, caseLocation));
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
            String alias = name_ele.getValue();
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

//            addDeclarationSymbol(funcDecl_ele, "function_decl", fileName, scope, parentLocation, "");
        }
    }


    private void parseMacros(Element ele, String fileName, int scope) {
        Element nameEle = ele.getFirstChildElement("name", NAMESPACEURI);
        Element argumentListEle = ele.getFirstChildElement("argument_list", NAMESPACEURI);
        String tag = "macro";
        if (argumentListEle != null) {
            tag = "call";
        }

        String macroName = nameEle.getValue();
        String location = getLocationOfElement(nameEle, fileName);
        Symbol macro = new Symbol(macroName, "", location, tag, scope);
        storeIntoNodeList(location);

        symbolTable.add(macro);
        lonelySymbolSet.add(macro);
        ProcessingText.writeTofile(location + "\n", parsedLineTxt);

        if (argumentListEle != null) {
            Elements arguments = argumentListEle.getChildElements();
            for (int x = 0; x < arguments.size(); x++) {

                Element argument = arguments.get(x);
                String argumentLocation = getLocationOfElement(argument, fileName);

                //save into nodeList
                storeIntoNodeList(argumentLocation);

                String var = argument.getValue();
                Symbol dependent = new Symbol(var, "", argumentLocation, "name", scope);
                findVarDependency(dependent);

            }
        }
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

    private boolean isValideHierachy(Element element) {
        return element.getLocalName().equals("function")
                || element.getLocalName().equals("struct")
                || element.getLocalName().equals("class")
                || element.getLocalName().equals("namespace")
                || element.getLocalName().equals("enum");
    }


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
            if (block.getChildElements().size() > 0) {
                if (block.getChildElements().get(0).getLocalName().equals("public")
                        || block.getChildElements().get(0).getLocalName().equals("private")
                        || block.getChildElements().get(0).getLocalName().equals("protected")) {

                    //for common struct definition
                    for (int s = 0; s < block.getChildElements().size(); s++) {
                        Element group = block.getChildElements().get(s);

                        //get children
                        ArrayList<String> children = parseDependencyForSubTree(group, fileName, scope, parentLocation);
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
                    ArrayList<String> children = parseDependencyForSubTree(block, fileName, scope, parentLocation);
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
     * Second, use {@link #parseDependencyForSubTree(Element, String, int, String)} function to parse the <block> subtree
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
            String lineNumber = getLineNumOfElement(parameter_list);
//            storeStrings(fileName + "-" + lineNumber, functionSymbol.getType() + " " + functionSymbol.getName() + " " + parameter_list.getValue());
        }

        //check block
        Element block = element.getFirstChildElement("block", NAMESPACEURI);
        if (block != null) {
            ArrayList<String> stmtInBlock = parseDependencyForSubTree(block, fileName, scope + 1, parentLocation);
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
                symbolsInThen = parseDependencyForSubTree(block, fileName, scope, ifStmtLocation);
            } else {
                symbolsInThen = parseDependencyForSubTree(then_Node, fileName, scope, ifStmtLocation);
            }

            //else is optional
            ArrayList<String> symbolsInElse = null;
            Element else_Node = ele.getFirstChildElement("else", NAMESPACEURI);
            if (else_Node != null) {
                if (else_Node.getFirstChildElement("block", NAMESPACEURI) != null) {
                    Element block = else_Node.getFirstChildElement("block", NAMESPACEURI);
                    symbolsInElse = parseDependencyForSubTree(block, fileName, scope, ifStmtLocation);
                } else {
                    symbolsInElse = parseDependencyForSubTree(else_Node, fileName, scope, ifStmtLocation);
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

        if (whileLocation.equals("")) {
            whileLocation = fileName + "-" + getLineNumOfElement(condition);
            //save into nodeList
            storeIntoNodeList(whileLocation);
        }
        //Block
        Element block = ele.getFirstChildElement("block", NAMESPACEURI);
        if (block != null) {
            stmtList.addAll(parseDependencyForSubTree(block, fileName, scope + 1, whileLocation));
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
        boolean allBelongToNewCode = true;
        if(forkaddedNodeList.size()>0) {
            boolean stmtIsNew = true;
            for (String stmtLoc : stmtList) {
                stmtIsNew = stmtIsNew && forkaddedNodeList.contains(stmtLoc);
                if (stmtIsNew == false) {
                    allBelongToNewCode = false;
                    break;
                }
            }
        }
        if(!allBelongToNewCode){
            linkChildToParent(stmtList,headLocation,"<Control-Flow>");
        }else {
            addEdgesToFile(stmtList.get(0), headLocation, CONTROLFLOW_LABEL + " " + label);
            for (int i = 0; i < stmtList.size() - 1; i++) {
                String pre_loc = stmtList.get(i);
                String after_loc = stmtList.get(i + 1);
                if (!pre_loc.equals(after_loc)) {
                    addEdgesToFile(after_loc, pre_loc, CONTROLFLOW_LABEL + " " + label);
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
        Element cond_exprNode = condition.getFirstChildElement("expr", NAMESPACEURI);

        if (cond_exprNode != null) {
            parseVariableInExpression(condition, getLocationOfElement(cond_exprNode, fileName), scope, parentLocation, false);
        }
        storeIntoNodeList(forLocation);
        Element block = ele.getFirstChildElement("block", NAMESPACEURI);
        if (block != null && block.getChildElements().size() > 0) {
            tmpStmtList.addAll(parseDependencyForSubTree(block, fileName, scope + 1, forLocation));
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
            Element name_ele =  type_Node.getFirstChildElement("name", NAMESPACEURI);
            if(name_ele!=null) {
                type =name_ele.getValue();
            }else {
                type=type_Node.getValue();
            }
//            type = type_Node.getValue();
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
//            line = getLineNumOfElement(nameElement);
                location = getLocationOfElement(nameElement, fileName);
            } else {
//            line = getLineNumOfElement(element);
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
//                storeStrings(fileName + "-" + lineNumber, type + " " + name);
            }
        } else if (!element.getLocalName().equals("function")
                && !element.getLocalName().equals("function_decl")
                && !element.getLocalName().equals("constructor")) {
//            storeStrings(fileName + "-" + lineNumber, type + " " + name);
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
//            if (type.equals("stm_status_t")) {
//                System.out.print("");
//            }
            Symbol type_symbol = new Symbol(type, "type", location, "", scope);
            findVarDependency(type_symbol);
        }


        ProcessingText.writeTofile(getLocationOfElement(element, fileName) + "\n", parsedLineTxt);
        return symbol;
    }


    private boolean is_user_defined_type(String type) {
        if (type.contains("char") || type.contains("int") || type.contains("long") || type.contains("float") || type.contains("double")
                || type.contains("void") || type.contains("boolean") || type.contains("struct") || type.equals("")) {
            return false;
        }
        return true;
    }


    int stringID = 0;

//    private void storeStrings(String location, String content) {
////TODO: FORK ADDED NODE
//        if (forkaddedNodeList.contains(location + "\r")) {
////        if (true) {
//            String strList ;
//            if (idMap.get(location) != null) {
//                int strID = idMap.get(location);
//                strList = candidateStrings.get(strID);
//                strList += " " + content;
//                candidateStrings.set(strID, strList);
//            } else {
//                strList = " " + content;
//                idMap.put(nodeList.get(location), stringID);
//                stringID++;
//            }
//            candidateStrings.add(strList.replace("\\n", " ").replace("\n", "").replace("%i", "").replaceAll("[^a-zA-Z ]", " ").replaceAll("\\s+", " "));
//        }
//    }

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
            if (name_Elements.size() > 0) {
                for (int i = 0; i < name_Elements.size(); i++) {
                    Elements nameList = name_Elements.get(i).getChildElements("name", NAMESPACEURI);
                    String var;
                    Symbol dependent;
                    if (nameList.size() > 0) {
                        if (stmtLocation.equals("")) {
                            stmtLocation = getLocationOfElement(nameList.get(0), fileName);
                        }
                        exprLocation = stmtLocation;
                        //save into nodeList
                        if (!isInit) {
                            storeIntoNodeList(exprLocation);
                        }
                        for (int x = 0; x < nameList.size(); x++) {
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
            for (int i = 0; i < callElementList.size(); i++) {
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
            if (sizeofElements.size() > 0)
                for (int i = 0; i < sizeofElements.size(); i++) {
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
                ArrayList<String> tmpStmtList = parseDependencyForSubTree(exprBlockEle, fileName, scope + 1, parentLocation, isInit);
                linkChildToParent(tmpStmtList, parentLocation, "<init>");
            }

            //macro node  (hard code wrong output from src2srcml)
            Elements macroEleList = exprNode.getChildElements("macro", NAMESPACEURI);
            for (int j = 0; j < macroEleList.size(); j++) {
                Element macroEle = macroEleList.get(j);
                parseMacros(macroEle, fileName, scope);
            }

            linkChildToParent(exprLocation, parentLocation, element);
            ProcessingText.writeTofile(exprLocation + "\n", parsedLineTxt);
        }
        return exprLocation;
    }

    /**
     * This function create edges between consecutive lines,
     * ignore comments
     */
    private void createNeighborEdges() {
        String currentFile = "";
        int preLineNum = -1;
        int diff = 1;
        //todo: fork added node


        for (String s : forkaddedNodeList) {
            if (s.contains("bpfH")) {
                System.out.print("");
            }
            s = s.trim();
            if (!s.equals("")) {
                String[] nodelabel = s.trim().split("-");
                String fileName = nodelabel[0];
                int lineNum = Integer.valueOf(nodelabel[1]);
                if (fileName.equals(currentFile)) {
                    if (lineNum == preLineNum - diff) {
                        String preloc = fileName + "-" + preLineNum;
//                        if (completeGraph.get(s) != null && completeGraph.get(preloc) != null) {
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

    private boolean is_not_a_C_dataType(String var) {
        return !var.equals("void") && !var.equals("float") && !var.equals("uint32") && !var.equals("NULL");
    }

    private String getLineNumOfElement(Element element) {
        String lineNum = "-1";
        if (element.getAttributeCount() > 0) {
            lineNum = element.getAttribute("line", "http://www.sdml.info/srcML/position").getValue();
        } else if (element.getChildElements().size() > 0) {
            for (int i = 0; i < element.getChildElements().size(); i++) {
                lineNum = getLineNumOfElement(element.getChildElements().get(i));
                if (!lineNum.equals("")) {
                    break;
                }
            }
        } else {
            lineNum = getLineNumOfElement((Element) element.getParent());
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

        // -----------for dependency graph
        if (!nodeList.containsKey(exprLocation)) {
            id++;
            nodeList.put(exprLocation, id);
            //TODO: Fork added node
            if (forkaddedNodeList.contains(exprLocation)) {
                dependencyGraph.put(exprLocation, new HashSet<>());
            }
            completeGraph.put(exprLocation, new HashSet<>());

        }
        System.out.println(exprLocation);
        // --------for compact graph--------------
        String filename = exprLocation.split("-")[0];
        if (!changedFiles.contains(filename)) {
            exprLocation = filename + "-all";
        }
        if (!compact_nodeList.containsKey(exprLocation)) {
            compact_graph_node_id++;
            compact_nodeList.put(exprLocation, compact_graph_node_id);
            compactGraph.put(exprLocation, new HashSet<>());
        }

        System.out.println(exprLocation);

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
        Elements argumentList = argumentListEle.getChildElements("argument", NAMESPACEURI);
        if (location.equals("")) {
            location = getLocationOfElement(argumentListEle, fileName);
        }
        handleArgumentList(argumentList, location, scope, parentLocation, isInit);
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
        for (int i = 0; i < argument_List.size(); i++) {
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
        String var_name = variable.getName();
        String var_alias = variable.getAlias();
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
                    if (variable.getTag().contains("decl") && !s.getTag().equals("macro")) {
                        match = false;
                    }
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
                                if (candidates.size() > 0) {
                                    candidates.clear();
                                }
                                break;
                            }
                        }
                        // if the variable is not local, then check global variable def
                        if (((((s.getName().equals(var_name)) || (s.getName().equals(var_alias))) && !s.getName().equals(""))
                                || ((s.getAlias().equals(var_name)) || (s.getAlias().equals(var_alias))) && !s.getAlias().equals(""))
                                && scope > s.getScope()) {
//                        String edgeLable = "<Def-Use> " + var;
                            candidates.add(s);
//                     addEdgesToFile(depenNodeLabel, s, edgeLable);
                            edgeNum++;
                            if ((s.getName().equals(var_name)) || (s.getName().equals(var_alias))) {
                                var = s.getName();
                            } else {
                                var = s.getAlias();
                            }
                        }
                    }
                }
            }
        }

        Symbol decl_symbol = null;
        int min_scope = 99;
        for (Symbol candidate : candidates) {
            if (candidate.scope < min_scope) {
                if (candidate.getLocation().split("-")[0].equals(fileName)) {
                    decl_symbol = candidate;
                    min_scope = candidate.scope;
                } else if (decl_symbol == null) {
                    decl_symbol = candidate;
                }
            }
        }
        if (decl_symbol != null) {
            String edgeLable = "<Def-Use> " + var;
            addEdgesToFile(depenNodeLabel, decl_symbol, edgeLable);
        }
        if (edgeNum == 0) {
            lonelySymbolSet.add(variable);
        }
    }

    /**
     * This function add the edge between call->function and call->function_declaration
     *
     * @param depend function node / call node
     */

    public void addFuncDependency(Symbol depend) {
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
            String compact_depend_label, compact_decl_label;
            HashSet<String[]> compact_dependNodes = new HashSet<>();
            if (!changedFiles.contains(dependNode_file)) {
                compact_depend_label = dependNode_file + "-all";
            } else {
                compact_depend_label = depen_position;
            }
            if (!changedFiles.contains(declNode_file)) {
                compact_decl_label = declNode_file + "-all";
            } else {
                compact_decl_label = decl_position;
            }
            if (!compact_decl_label.equals(compact_depend_label)) {
                String edge = compact_depend_label + "," + compact_decl_label + "," + edgeLabel;
                ProcessingText.writeTofile(edge + "\n", compact_graph_edgeList_txt);
                compact_edgeList.add(edge);
                compact_dependNodes = compactGraph.get(compact_decl_label);
                if (compact_dependNodes == null) {
                    compact_dependNodes = new HashSet<>();
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
                    tmpEdge = new String[]{compact_depend_label, edgeLabel, "1"};
                } else {
                    tmpEdge = new String[]{compact_depend_label, edgeLabel, "5"};
                }

                //------------compact graph---------------

                if (!compact_decl_label.equals(compact_depend_label)) {
                    System.out.println(compact_decl_label + "," + tmpEdge[0] + "," + tmpEdge[1] + "," + tmpEdge[2]);
                    compact_dependNodes.add(tmpEdge);
                    compactGraph.put(compact_decl_label, compact_dependNodes);
                }

            }

            if (dependId != declId) {
                ProcessingText.writeTofile(depen_position + "," + decl_position + "," + edgeLabel + "\n", edgeListTxt);
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

//TODO:  FORK ADDED NODE
                if (forkaddedNodeList.contains(depen_position) && forkaddedNodeList.contains(decl_position)) {
                    dependNodes.add(tmpEdge);
                    dependencyGraph.put(decl_position, dependNodes);
                }
                complete_dependNodes.add(tmpEdge);
                completeGraph.put(decl_position, complete_dependNodes);
            }
        }
    }

    private void writeStringsToFile(ArrayList<String> candidateStrings) {
        StringBuffer sb = new StringBuffer();
        for (String str : candidateStrings) {
            sb.append("\"" + str + "\",");
        }
        ProcessingText.rewriteFile(sb.toString(), analysisDir + "/StringList.txt");
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
}
