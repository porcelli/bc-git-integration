package porcelli.me.git.integration.githook.command;

public class TrackingStatus {

    private final String branchName;
    private final int commitsAhead;
    private final int commitsBehind;

    public TrackingStatus(final String branchName) {
        this(branchName, 0, 0);
    }

    public TrackingStatus(final String branchName,
                          final int commitsAhead,
                          final int commitsBehind) {
        this.branchName = branchName;
        this.commitsAhead = commitsAhead;
        this.commitsBehind = commitsBehind;
    }

    public int getCommitsAhead() {
        return commitsAhead;
    }

    public int getCommitsBehind() {
        return commitsBehind;
    }

    @Override
    public String toString() {
        return "TrackingStatus{" +
                "branchName='" + branchName + '\'' +
                ", commitsAhead=" + commitsAhead +
                ", commitsBehind=" + commitsBehind +
                '}';
    }
}
