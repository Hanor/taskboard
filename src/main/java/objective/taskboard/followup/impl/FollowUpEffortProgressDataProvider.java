package objective.taskboard.followup.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import objective.taskboard.followup.EffortProgressDataRow;
import objective.taskboard.followup.EffortProgressDataSet;
import objective.taskboard.followup.FromJiraDataRow;
import objective.taskboard.followup.FromJiraDataSet;
import objective.taskboard.followup.TshirtSizeDataRow;

public class FollowUpEffortProgressDataProvider {

    public static EffortProgressDataSet getEffortProgressDs(String date, FromJiraDataSet selectedFromJiraDs) {
        EffortProgressDataSet EffortProgressDs = new EffortProgressDataSet();
        EffortProgressDs.headers = getEffortProgressDsHeaders();
        EffortProgressDs.rows = getEffortProgressDsRows(date, selectedFromJiraDs);
        return EffortProgressDs;
    }

    private static List<String> getEffortProgressDsHeaders() {
        return Arrays.asList("Date", "effortDoneTotal", "effortBacklogTotal");
    }

    private static List<EffortProgressDataRow> getEffortProgressDsRows(String date, FromJiraDataSet selectedFromJiraDs) {
        List<FromJiraDataSet> fromJiraDataSets = getFromJiraDataSets(selectedFromJiraDs);
        List<TshirtSizeDataRow> tshirtSizeDataRows = getTshirtSizeDataRowsFromTemplate();
        List<EffortProgressDataRow> rows = new ArrayList<>();

        for (FromJiraDataSet fromJiraDataDs : fromJiraDataSets) {
            Optional<EffortProgressDataRow> newRow = getEffortProgressDsRow(date, fromJiraDataDs, tshirtSizeDataRows);
            if (newRow.isPresent())
                rows.add(newRow.get());
        }

        return rows;
    }

    private static Optional<EffortProgressDataRow> getEffortProgressDsRow(String date, FromJiraDataSet fromJiraDs, List<TshirtSizeDataRow> tshirtSizeDataRows) {
        if (fromJiraDs.rows.size() == 0)
            return Optional.empty();

        Double effortDoneTotal = 0.00;
        Double effortBacklogTotal = 0.00;

        for (FromJiraDataRow fromJiraDataRow : fromJiraDs.rows) {
            Double effortEstimate = getEffortEstimate(fromJiraDataRow, tshirtSizeDataRows);
            Double effortDone = getEffortDone(fromJiraDataRow, effortEstimate);

            effortDoneTotal += effortDone;
            effortBacklogTotal += getEffortBacklog(effortEstimate, effortDone);
        }
        EffortProgressDataRow newRow = new EffortProgressDataRow(date, effortDoneTotal, effortBacklogTotal);
        return Optional.of(newRow);
    }

    private static List<FromJiraDataSet> getFromJiraDataSets(FromJiraDataSet selectedFromJiraDs) {
        List<FromJiraDataSet> fromJiraDataSets = new ArrayList<>();

        fromJiraDataSets.add(selectedFromJiraDs);

        return fromJiraDataSets; 
    }
    
    private static Double getEffortBacklog(Double effortEstimate, Double effortDone) {
        // EffortOnBacklog
        /*
         *  AllIssues[EffortEstimate] - AllIssues[EffortDone]
         */
        return effortEstimate - effortDone;
    }

    private static Double getEffortDone(FromJiraDataRow fromJiraDataRows, Double effortEstimate) {
        // EffortDone
        /*
         * SE (AllIssues[SUBTASK_STATUS]="Done" || AllIssues[SUBTASK_STATUS]="Cancelled"))
                AllIssues[EffortEstimate]
           SENAO
                0
         */
        if ("Done".equals(fromJiraDataRows.subtaskStatus) || "Cancelled".equals(fromJiraDataRows.subtaskStatus))
            return effortEstimate;
        return 0.00;
    }


    private static Double getEffortEstimate(FromJiraDataRow fromJiraDataRow, List<TshirtSizeDataRow> tshirtSizeDataRows) {
        // EffortEstimate
        /*
         *  SE ( AllIssues[[#Esta linha];[TASK_BALLPARK]] > 0 )
         *      AllIssues[[#Esta linha];[TASK_BALLPARK]];
         *  SENAO
         *      SOMASES(
         *          Intervalo_soma:Clusters[Effort];
         *          
         *          Intervalo_criterio1: Clusters[Cluster Name];
         *          critério1: AllIssues[[#Esta linha];[SUBTASK_TYPE]];
         *          
         *          Intervalo_criterio2: Clusters[T-Shirt Size];
         *          Criterio2: AllIssues[tshirt_size]
         *      )
         *  
         **/

        if (fromJiraDataRow.taskBallpark > 0)
            return fromJiraDataRow.taskBallpark;

        Optional<TshirtSizeDataRow> row = tshirtSizeDataRows.stream()
                .filter(r -> fromJiraDataRow.subtaskType.equals(r.clusterName) && fromJiraDataRow.tshirtSize.equals(r.tshirtSize))
                .findFirst();
        if (!row.isPresent())
            throw new IllegalArgumentException("Cluster name '" + fromJiraDataRow.subtaskType + "' with  T-Shirt Size '" + fromJiraDataRow.tshirtSize + "' doesn't exists.");
        return row.get().effort;
    }

    private static List<TshirtSizeDataRow> getTshirtSizeDataRowsFromTemplate() {
        List<TshirtSizeDataRow> tshirtSizeDataRows = new ArrayList<>();
        tshirtSizeDataRows.add(new TshirtSizeDataRow("Sub-Task", "XS"   , "Hours", 4.00, 4.80));
        tshirtSizeDataRows.add(new TshirtSizeDataRow("Sub-Task", "S"    , "Hours", 8.00, 9.60));
        tshirtSizeDataRows.add(new TshirtSizeDataRow("Sub-Task", "M"    , "Hours", 16.00, 19.20));
        tshirtSizeDataRows.add(new TshirtSizeDataRow("Sub-Task", "L"    , "Hours", 32.00, 38.40));
        tshirtSizeDataRows.add(new TshirtSizeDataRow("Sub-Task", "XL"   , "Hours", 64.00, 76.80));
        return tshirtSizeDataRows;
    }

}
