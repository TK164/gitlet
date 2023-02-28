package gitlet;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.io.Serializable;
import java.io.File;
import java.io.IOException;


public class Repo implements Serializable {
    /** .gitlet directory. */
    static final File GITLET_FOLDER = new File(".gitlet");
    /** Directory containing all commits. */
    static final File COMMIT_FOLDER = Utils.join(GITLET_FOLDER, "commits");
    /** Directory containing subdirectories for addition and removal. */
    static final File STAGING_FOLDER = Utils.join(GITLET_FOLDER, "staging");
    /** Directory containing all files staged for addition. */
    static final File STAGE_ADD_FOLDER = Utils.join(STAGING_FOLDER, "addition");
    /** Directory containing all files staged for removal. */
    static final File STAGE_REMOVE_FOLDER
            = Utils.join(STAGING_FOLDER, "removal");
    /** Directory containing all blobs. */
    static final File BLOBS_FOLDER = Utils.join(GITLET_FOLDER, "blobs");
    /** String denoting the branch that is the current head. */
    private String head;
    /** TreeMap that maps branch names to the sha1 of the latest commit. */
    private TreeMap<String, String> branches;
    /** List of all commits created. */
    private ArrayList<String> commits;

    public Repo() {
        head = "master";
        branches = new TreeMap<>();
        commits = new ArrayList<>();
    }

    public Commit getHeadCommit() {
        File commit = Utils.join(COMMIT_FOLDER, branches.get(head));
        return Commit.readCommit(commit);
    }

    public String getHeadCommitName() {
        return branches.get(head);
    }

    public void copyFileData(File fromFile, File toFile) {
        byte[] data = Utils.readContents(fromFile);
        Utils.writeContents(toFile, data);
    }

    public boolean sameContents(File f1, File f2) {
        return Arrays.equals(Utils.readContents(f1), Utils.readContents(f2));
    }

    public Commit getCommitFromId(String id) {
        String fullSha = getFullSha(id);
        File prevFile = Utils.join(COMMIT_FOLDER, fullSha);
        Commit c = null;
        if (prevFile.exists()) {
            c = Commit.readCommit(prevFile);
        }
        return c;
    }

    public String getFullSha(String id) {
        if (id.length() == getHeadCommitName().length()) {
            return id;
        }
        for (String sha : Utils.plainFilenamesIn(COMMIT_FOLDER)) {
            if (sha.startsWith(id)) {
                return sha;
            }
        }
        return null;
    }

    public void init() {
        GITLET_FOLDER.mkdir();
        COMMIT_FOLDER.mkdir();
        STAGING_FOLDER.mkdir();
        STAGE_ADD_FOLDER.mkdir();
        STAGE_REMOVE_FOLDER.mkdir();
        BLOBS_FOLDER.mkdir();

        Commit c = new Commit();

        String commitName = Utils.sha1(Utils.serialize(c));
        branches.put("master", commitName);
        commits.add(commitName);
        File initCommit = Utils.join(COMMIT_FOLDER, commitName);
        try {
            initCommit.createNewFile();
        } catch (IOException e) {
            System.out.println("yeeyeeyee");
        }
        Utils.writeObject(initCommit, c);
    }

    public void add(String name) {
        File toAdd = new File(name);
        if (!toAdd.exists()) {
            System.out.println("File does not exist");
            return;
        }

        File rmStage = Utils.join(STAGE_REMOVE_FOLDER, name);
        if (rmStage.exists()) {
            rmStage.delete();
        }

        File toStaging = Utils.join(STAGE_ADD_FOLDER, name);
        Commit currCommit = getHeadCommit();

        if (currCommit.getFiles().containsKey(name)) {
            String blob = currCommit.getBlob(name);
            File b = Utils.join(BLOBS_FOLDER, blob);
            if (sameContents(toAdd, b)) {
                if (toStaging.exists()) {
                    toStaging.delete();
                }
            } else {
                copyFileData(toAdd, toStaging);
            }
        } else if (toStaging.exists()) {
            if (!sameContents(toAdd, toStaging)) {
                copyFileData(toAdd, toStaging);
            }
        } else {
            try {
                toStaging.createNewFile();
            } catch (IOException e) {
                System.out.println("IOException");
            }
            copyFileData(toAdd, toStaging);
        }
    }

