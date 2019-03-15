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

    private static final String BC_URL = System.getProperty("bc.url");
    private static final String WH_URL = System.getProperty("wh.url", null);
    private static final String GH_USERNAME = System.getProperty("gh.username");
    private static final String GH_PASSWORD = System.getProperty("gh.password");

    private final GitHubCredentials credentials;

    public GitHubIntegration(final GitHubCredentials credentials) {
        this.credentials = credentials;
    }

    public String createRepository(final String spaceName, final String repoName) throws IOException {
        final GitHub github = GitHub.connect(GH_USERNAME, GH_PASSWORD);
        final GHRepository repo = github.createRepository(repoName)
                .description("ssh://" + BC_URL + "/" + spaceName + "/" + repoName)
                .autoInit(false)
                .create();

        if (WH_URL != null) {
            final Map<String, String> config = new HashMap<String, String>() {{
                put("url", new URL(WH_URL).toExternalForm());
                put("content_type", "json");
            }};
            repo.createHook("web", config, asList(GHEvent.PULL_REQUEST, GHEvent.PUSH), true);
        }

        return repo.getHttpTransportUrl();
    }

    public void createPR(final String repoName,
                         final String sourceBranch) throws IOException {
        final GitHub github = GitHub.connect(GH_USERNAME, GH_PASSWORD);
        final GHPullRequest pullRequest = github.getRepository(credentials.getSpace() + "/" + repoName).createPullRequest("PR from RHPAM", sourceBranch, "master", sourceBranch);
    }
}
