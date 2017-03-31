package getCodeChanges;

import Util.ProcessingText;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by shuruiz on 1/14/17.
 */
public class MozillaRepoAnalysis {
    static ProcessingText processingText = new ProcessingText();


    /**
     * This function used for finding available changeSet from Firefox buglist
     */
    public static void checkAvailableChangeSet() {
        processingText.rewriteFile("", "/Users/shuruiz/Box Sync/INFOX-doc/experiment/AvailableFirefox_BugIdList.txt");
        StringBuilder stringBuilder = new StringBuilder();
        List<String> bugidList = new ArrayList<>();

        String fileName = "/Users/shuruiz/Box Sync/INFOX-doc/experiment/feature.csv";

        try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
            List<List<String>> values = lines.map(line -> Arrays.asList(line.split(","))).collect(Collectors.toList());
            for (List<String> line : values.subList(1, values.size() - 1)) {
                stringBuilder.append(line.get(0) + "\n");
                bugidList.add(line.get(0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder builder = new StringBuilder();

        for (String bugid : bugidList) {
            System.out.println("searching... " + bugid);
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("hg", "log", "-k", "bug " + bugid);
                processBuilder.directory(new File("/Users/shuruiz/Work/Open Source Project/Mozilla/firefox/.hg").getParentFile()); // this is where you set the root folder for the executable to run with
                Process process = processBuilder.start();

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line = null;
                boolean addBugid = false;
                while ((line = reader.readLine()) != null) {

                    if (!addBugid) {
                        builder.append(bugid + "---------------------------\n");
                        System.out.println("--------" + bugid);
                        addBugid = true;
                    }

                    builder.append(line);
                    builder.append(System.getProperty("line.separator"));
                }
                String result = builder.toString();
                process.waitFor();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
        processingText.writeTofile(builder.toString(), "/Users/shuruiz/Box Sync/INFOX-doc/experiment/AvailableFirefox_BugIdList.txt");
    }

    public static void getCommitChangeCfile() {
        Map<String, List<String>> changesetID_revision = getRevisionNumber();
        Map<String, Map<String, List<String>>> useful_changesetID_revision_fileName = new HashMap<>();


        changesetID_revision.forEach((k, v) -> {
            Map<String, List<String>> usefulChangeset = new HashMap<>();
            List<String> fileList = new ArrayList<String>();
            String currentRev = "";
            for (String rev : v) {
                currentRev = rev;
                fileList = new ArrayList<String>();
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder("hg", "stat", "--change", rev);
                    processBuilder.directory(new File("/Users/shuruiz/Work/Open Source Project/Mozilla/firefox/.hg").getParentFile()); // this is where you set the root folder for the executable to run with
                    Process process = processBuilder.start();

                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(process.getInputStream()));

                    String line = null;
                    while ((line = reader.readLine()) != null) {

                        String fileName = line.split(" ")[1];
                        if (processingText.isCFile(fileName) || processingText.isHeaderFile(fileName)) {
                            fileList.add(fileName);
                        }
                    }
                    process.waitFor();

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (fileList.size() > 0) {
                    usefulChangeset.put(currentRev, fileList);
                    useful_changesetID_revision_fileName.put(k, usefulChangeset);
                }
            }

            StringBuilder sb = new StringBuilder();
            processingText.rewriteFile("", "/Users/shuruiz/Work/Open Source Project/Mozilla/allRevisionChangedFileList.txt");
            useful_changesetID_revision_fileName.forEach((chid, revmap) -> {
                sb.append(chid + "------------\n");

                revmap.forEach((rev, fl) -> {
                    sb.append(rev + "----\n");
                    for (String file : fl) {
                        sb.append(file + "\n");
                    }
                });


            });
            processingText.writeTofile(sb.toString(), "/Users/shuruiz/Work/Open Source Project/Mozilla/allRevisionChangedFileList.txt");

        });


        System.out.print("");

    }

    private static Map<String, List<String>> getRevisionNumber() {
        Map<String, List<String>> changesetID_revision = new HashMap<>();
        List<String> changesetIdList = new ArrayList<>();
        String currentChangesetID = "";
        try {
            String str = processingText.readResult("/Users/shuruiz/Work/Open Source Project/Mozilla/AvailableBugIdList.txt");
            for (String s : str.split("\n")) {
                changesetIdList = new ArrayList<>();
                if (s.contains("---------------------------")) {
                    currentChangesetID = s.replace("---------------------------", "");
                    changesetID_revision.put(currentChangesetID, changesetIdList);
                    continue;
                }
                if (s.contains("changeset:")) {
                    s = s.split(":")[1].trim();
                    changesetIdList = changesetID_revision.get(currentChangesetID);
                    changesetIdList.add(s);
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return changesetID_revision;
    }

    public static void getChangedCode() {
//        String revision = "94349";
        String revision = "20396";
        String[] filePath = {
                "js/src/jsbuiltins.cpp",
                "js/src/jsmath.cpp"
        };
        processingText.rewriteFile("", "/Users/shuruiz/Work/Open Source Project/Mozilla/forkAddedNode.txt");
        StringBuilder sb = new StringBuilder();
        for (String file : filePath) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("hg", "blame", "-r", revision, file);
                processBuilder.directory(new File("/Users/shuruiz/Work/Open Source Project/Mozilla/firefox/.hg").getParentFile()); // this is where you set the root folder for the executable to run with
                Process process = processBuilder.start();

                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line = null;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(revision + ":")) {
                        String newFileName = processingText.changeFileName(file);
                        sb.append(newFileName + "-" + lineNumber + "\n");
                    }
                    lineNumber++;
                }
                process.waitFor();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        processingText.writeTofile(sb.toString(), "/Users/shuruiz/Work/Open Source Project/Mozilla/forkAddedNode.txt");
    }


    public static void filterOutNonFeatureBug() {
        String line = "";
        String input = "mozilla_bug.csv";
        String output = "feature.csv";
        String path = "/Users/shuruiz/Box Sync/INFOX-doc/experiment/";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path + input))) {
            while ((line = br.readLine()) != null) {
                if (line.split(",")[6].contains("feature")) {
                    sb.append(line + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        new ProcessingText().rewriteFile(sb.toString(), path + output);
    }

    public static void main(String[] args) {
        checkAvailableChangeSet();
        getCommitChangeCfile();

        getChangedCode();

//        filterOutNonFeatureBug();
    }


}
