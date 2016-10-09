package ColorCode;
import Util.ProcessingText;

import java.io.File;
import java.io.IOException;
import java.util.*;
/**
 * Created by shuruiz on 8/29/16.
 */
public class AnalyzingCommunityDetectionResult {
    ProcessingText processingText = new ProcessingText();
    String analysisDir,testCaseDir;
    List<String> bgcolorList = BackgroundColor.getExpectColorList();
    static final String FS = File.separator;
    //    public void generatingClusteringTable(String analysisDir, int numberOfCommunities, ArrayList<String> macroList) {
    public void generatingClusteringTable( String testCaseDir, String testDir, int numberOfCommunities) {
        this.analysisDir=testCaseDir+testDir+FS;
        this.testCaseDir=testCaseDir;
        HashMap<String, HashMap<String, Integer>> resultTable = new HashMap<>();

//        for (int i = 0; i < macroList.size(); i++) {
//            resultTable.put(bgcolorList.get(i), new HashMap<>());
//        }


        String filePath = analysisDir + numberOfCommunities + "_colorTable.txt";
        try {
            String cssString = processingText.readResult(filePath);
            String[] colorArray = cssString.split("\n");
            ArrayList<String> nodeColorList = new ArrayList(Arrays.asList(colorArray));
            ArrayList<String> communityColorList = new ArrayList<>();
            for (String node : nodeColorList) {
                if (node.length() > 0) {

                    String[] nodeInfo = node.split(",");
                    String id = nodeInfo[0];
                    String bgColor = nodeInfo[1];
                    String expectColor = nodeInfo[2];
                    if (!communityColorList.contains(bgColor)) {
                        communityColorList.add(bgColor);
                    }

                    HashMap<String, Integer> distributedColor = resultTable.get(expectColor.trim());
                    if (distributedColor != null) {
                        if (distributedColor.get(bgColor) == null) {
                            distributedColor.put(bgColor, 1);
                        } else {
                            int num = distributedColor.get(bgColor);
                            distributedColor.put(bgColor, num + 1);
                        }
                    } else {
                        distributedColor = new HashMap<>();
                        distributedColor.put(bgColor, 1);
                    }
                    resultTable.put(expectColor.trim(), distributedColor);

                }
            }

            printResultTable(resultTable, communityColorList, numberOfCommunities);
//            printResultTable(resultTable, communityColorList, numberOfCommunities, macroList);

            printClusterDistanceTable(numberOfCommunities);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void printClusterDistanceTable(int numberOfCommunities) {
        StringBuffer sb = new StringBuffer();
        HashMap<String, String[]> distanceTable = new HashMap<>();

        ArrayList<String> clusterIDList = new ArrayList<>();
        String[] colorList;
        String[] distanceList;
        try {
            String[] tmp = processingText.readResult(analysisDir + numberOfCommunities + "_clusterIdList.txt").split(",");
            for (String s : tmp) {
                if (!s.equals("\n")) {
                    clusterIDList.add(s);
                }

            }
            for (String s : clusterIDList) {
                if (!s.equals("\n")) {
                    distanceTable.put(s, new String[clusterIDList.size()]);
                }
            }

            // TODO: table color
            colorList = processingText.readResult(analysisDir + numberOfCommunities + "_clusterColor.txt").split("\n");
            HashMap<String, String> colorTable = new HashMap();
            for (String c : colorList) {
                //[0] id  [1] current color  [2] expect color
                String[] content = c.split(",");
                String id = content[0];
                String current_color = content[1];
                String expect_color = content[2];

                colorTable.put(id, current_color);
            }


            distanceList = processingText.readResult(analysisDir + numberOfCommunities + "_distanceBetweenCommunityies.txt").split("\n");
            for (String d : distanceList) {
                String[] content = d.split(",");
                String[] array = distanceTable.get(content[0]);
                array[Integer.valueOf(clusterIDList.indexOf(content[1]))] = content[2];
                distanceTable.put(content[0], array);
            }

            sb.append("<table id=\"distance\"> <tr> <td> </td>\n");
            for (String id : clusterIDList) {
                sb.append("<td bgcolor=\"#" + colorTable.get(id) + "\">" + id + "</td>\n");

            }
            sb.append("</tr>\n");

            for (String id : clusterIDList) {
                sb.append("<tr><td bgcolor=\"#" + colorTable.get(id) + "\">" + id + "</td>\n");

                for (String s : distanceTable.get(id)) {
                    sb.append("<td>" + s + "</td>\n");
                }
                sb.append("</tr>\n");
            }


            sb.append("</table>\n");


            processingText.rewriteFile(sb.toString(), analysisDir + numberOfCommunities + ".distanceTable");


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //    private void printResultTable(HashMap<String, HashMap<String, Integer>> resultTable, ArrayList<String> communityColorList, int numberOfCommunities, ArrayList<String> macroList) {
    private void printResultTable(HashMap<String, HashMap<String, Integer>> resultTable, ArrayList<String> communityColorList, int numberOfCommunities) {

        StringBuffer sb = new StringBuffer();
        Iterator it = resultTable.keySet().iterator();

        ArrayList<String> featureList = getFeatureList();

//                new ArrayList<>();
//        macroList.add("ots_firewall");
//        macroList.add("stm2");
        //print 1st line
        sb.append("<table id=\"cluster\">\n" +
                "    <tr>\n" +
                "        <td>\n" +
                "            <span>Result</span>\n" +
                "            <hr/>\n" +
                "            <span>CRs ( LOC )</span>\n" +
                "        </td>\n");

        for (String color : communityColorList) {
            sb.append("<td bgcolor=\"#" + color + "\"></td>\n");
        }
        sb.append("</tr>\n");
// ----------------for ifdef evaluation----------------------------------
        for (int i = 0; i < featureList.size(); i++) {
//        for (int i = 0; i < communityColorList.size(); i++) {
            String expectColor = bgcolorList.get(i);
            //print rest lines
            sb.append("  <tr>\n" +
                    "        <td bgcolor=\"" + expectColor + "\">\n" +
                    featureList.get(i).trim() +
                    "\n        </td>\n");

            HashMap<String, Integer> distributedMap = resultTable.get(expectColor.trim());

            if (distributedMap != null) {
                for (String s : communityColorList) {
                    String number;
                    if(distributedMap.get(s)==null){
                        number="-";
                    }else{
                        number=distributedMap.get(s).toString();
                    }
                    sb.append("<td>" + number+ "</td>\n");
                }
                sb.append("</tr>\n");
            }else{
                System.out.println("distributedMap is null: "+expectColor);
            }
        }

//        else{
//
//        }
//         ----------------for ifdef evaluation----------------------------------


        sb.append("</table>");
        processingText.rewriteFile(sb.toString(), analysisDir + numberOfCommunities + ".color");
    }

    private ArrayList<String> getFeatureList() {
        try {
            String[] featureArray = processingText.readResult( testCaseDir+ "featureList.txt").split("\n");
            return new ArrayList<>(Arrays.asList(featureArray));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
