package analyzingResult;

import Util.ProcessingText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by shuruiz on 2/8/17.
 */
public class analyzingResult {
    //    static String testCasesDir = "/Users/shuruiz/Work/MarlinRepo/testINFOX/Marlin/DPGraph/";
//    static String testCasesDir = "/Users/shuruiz/Work/MarlinRepo/testMarlin/Marlin/";
    static String testCasesDir = "/Users/shuruiz/Work/MarlinRepo/testopenvpn/openvpn/";
    static final String FS = File.separator;
    static String dpPath = "";
    static int total_num_of_cuts = 5;
    static boolean isMs = false;
    static String csvPathList_txt = "csvPath_List.txt";
    static String outputFile = "accuracy.csv";
    static ProcessingText processingText = new ProcessingText();
    static int STOP_CRITERIA = 50;

    public static void collect_AllCSV_to_oneplace(String analysisDirName) {

        dpPath = testCasesDir + analysisDirName + FS;
//        String[] paths = { "macros"};
        String[] paths = {"macros", "macros_oneFile"};
        StringBuilder sb = new StringBuilder();
        for (int i = 3; i <= 15; i++) {
            for (String path : paths) {
                for (int j = 1; j <= 6; j++) {

                    try {
                        String testDir = dpPath + i + path + FS + j + FS;
                        File file = new File(testDir);
                        if (file.exists()) {
                            Files.walk(Paths.get(testDir), 1).forEach(filePath -> {
                                if (Files.isDirectory(filePath)) {
                                    try {
                                        Files.walk(Paths.get(String.valueOf(filePath)), 1).forEach(filePath_sub -> {
                                            if (Files.isDirectory(filePath_sub)) {
                                                String[] names = filePath_sub.toFile().list();
                                                for (String name : names) {
                                                    String dir = filePath_sub + FS + name;
                                                    File sub_dir = new File(dir);
                                                    if (sub_dir.exists() && sub_dir.isDirectory()) {
                                                        String[] sub_names = sub_dir.list();
                                                        for (String sn : sub_names) {
                                                            if (sn.contains("resultTable_joinThreshold")) {
//                                                            if (sn.contains("resultTable_joinThreshold") && sn.contains("50.csv")) {
                                                                sb.append(sub_dir + FS + sn + "\n");
                                                            }
                                                        }
                                                    }
                                                }

                                            }
                                        });
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }


                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        new ProcessingText().rewriteFile(sb.toString(), dpPath + csvPathList_txt);
    }


    static public void collectAcc(String csvFile) {
        String line = "";
        String cvsSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            int edgeRemoved_total = 0;
            while ((line = br.readLine()) != null) {
                if (!line.contains("Clusters")) {
                    // use comma as separator
                    String[] cutList = line.split(cvsSplitBy);
                    int edgeRemoved_current = Integer.valueOf(cutList[1]);
                    edgeRemoved_total += edgeRemoved_current;
                    System.out.println("Country [code= " + cutList[4] + " , name=" + cutList[5] + "]");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void findMaxAccuracyPoint(String[] listOfCSV, String analysisDirName, boolean isMs,boolean findMaxAcc) {

        outputFile=findMaxAcc?"max_"+outputFile:outputFile;
        if (isMs) {
            new ProcessingText().rewriteFile("filePath, accuracy\n", testCasesDir + analysisDirName + FS + outputFile);

        } else {
            new ProcessingText().rewriteFile("filePath, split_stop, join_stop, accuracy\n", testCasesDir + analysisDirName + FS + outputFile);


        }

        for (int stop_split_if_num_of_removedEdge_larger_than = 5; stop_split_if_num_of_removedEdge_larger_than <= 5; stop_split_if_num_of_removedEdge_larger_than++) {
            StringBuilder sb = new StringBuilder();
            String line;
            String pre_TestCase = "";
            String currentTestCase = "";
            boolean checked_no_split_acc = false;
            String[] inital_cutList = null;
            String[] previous_cutList = null;

            for (String csvFile : listOfCSV) {
                currentTestCase = csvFile.split("resultTable")[0];
                if (!pre_TestCase.equals(currentTestCase) && checked_no_split_acc) {
                    checked_no_split_acc = false;
                    inital_cutList = null;
                }
                pre_TestCase = currentTestCase;

                double max_acc = 0;
                int clusterInc = -1;

                try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                    System.out.println(csvFile);
                    int cuts_total = 0;
                    String[] current_cutList = new String[8];
                    while ((line = br.readLine()) != null) {
                        if (!line.contains("Clusters")) {
                            String[] cutList = line.split(",");
                            double acc = Double.valueOf(cutList[6].split("=")[0]);
                            int edgeRemoved_current = Integer.valueOf(cutList[1]);


                            if (!isMs && !findMaxAcc) {

                                if (edgeRemoved_current < stop_split_if_num_of_removedEdge_larger_than) {
                                    if (inital_cutList == null) {
                                        inital_cutList = cutList;
                                    }

                                    current_cutList = cutList;
                                    cuts_total++;
                                    if (cuts_total > total_num_of_cuts) {
                                        System.out.println(pre_TestCase + "  never stop if split stop criteria is " + stop_split_if_num_of_removedEdge_larger_than);
                                        double acc_now = Double.valueOf(current_cutList[7].split("=")[0]);
                                        sb.append(csvFile + "," + stop_split_if_num_of_removedEdge_larger_than + ", 0 ," + acc_now + "\n");

//                                        sb.append(csvFile + "," + stop_split_if_num_of_removedEdge_larger_than + ", 0 ," + Double.valueOf(inital_cutList[5].split("=")[0]) + "\n");

                                        break;
                                    }
                                    previous_cutList = current_cutList;
                                    continue;
                                } else {
                                    double join_acc;
                                    if (edgeRemoved_current == stop_split_if_num_of_removedEdge_larger_than) {
                                        join_acc = Double.valueOf(current_cutList[7].split("=")[0]);

                                    } else {
                                        join_acc = Double.valueOf(previous_cutList[7].split("=")[0]);
                                    }
                                    int join_stop_when_feature_size_larger_than = Integer.valueOf(csvFile.split("-")[1].replace(".csv", ""));
                                    if (join_stop_when_feature_size_larger_than == STOP_CRITERIA && (join_stop_when_feature_size_larger_than + "").endsWith("0")) {
                                        sb.append(csvFile + "," + stop_split_if_num_of_removedEdge_larger_than + "," + join_stop_when_feature_size_larger_than + "," + join_acc + "\n");
                                        System.out.println(csvFile + "," + stop_split_if_num_of_removedEdge_larger_than + "," + join_stop_when_feature_size_larger_than + "," + join_acc + "\n");
                                        break;
                                    }
                                }
                            } else if(isMs){  //MS
                                sb.append(csvFile + "," + acc + "\n");
                                System.out.println(csvFile + "," + stop_split_if_num_of_removedEdge_larger_than + "," + acc);
                            } else if(findMaxAcc){
                                System.out.println();
                               double current_acc =  Double.valueOf(cutList[6].split("=")[0]);
                               double join_acc = Double.valueOf(cutList[7].split("=")[0]);
                               double current_acc_max = current_acc>join_acc?current_acc:join_acc;

                                max_acc = current_acc_max>max_acc?current_acc_max:max_acc;
                            }
                        }
                    }

                    if(findMaxAcc){
                        sb.append(csvFile + "," + max_acc + "\n");
                        System.out.println(csvFile  + "," + max_acc);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            new ProcessingText().writeTofile(sb.toString(), testCasesDir + analysisDirName + FS + outputFile);
        }
    }

    public static void main(String[] args) {

        boolean findMaxAcc = false;
        for (int method = 1; method <=1; method++) {
            String analysisDirName = "";

            if (method == 1) {
                analysisDirName = "testINFOX";
                findMaxAcc=true;
            } else if (method == 2) {
                analysisDirName = "testMS";
                isMs = true;
            } else if (method == 3) {
                analysisDirName = "testMS_plus_CF_Hierachy";
                isMs = true;
            } else if (method == 4) {
                analysisDirName = "testINFOX_NO_DefUse";
            } else if (method == 5) {
                analysisDirName = "testINFOX_NO_ControlF";
            } else if (method == 6) {
                analysisDirName = "testINFOX_NO_Hierarchy";
            } else if (method == 7) {
                analysisDirName = "testINFOX_NO_Consec";
            } else if (method == 8) {
                analysisDirName = "testMS_NO_Consec";
                isMs = true;
            }


            /** collect all the csv file path **/
            collect_AllCSV_to_oneplace(analysisDirName);

            String[] listOfCSV = new String[]{};
            try {
                listOfCSV = new ProcessingText().readResult(testCasesDir + analysisDirName + FS + csvPathList_txt).split("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }


            /**  generate a csv to find the max acc and corresponding 1)  #cluster_inc  and 2) #edgeRemoved. **/
            findMaxAccuracyPoint(listOfCSV, analysisDirName, isMs,findMaxAcc);


//        for (String csv : listOfCSV) {
//
//            collectAcc(csv);
//        }
        }
    }
}
