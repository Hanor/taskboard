package objective.taskboard.followup;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import objective.taskboard.followup.FollowUpSnapshot.SnapshotRow;
import objective.taskboard.followup.cluster.ClusterNotConfiguredException;

@Component
public class PlannedVsBallparkCalculator {

    private final FollowUpSnapshotService snapshotService;
    
    @Autowired
    public PlannedVsBallparkCalculator(FollowUpSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    public List<PlannedVsBallparkChartData> calculate(String projectKey) throws ClusterNotConfiguredException {
        FollowUpSnapshot snapshot = snapshotService.getFromCurrentState(ZoneId.systemDefault(), projectKey);

        if (!snapshot.hasClusterConfiguration())
            throw new ClusterNotConfiguredException();

        PlannedVsBallparkChartData plannedChartData = new PlannedVsBallparkChartData("Planned", 0);
        PlannedVsBallparkChartData ballparkChartData = new PlannedVsBallparkChartData("Ballpark", 0);

        for (SnapshotRow snapshotRow : snapshot.getSnapshotRows()) {
            double effortEstimate = snapshotRow.calcutatedData.getEffortEstimate();
            if (FromJiraDataRow.QUERY_TYPE_SUBTASK_PLAN.equals(snapshotRow.rowData.queryType)) {
                plannedChartData.totalEffort += effortEstimate;
            } else {
                ballparkChartData.totalEffort += effortEstimate;
            }
        }

        return Arrays.asList(plannedChartData, ballparkChartData);
    }
}