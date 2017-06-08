package Util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;

/**
 * Created by shuruiz on 4/7/17.
 */
public class JgitUtility {


    public void cloneRepo(String uri, String localDirPath,String branchName) {
        File dir =new File(localDirPath);
        dir.mkdir();

        try {
            if(branchName.equals("")){
                Git git = Git.cloneRepository()
                        .setURI(uri)
                        .setDirectory(dir)
                        .call();
            }else {
                Git git = Git.cloneRepository()
                        .setURI(uri)
                        .setDirectory(dir)
                        .setBranch(branchName)
                        .call();
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

    }

}
