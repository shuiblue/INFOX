package Util;

/**
 * Created by shuruiz on 6/2/2016.
 */

import DependencyGraph.Symbol;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessingText {

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

    /**
     * This function will collect all the fork added node from  "forkAddedNode.txt", and generate a list of forkAddedNode
     *
     * @return Arraylist of fork Added Nodes
     * @throws IOException
     */
    public ArrayList getForkAddedNodeList(String forkAddedNodeTxt) throws IOException {
        String[] lines = readResult(forkAddedNodeTxt).split("\n");
        ArrayList<String> forkAddedNodeList = new ArrayList<>();
        for (String s : lines) {
            String node = s.split(" ")[0];

            if(node.equals("")){
                System.out.println();
            }
            if (!forkAddedNodeList.contains(node)&&!node.equals("")) {
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
     * @param inputFile file that need to be parsed by srcML
     * @return path of XML file
     * @throws IOException e
     */
    public static String getXmlFile(String inputFile) {
        // create dir for store xml files

        String outXmlFile = inputFile + ".xml";
        //run srcML
        if (new File(inputFile).isFile()) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("src2srcml", "--xmlns:PREFIX=http://www.sdml.info/srcML/position", inputFile, "-o", outXmlFile);
//                ProcessBuilder processBuilder = new ProcessBuilder("srcML/src2srcml", "--xmlns:PREFIX=http://www.sdml.info/srcML/position", inputFile, "-o", outXmlFile);
    /* for windows
              ProcessBuilder processBuilder = new ProcessBuilder("C:\\Users\\shuruiz\\Documents\\srcML-Win\\src2srcml.exe", "--position",
              inputFile, "-o", outXmlFile);
     */

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
                if (line.contains("__attribute__") && line.contains("((packed))")) {
                    line = line.replace("__attribute__", "").replace("((packed))", "");
                }
                if (line.contains("off_t")) {
                    line = line.replace("off_t", "");
                }
                if (line.contains("unsigned")) {
                    line = line.replace("unsigned", "unknowntype");
                }
                if (line.contains("const")) {
                    line = line.replace("const", "");
                }
                if (line.startsWith("%")) {
                    line = line.replace(line, "");
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
        return fileName.replace("." + suffix, suffix.toUpperCase()).replace("-", "~").replace("/", "~").replace("\\", "~");
    }

    /**
     * This function get origin file name from node label
     *
     * @param nodeLabel filename+linenumber
     * @return origin file name
     */
    public String getOriginFileName(String nodeLabel) {
        return nodeLabel.split("-")[0].replace("~", "/").replaceAll("[H]$", ".h").replace("CPP", ".cpp").replace("PDE", ".pde").replaceAll("[C]$", ".c").replace("INO",".ino");

    }

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
        return filePath.endsWith(".cpp") || filePath.endsWith(".c");
//        return filePath.endsWith(".cpp") || filePath.endsWith(".h") || filePath.endsWith(".c") || filePath.endsWith(".pde");
    }

    public boolean isHeaderFile(String filePath) {
        return filePath.endsWith(".h");
//        return filePath.endsWith(".cpp") || filePath.endsWith(".h") || filePath.endsWith(".c") || filePath.endsWith(".pde");
    }

    public boolean isPdeFile(String filePath) {
        return filePath.endsWith(".pde");
    }
    public boolean isInoFile(String filePath) {
        return filePath.endsWith(".ino");
    }

    public boolean isCFile_general(String filePath) {
        return isCFile(filePath) || isHeaderFile(filePath) || isPdeFile(filePath)||isInoFile(filePath);
    }

    public String removeUselessLine(String line) {
        /**remove comments **/
//        line = removeComments(line);

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
        if (line.startsWith("*") || line.startsWith("//") || line.startsWith("/*") || line.endsWith("*/") || line.trim().length() == 0) {
            return false;
        }
        return true;
    }


    public void ReadTextFromURL(String urlStr, String outputFile) {
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



    public HashMap<String, String> getNodeLabel_to_id_map(String filePath) {
        HashMap<String, String> label_to_id = new HashMap<>();

        String[] nodeList = new String[0];
        try {
            nodeList = readResult(filePath).split("\n");
            for (String line : nodeList) {
                if (!line.startsWith("*")) {
                    String label = line.split(":")[0];
                    String id = line.split(":")[1];
                    if(!label.equals("")) {
                        label_to_id.put(label, id);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return label_to_id;
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
}