package objective.taskboard.followup;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import objective.taskboard.followup.FromJiraRowCalculator.FromJiraRowCalculation;

public class FollowUpDataSnapshot {
    private final FollowUpTimeline timeline;
    private final FollowupData followupData;
    private final FollowupCluster followupCluster;
    private final List<EffortHistoryRow> effortHistory;
    private final FromJiraRowCalculator rowCalculator;

    public FollowUpDataSnapshot(FollowUpTimeline timeline, FollowupData followupData, FollowupCluster followupCluster, List<EffortHistoryRow> effortHistory) {
        this.timeline = timeline;
        this.followupData = followupData;
        this.followupCluster = followupCluster;
        this.rowCalculator = new FromJiraRowCalculator(followupCluster);
        this.effortHistory = buildEffortHistory(effortHistory);
    }

    public FollowUpTimeline getTimeline() {
        return timeline;
    }

    public FollowupData getData() {
        return followupData;
    }

    public List<EffortHistoryRow> getEffortHistory() {
        return effortHistory;
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

    private List<EffortHistoryRow> buildEffortHistory(List<EffortHistoryRow> originalEffortHistory) {
        List<EffortHistoryRow> result = new ArrayList<>(originalEffortHistory);
        result.add(getEffortHistoryRow());

        Collections.sort(result, comparing(r -> r.date));
        return Collections.unmodifiableList(result);
    }
}