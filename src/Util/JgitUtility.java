package Util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;

/**
 * Created by shuruiz on 4/7/17.
 */
public class JgitUtility {
    String dirPath = "/Users/shuruiz/Work/GithubProject/";

    public void cloneRepo(String uri, String forkName) {
        File dir =new File(dirPath+forkName);
        dir.mkdir();

        try {
            Git git = Git.cloneRepository()
                    .setURI(uri)
                    .setDirectory(dir)
                    .call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

    }

}
