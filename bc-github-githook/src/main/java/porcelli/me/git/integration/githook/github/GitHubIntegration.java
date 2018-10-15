package porcelli.me.git.integration.githook.github;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import static java.util.Arrays.asList;

public class GitHubIntegration {

    private final GitHubCredentials credentials;

    public GitHubIntegration(final GitHubCredentials credentials) {
        this.credentials = credentials;
    }

    public String createRepository(final String repoName) throws IOException {
        final GitHub github = GitHub.connect();
        final GHRepository repo = github.createRepository(repoName)
                .description("ssh://localhost:8001/MySpace/" + repoName)
                .autoInit(false)
                .create();

        final Map<String, String> config = new HashMap<String, String>() {{
            put("url", new URL("http://e5997029.ngrok.io/api/hook/").toExternalForm());
            put("content_type", "json");
        }};
        repo.createHook("web", config, asList(GHEvent.PULL_REQUEST, GHEvent.PUSH), true);

        return repo.getHttpTransportUrl();
    }

    public void createPR(final String repoName,
                         final String sourceBranch) throws IOException {
        final GitHub github = GitHub.connect();
        final GHPullRequest pullRequest = github.getRepository(credentials.getSpace() + "/" + repoName).createPullRequest("PR from RHPAM", sourceBranch, "master", sourceBranch);
    }
}