    public void commit(String msg) {
        List<String> addFiles = Utils.plainFilenamesIn(STAGE_ADD_FOLDER);
        List<String> rmFiles = Utils.plainFilenamesIn(STAGE_REMOVE_FOLDER);
        if (addFiles.size() == 0 && rmFiles.size() == 0) {
            System.out.println("No changes added to the commit.");
        } else {
            Commit c = new Commit(branches.get(head), msg);

            for (String file: addFiles) {
                File blob = Utils.join(STAGE_ADD_FOLDER, file);
                File stageFile = Utils.join(STAGE_ADD_FOLDER, file);
                c.addFile(file, blob);
                stageFile.delete();

            }
            for (String file: rmFiles) {
                c.removeFile(file);
                File stageFile = Utils.join(STAGE_REMOVE_FOLDER, file);
                stageFile.delete();
            }

            String commitName = Utils.sha1(Utils.serialize(c));
            File newCommit = Utils.join(COMMIT_FOLDER, commitName);
            try {
                newCommit.createNewFile();
            } catch (IOException e) {
                System.out.println("IOException");
            }
            branches.replace(head, commitName);
            commits.add(commitName);
            Utils.writeObject(newCommit, c);
        }
    }

    public void recoverFile(Commit c, String file) {
        File recoveredFile = new File(file);
        String blob = c.getBlob(file);
        File blobFile = Utils.join(BLOBS_FOLDER, blob);
        if (!recoveredFile.exists()) {
            try {
                recoveredFile.createNewFile();
            } catch (IOException e) {
                System.out.println("IOException");
            }
        }
        copyFileData(blobFile, recoveredFile);

    }

    public void checkoutLastCommit(String file) {
        Commit currCommit = getHeadCommit();
        if (!currCommit.getFiles().containsKey(file)) {
            System.out.println("File does not exist in that commit.");
        } else {
            recoverFile(currCommit, file);
        }
    }

    public void checkoutId(String id, String file) {
        Commit c = getCommitFromId(id);
        if (c == null) {
            System.out.println("No commit with that id exists.");
        } else if (!c.getFiles().containsKey(file)) {
            System.out.println("File does not exist in that commit.");
        } else {
            recoverFile(c, file);
        }
    }

    public void checkoutBranch(String branch) {
        if (branch.equals(head)) {
            System.out.println("No need to checkout the current branch.");
        } else if (!branches.containsKey(branch)) {
            System.out.println("No such branch exists.");
        } else {
            List<String> cwdFiles = Utils.plainFilenamesIn(".");
            File branchHeadFile =
                    Utils.join(COMMIT_FOLDER, branches.get(branch));

            ArrayList<String> filesToDelete = new ArrayList<>();

            Commit currCommit = getHeadCommit();
            Commit branchHead = Utils.readObject(branchHeadFile, Commit.class);

            for (String file : cwdFiles) {
                if (!currCommit.isTracked(file) && branchHead.isTracked(file)) {
                    System.out.println("There is an untracked file in the way;"
                            + " delete it, or add and commit it first.");
                    return;
                } else if (currCommit.isTracked(file)
                        && !branchHead.isTracked(file)) {
                    filesToDelete.add(file);
                }
            }
            for (String file : filesToDelete) {
                Utils.restrictedDelete(file);
            }
            for (String file : branchHead.getFiles().keySet()) {
                String branchBlob = branchHead.getBlob(file);
                File blob = Utils.join(BLOBS_FOLDER, branchBlob);
                File repoFile = new File(file);
                if (!repoFile.exists()) {
                    try {
                        repoFile.createNewFile();
                    } catch (IOException e) {
                        System.out.println("IOException");
                    }
                }
                copyFileData(blob, repoFile);
            }

            for (String file : Utils.plainFilenamesIn(STAGE_ADD_FOLDER)) {
                File f = Utils.join(STAGE_ADD_FOLDER, file);
                f.delete();
            }
            head = branch;
        }
    }

    public void log() {
        Commit c = getHeadCommit();
        String shaId = branches.get(head);
        while (c.getParent() != null) {
            c.printLog(shaId);
            shaId = c.getParent();
            c = c.getParentCommit();
        }
        c.printLog(shaId);
    }

    public void globalLog() {
        for (String file : Utils.plainFilenamesIn(COMMIT_FOLDER)) {
            Commit c = Commit.readCommit(Utils.join(COMMIT_FOLDER, file));
            c.printLog(file);
        }
    }

