package NamingClusters;

import Util.ProcessingText;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by shuruiz on 10/26/16.
 */
public class Tokenizer {
    Stemmer stemmer = new Stemmer();

    /**
     * This function tokenizes the repository source code and store into tokenizedSouceCode.txt
     *
     * @param sourcecodeDir path of the repostiroy
     */

    public void tokenizeSourceCode(String sourcecodeDir, String testCaseDir) {
        Stemmer stemmer = new Stemmer();
        ProcessingText processingText = new ProcessingText();
        processingText.rewriteFile("", testCaseDir + "tokenizedSouceCode_oneGram.txt");
        processingText.rewriteFile("", testCaseDir + "tokenizedSouceCode_twoGram.txt");
        HashMap<String, String> oneGram_sourceCodeLocMap = new HashMap<>();
        HashMap<String, String> twoGram_sourceCodeLocMap = new HashMap<>();
        //parse every header file in the project
        try {
            Files.walk(Paths.get(sourcecodeDir)).forEach(filePath -> {
                int lineNumber = 0;
                if (Files.isRegularFile(filePath) && (processingText.isHeaderFile(filePath.toString()) || processingText.isCFile(filePath.toString()))) {

                    //get fileName
                    String fileName = processingText.getFileNameFromDir(filePath.toString(), sourcecodeDir);
                    String newFileName = processingText.changeFileName(fileName);

                    System.out.println("_______file    :" + newFileName);
                    // read source code
                    BufferedReader result_br = null;

                    try {
                        result_br = new BufferedReader(new FileReader(filePath.toFile()));
                        String line = result_br.readLine();
                        while (line != null) {
                            lineNumber++;

                            line = processingText.removeUselessLine(line);
                            line = line.toLowerCase().replaceAll("[^a-zA-Z0-9_]", " ");

                            if (line.trim().length() == 0) {
                                line = result_br.readLine();
                                continue;
                            }
                            String oneGram_newLine = "";
                            String twoGram_newLine = "";
                            String[] lineArray = line.split(" ");
                            for (String s : lineArray) {
                                if (!s.matches("\\d+") && s.length() > 0) {
                                    s = new StopWords().removeStopWord(s);
                                    s = stemmer.stemmingAWord(s);
                                    if (s.trim().length() > 0) {
                                        if (s.contains("_") && !s.endsWith("_h") && !s.endsWith("_")) {

                                            //2-grams
                                            List<String> twoGramsList = generateNgrams(2, s);
                                            for (String tg : twoGramsList) {
                                                twoGram_newLine += tg + " ";
                                            }

                                            //1-grams
                                            oneGram_newLine += s.replace("_", " ");
                                        } else {
                                            oneGram_newLine += s + " ";
                                            twoGram_newLine += s + " ";
                                        }

                                    }
                                }
                            }


                            if (oneGram_newLine.trim().length() > 0) {
                                String location = newFileName + "-" + lineNumber;
                                oneGram_sourceCodeLocMap.put(location, oneGram_newLine);
                                twoGram_sourceCodeLocMap.put(location, twoGram_newLine);
                            }
                            line = result_br.readLine();

                        }


                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            result_br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuffer sb_one = new StringBuffer();
        StringBuffer sb_two = new StringBuffer();

        oneGram_sourceCodeLocMap.forEach((k, v) -> sb_one.append(k + ":" + v + "\n"));
        twoGram_sourceCodeLocMap.forEach((k, v) -> sb_two.append(k + ":" + v + "\n"));

        processingText.writeTofile(sb_one.toString(), testCaseDir + "tokenizedSouceCode_oneGram.txt");
        processingText.writeTofile(sb_two.toString(), testCaseDir + "tokenizedSouceCode_twoGram.txt");

        /**stemming tokens**/

        new Stemmer().stemmingFile(testCaseDir + "tokenizedSouceCode_oneGram.txt");
        new Stemmer().stemmingFile(testCaseDir + "tokenizedSouceCode_twoGram.txt");

    }

    /**
     * This function generates n-grams
     *
     * @param N
     * @param wordWithUnderscore
     */
    public List<String> generateNgrams(int N, String wordWithUnderscore) {
        List ngramList = new ArrayList();
        //split wordWithUnderscore into tokens
        ArrayList<String> tokenList = new ArrayList<String>(Arrays.asList(wordWithUnderscore.split("_")));


        //GENERATE THE N-GRAMS
        for (int k = 0; k < (tokenList.size() - N + 1); k++) {
            String s = "";
            int start = k;
            int end = k + N;
            for (int j = start; j < end; j++) {
                String word = stemmer.stemmingAWord(tokenList.get(j));
                if (s.equals("")) {
                    s = word;
                } else {
                    s = s + "_" + word;
                }
            }
            //Add n-gram to a list
            ngramList.add(s);
        }
        return ngramList;
    }
}
