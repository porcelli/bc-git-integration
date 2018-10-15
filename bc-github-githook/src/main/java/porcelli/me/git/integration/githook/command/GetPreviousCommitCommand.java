package porcelli.me.git.integration.githook.command;

import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

public class GetPreviousCommitCommand implements Command {

    private final Repository repo;

    public GetPreviousCommitCommand(Repository repo) {
        this.repo = repo;
    }

    public RevCommit execute(final RevCommit commit,
                             final int commitsAhead) throws IOException {
        RevCommit result = commit;
        for (int i = 0; i < commitsAhead; i++) {
            result = repo.parseCommit(repo.resolve(result.getParent(0).name()));
        }
        return result;
    }
}
