package objective.taskboard.followup;

import java.util.List;
import java.util.Optional;

import objective.taskboard.followup.ReleaseHistoryProvider.ProjectRelease;

interface FollowUpSnapshotValuesProvider {

    List<EffortHistoryRow> getEffortHistory();
    List<ProjectRelease> getReleases();
    Optional<FollowUpData> getScopeBaseline();
 
}
