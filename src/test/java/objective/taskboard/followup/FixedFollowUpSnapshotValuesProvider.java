package objective.taskboard.followup;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import objective.taskboard.followup.ReleaseHistoryProvider.ProjectRelease;

class FixedFollowUpSnapshotValuesProvider implements FollowUpSnapshotValuesProvider {

    private final List<EffortHistoryRow> effortHistory;
    private final List<ProjectRelease> releases;
    private final Optional<FollowUpData> scopeBaseline;

    public FixedFollowUpSnapshotValuesProvider(List<EffortHistoryRow> effortHistory, List<ProjectRelease> releases, FollowUpData scopeBaseline) {
        this.effortHistory = effortHistory;
        this.releases = releases;
        this.scopeBaseline = Optional.ofNullable(scopeBaseline);
    }
    
    public FixedFollowUpSnapshotValuesProvider(List<EffortHistoryRow> effortHistory) {
        this(effortHistory, Collections.emptyList(), null);
    }
    
    public FixedFollowUpSnapshotValuesProvider() {
        this(Collections.emptyList());
    }
    
    public static FollowUpSnapshotValuesProvider emptyValuesProvider() {
        return new FixedFollowUpSnapshotValuesProvider();
    }

    @Override
    public List<EffortHistoryRow> getEffortHistory() {
        return effortHistory;
    }

    @Override
    public List<ProjectRelease> getReleases() {
        return releases;
    }

    @Override
    public Optional<FollowUpData> getScopeBaseline() {
        return scopeBaseline;
    }
}
