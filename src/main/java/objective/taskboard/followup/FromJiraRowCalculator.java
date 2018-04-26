package objective.taskboard.followup;

import static objective.taskboard.followup.FromJiraDataRow.QUERY_TYPE_SUBTASK_PLAN;

import java.util.Collection;
import java.util.Optional;

import objective.taskboard.followup.cluster.FollowUpClusterItem;

public class FromJiraRowCalculator {

    private final FollowupCluster followupCluster;

    public FromJiraRowCalculator(FollowupCluster cluster) {
        this.followupCluster = cluster;
    }

    public FromJiraRowCalculation calculate(FromJiraDataRow row) {
        Optional<FollowUpClusterItem> clusterItem = followupCluster.getClusterFor(row.subtaskType, row.tshirtSize); 
        
        FromJiraRowCalculation result = new FromJiraRowCalculation();
        
        fillEffortEstimate(row, clusterItem, result);
        fillEffortDone(row, result);
        fillEffortOnBacklog(result);

        return result;
    }
    
    public FromJiraRowCalculation summarize(Collection<FromJiraDataRow> rows) {
        FromJiraRowCalculation sumResult = new FromJiraRowCalculation();
        
        for (FromJiraDataRow row : rows) {
            FromJiraRowCalculation rowResult = calculate(row);
            
            sumResult.effortEstimate   += rowResult.effortEstimate;
            sumResult.effortDone       += rowResult.effortDone;
            sumResult.effortOnBacklog  += rowResult.effortOnBacklog;
        }
        
        return sumResult;
    }

    private void fillEffortEstimate(FromJiraDataRow row, Optional<FollowUpClusterItem> clusterItem, FromJiraRowCalculation result) {
        if (row.taskBallpark != null && row.taskBallpark > 0 && !QUERY_TYPE_SUBTASK_PLAN.equals(row.queryType)) {
            result.effortEstimate = row.taskBallpark;
        } else {
            result.effortEstimate = clusterItem.map(i -> i.getEffort()).orElse(0d);
        }
    }

    private void fillEffortDone(FromJiraDataRow row, FromJiraRowCalculation result) {
        if ("Done".equals(row.subtaskStatus) || "Cancelled".equals(row.subtaskStatus)) {
            result.effortDone = result.effortEstimate;
        } else {
            result.effortDone = 0d;
        }
    }
    
    private void fillEffortOnBacklog(FromJiraRowCalculation result) {
        result.effortOnBacklog = result.effortEstimate - result.effortDone;
    }

    
    public static class FromJiraRowCalculation {
        
        public FromJiraRowCalculation(double effortEstimate, double effortDone, double effortOnBacklog) {
            this.effortEstimate = effortEstimate;
            this.effortDone = effortDone;
            this.effortOnBacklog = effortOnBacklog;
        }
        
        public FromJiraRowCalculation() {
        }

        private double effortEstimate = 0;
        private double effortDone = 0;
        private double effortOnBacklog = 0;

        public double getEffortEstimate() {
            return effortEstimate;
        }

        public double getEffortDone() {
            return effortDone;
        }

        public double getEffortOnBacklog() {
            return effortOnBacklog;
        }
    }
}
