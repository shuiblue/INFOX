package getAllForks;

import Util.ProcessingText;

import java.io.File;
import java.io.IOException;

/**
 * Created by shuruiz on 8/3/17.
 */
public class getActiveForks {
    public static void main(String[] args) throws IOException {
        ProcessingText pt = new ProcessingText();
        String forkList = pt.readResult("/Users/shuruiz/Box Sync/INFOX-doc/experiment/projectSelection/forks.csv");
        String[] forkArray = forkList.split("\n");
        for (String fork : forkArray) {
            String[] array = fork.split(",");
            String url = array[1] + "/" + array[2];
            System.out.println(url);
            ProcessBuilder processBuilder = null;
            processBuilder = new ProcessBuilder("java", "-jar", "infox_forks.jar", url);
            processBuilder.directory(new File("/Users/shuruiz/Box sync/INFOX-doc/experiment/interview/contact/getContact"));


            Process process = processBuilder.start();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }
}
