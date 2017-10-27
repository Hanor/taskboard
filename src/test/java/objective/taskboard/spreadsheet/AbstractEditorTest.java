package objective.taskboard.spreadsheet;

import objective.taskboard.followup.FollowUpTemplate;
import objective.taskboard.followup.FromJiraDataRow;
import objective.taskboard.utils.DateTimeUtils;
import objective.taskboard.utils.IOUtilities;
import org.springframework.core.io.Resource;

public abstract class AbstractEditorTest {

    private static final String PATH_FOLLOWUP_TEMPLATE = "followup/Followup-template.xlsm";

    protected static FollowUpTemplate getBasicTemplate() {
        return new FollowUpTemplate(resolve(PATH_FOLLOWUP_TEMPLATE));
    }

    private static Resource resolve(String resourceName) {
        return IOUtilities.asResource(SimpleSpreadsheetEditorTest.class.getClassLoader().getResource(resourceName));
    }

    protected void addRow(SimpleSpreadsheetEditor.Sheet sheet, FromJiraDataRow followUpData) {
        SimpleSpreadsheetEditor.SheetRow row = sheet.createRow();

        row.addColumn(followUpData.project);
        row.addColumn(followUpData.demandType);
        row.addColumn(followUpData.demandStatus);
        row.addColumn(followUpData.demandNum);
        row.addColumn(followUpData.demandSummary);
        row.addColumn(followUpData.demandDescription);
        row.addColumn(followUpData.taskType);
        row.addColumn(followUpData.taskStatus);
        row.addColumn(followUpData.taskNum);
        row.addColumn(followUpData.taskSummary);
        row.addColumn(followUpData.taskDescription);
        row.addColumn(followUpData.taskFullDescription);
        row.addColumn(followUpData.subtaskType);
        row.addColumn(followUpData.subtaskStatus);
        row.addColumn(followUpData.subtaskNum);
        row.addColumn(followUpData.subtaskSummary);
        row.addColumn(followUpData.subtaskDescription);
        row.addColumn(followUpData.subtaskFullDescription);
        row.addColumn(followUpData.demandId);
        row.addColumn(followUpData.taskId);
        row.addColumn(followUpData.subtaskId);
        row.addColumn(followUpData.planningType);
        row.addColumn(followUpData.taskRelease);
        row.addColumn(followUpData.worklog);
        row.addColumn(followUpData.wrongWorklog);
        row.addColumn(followUpData.demandBallpark);
        row.addColumn(followUpData.taskBallpark);
        row.addColumn(followUpData.tshirtSize);
        row.addColumn(followUpData.queryType);
        row.addFormula("SUMIFS(Clusters[Effort],Clusters[Cluster Name],AllIssues[[#This Row],[SUBTASK_TYPE]],Clusters[T-Shirt Size],AllIssues[tshirt_size])");
        row.addFormula("SUMIFS(Clusters[Cycle],Clusters[Cluster Name],AllIssues[[#This Row],[SUBTASK_TYPE]],Clusters[T-Shirt Size],AllIssues[tshirt_size])");
        row.addFormula("AllIssues[EffortEstimate]-AllIssues[EffortDone]");
        row.addFormula("AllIssues[CycleEstimate]-AllIssues[CycleDone]");
        row.addFormula("IF(AllIssues[[#This Row],[planning_type]]=\"Ballpark\",AllIssues[EffortEstimate],0)");
        row.addFormula("IF(AllIssues[[#This Row],[planning_type]]=\"Plan\",AllIssues[EffortEstimate],0)");
        row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),AllIssues[EffortEstimate],0)");
        row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),AllIssues[CycleEstimate],0)");
        row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),AllIssues[worklog],0)");
        row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),0, AllIssues[worklog])");
        row.addFormula("IF(COUNTIFS(AllIssues[TASK_ID],AllIssues[TASK_ID],AllIssues[TASK_ID],\">0\")=0,0,1/COUNTIFS(AllIssues[TASK_ID],AllIssues[TASK_ID],AllIssues[TASK_ID],\">0\"))");
        row.addFormula("IF(COUNTIFS(AllIssues[demand_description],AllIssues[demand_description])=0,0,1/COUNTIFS(AllIssues[demand_description],AllIssues[demand_description]))");
        row.addFormula("IF(AllIssues[planning_type]=\"Plan\",1,0)");
        row.addFormula("IF(AllIssues[[#This Row],[SUBTASK_STATUS]]=\"Done\", AllIssues[[#This Row],[EffortDone]],0)");
        row.addFormula("IF(AllIssues[TASK_TYPE]=\"Bug\",AllIssues[EffortEstimate], 0)");
        row.addFormula("IF(AllIssues[TASK_TYPE]=\"Bug\",AllIssues[worklog],0)");
        row.addColumn(1L);
        row.addColumn(2);
        row.addColumn(3D);
        row.addColumn(DateTimeUtils.parseDate("2017-01-01"));

        row.save();
    }
}