    public void remove(String file) {
        File repoFile = new File(file);
        File stagedAdd = Utils.join(STAGE_ADD_FOLDER, file);
        File stagedRemove = Utils.join(STAGE_REMOVE_FOLDER, file);

        Commit c = getHeadCommit();

        if (stagedAdd.exists()) {
            stagedAdd.delete();
        } else if (c.isTracked(file)) {
            try {
                stagedRemove.createNewFile();
            } catch (IOException e) {
                System.out.println("IOException");
            }
            if (repoFile.exists()) {
                repoFile.delete();
            }
        } else {
            System.out.println("No reason to remove the file");
        }
    }

    public void find(String msg) {
        boolean printed = false;
        for (String name : Utils.plainFilenamesIn(COMMIT_FOLDER)) {
            File commitFile = Utils.join(COMMIT_FOLDER, name);
            Commit c = Commit.readCommit(commitFile);
            if (c.getMessage().equals(msg)) {
                System.out.println(name);
                printed = true;
            }
        }
        if (!printed) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        printBranches();
        printStagedFiles();
        printRemovedFiles();
        printModifiedFiles();
        printUntrackedFiles();
    }

    public void printBranches() {
        System.out.println("=== Branches ===");
        String[] keys = branches.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        for (String k : keys) {
            if (head.equals(k)) {
                k = "*" + k;
            }
            System.out.println(k);
        }
        System.out.println();
    }

    public void printStagedFiles() {
        System.out.println("=== Staged Files ===");
        for (String file : Utils.plainFilenamesIn(STAGE_ADD_FOLDER)) {
            System.out.println(file);
        }
        System.out.println();
    }

    public void printRemovedFiles() {
        System.out.println("=== Removed Files ===");
        for (String file : Utils.plainFilenamesIn(STAGE_REMOVE_FOLDER)) {
            System.out.println(file);
        }
        System.out.println();
    }

    public void printModifiedFiles() {
        System.out.println("=== Modifications Not Staged For Commit ===");
        Commit c = getHeadCommit();
        ArrayList<String> allFiles = new ArrayList<>();

        for (String file : Utils.plainFilenamesIn(".")) {
            File repoFile = new File(file);
            File stageFile = Utils.join(STAGE_ADD_FOLDER, file);
            if (stageFile.exists() && !sameContents(repoFile, stageFile)) {
                allFiles.add(file + " (modified)");
            } else if (c.isTracked(file)) {
                File commitBlob = Utils.join(BLOBS_FOLDER, c.getBlob(file));
                if (!sameContents(repoFile, commitBlob)) {
                    allFiles.add(file + " (modified)");
                }
            }
        }

        for (String file : Utils.plainFilenamesIn(STAGE_ADD_FOLDER)) {
            File repoFile = new File(file);
            if (!repoFile.exists()) {
                allFiles.add(file + " (deleted)");
            }
        }

        String[] trackedFiles = c.getFiles().keySet().toArray(new String[0]);
        for (String file : trackedFiles) {
            File repoFile = new File(file);
            File rmStageFile = Utils.join(STAGE_REMOVE_FOLDER, file);
            if (!rmStageFile.exists() && !repoFile.exists()) {
                allFiles.add(file + " (deleted)");
            }
        }

        String[] sortedFiles = allFiles.toArray(new String[0]);
        Arrays.sort(sortedFiles);
        for (String file : sortedFiles) {
            System.out.println(file);
        }
        System.out.println();
    }

    public void printUntrackedFiles() {
        System.out.println("=== Untracked Files ===");
        Commit c = getHeadCommit();
        for (String file : Utils.plainFilenamesIn(".")) {
            File stage = Utils.join(STAGE_ADD_FOLDER, file);
            if (!stage.exists() && !c.isTracked(file)) {
                System.out.println(file);
            }
        }
        System.out.println();
    }

    public void branch(String branch) {
        if (branches.containsKey(branch)) {
            System.out.println("A branch with that name already exists.");
        } else {
            branches.put(branch, getHeadCommitName());
            Commit c = getHeadCommit();
            File headCommit = Utils.join(COMMIT_FOLDER, getHeadCommitName());
            c.createSplit();
            Utils.writeObject(headCommit, c);

        }
    }

    public void removeBranch(String branch) {
        if (head.equals(branch)) {
            System.out.println("Cannot remove the current branch.");
        } else if (!branches.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
        } else {
            branches.remove(branch);
        }
    }

