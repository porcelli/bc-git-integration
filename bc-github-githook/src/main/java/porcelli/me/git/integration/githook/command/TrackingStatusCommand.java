package porcelli.me.git.integration.githook.command;

import java.io.IOException;

import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;

public class TrackingStatusCommand implements Command {

    private final Repository repository;

    public TrackingStatusCommand(Repository repository) {
        this.repository = repository;
    }

    public TrackingStatus getCounts(final String branchName) throws IOException {
        BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(repository, branchName);
        if (trackingStatus != null) {
            return new TrackingStatus(branchName,
                                      trackingStatus.getAheadCount(),
                                      trackingStatus.getBehindCount());
        }
        return new TrackingStatus(branchName);
    }
}
