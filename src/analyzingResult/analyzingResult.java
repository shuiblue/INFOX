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
    static String testCasesDir = "/Users/shuruiz/Work/MarlinRepo/testMarlin/Marlin/";
    static final String FS = File.separator;
    static String dpPath = "";
    static int stopCriteria_edgeRemoved = 8;
    static boolean isMs = false;
    static String csvPathList_txt = "csvPath_List.txt";
    static String outputFile = "maxAcc.csv";


    public static void collect_AllCSV_to_oneplace(String analysisDirName) {
        dpPath = testCasesDir + analysisDirName + FS;
        String[] paths = {"macros", "macros_oneFile"};
        StringBuilder sb = new StringBuilder();
        for (int i = 3; i <= 3; i++) {
            for (String path : paths) {
                for (int j = 2; j <= 2; j++) {

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
                                                            if (sn.contains("resultTable_")) {
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


    private static void findStopCriteria(String[] listOfCSV, String analysisDirName, boolean isMs) {
        new ProcessingText().rewriteFile("filePath, num_cluster_inc, num_edgeRemoved, maxAcc\n", testCasesDir + analysisDirName + FS + outputFile);
        StringBuilder sb = new StringBuilder();
        String line;
        for (String csvFile : listOfCSV) {

//            if(!csvFile.contains("47110")&&!csvFile.contains("58110")&&!csvFile.contains("69110")&&!csvFile.contains("710110")&&!csvFile.contains("811110")
//                    &&!csvFile.contains("912110")&&!csvFile.contains("1013110")&&!csvFile.contains("111410110")&&!csvFile.contains("1215110")&&!csvFile.contains("1316110")
//                    &&!csvFile.contains("1417110")&&!csvFile.contains("1518110")
//                    ) {
            double maxAcc = 0;
            int clusterInc = -1;
            try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                System.out.println(csvFile);
                int edgeRemoved_total = 0;
                while ((line = br.readLine()) != null) {
                    if (!line.contains("Clusters")) {
                        String[] cutList = line.split(",");
                        double acc = Double.valueOf(cutList[5].split("=")[0]);
                        int edgeRemoved_current = Integer.valueOf(cutList[1]);
                        if (!isMs) {
                            double join_acc = Double.valueOf(cutList[6].split("=")[0]);
                            if (maxAcc < acc || maxAcc < join_acc) {
                                maxAcc = acc > join_acc ? acc : join_acc;
                                edgeRemoved_total += edgeRemoved_current;
                                clusterInc++;
                            } else {
                                sb.append(csvFile + "," + clusterInc + "," + edgeRemoved_total + "," + maxAcc + "\n");
                                System.out.println(edgeRemoved_total + "," + maxAcc);
                                break;
                            }
                        } else {
                            sb.append(csvFile + "," + acc + "\n");
                            System.out.println(edgeRemoved_total + "," + maxAcc);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        }
        new ProcessingText().writeTofile(sb.toString(), testCasesDir + analysisDirName + FS + outputFile);
    }


    public static void main(String[] args) {
//        int method = 2;

        for(int method = 1;method<=1;method++){
        String analysisDirName = "";

        if (method == 1) {
            analysisDirName = "testINFOX";
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

//        String[] listOfCSV = new String[]{};
//        try {
//            listOfCSV = new ProcessingText().readResult(testCasesDir + analysisDirName + FS + csvPathList_txt).split("\n");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        /**  generate a csv to find the max acc and corresponding 1)  #cluster_inc  and 2) #edgeRemoved. **/
//        findStopCriteria(listOfCSV, analysisDirName, isMs);


//        for (String csv : listOfCSV) {
//
//            collectAcc(csv);
//        }
    }}
}