    public void reset(String id) {
        Commit c = getCommitFromId(id);
        if (c == null) {
            System.out.println("No commit with that id exists.");
        } else {
            String tempHead = head;
            String fullSha = getFullSha(id);
            branches.put("temp", fullSha);
            checkoutBranch("temp");
            head = tempHead;
            branches.replace(head, fullSha);
            removeBranch("temp");
        }
    }

    public void merge(String branch) {
        List<String> add = Utils.plainFilenamesIn(STAGE_ADD_FOLDER);
        List<String> rm = Utils.plainFilenamesIn(STAGE_REMOVE_FOLDER);
        if (add.size() != 0 || rm.size() != 0) {
            System.out.println("You have uncommitted changes.");
        } else if (!branches.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
        } else if (head.equals(branch)) {
            System.out.println("Cannot merge a branch with itself.");
        } else {
            if (branch.equals("1")) {
                System.out.println(Utils.plainFilenamesIn(COMMIT_FOLDER));
            }

            String lca = findLCA(branch);
            if (lca.equals(branches.get(branch))) {
                System.out.println("Given branch is an ancestor of "
                        + "the current branch.");
                return;
            } else if (lca.equals(getHeadCommitName())) {
                System.out.println("Current branch fast-forwarded.");
                checkoutBranch(branch);
                return;
            }

            Commit splitPoint = Commit.readCommit(lca);
            Commit currBranch = getHeadCommit();
            Commit givenBranch = Commit.readCommit(branches.get(branch));

            TreeMap<String, String> splitPointFiles = splitPoint.getFiles();
            TreeMap<String, String> givenBranchFiles = givenBranch.getFiles();

            if (!lookInCWD(splitPoint, currBranch, givenBranch)) {
                return;
            }
            lookInSplitPoint(branch, splitPointFiles, splitPoint, currBranch,
                    givenBranch);
            lookInGivenBranch(branch, givenBranchFiles,
                    splitPoint, currBranch, givenBranch);

            commit("Merged " + branch + " into " + head + ".");

            Commit c = getHeadCommit();
            File headCommit = Utils.join(COMMIT_FOLDER, getHeadCommitName());
            c.addParent(branches.get(branch));
            Commit p = Commit.readCommit(branches.get(branch));
            File newParentCommit
                    = Utils.join(COMMIT_FOLDER, branches.get(branch));
            p.createSplit();
            Utils.writeObject(headCommit, c);
            Utils.writeObject(newParentCommit, p);
        }
    }

    public boolean lookInCWD(Commit splitPoint, Commit currBranch,
                          Commit givenBranch) {
        for (String file : Utils.plainFilenamesIn(".")) {
            File splitBlobFile = new File(BLOBS_FOLDER + "/"
                    + splitPoint.getBlob(file));
            File currBranchBlobFile = new File(BLOBS_FOLDER + "/"
                    + currBranch.getBlob(file));
            File givenBranchBlobFile = new File(BLOBS_FOLDER + "/"
                    + givenBranch.getBlob(file));

            byte[] spBlob = getBlobForMerge(splitBlobFile);
            byte[] cbBlob = getBlobForMerge(currBranchBlobFile);
            byte[] gbBlob = getBlobForMerge(givenBranchBlobFile);

            if (gbBlob != null && spBlob == null && cbBlob == null) {
                System.out.println("There is an untracked file in the"
                        + " way; delete it, or add and commit it first.");
                return false;
            }
        }
        return true;
    }

    public void lookInSplitPoint(String branch,
                                 TreeMap<String, String> splitPointFiles,
                                 Commit splitPoint, Commit currBranch,
                                 Commit givenBranch) {
        for (String file : splitPointFiles.keySet()) {
            File splitBlobFile
                    = new File(BLOBS_FOLDER + "/"
                    + splitPoint.getBlob(file));
            File currBranchBlobFile
                    = new File(BLOBS_FOLDER + "/"
                    + currBranch.getBlob(file));
            File givenBranchBlobFile
                    = new File(BLOBS_FOLDER + "/"
                    + givenBranch.getBlob(file));

            byte[] spBlob = getBlobForMerge(splitBlobFile);
            byte[] cbBlob = getBlobForMerge(currBranchBlobFile);
            byte[] gbBlob = getBlobForMerge(givenBranchBlobFile);

            if (gbBlob == null && Arrays.equals(cbBlob, spBlob)) {
                remove(file);
            } else if (Arrays.equals(spBlob, cbBlob)
                    && !Arrays.equals(spBlob, gbBlob)) {
                checkoutId(branches.get(branch), file);
                add(file);
            } else if (!Arrays.equals(spBlob, cbBlob)
                    && !Arrays.equals(spBlob, gbBlob)
                    && !Arrays.equals(cbBlob, gbBlob)) {
                dealWithMergeConflict(cbBlob, gbBlob, file);
            }
        }
    }

