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
import static objective.taskboard.followup.impl.FollowUpTransitionsDataProvider.TYPE_DEMAND;
import static objective.taskboard.followup.impl.FollowUpTransitionsDataProvider.TYPE_FEATURES;
import static objective.taskboard.followup.impl.FollowUpTransitionsDataProvider.TYPE_SUBTASKS;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.Resource;

import objective.taskboard.followup.impl.FollowUpEffortProgressDataProvider;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor.Sheet;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor.SheetRow;
import objective.taskboard.utils.IOUtilities;

public class FollowUpGenerator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FollowUpGenerator.class);

    private final FollowupDataProvider provider;

	private SimpleSpreadsheetEditor editor;

    public FollowUpGenerator(FollowupDataProvider provider, SimpleSpreadsheetEditor editor) {
        this.provider = provider;
        this.editor = editor;
    }

    public Resource generate(String [] includedProjects, ZoneId timezone) {
        try {
            editor.open();

            FollowupData followupData = provider.getJiraData(includedProjects, timezone);

            generateFromJiraSheet(followupData);
            generateTransitionsSheets(followupData);
            generateEffortProgress(followupData);

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

        addAnalyticsHeadersIfExist(followupData.analyticsTransitionsDsList, TYPE_DEMAND, rowHeader);
        addAnalyticsHeadersIfExist(followupData.analyticsTransitionsDsList, TYPE_FEATURES, rowHeader);
        addAnalyticsHeadersIfExist(followupData.analyticsTransitionsDsList, TYPE_SUBTASKS, rowHeader);

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

            addTransitionsDatesIfExist(followupData.analyticsTransitionsDsList, TYPE_DEMAND, fromJiraDataRow.demandNum, row);
            addTransitionsDatesIfExist(followupData.analyticsTransitionsDsList, TYPE_FEATURES, fromJiraDataRow.taskNum, row);
            addTransitionsDatesIfExist(followupData.analyticsTransitionsDsList, TYPE_SUBTASKS, fromJiraDataRow.subtaskNum, row);

            row.save();
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

    List<Sheet> generateTransitionsSheets(FollowupData followupData) {
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
                row.save();
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

    private Sheet generateEffortProgress(FollowupData followupData) {

        EffortProgressDataSet effortProgressDs = FollowUpEffortProgressDataProvider.getEffortProgressDs(followupData.followupDataVersion, followupData.fromJiraDs);

        Sheet sheet = editor.getOrCreateSheet("Effort Progress");
        sheet.truncate(0);

        SheetRow rowHeader = sheet.createRow();
        for(String header : effortProgressDs.headers) {
            rowHeader.addColumn(header);
        }
        rowHeader.save();

        for (EffortProgressDataRow efforProgressDataRow : effortProgressDs.rows) {
            SheetRow row = sheet.createRow();
            row.addColumn(efforProgressDataRow.date);
            row.addColumn(efforProgressDataRow.effortDone);
            row.addColumn(efforProgressDataRow.effortOnBacklog);
            row.save();
        }

        sheet.save();

        return sheet;
    }

}
