package gitlet;

import java.io.File;
import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Teja Kanthamneni
 */
public class Main {
    /** .gitlet directory. */
    private static File gitletFolder = new File(".gitlet");
    /** Current repo object. */
    private static Repo r = new Repo();
    /** File containing the repo object. */
    private static File repo = new File(".gitlet/repo");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        if (gitletFolder.exists()) {
            r = Utils.readObject(repo, Repo.class);
        }
        switch (args[0]) {
        case "init":
            init(args);
            break;
        case "add":
            add(args);
            break;
        case "commit":
            commit(args);
            break;
        case "log":
            log(args);
            break;
        case "global-log":
            globalLog(args);
            break;
        case "checkout":
            checkout(args);
            break;
        case "rm":
            rm(args);
            break;
        case "find":
            find(args);
            break;
        case "status":
            status(args);
            break;
        case "branch":
            branch(args);
            break;
        case "rm-branch":
            rmBranch(args);
            break;
        case "reset":
            reset(args);
            break;
        case "merge":
            merge(args);
            break;
        default:
            System.out.println("No command with that name exists.");
        }
    }

    public static void init(String[] command) {
        if (gitletFolder.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            return;
        } else if (command.length > 1) {
            System.out.println("Incorrect operands.");
        } else {
            r.init();
            try {
                repo.createNewFile();
            } catch (IOException e) {
                System.out.println("IOException");
            }
            Utils.writeObject(repo, r);
        }
    }

    public static void add(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            r.add(command[1]);
            Utils.writeObject(repo, r);
        }
    }

    public static void commit(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length > 2) {
            System.out.println("Incorrect operands.");
        } else if (command.length < 2 || command[1].length() == 0) {
            System.out.println("Please enter a commit message.");
        } else {
            r.commit(command[1]);
            Utils.writeObject(repo, r);
        }
    }

    public static void log(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length > 1) {
            System.out.println("Incorrect operands.");
        } else {
            r.log();
            Utils.writeObject(repo, r);
        }
    }

    public static void globalLog(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length > 1) {
            System.out.println("Incorrect operands.");
        } else {
            r.globalLog();
            Utils.writeObject(repo, r);
        }
    }

    public static void checkout(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length < 2) {
            System.out.println("Incorrect operands.");
        } else if (command[1].equals("--")) {
            if (command.length != 3) {
                System.out.println("Incorrect operands.");
                return;
            }
            r.checkoutLastCommit(command[2]);
            Utils.writeObject(repo, r);
        } else if (command.length == 2) {
            r.checkoutBranch(command[1]);
            Utils.writeObject(repo, r);
        } else if (command[2].equals("--")) {
            if (command.length != 4) {
                System.out.println("Incorrect operands.");
                return;
            }
            r.checkoutId(command[1], command[3]);
            Utils.writeObject(repo, r);
        } else {
            System.out.println("Incorrect operands.");
        }
    }

    public static void rm(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            r.remove(command[1]);
            Utils.writeObject(repo, r);
        }
    }

    public static void find(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            r.find(command[1]);
            Utils.writeObject(repo, r);
        }
    }

    public static void status(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length != 1) {
            System.out.println("Incorrect operands.");
        } else {
            r.status();
            Utils.writeObject(repo, r);
        }
    }

    public static void branch(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            r.branch(command[1]);
            Utils.writeObject(repo, r);
        }
    }

    public static void rmBranch(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            r.removeBranch(command[1]);
            Utils.writeObject(repo, r);
        }
    }

    public static void reset(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            r.reset(command[1]);
            Utils.writeObject(repo, r);
        }
    }

    public static void merge(String[] command) {
        if (!gitletFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
        } else if (command.length != 2) {
            System.out.println("Incorrect operands.");
        } else {
            r.merge(command[1]);
            Utils.writeObject(repo, r);
        }
    }
}
