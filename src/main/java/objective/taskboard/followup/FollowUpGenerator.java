/*-
 * [LICENSE]
 * Taskboard
 * ---
 * Copyright (C) 2015 - 2017 Objective Solutions
 * ---
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * [/LICENSE]
 */
package objective.taskboard.followup;

import static objective.taskboard.followup.impl.FollowUpTransitionsDataProvider.TYPE_DEMAND;
import static objective.taskboard.followup.impl.FollowUpTransitionsDataProvider.TYPE_FEATURES;
import static objective.taskboard.followup.impl.FollowUpTransitionsDataProvider.TYPE_SUBTASKS;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.Resource;

import objective.taskboard.jira.JiraProperties;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor.Sheet;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor.SheetRow;
import objective.taskboard.utils.IOUtilities;

public class FollowUpGenerator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FollowUpGenerator.class);

    private final FollowupDataProvider provider;

	private SimpleSpreadsheetEditor editor;

    private JiraProperties jiraProperties;

    public FollowUpGenerator(FollowupDataProvider provider, SimpleSpreadsheetEditor editor, JiraProperties jiraProperties) {
        this.provider = provider;
        this.editor = editor;
        this.jiraProperties = jiraProperties;
    }

    public Resource generate(String [] includedProjects, ZoneId timezone) {
        try {
            editor.open();

            FollowupData followupData = provider.getJiraData(includedProjects, timezone);

            generateFromJiraSheet(followupData);
            generateTransitionsSheets(followupData);

            return IOUtilities.asResource(editor.toBytes());
        } catch (Exception e) {
            log.error(e.getMessage() == null ? e.toString() : e.getMessage());
            throw e;
        } finally {
            editor.close();
        }
    }

    Sheet generateFromJiraSheet(FollowupData followupData) {
        Sheet sheet = editor.getSheet("From Jira");
        sheet.truncate(0);

        SheetRow rowHeader = sheet.createRow();
        for (String header : followupData.fromJiraDs.headers)
            rowHeader.addColumn(header);
        for (String statusDemand : jiraProperties.getStatusPriorityOrder().getDemandsInOrder())
            rowHeader.addColumn(TYPE_DEMAND + " - " + statusDemand);
        for (String statusTask : jiraProperties.getStatusPriorityOrder().getTasksInOrder())
            rowHeader.addColumn(TYPE_FEATURES + " - " + statusTask);
        for (String statusSubtask : jiraProperties.getStatusPriorityOrder().getSubtasksInOrder())
            rowHeader.addColumn(TYPE_SUBTASKS + " - " + statusSubtask);
        rowHeader.save();

        for (FromJiraDataRow fromJiraDataRow : followupData.fromJiraDs.rows) {
            SheetRow row = sheet.createRow();

            row.addColumn(fromJiraDataRow.project);
            row.addColumn(fromJiraDataRow.demandType);
            row.addColumn(fromJiraDataRow.demandStatus);
            row.addColumn(fromJiraDataRow.demandNum);
            row.addColumn(fromJiraDataRow.demandSummary);
            row.addColumn(fromJiraDataRow.demandDescription);
            row.addColumn(fromJiraDataRow.taskType);
            row.addColumn(fromJiraDataRow.taskStatus);
            row.addColumn(fromJiraDataRow.taskNum);
            row.addColumn(fromJiraDataRow.taskSummary);
            row.addColumn(fromJiraDataRow.taskDescription);
            row.addColumn(fromJiraDataRow.taskFullDescription);
            row.addColumn(fromJiraDataRow.subtaskType);
            row.addColumn(fromJiraDataRow.subtaskStatus);
            row.addColumn(fromJiraDataRow.subtaskNum);
            row.addColumn(fromJiraDataRow.subtaskSummary);
            row.addColumn(fromJiraDataRow.subtaskDescription);
            row.addColumn(fromJiraDataRow.subtaskFullDescription);
            row.addColumn(fromJiraDataRow.demandId);
            row.addColumn(fromJiraDataRow.taskId);
            row.addColumn(fromJiraDataRow.subtaskId);
            row.addColumn(fromJiraDataRow.planningType);
            row.addColumn(fromJiraDataRow.taskRelease);
            row.addColumn(fromJiraDataRow.worklog);
            row.addColumn(fromJiraDataRow.wrongWorklog);
            row.addColumn(fromJiraDataRow.demandBallpark);
            row.addColumn(fromJiraDataRow.taskBallpark);
            row.addColumn(fromJiraDataRow.tshirtSize);
            row.addColumn(fromJiraDataRow.queryType);
            row.addFormula("IF(AllIssues[[#This Row],[TASK_BALLPARK]]>0,AllIssues[[#This Row],[TASK_BALLPARK]],SUMIFS(Clusters[Effort],Clusters[Cluster Name],AllIssues[[#This Row],[SUBTASK_TYPE]],Clusters[T-Shirt Size],AllIssues[tshirt_size]))");
            row.addFormula("IF(AllIssues[[#This Row],[TASK_BALLPARK]]>0,AllIssues[[#This Row],[TASK_BALLPARK]]*1.3,SUMIFS(Clusters[Cycle],Clusters[Cluster Name],AllIssues[[#This Row],[SUBTASK_TYPE]],Clusters[T-Shirt Size],AllIssues[tshirt_size]))");
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

            addTransitionsDates(followupData.analyticsTransitionsDsList, TYPE_DEMAND, fromJiraDataRow.demandNum, row,
                    jiraProperties.getStatusPriorityOrder().getDemandsInOrder().length);
            addTransitionsDates(followupData.analyticsTransitionsDsList, TYPE_FEATURES, fromJiraDataRow.taskNum, row,
                    jiraProperties.getStatusPriorityOrder().getTasksInOrder().length);
            addTransitionsDates(followupData.analyticsTransitionsDsList, TYPE_SUBTASKS, fromJiraDataRow.subtaskNum, row,
                    jiraProperties.getStatusPriorityOrder().getSubtasksInOrder().length);

            row.save();
        }
        sheet.save();

        return sheet;
    }

    private void addTransitionsDates(List<AnalyticsTransitionsDataSet> analyticsTransitionsDsList, String type,
            String issueKey, SheetRow row, int statusCount) {
        if (analyticsTransitionsDsList == null)
            return;

        Optional<AnalyticsTransitionsDataSet> transitionsDS = analyticsTransitionsDsList.stream()
            .filter(dataset -> type.equals(dataset.issueType))
            .findFirst();

        if (!transitionsDS.isPresent())
            return;

        Optional<AnalyticsTransitionsDataRow> transitionsRowIssue = transitionsDS.get().rows.stream()
            .filter(r -> r.issueKey.equals(issueKey))
            .findFirst();

        if (transitionsRowIssue.isPresent()) {
            transitionsRowIssue.get().transitionsDates.stream()
                .forEach(td -> row.addColumn(td));
        } else {
            ZonedDateTime dateNull = null;
            for (int i = 0; i < statusCount; i++)
                row.addColumn(dateNull);
        }
    }

    List<Sheet> generateTransitionsSheets(FollowupData followupData) {
        List<Sheet> sheets = new LinkedList<>();
        sheets.addAll(generateAnalyticTransitionsSheets(followupData.analyticsTransitionsDsList));
        sheets.addAll(generateSyntheticTransitionsSheets(followupData.syntheticsTransitionsDsList));
        return sheets;
    }

    private List<Sheet> generateAnalyticTransitionsSheets(List<AnalyticsTransitionsDataSet> analyticTransitionDataSets) {
        List<Sheet> sheets = new LinkedList<>();
        for (AnalyticsTransitionsDataSet analyticTransitionDataSet : analyticTransitionDataSets) {
            if (analyticTransitionDataSet.rows.isEmpty())
                continue;

            Sheet sheet = createSheetWithHeader("Analytic - ", analyticTransitionDataSet);

            for (AnalyticsTransitionsDataRow analyticTransitionDataRow : analyticTransitionDataSet.rows) {
                SheetRow row = sheet.createRow();
                row.addColumn(analyticTransitionDataRow.issueKey);
                row.addColumn(analyticTransitionDataRow.issueType);
                for (ZonedDateTime transitionDate : analyticTransitionDataRow.transitionsDates)
                    row.addColumn(transitionDate);
                row.save();
            }

            sheet.save();
            sheets.add(sheet);
        }
        return sheets;
    }

    private List<Sheet> generateSyntheticTransitionsSheets(List<SyntheticTransitionsDataSet> syntheticTransitionDataSets) {
        List<Sheet> sheets = new LinkedList<>();
        for (SyntheticTransitionsDataSet syntheticTransitionDataSet : syntheticTransitionDataSets) {
            if (syntheticTransitionDataSet.rows.isEmpty())
                continue;

            Sheet sheet = createSheetWithHeader("Synthetic - ", syntheticTransitionDataSet);

            for (SyntheticTransitionsDataRow syntheticTransitionDataRow : syntheticTransitionDataSet.rows) {
                SheetRow row = sheet.createRow();
                row.addColumn(syntheticTransitionDataRow.date);
                for (Integer amountOfIssue : syntheticTransitionDataRow.amountOfIssueInStatus)
                    row.addColumn(amountOfIssue);
                row.save();
            }

            sheet.save();
            sheets.add(sheet);
        }
        return sheets;
    }

    private Sheet createSheetWithHeader(String prefixSheetName, TransitionDataSet<? extends TransitionDataRow> transitionDataSet) {
        Sheet sheet = editor.getOrCreateSheet(prefixSheetName + transitionDataSet.issueType);
        sheet.truncate(0);
        SheetRow rowHeader = sheet.createRow();
        for (String header : transitionDataSet.headers)
            rowHeader.addColumn(header);
        rowHeader.save();
        return sheet;
    }

    public SimpleSpreadsheetEditor getEditor() {
        return editor;
    }
}
