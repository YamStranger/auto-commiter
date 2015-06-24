package autocommiter;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * User: YamStranger
 * Date: 6/12/15
 * Time: 4:29 PM
 */
public class JGitCommitter {
    private final String remote;
    private final Path repository;
    private CredentialsProvider credentials = null;

    JGitCommitter(String token, String remote, Path repository) {
        this.credentials = new UsernamePasswordCredentialsProvider(token,"");
        this.remote = remote;
        this.repository = repository;
    }

    public void synchronize() throws IOException, TransportException, InvalidRemoteException, GitAPIException {
        File directory = this.repository.resolve(".git").toFile();
        if (!directory.exists()) {
            Git.cloneRepository().setURI(this.remote)
                    .setCredentialsProvider(this.credentials)
                    .setDirectory(this.repository.toFile()).call();
        }
        Repository repo = new FileRepository(directory);
        if (!containsRemote(repo, this.remote)) {
            throw new RuntimeException("Incorrect repo");
        }
        Git git = new Git(repo);
        List<Ref> branches = git.branchList().call();
        if (branches.isEmpty()) {
            git.branchCreate().setName("master");
            this.commit(git, "initial commit");
            git.push().setCredentialsProvider(this.credentials).call();
        }
        git.pull().setCredentialsProvider(this.credentials).call();
        this.commit(git, "scheduled commit");
        git.push().setCredentialsProvider(this.credentials).call();
    }

    void commit(Git git, String message) throws TransportException, InvalidRemoteException, GitAPIException {
        Status status = git.status().call();
        status.getChanged();

        final Set<String> changed = new HashSet<>(status.getChanged());
        if (!changed.isEmpty()) {
            final AddCommand add = git.add();
            for (final String file : changed) {
                add.addFilepattern(file);
            }
            add.call();
        }

        final Set<String> modified = new HashSet<>(status.getModified());
        if (!modified.isEmpty()) {
            final AddCommand add = git.add();
            for (final String file : modified) {
                add.addFilepattern(file);
            }
            add.call();
        }

        final Set<String> untracked = new HashSet<>(status.getUntracked());
        if (!untracked.isEmpty()) {
            final AddCommand add = git.add();
            for (final String file : untracked) {
                add.addFilepattern(file);
            }
            add.call();
        }

        final Set<String> removed = status.getMissing();
        if (!removed.isEmpty()) {
            final RmCommand rm = git.rm();
            for (final String file : removed) {
                rm.addFilepattern(file);
            }
            rm.call();
        }

        CommitCommand commitCommand = git.commit();
        commitCommand.setMessage(message);
        commitCommand.call();
    }

    public Path create(String file) throws IOException, TransportException, InvalidRemoteException, GitAPIException {
        if (!Files.exists(this.repository)) {
            this.synchronize();
        }
        Path created = this.repository.resolve(file);
        if (!Files.exists(created)) {
            Files.createDirectories(created.getParent());
            Files.createFile(created);
        }
        return created;
    }

    boolean containsRemote(Repository repo, String remote) {
        StoredConfig config = repo.getConfig();
        Set<String> remotes = config.getSubsections("remote");
        boolean contains = false;
        for (String remoteName : remotes) {
            String url = config.getString("remote", remoteName, "url");
            if (url.equals(remote)) {
                contains = true;
                break;
            }
        }
        return contains;
    }

/*

    public void init() throws IOException {
        localPath = "/home/me/repos/mytest";
        remotePath = "git@github.com:me/mytestrepo.git";
        repository = new FileRepository(localPath + "/.git");
        git = new Git(repository);
        git.
    }

    public void testCreate() throws IOException {
        Repository newRepo = new FileRepository(localPath + ".git");
        newRepo.create();
    }


    public void testClone() throws IOException, GitAPIException {
        Git.cloneRepository().setURI(remotePath)
                .setDirectory(new File(localPath)).call();
    }

    public void testAdd() throws IOException, GitAPIException {
        File myfile = new File(localPath + "/myfile");
        myfile.createNewFile();
        git.add().addFilepattern("myfile").call();
    }

    public void testCommit() throws IOException, GitAPIException,
            JGitInternalException {
        git.commit().setMessage("Added myfile").call();
    }

    public void testPush() throws IOException, JGitInternalException,
            GitAPIException {
        git.push() call();
    }

    public void testTrackMaster() throws IOException, JGitInternalException,
            GitAPIException {
        git.branchCreate().setName("master")
                .setUpstreamMode(SetupUpstreamMode.SET_UPSTREAM)
                .setStartPoint("origin/master").setForce(true).call();
    }

    public void testPull() throws IOException, GitAPIException {
        git.pull().call();
    }
*/
}
