package porcelli.me.git.integration.webhook;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.URIish;
import porcelli.me.git.integration.webhook.model.PullRequestEvent;
import porcelli.me.git.integration.webhook.model.PushEvent;

public class BCIntegration {

    private final Map<String, Repository> repositoryMap = new HashMap<>();

    public void onPush(final PushEvent pushEvent) throws GitAPIException, URISyntaxException, IOException {
        if (!pushEvent.getRef().contains("master")) {
            return;
        }
        final Git git = getGit(pushEvent.getRepository());

        try {
            git.pull().setRemote("origin").setRebase(true).call();
            git.pull().setRemote("github").setRebase(true).call();
            git.push().setRemote("origin").setForce(true).call();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onPullRequest(final PullRequestEvent pullRequestEvent) throws GitAPIException, IOException, URISyntaxException {
        if (!pullRequestEvent.getAction().equals(PullRequestEvent.Action.CLOSED)) {
            return;
        }
        final String branchName = pullRequestEvent.getPullRequest().getBody();

        final Git git = getGit(pullRequestEvent.getRepository());
        git.branchDelete().setBranchNames("refs/heads/" + branchName).call();

        final RefSpec refSpec = new RefSpec().setSource(null).setDestination("refs/heads/" + branchName);
        git.push().setRefSpecs(refSpec).setRemote("origin").call();
    }

    private Git getGit(porcelli.me.git.integration.webhook.model.Repository repository) throws GitAPIException, URISyntaxException, IOException {
        final Git git;
        if (!repositoryMap.containsKey(repository.getDescription())) {
            final String bcRepo = repository.getDescription();

            try {
                SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                    @Override
                    protected void configure(OpenSshConfig.Host host, Session session) {
                    }
                };

                git = Git.cloneRepository()
                        .setTransportConfigCallback(transport -> {
                            SshTransport sshTransport = (SshTransport) transport;
                            sshTransport.setSshSessionFactory(sshSessionFactory);
                        }).setURI(bcRepo)
                        .setDirectory(tempDir(repository.getFullName()))
                        .call();
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }

            final RemoteAddCommand remoteAddCommand = git.remoteAdd();
            remoteAddCommand.setName("github");
            remoteAddCommand.setUri(new URIish(repository.getCloneUrl()));
            remoteAddCommand.call();
            repositoryMap.put(repository.getDescription(), git.getRepository());
        } else {
            git = new Git(repositoryMap.get(repository.getDescription()));
        }
        return git;
    }

    private File tempDir(String reponame) throws IOException {
        return Files.createTempDirectory(new File(System.getProperty("java.io.tmpdir")).toPath(), "temp").resolve(reponame).toFile();
    }
}
