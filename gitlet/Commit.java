package gitlet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;
import java.util.ArrayList;
import java.io.Serializable;
import java.io.File;
import java.io.IOException;

public class Commit implements Serializable {
    /** String denoting the inputed commit message. */
    private String message;
    /** Time at which the commit was created. */
    private Date timestamp;
    /** String containing sha1 of commit's parent. */
    private String parent;
    /** Contains all files in the commit. */
    private TreeMap<String, String> files;
    /** List of all parents (may have multiple due to merging). */
    private ArrayList<String> parents;
    /** Determines whether commit is a split point. */
    private boolean splitPoint;

    public Commit() {
        message = "initial commit";
        parent = null;
        timestamp = new Date();
        timestamp.setTime(0);
        files = new TreeMap<>();
        parents = new ArrayList<>();
        parents.add(parent);
        splitPoint = false;
    }

    public Commit(String p, String msg) {
        message = msg;
        parent = p;
        timestamp = new Date();
        File pFile = Utils.join(Repo.COMMIT_FOLDER, parent);
        Commit c = Utils.readObject(pFile, Commit.class);
        files = c.getFiles();
        for (String file : Utils.plainFilenamesIn(Repo.STAGE_REMOVE_FOLDER)) {
            files.remove(file);
        }
        parents = new ArrayList<>();
        parents.add(parent);
        splitPoint = false;
    }

    public TreeMap<String, String> getFiles() {
        return files;
    }

    public String getBlob(String file) {
        return getFiles().get(file);
    }

    public String getMessage() {
        return message;
    }

    public String getParent() {
        return parent;
    }

    public ArrayList<String> getAllParents() {
        return parents;
    }

    public void addParent(String p) {
        parents.add(p);
    }

    public Commit getParentCommit() {
        if (parent == null) {
            return null;
        } else {
            return readCommit(this.parent);
        }

    }

    public String getDate() {
        SimpleDateFormat format =
                new SimpleDateFormat("EEE MMM d hh:mm:ss yyyy ZZZZ");
        String date = format.format(timestamp);
        return date;
    }

    public boolean isSplitPoint() {
        return splitPoint;
    }

    public static Commit readCommit(File f) {
        return Utils.readObject(f, Commit.class);
    }

    public static Commit readCommit(String name) {
        File f = Utils.join(Repo.COMMIT_FOLDER, name);
        return readCommit(f);
    }

    public boolean isTracked(String name) {
        return files.containsKey(name);
    }

    public void addFile(String name, File blob) {
        byte[] data = Utils.readContents(blob);
        String blobSha = Utils.sha1(data);
        File newBlob = Utils.join(Repo.BLOBS_FOLDER, blobSha);
        try {
            newBlob.createNewFile();
        } catch (IOException e) {
            System.out.println("IOException");
        }
        if (files.containsKey(name)) {
            files.replace(name, blobSha);
        } else {
            files.put(name, blobSha);
        }
        Utils.writeContents(newBlob, data);
    }

    public void removeFile(String name) {
        files.remove(name);
    }

    public void printLog(String sha) {
        System.out.println("===");
        System.out.println("commit " + sha);
        System.out.println("Date: " + getDate());
        System.out.println(getMessage());
        System.out.println();
    }

    public void createSplit() {
        splitPoint = true;
    }


}