    public void lookInGivenBranch(String branch,
                                  TreeMap<String, String> givenBranchFiles,
                                       Commit splitPoint, Commit currBranch,
                                       Commit givenBranch) {
        for (String file : givenBranchFiles.keySet()) {
            File splitBlobFile
                    = new File(BLOBS_FOLDER
                    + "/" + splitPoint.getBlob(file));
            File currBranchBlobFile
                    = new File(BLOBS_FOLDER
                    + "/" + currBranch.getBlob(file));
            File givenBranchBlobFile
                    = new File(BLOBS_FOLDER
                    + "/" + givenBranch.getBlob(file));

            byte[] spBlob = getBlobForMerge(splitBlobFile);
            byte[] cbBlob = getBlobForMerge(currBranchBlobFile);
            byte[] gbBlob = getBlobForMerge(givenBranchBlobFile);

            if (spBlob == null && cbBlob == null) {
                checkoutId(branches.get(branch), file);
                add(file);
            } else if (spBlob == null && !Arrays.equals(cbBlob, gbBlob)) {
                dealWithMergeConflict(cbBlob, gbBlob, file);
            }
        }
    }

    public byte[] getBlobForMerge(File f) {
        byte[] blob = null;
        if (f.exists()) {
            blob = Utils.readContents(f);
        }
        return blob;
    }

    public void dealWithMergeConflict(byte[] currBlob,
                                      byte[] givenBlob, String file) {
        File f = new File(file);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                System.out.println("IOException");
            }
        }
        if (currBlob == null) {
            Utils.writeContents(f, "<<<<<<< HEAD\n",
                    "=======\n", givenBlob, ">>>>>>>\n");
        } else if (givenBlob == null) {
            Utils.writeContents(f, "<<<<<<< HEAD\n",
                    currBlob, "=======\n", ">>>>>>>\n");
        } else {
            Utils.writeContents(f, "<<<<<<< HEAD\n",
                    currBlob, "=======\n", givenBlob, ">>>>>>>\n");
        }
        add(file);
        System.out.println("Encountered a merge conflict.");
    }

    public String findLCA(String branch) {
        HashSet<String> headAncestors = getSplitAncestors(getHeadCommitName());
        HashSet<String> branchAncestors
                = getSplitAncestors(branches.get(branch));
        HashSet<String> commonAncestors = new HashSet<>();
        for (String ancestor : headAncestors) {
            if (branchAncestors.contains(ancestor)) {
                commonAncestors.add(ancestor);
            }
        }
        String lca = "";
        int minDistance = Integer.MAX_VALUE;
        for (String ancestor : commonAncestors) {
            int commitDistance = getDistance(getHeadCommitName(), ancestor);
            if (commitDistance < minDistance) {
                minDistance = commitDistance;
                lca = ancestor;
            }
        }
        return lca;
    }

    public HashSet<String> getSplitAncestors(String commitSha) {
        HashSet<String> ancestors = new HashSet<>();
        Commit c = Commit.readCommit(commitSha);
        if (c.isSplitPoint()) {
            ancestors.add(commitSha);
        }
        if (c.getParentCommit() != null) {
            for (String p : c.getAllParents()) {
                ancestors.addAll(getSplitAncestors(p));
            }
        }
        return ancestors;
    }

    public int getDistance(String currCommit, String checkCommit) {
        Commit c = Commit.readCommit(currCommit);
        if (currCommit.equals(checkCommit) || c.getParentCommit() == null) {
            return 0;
        } else {
            ArrayList<Integer> distances = new ArrayList<>();
            for (String parent : c.getAllParents()) {
                distances.add(1 + getDistance(parent, checkCommit));
            }
            int min = Integer.MAX_VALUE;
            for (int dist : distances) {
                if (dist < min) {
                    min = dist;
                }
            }
            return min;
        }
    }
}
