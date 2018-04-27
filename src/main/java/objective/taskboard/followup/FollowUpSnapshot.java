package objective.taskboard.followup;

import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import objective.taskboard.followup.FromJiraRowCalculator.FromJiraRowCalculation;
import objective.taskboard.followup.ReleaseHistoryProvider.ProjectRelease;
import objective.taskboard.followup.cluster.FollowupCluster;

public class FollowUpSnapshot {
    private final FollowUpTimeline timeline;
    private final FollowUpData followupData;
    private final FollowupCluster followupCluster;
    private final FromJiraRowCalculator rowCalculator;
    private final FollowUpSnapshotValuesProvider valuesProvider;
    
    private List<EffortHistoryRow> effortHistory;
    private List<ProjectRelease> releases;
    private Optional<FollowUpData> scopeBaseline;

    public FollowUpSnapshot(FollowUpTimeline timeline, FollowUpData followupData, FollowupCluster followupCluster, FollowUpSnapshotValuesProvider valuesProvider) {
        this.timeline = timeline;
        this.followupData = followupData;
        this.followupCluster = followupCluster;
        this.valuesProvider = valuesProvider;
        this.rowCalculator = new FromJiraRowCalculator(followupCluster);
    }

    public FollowUpTimeline getTimeline() {
        return timeline;
    }

    public FollowUpData getData() {
        return followupData;
    }

    public List<EffortHistoryRow> getEffortHistory() {
        if (effortHistory == null)
            buildEffortHistory();
        
        return effortHistory;
    }
    
    public List<ProjectRelease> getReleases() {
        if (releases == null)
            releases = unmodifiableList(valuesProvider.getReleases());

        return releases;
    }
    
    public Optional<FollowUpData> getScopeBaseline() {
        if (scopeBaseline == null)
            scopeBaseline = valuesProvider.getScopeBaseline();
        
        return scopeBaseline;
    }

    public List<SnapshotRow> getSnapshotRows() {
        return followupData.fromJiraDs.rows.stream()
                .map(row -> new SnapshotRow(row, rowCalculator.calculate(row)))
                .collect(toList());
    }

    public static class SnapshotRow {
        public final FromJiraDataRow rowData;
        public final FromJiraRowCalculation calcutatedData;

        public SnapshotRow(FromJiraDataRow row, FromJiraRowCalculation calculate) {
            this.rowData = row;
            this.calcutatedData = calculate;
        }
    }

    public EffortHistoryRow getEffortHistoryRow() {
        FromJiraRowCalculation sumCalculation = rowCalculator.summarize(getData().fromJiraDs.rows);
        return EffortHistoryRow.from(getTimeline().getReference(), sumCalculation);
    }

    public boolean hasClusterConfiguration() {
        return !followupCluster.isEmpty();
    }

    public FollowupCluster getCluster() {
        return followupCluster;
    }

    private void buildEffortHistory() {
        List<EffortHistoryRow> result = new ArrayList<>(valuesProvider.getEffortHistory());
        result.add(getEffortHistoryRow());
        Collections.sort(result, comparing(r -> r.date));

        this.effortHistory = unmodifiableList(result);
    }
}