package porcelli.me.git.integration.githook;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import porcelli.me.git.integration.githook.command.GetPreviousCommitCommand;
import porcelli.me.git.integration.githook.command.GetRepoName;
import porcelli.me.git.integration.githook.command.SetupRemote;
import porcelli.me.git.integration.githook.command.SquashCommand;
import porcelli.me.git.integration.githook.command.TrackingStatus;
import porcelli.me.git.integration.githook.command.TrackingStatusCommand;
import porcelli.me.git.integration.githook.github.GitHubCredentials;
import porcelli.me.git.integration.githook.github.GitHubIntegration;

import static java.util.Comparator.comparing;

public class GitHook {

    public static void main(String[] args) throws IOException, GitAPIException {
        final Path currentPath = new File("").toPath().toAbsolutePath();
        final String parentFolderName = currentPath.getParent().getName(currentPath.getParent().getNameCount() - 1).toString();
        if (parentFolderName.equalsIgnoreCase("system")) {
            return;
        }
        final Repository repo = new FileRepositoryBuilder()
                .setGitDir(currentPath.toFile())
                .build();

        final Git git = new Git(repo);
        final StoredConfig storedConfig = repo.getConfig();
        final Set<String> remotes = storedConfig.getSubsections("remote");

        final GitHubCredentials ghCredentials = new GitHubCredentials();
        final GitHubIntegration integration = new GitHubIntegration(ghCredentials);

        if (remotes.isEmpty()) {
            new SetupRemote(ghCredentials, integration).execute(git, currentPath);
            return;
        }

        final List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        final RevWalk revWalk = new RevWalk(git.getRepository());

        branches.stream()
                .map(branch -> {
                    try {
                        return revWalk.parseCommit(branch.getObjectId());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .max(comparing((RevCommit commit) -> commit.getAuthorIdent().getWhen()))
                .ifPresent(newestCommit -> {
                    RevCommit commit = newestCommit;
                    try {
                        boolean hasSyncOnCommitMessage = commit.getFullMessage().trim().startsWith("sync:");

                        if (hasSyncOnCommitMessage) {
                            final Map<ObjectId, String> branchesAffected = git
                                    .nameRev()
                                    .addPrefix("refs/heads")
                                    .add(commit)
                                    .call();

                            for (String remoteName : remotes) {
                                final String remoteURL = storedConfig.getString("remote", remoteName, "url");
                                for (String ref : branchesAffected.values()) {
                                    final String remote = storedConfig.getString("branch", ref, "remote");

                                    if (remote == null) {
                                        git.push()
                                                .setRefSpecs(new RefSpec(ref + ":" + ref))
                                                .setRemote(remoteURL)
                                                .setCredentialsProvider(ghCredentials.getCredentials())
                                                .call();
                                        storedConfig.setString("branch", ref, "remote", "origin");
                                        storedConfig.setString("branch", ref, "merge", "refs/heads/" + ref);
                                        storedConfig.save();
                                        if (ref.contains("-pr")) {
                                            integration.createPR(new GetRepoName().execute(currentPath), ref);
                                        }
                                    } else {
                                        git.fetch().call();
                                        final TrackingStatusCommand trackingStatusCommand = new TrackingStatusCommand(git.getRepository());
                                        final TrackingStatus counts = trackingStatusCommand.getCounts(ref);
                                        if (counts.getCommitsAhead() > 0) {
                                            final RevCommit id = new GetPreviousCommitCommand(repo).execute(commit, counts.getCommitsAhead() - 1);
                                            new SquashCommand(git, ref, id.name(), commit.getFullMessage()).execute(commit);
                                        }
                                        git.push()
                                                .setRefSpecs(new RefSpec(ref + ":" + ref))
                                                .setRemote(remoteURL)
                                                .setCredentialsProvider(ghCredentials.getCredentials())
                                                .call();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
