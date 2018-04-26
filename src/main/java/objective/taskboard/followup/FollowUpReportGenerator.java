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

import static java.util.Optional.empty;
import static objective.taskboard.followup.FollowUpTransitionsDataProvider.TYPE_DEMAND;
import static objective.taskboard.followup.FollowUpTransitionsDataProvider.TYPE_FEATURES;
import static objective.taskboard.followup.FollowUpTransitionsDataProvider.TYPE_SUBTASKS;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.Resource;

import objective.taskboard.followup.cluster.FollowUpClusterItem;
import objective.taskboard.followup.cluster.FollowupCluster;
import objective.taskboard.spreadsheet.Sheet;
import objective.taskboard.spreadsheet.SheetRow;
import objective.taskboard.spreadsheet.SpreadsheetEditor;
import objective.taskboard.utils.DateTimeUtils;
import objective.taskboard.utils.IOUtilities;

public class FollowUpReportGenerator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FollowUpReportGenerator.class);

    private final SpreadsheetEditor editor;

    public FollowUpReportGenerator(SpreadsheetEditor editor) {
        this.editor = editor;
    }

    public Resource generate(FollowUpSnapshot snapshot, ZoneId timezone) throws IOException {
        try {
            editor.open();

            FollowUpData followupData = snapshot.getData();

            updateTimelineDates(snapshot.getTimeline());
            generateFromJiraSheet(followupData);
            generateTransitionsSheets(followupData);
            generateEffortHistory(snapshot);
            generateTShirtSizeSheet(snapshot);
            generateWorklogSheet(followupData, timezone);

            return IOUtilities.asResource(editor.toBytes());
        } catch (Exception e) {
            log.error(e.getMessage() == null ? e.toString() : e.getMessage());
            throw e;
        } finally {
            editor.close();
        }
    }

    void updateTimelineDates(FollowUpTimeline timeline) {
        Sheet sheet = editor.getSheet("Timeline");
        timeline.getStart().ifPresent(start -> sheet.getOrCreateRow(2).setValue("B", start));
        timeline.getEnd().ifPresent(end -> sheet.getOrCreateRow(5).setValue("B", end));
        sheet.getOrCreateRow(6).setValue("B", timeline.getReference());
        sheet.save();
    }

    void generateWorklogSheet(FollowUpData followupData, ZoneId timezone) {
        Sheet sheet = editor.getOrCreateSheet("Worklogs");
        sheet.truncate();
        SheetRow rowHeader = sheet.createRow();
        rowHeader.addColumn("AUTHOR");
        rowHeader.addColumn("ISSUE");
        rowHeader.addColumn("STARTED");
        rowHeader.addColumn("TIMESPENT");
        
        followupData.fromJiraDs.rows.stream().forEach(row -> {
            if (row.worklogs == null) return;
            String issueKey = row.subtaskNum;
            row.worklogs.forEach(worklog -> {
                SheetRow worklogRow = sheet.createRow();
                worklogRow.addColumn(worklog.author);
                worklogRow.addColumn(issueKey);
                worklogRow.addColumn(DateTimeUtils.get(worklog.started, timezone));
                worklogRow.addColumn(worklog.timeSpentSeconds/3600.0);
            });
        });
        sheet.save();
    }

    Sheet generateFromJiraSheet(FollowUpData followupData) {
        Sheet sheet = editor.getSheet("From Jira");
        sheet.truncate();

        SheetRow rowHeader = sheet.createRow();
        for (String header : followupData.fromJiraDs.headers)
            rowHeader.addColumn(header);

        addAnalyticsHeadersIfExist(followupData.analyticsTransitionsDsList, TYPE_DEMAND, rowHeader);
        addAnalyticsHeadersIfExist(followupData.analyticsTransitionsDsList, TYPE_FEATURES, rowHeader);
        addAnalyticsHeadersIfExist(followupData.analyticsTransitionsDsList, TYPE_SUBTASKS, rowHeader);

        for (FromJiraDataRow fromJiraDataRow : followupData.fromJiraDs.rows) {
            SheetRow row = sheet.createRow();

            row.addColumn(fromJiraDataRow.project);
            row.addColumn(fromJiraDataRow.demandType);
            row.addColumn(fromJiraDataRow.demandStatus);
            row.addColumn(fromJiraDataRow.demandNum);
            row.addColumn(fromJiraDataRow.demandSummary);
            row.addColumn(fromJiraDataRow.demandDescription);
            row.addColumn(fromJiraDataRow.demandStartDateStepMillis);
            row.addColumn(fromJiraDataRow.demandAssignee);
            row.addColumn(fromJiraDataRow.demandDueDate);
            row.addColumn(fromJiraDataRow.demandCreated);
            row.addColumn(fromJiraDataRow.demandLabels);
            row.addColumn(fromJiraDataRow.demandComponents);
            row.addColumn(fromJiraDataRow.demandReporter);
            row.addColumn(fromJiraDataRow.demandCoAssignees);
            row.addColumn(fromJiraDataRow.demandClassOfService);
            row.addColumn(fromJiraDataRow.demandUpdatedDate);
            row.addColumn(fromJiraDataRow.demandCycletime);
            row.addColumn(fromJiraDataRow.demandIsBlocked);
            row.addColumn(fromJiraDataRow.demandLastBlockReason);

            row.addColumn(fromJiraDataRow.taskType);
            row.addColumn(fromJiraDataRow.taskStatus);
            row.addColumn(fromJiraDataRow.taskNum);
            row.addColumn(fromJiraDataRow.taskSummary);
            row.addColumn(fromJiraDataRow.taskDescription);
            row.addColumn(fromJiraDataRow.taskFullDescription);
            row.addColumn(fromJiraDataRow.taskAdditionalEstimatedHours);
            row.addColumn(fromJiraDataRow.taskStartDateStepMillis);
            row.addColumn(fromJiraDataRow.taskAssignee);
            row.addColumn(fromJiraDataRow.taskDueDate);
            row.addColumn(fromJiraDataRow.taskCreated);
            row.addColumn(fromJiraDataRow.taskLabels);
            row.addColumn(fromJiraDataRow.taskComponents);
            row.addColumn(fromJiraDataRow.taskReporter);
            row.addColumn(fromJiraDataRow.taskCoAssignees);
            row.addColumn(fromJiraDataRow.taskClassOfService);
            row.addColumn(fromJiraDataRow.taskUpdatedDate);
            row.addColumn(fromJiraDataRow.taskCycletime);
            row.addColumn(fromJiraDataRow.taskIsBlocked);
            row.addColumn(fromJiraDataRow.taskLastBlockReason);

            row.addColumn(fromJiraDataRow.subtaskType);
            row.addColumn(fromJiraDataRow.subtaskStatus);
            row.addColumn(fromJiraDataRow.subtaskNum);
            row.addColumn(fromJiraDataRow.subtaskSummary);
            row.addColumn(fromJiraDataRow.subtaskDescription);
            row.addColumn(fromJiraDataRow.subtaskFullDescription);
            row.addColumn(fromJiraDataRow.subtaskStartDateStepMillis);
            row.addColumn(fromJiraDataRow.subtaskAssignee);
            row.addColumn(fromJiraDataRow.subtaskDueDate);
            row.addColumn(fromJiraDataRow.subtaskCreated);
            row.addColumn(fromJiraDataRow.subtaskLabels);
            row.addColumn(fromJiraDataRow.subtaskComponents);
            row.addColumn(fromJiraDataRow.subtaskReporter);
            row.addColumn(fromJiraDataRow.subtaskCoAssignees);
            row.addColumn(fromJiraDataRow.subtaskClassOfService);
            row.addColumn(fromJiraDataRow.subtaskUpdatedDate);
            row.addColumn(fromJiraDataRow.subtaskCycletime);
            row.addColumn(fromJiraDataRow.subtaskIsBlocked);
            row.addColumn(fromJiraDataRow.subtaskLastBlockReason);

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
            //EffortEstimate
            row.addFormula("IF(AND(AllIssues[[#This Row],[TASK_BALLPARK]]>0,AllIssues[[#This Row],[Query_Type]]<>\"SUBTASK PLAN\"),AllIssues[[#This Row],[TASK_BALLPARK]],SUMIFS(Clusters[Effort],Clusters[Cluster Name],AllIssues[[#This Row],[SUBTASK_TYPE]],Clusters[T-Shirt Size],AllIssues[tshirt_size]))");
            //CycleEstimate            
            row.addFormula("IF(AND(AllIssues[[#This Row],[TASK_BALLPARK]]>0,AllIssues[[#This Row],[Query_Type]]<>\"SUBTASK PLAN\"),AllIssues[[#This Row],[TASK_BALLPARK]]*1.3,SUMIFS(Clusters[Cycle],Clusters[Cluster Name],AllIssues[[#This Row],[SUBTASK_TYPE]],Clusters[T-Shirt Size],AllIssues[tshirt_size]))");
            // EffortOnBacklog
            row.addFormula("AllIssues[EffortEstimate]-AllIssues[EffortDone]");
            // CycleOnBacklog
            row.addFormula("AllIssues[CycleEstimate]-AllIssues[CycleDone]");
            // BallparkEffort
            row.addFormula("IF(AllIssues[[#This Row],[planning_type]]=\"Ballpark\",AllIssues[EffortEstimate],0)");
            // PlannedEffort
            row.addFormula("IF(AllIssues[[#This Row],[planning_type]]=\"Plan\",AllIssues[EffortEstimate],0)");
            // EffortDone
            row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),AllIssues[EffortEstimate],0)");
            // CycleDone
            row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),AllIssues[CycleEstimate],0)");
            // WorklogDone
            row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),AllIssues[worklog],0)");
            // WorklogDoing
            row.addFormula("IF(OR(AllIssues[SUBTASK_STATUS]=\"Done\",AllIssues[SUBTASK_STATUS]=\"Cancelled\"),0, AllIssues[worklog])");
            // CountTasks
            row.addFormula("IF(COUNTIFS(AllIssues[TASK_ID],AllIssues[TASK_ID],AllIssues[TASK_ID],\">0\")=0,0,1/COUNTIFS(AllIssues[TASK_ID],AllIssues[TASK_ID],AllIssues[TASK_ID],\">0\"))");
            // CountDemands
            row.addFormula("IF(COUNTIFS(AllIssues[demand_description],AllIssues[demand_description])=0,0,1/COUNTIFS(AllIssues[demand_description],AllIssues[demand_description]))");
            // CountSubtasks
            row.addFormula("IF(AllIssues[planning_type]=\"Plan\",1,0)");
            // SubtaskEstimativeForEEPCalculation
            row.addFormula("IF(AllIssues[[#This Row],[SUBTASK_STATUS]]=\"Done\", AllIssues[[#This Row],[EffortDone]],0)");
            // PlannedEffortOnBug
            row.addFormula("IF(AllIssues[TASK_TYPE]=\"Bug\",AllIssues[EffortEstimate], 0)");
            // WorklogOnBug
            row.addFormula("IF(AllIssues[TASK_TYPE]=\"Bug\",AllIssues[worklog],0)");

            addTransitionsDatesIfExist(followupData.analyticsTransitionsDsList, TYPE_DEMAND, fromJiraDataRow.demandNum, row);
            addTransitionsDatesIfExist(followupData.analyticsTransitionsDsList, TYPE_FEATURES, fromJiraDataRow.taskNum, row);
            addTransitionsDatesIfExist(followupData.analyticsTransitionsDsList, TYPE_SUBTASKS, fromJiraDataRow.subtaskNum, row);
        }
        sheet.save();

        return sheet;
    }

    private void addAnalyticsHeadersIfExist(List<AnalyticsTransitionsDataSet> analyticsDataSets, String type, SheetRow rowHeader) {
        Optional<AnalyticsTransitionsDataSet> analyticDataSetOfType = getAnalyticDataSetWithRowByType(analyticsDataSets, type);

        if (!analyticDataSetOfType.isPresent())
            return;

        List<String> headers = analyticDataSetOfType.get().headers;
        for (int i = analyticDataSetOfType.get().getInitialIndexStatusHeaders(); i < headers.size(); i++)
            rowHeader.addColumn(analyticDataSetOfType.get().issueType + " - " + headers.get(i));
    }

    private void addTransitionsDatesIfExist(List<AnalyticsTransitionsDataSet> analyticsDataSets, String type, String issueKey, SheetRow row) {
        Optional<AnalyticsTransitionsDataSet> analyticDataSetOfType = getAnalyticDataSetWithRowByType(analyticsDataSets, type);

        if (!analyticDataSetOfType.isPresent())
            return;

        Optional<AnalyticsTransitionsDataRow> analyticRowOfIssue = analyticDataSetOfType.get().rows.stream()
            .filter(analyticRow -> analyticRow.issueKey.equals(issueKey))
            .findFirst();

        if (analyticRowOfIssue.isPresent()) {
            analyticRowOfIssue.get().transitionsDates.stream()
                .forEachOrdered(transitionDate -> row.addColumn(transitionDate));
        } else {
            ZonedDateTime dateNull = null;
            List<String> headers = analyticDataSetOfType.get().headers;
            for (int i = analyticDataSetOfType.get().getInitialIndexStatusHeaders(); i < headers.size(); i++)
                row.addColumn(dateNull);
        }
    }

    private Optional<AnalyticsTransitionsDataSet> getAnalyticDataSetWithRowByType(List<AnalyticsTransitionsDataSet> analyticsDataSets, String type) {
        if (isEmpty(analyticsDataSets))
            return empty();

        return analyticsDataSets.stream()
            .filter(dataSet -> type.equals(dataSet.issueType) && !isEmpty(dataSet.rows))
            .findFirst();
    }

    List<Sheet> generateTransitionsSheets(FollowUpData followupData) {
        List<Sheet> sheets = new LinkedList<>();
        sheets.addAll(generateAnalyticTransitionsSheets(followupData.analyticsTransitionsDsList));
        sheets.addAll(generateSyntheticTransitionsSheets(followupData.syntheticsTransitionsDsList));
        return sheets;
    }

    private List<Sheet> generateAnalyticTransitionsSheets(List<AnalyticsTransitionsDataSet> analyticTransitionDataSets) {
        List<Sheet> sheets = new LinkedList<>();

        if (isEmpty(analyticTransitionDataSets))
            return sheets;

        for (AnalyticsTransitionsDataSet analyticTransitionDataSet : analyticTransitionDataSets) {
            if (isEmpty(analyticTransitionDataSet.rows))
                continue;

            Sheet sheet = createSheetWithHeader("Analytic - ", analyticTransitionDataSet);

            for (AnalyticsTransitionsDataRow analyticTransitionDataRow : analyticTransitionDataSet.rows) {
                SheetRow row = sheet.createRow();
                row.addColumn(analyticTransitionDataRow.issueKey);
                row.addColumn(analyticTransitionDataRow.issueType);
                for (ZonedDateTime transitionDate : analyticTransitionDataRow.transitionsDates)
                    row.addColumn(transitionDate);
            }

            sheet.save();
            sheets.add(sheet);
        }
        return sheets;
    }

    private List<Sheet> generateSyntheticTransitionsSheets(List<SyntheticTransitionsDataSet> syntheticTransitionDataSets) {
        List<Sheet> sheets = new LinkedList<>();

        if (isEmpty(syntheticTransitionDataSets))
            return sheets;

        for (SyntheticTransitionsDataSet syntheticTransitionDataSet : syntheticTransitionDataSets) {
            if (isEmpty(syntheticTransitionDataSet.rows))
                continue;

            Sheet sheet = createSheetWithHeader("Synthetic - ", syntheticTransitionDataSet);

            for (SyntheticTransitionsDataRow syntheticTransitionDataRow : syntheticTransitionDataSet.rows) {
                SheetRow row = sheet.createRow();
                row.addColumn(syntheticTransitionDataRow.date);
                row.addColumn(syntheticTransitionDataRow.issueType);
                for (Integer amountOfIssue : syntheticTransitionDataRow.amountOfIssueInStatus)
                    row.addColumn(amountOfIssue);
            }

            sheet.save();
            sheets.add(sheet);
        }
        return sheets;
    }

    private Sheet createSheetWithHeader(String prefixSheetName, TransitionDataSet<? extends TransitionDataRow> transitionDataSet) {
        Sheet sheet = editor.getOrCreateSheet(prefixSheetName + transitionDataSet.issueType);
        sheet.truncate();
        SheetRow rowHeader = sheet.createRow();
        for (String header : transitionDataSet.headers)
            rowHeader.addColumn(header);

        return sheet;
    }
    
    void generateEffortHistory(FollowUpSnapshot snapshot) {
        Sheet sheet = editor.getOrCreateSheet("Effort History");
        sheet.truncate();
        
        SheetRow rowHeader = sheet.createRow();
        rowHeader.addColumn("Date");
        rowHeader.addColumn("SumEffortDone");
        rowHeader.addColumn("SumEffortBacklog");
        
        if (!snapshot.hasClusterConfiguration())
            return;

        for (EffortHistoryRow historyRow : snapshot.getEffortHistory()) {
            SheetRow row = sheet.createRow();
            row.addColumn(historyRow.date);
            row.addColumn(historyRow.sumEffortDone);
            row.addColumn(historyRow.sumEffortBacklog);
        }
        
        sheet.save();
    }

    void generateTShirtSizeSheet(FollowUpSnapshot snapshot) {
        FollowupCluster followupCluster= snapshot.getCluster();

        Sheet sheet = editor.getOrCreateSheet("T-shirt Size");
        sheet.truncate();

        SheetRow rowHeader = sheet.createRow();
        rowHeader.addColumn("Cluster Name");
        rowHeader.addColumn("T-Shirt Size");
        rowHeader.addColumn("Type");
        rowHeader.addColumn("Effort");
        rowHeader.addColumn("Cycle");
        rowHeader.addColumn("Project");
        
        if (snapshot.hasClusterConfiguration())
            for (FollowUpClusterItem cluster : followupCluster.getClusterItems()) {
                SheetRow row = sheet.createRow();
                row.addColumn(cluster.getSubtaskTypeName());
                row.addColumn(cluster.getSizing());
                row.addColumn("Hours");
                row.addColumn(cluster.getEffort());
                row.addColumn(cluster.getCycle());
                row.addColumn(cluster.getProject().getProjectKey());
            }
        sheet.save();
    }

    
    public SpreadsheetEditor getEditor() {
        return editor;
    }
}
