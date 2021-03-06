package Util;

/**
 * Created by shuruiz on 6/2/2016.
 */

import DependencyGraph.Symbol;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessingText {
    static String current_OS = System.getProperty("os.name").toLowerCase();

    public void writeTofile(String content, String filepath) {


        try {
            File file = new File(filepath);
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.getParentFile().mkdir();
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rewriteFile(String content, String filepath) {

        try {
            File file = new File(filepath);
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), false);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void writeToPajekFile(HashMap<String, HashSet<String[]>> dependencyGraph, HashMap<String, Integer> nodeList, String testCaseDir, String testDir, String filename, ArrayList<String> forkaddedNodeList) {
        final String FS = File.separator;
        String filepath = testCaseDir + filename;
//        String filepath = testCaseDir+testDir+FS+filename;
        System.out.println("Write to file: " + filepath);
//        String pajek = "/graph.pajek.net";
        rewriteFile("*Vertices " + nodeList.size() + "\n", filepath);
        // Getting a Set of Key-value pairs
        Set nodeSet = nodeList.entrySet();
        // Obtaining an iterator for the entry set
        Iterator it_node = nodeSet.iterator();
        int isolatedNode = 0;
        while (it_node.hasNext()) {
            Map.Entry node = (Map.Entry) it_node.next();

            String nodeId = (String) node.getKey();
            if ((filename.contains("change") && forkaddedNodeList.contains(nodeId)) || !filename.contains("change")) {
                writeTofile(nodeList.get(nodeId) + " \"" + nodeId + "\"\n", filepath);
            } else {
                isolatedNode++;
            }


        }
        if (filename.contains("change")) {
            rewriteFile("isolated nodes: " + isolatedNode, testCaseDir + "isolatedNode.txt");
        }
        // Getting a Set of Key-value pairs
        Set entrySet = dependencyGraph.entrySet();

        writeTofile("*arcs \n", filepath);

        // Obtaining an iterator for the entry set
        Iterator it_edge = entrySet.iterator();

        // Iterate through HashMap entries(Key-Value pairs)

        while (it_edge.hasNext()) {
            Map.Entry node = (Map.Entry) it_edge.next();

            String currentNode = (String) node.getKey();
            String to = nodeList.get(currentNode).toString();
            HashSet<String[]> dependencyNodes = (HashSet<String[]>) node.getValue();
            for (String[] dn : dependencyNodes) {

                writeTofile(nodeList.get(dn[0]) + " " + to + " " + dn[2] + "\n", filepath);

            }

        }
    }


    public String clearBlank(String s) {
        return s.replace("\n", "").replace(" ", "").replace("\t", "");
    }


    public HashMap<String, String> getNodeIdMap(String analysisDir, ProcessingText processingText) throws IOException {
        System.out.println("getting node-id map...");
        HashMap<String, String> nodeIdMap = new HashMap<>();
        /**   get node id -  node location (label)**/
        String nodeIdList = processingText.readResult(analysisDir + "NodeList.txt");
        String[] nodeIdArray = nodeIdList.split("\n");
        for (String s : nodeIdArray) {
            if (s.split("---------").length > 1) {
                nodeIdMap.put(s.split("---------")[0], s.split("---------")[1]);
            }
        }
        return nodeIdMap;
    }

    /**
     * This function will collect all the fork added node from  "forkAddedNode.txt", and generate a list of forkAddedNode
     *
     * @return Arraylist of fork Added Nodes
     * @throws IOException
     */
    public ArrayList<String> getForkAddedNodeList(String forkAddedNodeTxt) throws IOException {
        String[] lines = readResult(forkAddedNodeTxt).split("\n");
        ArrayList<String> forkAddedNodeList = new ArrayList<>();
        for (String s : lines) {
            String node = s.split(" ")[0];

            if (node.equals("")) {
                System.out.println();
            }
            if (!forkAddedNodeList.contains(node) && !node.equals("")) {
                forkAddedNodeList.add(node);
            }
            String filename = node.split("-")[0];


        }
        return forkAddedNodeList;
    }

    /**
     * this function read the content of the file from filePath, and ready for comparing
     *
     * @param filePath file path
     * @return content of the file
     * @throws IOException e
     */
    public String readResult(String filePath) throws IOException {
        BufferedReader result_br = new BufferedReader(new FileReader(filePath));
        String result = "";
        try {
            StringBuilder sb = new StringBuilder();
            String line = result_br.readLine();

            while (line != null) {

                sb.append(line);
                sb.append(System.lineSeparator());

                line = result_br.readLine();
            }
            result = sb.toString();
        } finally {
            result_br.close();
        }
        return result;
    }

    /**
     * create dir for store xml files
     *
     * @param inputFile file that need to be parsed by srcML
     * @return path of XML file
     * @throws IOException e
     */
    public static String getXmlFile(String inputFile) {
        String outXmlFile = inputFile + ".xml";
        //run srcML
        if (new File(inputFile).isFile()) {
            try {
                ProcessBuilder processBuilder = null;
//                if (current_OS.indexOf("mac") >= 0) {
                processBuilder = new ProcessBuilder("src2srcml", "--xmlns:PREFIX=http://www.sdml.info/srcML/position", inputFile, "-o", outXmlFile);
//                } else if (current_OS.indexOf("windows") >= 0) {
//                    processBuilder = new ProcessBuilder("C:\\Users\\shuruiz\\Documents\\srcML-Win\\src2srcml.exe", "--position",
//                            inputFile, "-o", outXmlFile);
//                }

                Process process = processBuilder.start();
                process.waitFor();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("File does not exist: " + inputFile);
        }
        return outXmlFile;
    }

    /**
     * parse xml file to DOM.
     *
     * @param xmlFilePath path of xml file
     */
    public static Document getXmlDom(String xmlFilePath) {
        ProcessingText io = new ProcessingText();
        Document doc = null;
        try {
            Builder builder = new Builder();
            File file = new File(xmlFilePath);

            sleep();
            String xml = io.readResult(xmlFilePath);
            if (xml.length() > 0) {
                doc = builder.build(file);

            }
        } catch (ParsingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }


    public static void sleep() {
        try {
            Thread.sleep(500);                 //1000 milliseconds is one second.
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * This method replace source code that cannot be correctly parsed by srcML,
     *
     * @param inputFile
     */
    public static void removeSrcmlCannotHandleText(String inputFile) {
        boolean removeCppParathesis = false;
        ProcessingText io = new ProcessingText();
        StringBuffer sb = new StringBuffer();
        if (!new File(inputFile).exists()) {
            Path pathToFile = Paths.get(inputFile);
            try {
                Files.createDirectories(pathToFile.getParent());
                Files.createFile(pathToFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(inputFile));
            String line;
            while ((line = br.readLine()) != null) {
                // replace PACK(void *)   to void for srcML
                line = line.replaceAll("PACK\\(void(\\s)?\\*\\)", "void");
                line = line.replaceAll("\\(void(\\s)?\\*\\)", "");
//                if (line.contains("typedef") && !line.contains("struct")) {
//                if (line.contains("typedef")) {
//                    line = line.replaceAll("typedef", "");
//                }
                if (removeCppParathesis) {
                    line = line.replace(line, "");
                    removeCppParathesis = false;
                }

                if (line.contains("__cplusplus") && line.contains("ifdef")) {
                    removeCppParathesis = true;
                }

                if (line.contains("extern") && !line.contains("\"C\"")) {
                    line = line.replaceAll("extern", "");
                }
                if (line.trim().endsWith("\\\n")) {
                    line = line.replace("\\\n", "");
                }
                if (line.contains("_ ##")) {
                    line = line.replace("_ ## ", "");
                }

                if (line.contains("(size_t)")) {
                    line = line.replace("(size_t)", "");
                }

                if (line.contains("static inline __attribute__((always_inline))")) {
                    line = line.replace("static inline __attribute__((always_inline))", "");
                }
                if (line.contains("inline _attribute_((always_inline))")) {
                    line = line.replace("inline _attribute_((always_inline))", "");
                }
                if (line.trim().startsWith("inline")) {
                    line = line.replace("inline ", "");
                }
                if (line.contains("__attribute__") && line.contains("((packed))")) {
                    line = line.replace("__attribute__", "").replace("((packed))", "");
                }
                if (line.contains("off_t")) {
                    line = line.replace("off_t", "");
                }
                if (line.contains("unsigned")) {
                    line = line.replace("unsigned", "unknowntype");
                }
                if (line.contains("static const")) {
                    line = line.replace("static const", "");
                }
                if (line.trim().startsWith("static")) {
                    line = line.replace("static ", "");
                }
                if (line.contains("const")) {
                    line = line.replace("const", "");
                }
                if (line.startsWith("%")) {
                    line = line.replace(line, "");
                }
                if (line.contains("virtual")) {
                    line = line.replace("virtual", "");
                }

                Pattern p1 = Pattern.compile("([x](\\d|[a-zA-Z])(\\d|[a-zA-Z]))*");
                Pattern p2 = Pattern.compile("\\\\[x](\\d|[a-zA-Z])(\\d|[a-zA-Z])");
                Matcher m1 = p1.matcher(line);
                Matcher m2 = p2.matcher(line);
                if (m1.find() && !m2.find() && !line.contains("extern")) {
                    line = m1.replaceFirst("");
                }
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        io.rewriteFile(sb.toString(), inputFile);
    }

    public String changeFileName(String fileName) {
        String[] nameArray = fileName.split("\\.");
        String suffix = nameArray[nameArray.length - 1];
        return fileName.replace("." + suffix, suffix.toUpperCase()).replace("-", "=").replace("/", "~").replace("\\", "~");
    }

    /**
     * This function get origin file name from node label
     *
     * @param nodeLabel filename+linenumber
     * @return origin file name
     */
    public String getOriginFileName(String nodeLabel) {
        return nodeLabel.split("-")[0].replace("~", "/").replace("=", "-").replaceAll("[H]$", ".h").replaceAll("[C][P][P]$", ".cpp").replaceAll("[H][P][P]$", ".hpp").replace("PDE", ".pde").replaceAll("[C]{2}$", ".cc").replaceAll("[C]$", ".c").replaceAll("[I][N][O]$", ".ino");

    }

    //todo
    public void writeSymboTableToFile(HashSet<Symbol> symbolTable, String analysisDir) {
    }

    public static int countLines(String filePath) throws IOException {
        int counter = 0;
        BufferedReader result_br = new BufferedReader(new FileReader(filePath));
        String result = "";
        try {
            StringBuilder sb = new StringBuilder();
            String line = result_br.readLine();
            while (line != null) {
                counter++;

                line = result_br.readLine();
            }
            result = sb.toString();
        } finally {
            result_br.close();
        }
        return counter;
    }

    /**
     * This method get file name by check the difference of (filePath - dirPath)
     *
     * @param filePath
     * @param dirPath
     * @return
     */
    public static String getFileNameFromDir(String filePath, String dirPath) {
        int index = filePath.lastIndexOf(dirPath);
        if (index > -1) {
            return filePath.substring(dirPath.length());
        }
        return filePath;
    }

    /**
     * This function checks whether the file is a C files when parsing the source code directory
     *
     * @param filePath
     * @return true if the file is a .c/.h/.cpp/.pde (Marlin) file
     */
    public boolean isCFile(String filePath) {
        return filePath.endsWith(".cpp") || filePath.endsWith(".c") || filePath.endsWith(".pde") || filePath.endsWith(".ino") || filePath.endsWith(".cc");
    }

    public boolean isHeaderFile(String filePath) {
        return filePath.endsWith(".h") || filePath.endsWith(".hpp");
    }

//    public boolean isPdeFile(String filePath) {
//        return filePath.endsWith(".pde");
//    }
//    public boolean isInoFile(String filePath) {
//        return filePath.endsWith(".ino");
//    }
//
//    public boolean isCCFile(String filePath) {
//        return filePath.endsWith(".cc");
//    }

    public boolean isCFile_general(String filePath) {
        return isCFile(filePath) || isHeaderFile(filePath);
    }

    public String removeUselessLine(String line) {
        /**remove comments **/
        if (line.trim().startsWith("#include")) {
            line = "";
        }
        return line;
    }


    /**
     * This function checks whether current line is a comment line
     *
     * @param line
     * @return true, if it is a comment line
     */
    public String removeComments(String line) {

        line = line.trim();

        if (line.contains("//")) {
            int index = line.indexOf("//");
            line = line.substring(0, index);
        }
        if (line.contains("/*")) {
            int index = line.indexOf("/*");
            line = line.substring(0, index);
        }
        if (line.startsWith("*") || line.startsWith("//")) {
            line = "";
        }

        return line;
    }

    /**
     * This function check current line is code or comment
     *
     * @param line the content of current line
     * @return true== if it is code; false=== it is comment
     */
    public boolean isCode(String line) {
        line = line.trim();
        if (line.startsWith("*") || line.startsWith("//") || line.startsWith("/*") || line.endsWith("*/") || line.startsWith("#") || line.length() == 0) {
            return false;
        }
        return true;
    }


    public void readTextFromURL(String urlStr, String outputFile) {
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(urlStr);
            // read text returned by server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line + "\n");
            }
            in.close();

        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }

        rewriteFile(sb.toString(), outputFile);
    }

    public String readTextFromURL(String urlStr) {
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(urlStr);
            // read text returned by server
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line + "\n");
            }
            in.close();

        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }

        return sb.toString();
    }


    public HashMap<String, String> getNodeLabel_to_id_map(String filePath) {
        HashMap<String, String> label_to_id = new HashMap<>();

        String[] nodeList = new String[0];
        try {
            nodeList = readResult(filePath).split("\n");
            for (String line : nodeList) {
                if (!line.startsWith("*")) {
                    String label = line.split(":")[0];
                    String id = line.split(":")[1];
                    if (!label.equals("")) {
                        label_to_id.put(label, id);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return label_to_id;
    }

    public ArrayList<String> getListFromFile(String analysisDir, String fileName) {
        ArrayList<String> topClusterList = new ArrayList<>();
        try {
            String[] topClusters = readResult(analysisDir + fileName).split("\n");
            for (String tc : topClusters) {
                topClusterList.add(tc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return topClusterList;
    }

    public boolean isTopCluster(ArrayList<String> topClusterList, String clusterID) {

        if (topClusterList.contains(clusterID)) {
            return true;
        }
        for (String cl : topClusterList) {

            if (clusterID.matches(cl + "(_[0-9])+")) {
                return true;
            }

        }
        return false;
    }

    public boolean isCLanguageFile(Path filePath) {
        return isCFile(filePath.toString());
    }

    public static void main(String[] args) {
        String path = "C:\\Users\\shuruiz\\Documents\\LineCounter\\txt\\";
        final int[] lines = {0};
        try {
            Files.walk(Paths.get(path)).forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    try {
                        lines[0] += countLines(filePath.toAbsolutePath().toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.print("linenumber : " + lines[0]);
    }

    /**
     * This function read unified source code and generates map of nodeid-source code terms
     *
     * @param n_gram      1 or 2 gram
     * @param analysisDir
     * @return map
     */
    public HashMap<String, String> getnodeToSourceCodeMap(int n_gram, String analysisDir) {
        HashMap<String, String> nodeToSourceCodeMap = new HashMap<>();
        String fileName = (n_gram == 1) ? "tokenizedSouceCode_oneGram.txt" : "tokenizedSouceCode_twoGram.txt";

        try {
            String sourceCode = new ProcessingText().readResult(analysisDir + fileName);
            String[] sourceCodeArray = sourceCode.split("\n");
            for (String s : sourceCodeArray) {
                nodeToSourceCodeMap.put(s.split(":")[0], s.split(":")[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nodeToSourceCodeMap;
    }

    public void deleteDir(File file) throws IOException {

        if (file.isDirectory()) {

            //directory is empty, then delete it
            if (file.list().length == 0) {

                file.delete();
                System.out.println("Directory is deleted : "
                        + file.getAbsolutePath());

            } else {

                //list all the directory contents
                String files[] = file.list();

                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp);

                    //recursive delete
                    deleteDir(fileDelete);
                }

                //check the directory again, if empty then delete it
                if (file.list().length == 0) {
                    file.delete();
                    System.out.println("Directory is deleted : "
                            + file.getAbsolutePath());
                }
            }

        } else {
            //if file, then delete it
            file.delete();
            System.out.println("File is deleted : " + file.getAbsolutePath());
        }
    }

    public static String getRootDir() {
        if (current_OS.indexOf("mac") >= 0) {
            return "/Users/shuruiz/Work/";
        } else if (current_OS.indexOf("windows") >= 0) {
            return "C:\\Users\\shuruiz\\Documents\\";
        } else {
            return "/home/feature/shuruiz/";
        }
    }


    public void printMemebershipOfCurrentGraph(HashMap<String, ArrayList<Integer>> clusters, String outputFile, boolean isOriginalGraph, String analysisDir) {
        //print
        StringBuffer membership_print = new StringBuffer();

        membership_print.append("\n---" + clusters.entrySet().size() + " communities\n");
        Iterator it = clusters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry cluster = (Map.Entry) it.next();
            ArrayList<Integer> cluster_content = (ArrayList<Integer>) cluster.getValue();


            if (isOriginalGraph || (!isOriginalGraph && cluster_content.size() > 1)) {
                ArrayList<Integer> mem = (ArrayList<Integer>) cluster.getValue();
                if (mem != null) {
                    membership_print.append(cluster.getKey() + ") ");
                    membership_print.append("[");
                    for (Integer m : mem) {
                        membership_print.append(m + " , ");
                    }
                    membership_print.append("]\n");
                }
            }
        }
        //print old edge
        writeTofile(membership_print.toString(), analysisDir + outputFile);
    }

    public void writeToJoinClusterFile(String analysisDir, HashMap<String, HashSet<Integer>> joined_clusters, String combination) {
        StringBuilder sb = new StringBuilder();
//        joined_clusters.forEach((k, v) -> {
//            sb.append(k + ":" + v.toString() + "\n");
//        });

        HashMap<String, Integer> clusterID_size = new HashMap<>();
        joined_clusters.forEach((k, v) -> {
            clusterID_size.put(k, v.size());
        });
        Map<String, Integer> result = new LinkedHashMap<>();
        clusterID_size.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEachOrdered(x -> {
                    result.put(x.getKey(), x.getValue());

                });
        final Iterator<String> cursor = result.keySet().iterator();
        StringBuilder sb_topClusters = new StringBuilder();
sb.append("---"+joined_clusters.size()+" communities\n");
        while (cursor.hasNext()) {
            final String clusterID = cursor.next();
            sb.append(clusterID + ") " + joined_clusters.get(clusterID).toString() + "\n");
        }
        new ProcessingText().rewriteFile(sb.toString(), analysisDir + combination + "_joined_cluster.txt");
    }

    public void getDiffText(String forkName, String analysisDir, String originalHtmlPath, String diffFilePath) {
        System.out.println("getting diff information ... ");
        ProcessingText processingText = new ProcessingText();
        processingText.rewriteFile("", diffFilePath);
        org.jsoup.nodes.Document currentPage = null;
        ArrayList<String> fileNameList = new ArrayList<>();
        try {
            String originalHtmlString = readResult(analysisDir + originalHtmlPath);
            currentPage = Jsoup.parse(originalHtmlString);
            Elements fileList = currentPage.getElementsByAttribute("data-path");
            for (org.jsoup.nodes.Element fileEle : fileList) {
                String fileName = fileEle.attr("data-path");
                fileNameList.add(fileName);
            }
            processingText.rewriteFile(fileNameList.toString(), analysisDir + "changedFileList.txt");

            String parentRepo = currentPage.getElementsByClass("fork-flag").text().replace("forked from", "").trim();
//            currentPage.getElementsByAttribute("data-fragment-url").first().attr("data-fragment-url");


            String[] SHA_Array = processingText.readResult(analysisDir + "SHA.txt").split(",");

            for (int i = 0; i < fileNameList.size(); i++) {
                System.out.println(i + " / " + fileNameList.size());
                String diffLink = "http://www.github.com/" + parentRepo + "/diffs/" + i + "?head_user=" + forkName.split("/")[0] + "&sha1=" + SHA_Array[0] + "&sha2=" + SHA_Array[1].trim() + "&w=1";


                WebClient webClient = new WebClient(BrowserVersion.CHROME);
                // turn off htmlunit warnings
                LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
                java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
                java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
                webClient.getOptions().setUseInsecureSSL(true); //ignore ssl certificate
                webClient.getOptions().setThrowExceptionOnScriptError(false);
                webClient.getOptions().setJavaScriptEnabled(true);
                webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
                webClient.setCssErrorHandler(new SilentCssErrorHandler());

                HtmlPage diff_page = webClient.getPage(diffLink);
                webClient.waitForBackgroundJavaScriptStartingBefore(2000);
                webClient.waitForBackgroundJavaScript(50000);
                org.jsoup.nodes.Document diffBlock = Jsoup.parse(diff_page.asXml());


                StringBuilder sb = new StringBuilder();
                for (org.jsoup.nodes.Element lineEle : diffBlock.getElementsByClass("blob-code-inner")) {
                    sb.append(lineEle.text() + "\n");
                }
                System.out.println("writing to diff txt file...");
                processingText.writeTofile("INFOX_DIFF_BLOCK\n" + sb.toString(), diffFilePath);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}