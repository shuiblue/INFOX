package analyzingResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by shuruiz on 2/15/17.
 *
 * This function remove the DPGraph fold
 */
public class cleanTMPFiles {
    static String analysisDirName = "DPGraph";
    static String testCasesDir = "/Users/shuruiz/Work/MarlinRepo/test";
    static final String FS = File.separator;

    public static void main(String[] args) {

        try {
            Files.walk(Paths.get(testCasesDir), 1).forEach(filePath -> {
                if (Files.isDirectory(filePath) && !filePath.toString().equals(testCasesDir)) {
                    String sourcecodeDir = filePath.toString() + FS;
                    String path = filePath + FS + analysisDirName + "/";
                    File file = new File(path);
                    File[] testcases = file.listFiles();
                    for (File test : testcases) {
                        if (test.isDirectory()) {
                            File[] testChild = test.listFiles();
                            for (File tc : testChild) {
                                if (!tc.toString().contains("DS_Store")) {
                                    File[] tcc = tc.listFiles();
                                    for (File f : tcc) {
                                        System.out.print(f.toPath() + "\n");
                                        if (f.isDirectory()) {
                                            File[] subf = f.listFiles();
                                            for (File sf : subf) {
                                                sf.delete();
                                            }
                                        }

                                        if (!(f.toString().contains("expectCluster.txt")
                                                || f.toString().contains("featureList.txt")
                                                || f.toString().contains("forkAddedNode.txt"))) {
                                            f.delete();
                                        }

                                    }
                                } else {
                                    System.out.print("");
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

}
