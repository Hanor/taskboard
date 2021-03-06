package objective.taskboard.followup;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import objective.taskboard.followup.FromJiraRowCalculator.FromJiraRowCalculation;

public class FollowUpDataSnapshot {
    private final FollowUpTimeline timeline;
    private final FollowupData followupData;
    private FollowUpDataSnapshotHistory history;
    private FollowupCluster followupCluster;

    public FollowUpDataSnapshot(FollowUpTimeline timeline, FollowupData followupData, FollowUpDataSnapshotHistory history, FollowupCluster followupCluster) {
        this.timeline = timeline;
        this.followupData = followupData;
        this.history = history;
        this.followupCluster = followupCluster;
    }

    public FollowUpDataSnapshot(FollowUpTimeline timeline, FollowupData followupData, FollowupCluster followupCluster) {
        this(timeline, followupData, null, followupCluster);
    }
    
    public FollowUpTimeline getTimeline() {
        return timeline;
    }

    public FollowupData getData() {
        return followupData;
    }

    public Optional<FollowUpDataSnapshotHistory> getHistory() {
        return Optional.ofNullable(history);
    }
    
    public Optional<EffortHistoryRow> getLatestEffortRow() {
        Optional<FollowUpDataSnapshotHistory> optHistory = getHistory();
        if (!optHistory.isPresent())
            return Optional.empty();
        
        List<EffortHistoryRow> historyRows = optHistory.get().getHistoryRows();
        return Optional.of(historyRows.get(historyRows.size()-1));
    }

    public void setFollowUpDataEntryHistory(FollowUpDataSnapshotHistory history) {
        this.history = history;
    }
    
    public void forEachRow(Consumer<SnapshotRow> consumer) {
        final FromJiraRowCalculator calculator = getCalculator();
                
        followupData.fromJiraDs.rows.forEach(row -> {
            consumer.accept(new SnapshotRow(row, calculator.calculate(row)));
        });
    }

    private FromJiraRowCalculator getCalculator() {
        return new FromJiraRowCalculator(followupCluster);
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
        FromJiraRowCalculator rowCalculator = getCalculator();
        EffortHistoryRow historyRow = new EffortHistoryRow(getTimeline().getReference());

        getData().fromJiraDs.rows.stream().forEach(fromJiraRow -> {
            FromJiraRowCalculation fromJiraRowCalculation = rowCalculator.calculate(fromJiraRow);

            historyRow.sumEffortDone    += fromJiraRowCalculation.getEffortDone();
            historyRow.sumEffortBacklog += fromJiraRowCalculation.getEffortOnBacklog();
        });

        return historyRow;
    }

    public boolean hasClusterConfiguration() {
        return followupCluster !=null && !followupCluster.isEmpty();
    }

    public FollowupCluster getCluster() {
        return followupCluster;
    }
}