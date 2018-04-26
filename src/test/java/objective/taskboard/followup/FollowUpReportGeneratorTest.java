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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static objective.taskboard.Constants.FROMJIRA_HEADERS;
import static objective.taskboard.followup.FollowUpHelper.getAnalyticsTransitionsDataSetWitNoRow;
import static objective.taskboard.followup.FollowUpHelper.getDefaultAnalyticsTransitionsDataSet;
import static objective.taskboard.followup.FollowUpHelper.getDefaultFollowupData;
import static objective.taskboard.followup.FollowUpHelper.getDefaultFromJiraDataRow;
import static objective.taskboard.followup.FollowUpHelper.getDefaultFromJiraDataRowList;
import static objective.taskboard.followup.FollowUpHelper.getDefaultSyntheticTransitionsDataSet;
import static objective.taskboard.followup.FollowUpHelper.getEmptyAnalyticsTransitionsDataSet;
import static objective.taskboard.followup.FollowUpHelper.getEmptyFollowupData;
import static objective.taskboard.followup.FollowUpHelper.getEmptySyntheticTransitionsDataSet;
import static objective.taskboard.followup.FollowUpHelper.getSyntheticTransitionsDataSetWithNoRow;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;

import objective.taskboard.data.Worklog;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.followup.FollowUpReportGenerator.InvalidTableRangeException;
import objective.taskboard.followup.cluster.EmptyFollowupCluster;
import objective.taskboard.followup.cluster.FollowUpClusterItem;
import objective.taskboard.followup.cluster.FollowupCluster;
import objective.taskboard.followup.cluster.FollowupClusterImpl;
import objective.taskboard.google.SpreadsheetUtils.SpreadsheetA1Range;
import objective.taskboard.spreadsheet.Sheet;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditorMock;
import objective.taskboard.utils.DateTimeUtils;
import objective.taskboard.utils.IOUtilities;

public class FollowUpReportGeneratorTest {

    private SimpleSpreadsheetEditorMock editor = new SimpleSpreadsheetEditorMock();
    private FollowUpReportGenerator subject = new FollowUpReportGenerator(editor);
    private FollowupCluster followupCluster = mock(FollowupCluster.class);
    private LocalDate followUpDate = LocalDate.parse("2007-12-03");
    private FollowUpTimeline timeline = new FollowUpTimeline(followUpDate);

    @Test
    public void generateJiraDataSheetTest() throws IOException {
        subject.getEditor().open();
        subject.generateFromJiraSheet(getDefaultFollowupData());
        subject.getEditor().close();

        String fromJiraSheetExpected = txtResourceAsString("followup/generateJiraDataSheetTest.txt");
        assertEquals("From Jira sheet", fromJiraSheetExpected, editor.loggerString());
    }

    @Test
    public void whenGeneratingWithoutData_generatesFromJiraKeepingOnlyHeaders() throws IOException {
        subject.getEditor().open();
        subject.generateFromJiraSheet(getEmptyFollowupData());
        subject.getEditor().close();

        String fromJiraSheetExpected = txtResourceAsString("followup/emptyFromJiraWithGeneratedHeaders.txt");
        assertEquals("From Jira sheet", fromJiraSheetExpected, editor.loggerString());
    }

    @Test
    public void givenEmptyTransitions_whenGenerateFromJiraSheet_thenShouldNotGenerateTransitions() throws IOException {
        FromJiraDataSet fromJiraDS = new FromJiraDataSet(FROMJIRA_HEADERS, getDefaultFromJiraDataRowList());
        FollowUpData followupData = new FollowUpData(fromJiraDS, getEmptyAnalyticsTransitionsDataSet(),
                getEmptySyntheticTransitionsDataSet());

        subject.getEditor().open();
        subject.generateFromJiraSheet(followupData);
        subject.getEditor().close();

        String fromJiraSheetExpected = txtResourceAsString("followup/fromJiraWithNoTransitions.txt");
        assertEquals("From Jira sheet", fromJiraSheetExpected, editor.loggerString());
    }

    @Test
    public void givenNoTransitions_whenGenerateFromJiraSheet_thenShouldNotGenerateTransitions() throws IOException {
        FromJiraDataSet fromJiraDS = new FromJiraDataSet(FROMJIRA_HEADERS, getDefaultFromJiraDataRowList());
        FollowUpData followupData = new FollowUpData(fromJiraDS, null, null);

        subject.getEditor().open();
        subject.generateFromJiraSheet(followupData);
        subject.getEditor().close();

        String fromJiraSheetExpected = txtResourceAsString("followup/fromJiraWithNoTransitions.txt");
        assertEquals("From Jira sheet", fromJiraSheetExpected, editor.loggerString());
    }

    @Test
    public void givenTransitionsDatesOfOtherIssueKey_whenGenerateFromJiraSheet_thenShouldGenerateEmptyTransitionsDates() throws IOException {
        FromJiraDataSet fromJiraDS = new FromJiraDataSet(FROMJIRA_HEADERS, getDefaultFromJiraDataRowList());
        FollowUpData followupData = new FollowUpData(fromJiraDS, getAnalyticsTransitionsDataSetWitNoRow(), getSyntheticTransitionsDataSetWithNoRow());

        subject.getEditor().open();
        subject.generateFromJiraSheet(followupData);
        subject.getEditor().close();

        String fromJiraSheetExpected = txtResourceAsString("followup/fromJiraWithEmptyTransitions.txt");
        assertEquals("From Jira sheet", fromJiraSheetExpected, editor.loggerString());
    }

    @Test
    public void generateJiraDataSheetWithSomeEmptyAndNullAttributesJiraDataTest() throws IOException {
        FromJiraDataRow fromJiraDefault = getDefaultFromJiraDataRow();
        fromJiraDefault.project = "";
        fromJiraDefault.demandType = null;
        fromJiraDefault.taskId = 0L;
        fromJiraDefault.subtaskId = null;
        fromJiraDefault.worklog = 0.0;
        fromJiraDefault.wrongWorklog = null;

        FromJiraDataSet fromJiraDs = new FromJiraDataSet(FROMJIRA_HEADERS, asList(fromJiraDefault));
        FollowUpData followupData = new FollowUpData(fromJiraDs, emptyList(), emptyList());

        subject.getEditor().open();
        subject.generateFromJiraSheet(followupData);
        subject.getEditor().close();

        String fromJiraSheetExpected = txtResourceAsString("followup/generateJiraDataSheetWithSomeEmptyAndNullAttributesJiraDataTest.txt");
        assertEquals("From Jira sheet", fromJiraSheetExpected, editor.loggerString());
    }

    @Test
    public void generateTest() throws IOException {
        FollowUpTemplate testTemplate = new FollowUpTemplate(resolve("followup/Followup-template.xlsm"));
        subject = new FollowUpReportGenerator(new SimpleSpreadsheetEditor(testTemplate));

        FollowUpSnapshot snapshot = new FollowUpSnapshot(timeline, getDefaultFollowupData(), new EmptyFollowupCluster(), emptyList());

        Resource resource = subject.generate(snapshot, ZoneId.systemDefault());
        assertNotNull("Resource shouldn't be null", resource);
    }

    @Test
    public void generateUsingGenericTemplateTest() throws IOException {
        FollowUpTemplate testTemplate = new FollowUpTemplate(resolve("followup/Followup-template.xlsm"));
        subject = new FollowUpReportGenerator(new SimpleSpreadsheetEditor(testTemplate));

        FollowUpSnapshot snapshot = new FollowUpSnapshot(timeline, getDefaultFollowupData(), new EmptyFollowupCluster(), emptyList());

        Resource resource = subject.generate(snapshot, ZoneId.systemDefault());
        assertNotNull("Resource shouldn't be null", resource);
    }

    @Test
    public void generateLotsOfLines() throws IOException {
        FollowUpTemplate testTemplate = new FollowUpTemplate(resolve("followup/Followup-template.xlsm"));
        subject = new FollowUpReportGenerator(new SimpleSpreadsheetEditor(testTemplate));

        List<FromJiraDataRow> fromJiraDataRowList = new LinkedList<>();
        for (int i=0; i < 5000; i++)
            fromJiraDataRowList.add(getDefaultFromJiraDataRow());

        FromJiraDataSet fromJiraDs = new FromJiraDataSet(FROMJIRA_HEADERS, fromJiraDataRowList);
        FollowUpData followupData = new FollowUpData(fromJiraDs, getDefaultAnalyticsTransitionsDataSet(), getDefaultSyntheticTransitionsDataSet());
        
        FollowUpSnapshot snapshot = new FollowUpSnapshot(timeline, followupData, new EmptyFollowupCluster(), emptyList());

        Resource resource = subject.generate(snapshot, ZoneId.systemDefault());
        assertNotNull("Resource shouldn't be null", resource);
    }

    @Test
    public void givenIssues_whenGenerateTransitionsSheets_thenSheetsShouldBeGenerated() throws IOException {
        subject.getEditor().open();
        subject.generateTransitionsSheets(getDefaultFollowupData());
        subject.getEditor().close();

        String transitionsSheetsExpected = txtResourceAsString("followup/transitionsSheets.txt");
        assertEquals("From Jira sheet", transitionsSheetsExpected, editor.loggerString());
    }

    @Test
    public void givenNoIssue_whenGenerateTransitionsSheets_thenSheetsShouldNotBeGenerated() throws IOException {
        FollowUpTemplate testTemplate = new FollowUpTemplate(resolve("followup/Followup-template.xlsm"));
        subject = new FollowUpReportGenerator(new SimpleSpreadsheetEditor(testTemplate));

        List<Sheet> transitionsSheets = subject.generateTransitionsSheets(getEmptyFollowupData());
        assertEquals("Transitions sheets quantity", 0, transitionsSheets.size());

        FollowUpData followupData = new FollowUpData(null, getEmptyAnalyticsTransitionsDataSet(), getEmptySyntheticTransitionsDataSet());
        transitionsSheets = subject.generateTransitionsSheets(followupData);
        assertEquals("Transitions sheets quantity", 0, transitionsSheets.size());

        followupData = new FollowUpData(null, null, null);
        transitionsSheets = subject.generateTransitionsSheets(followupData);
        assertEquals("Transitions sheets quantity", 0, transitionsSheets.size());

        AnalyticsTransitionsDataSet analytic = new AnalyticsTransitionsDataSet("", emptyList(), null);
        SyntheticTransitionsDataSet synthetic = new SyntheticTransitionsDataSet("", emptyList(), null);
        followupData = new FollowUpData(null, asList(analytic), asList(synthetic));
        transitionsSheets = subject.generateTransitionsSheets(followupData);
        assertEquals("Transitions sheets quantity", 0, transitionsSheets.size());

        followupData = new FollowUpData(null, getAnalyticsTransitionsDataSetWitNoRow(), getSyntheticTransitionsDataSetWithNoRow());
        subject.getEditor().open();
        transitionsSheets = subject.generateTransitionsSheets(followupData);
        assertEquals("Transitions sheets quantity", 2, transitionsSheets.size());
        subject.getEditor().close();
    }

    @Test
    public void generateEffortHistoryTest() throws IOException {
        ProjectFilterConfiguration project = Mockito.mock(ProjectFilterConfiguration.class);
        doReturn("PROJ").when(project).getProjectKey();

        List<EffortHistoryRow> effortHistory = asList(
                new EffortHistoryRow(LocalDate.of(2017, 10, 1), 4.0, 6.0),
                new EffortHistoryRow(LocalDate.of(2017, 10, 2), 13.0, 20.4),
                new EffortHistoryRow(LocalDate.of(2017, 10, 3), 3.9, 4.0));
        
        FollowUpSnapshot snapshot = followUpDataEntry(LocalDate.of(2017, 10, 4), emptyList(), effortHistory);

        subject.getEditor().open();
        subject.generateEffortHistory(snapshot);
        subject.getEditor().close();

        String expectedEditorLogger = 
                "Spreadsheet Open\n" + 
                "Sheet Create: Effort History\n" + 
                "Sheet \"Effort History\" Row Create: 1\n" + 
                "Sheet \"Effort History\" Row \"1\" AddColumn \"A1\": Date\n" + 
                "Sheet \"Effort History\" Row \"1\" AddColumn \"B1\": SumEffortDone\n" + 
                "Sheet \"Effort History\" Row \"1\" AddColumn \"C1\": SumEffortBacklog\n" + 
                "Sheet \"Effort History\" Row Create: 2\n" + 
                "Sheet \"Effort History\" Row \"2\" AddColumn \"A2\": 2017-10-01\n" + 
                "Sheet \"Effort History\" Row \"2\" AddColumn \"B2\": 4.0\n" + 
                "Sheet \"Effort History\" Row \"2\" AddColumn \"C2\": 6.0\n" + 
                "Sheet \"Effort History\" Row Create: 3\n" + 
                "Sheet \"Effort History\" Row \"3\" AddColumn \"A3\": 2017-10-02\n" + 
                "Sheet \"Effort History\" Row \"3\" AddColumn \"B3\": 13.0\n" + 
                "Sheet \"Effort History\" Row \"3\" AddColumn \"C3\": 20.4\n" + 
                "Sheet \"Effort History\" Row Create: 4\n" + 
                "Sheet \"Effort History\" Row \"4\" AddColumn \"A4\": 2017-10-03\n" + 
                "Sheet \"Effort History\" Row \"4\" AddColumn \"B4\": 3.9\n" + 
                "Sheet \"Effort History\" Row \"4\" AddColumn \"C4\": 4.0\n" + 
                "Sheet \"Effort History\" Row Create: 5\n" + 
                "Sheet \"Effort History\" Row \"5\" AddColumn \"A5\": 2017-10-04\n" + 
                "Sheet \"Effort History\" Row \"5\" AddColumn \"B5\": 0.0\n" + 
                "Sheet \"Effort History\" Row \"5\" AddColumn \"C5\": 0.0\n" +
                "Sheet \"Effort History\" Save\n" + 
                "Spreadsheet Close\n";

        assertEquals(expectedEditorLogger, editor.loggerString());
    }

    @Test
    public void updateTimelineDatesTest() throws IOException {
        subject.getEditor().open();
        subject.updateTimelineDates(new FollowUpTimeline(LocalDate.parse("2017-10-03")
                                                         , Optional.of(LocalDate.parse("2017-10-01"))
                                                         , Optional.of(LocalDate.parse("2017-11-01"))));
        subject.getEditor().close();

        String expectedEditorLogger = 
                "Spreadsheet Open\n" + 
                "Sheet Create: Timeline\n" + 
                "Sheet \"Timeline\" Row Get/Create: 2\n" + 
                "Sheet \"Timeline\" Row \"2\" SetValue (date) \"B2\": 2017-10-01\n" + 
                "Sheet \"Timeline\" Row Get/Create: 5\n" + 
                "Sheet \"Timeline\" Row \"5\" SetValue (date) \"B5\": 2017-11-01\n" + 
                "Sheet \"Timeline\" Row Get/Create: 6\n" + 
                "Sheet \"Timeline\" Row \"6\" SetValue (date) \"B6\": 2017-10-03\n" + 
                "Sheet \"Timeline\" Save\n" + 
                "Spreadsheet Close\n";

        assertEquals(expectedEditorLogger, editor.loggerString());
    }

    @Test
    public void whenClusteIsEmpty_ShouldOnlyTruncateTab() throws IOException {
        FromJiraDataRow data2row1 = getDefaultFromJiraDataRow();
        FromJiraDataRow data2row2 = getDefaultFromJiraDataRow();
        FromJiraDataRow data2row3 = getDefaultFromJiraDataRow();

        List<EffortHistoryRow> effortHistory = asList(
                new EffortHistoryRow(LocalDate.of(2017, 10, 1), 4.0, 6.0),
                new EffortHistoryRow(LocalDate.of(2017, 10, 2), 13.0, 20.4),
                new EffortHistoryRow(LocalDate.of(2017, 10, 3), 3.9, 4.0));
        
        FollowUpSnapshot followUpDataEntry2 = followUpDataEntry(
                LocalDate.of(2017, 10, 2), 
                asList(data2row1, data2row2, data2row3),
                new EmptyFollowupCluster(),
                effortHistory);

        subject.getEditor().open();
        subject.generateEffortHistory(followUpDataEntry2);
        subject.getEditor().close();

        String expectedEditorLogger = 
                "Spreadsheet Open\n" + 
                "Sheet Create: Effort History\n" + 
                "Sheet \"Effort History\" Row Create: 1\n" + 
                "Sheet \"Effort History\" Row \"1\" AddColumn \"A1\": Date\n" + 
                "Sheet \"Effort History\" Row \"1\" AddColumn \"B1\": SumEffortDone\n" + 
                "Sheet \"Effort History\" Row \"1\" AddColumn \"C1\": SumEffortBacklog\n" + 
                "Spreadsheet Close\n" + 
                "";

        assertEquals(expectedEditorLogger, editor.loggerString());
    }

    @Test
    public void givenFollowUpClusterItems_whenGenerateTShirtSizeSheet_thenSheetShouldContainsItems() throws IOException {
        editor.addTable("T-shirt Size", "Cluster", SpreadsheetA1Range.parse("A1:D4"));

        ProjectFilterConfiguration project = Mockito.mock(ProjectFilterConfiguration.class);
        doReturn("PROJ").when(project).getProjectKey();

        FollowupCluster cluster = new FollowupClusterImpl(asList(
            new FollowUpClusterItem(project, "Alpha Bug", "notused", "L", 12.0, 14.4),
            new FollowUpClusterItem(project, "Alpha Test", "notused", "M", 6.0, 7.2),
            new FollowUpClusterItem(project, "UAT", "notused", "S", 4.0, 4.8)));

        subject.getEditor().open();
        subject.generateTShirtSizeSheet(cluster);
        subject.getEditor().close();

        String expectedEditorLogger =
                "Spreadsheet Open\n" +
                "Sheet Create: T-shirt Size\n" +
                "Sheet \"T-shirt Size\" Row Create: 1\n" +
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"A1\": Cluster Name\n" +
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"B1\": T-Shirt Size\n" +
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"C1\": Type\n" +
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"D1\": Effort\n" +
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"E1\": Cycle\n" +
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"F1\": Project\n" +
                "Sheet \"T-shirt Size\" Row Create: 2\n" +
                "Sheet \"T-shirt Size\" Row \"2\" AddColumn \"A2\": Alpha Bug\n" +
                "Sheet \"T-shirt Size\" Row \"2\" AddColumn \"B2\": L\n" +
                "Sheet \"T-shirt Size\" Row \"2\" AddColumn \"C2\": Hours\n" +
                "Sheet \"T-shirt Size\" Row \"2\" AddColumn \"D2\": 12.0\n" +
                "Sheet \"T-shirt Size\" Row \"2\" AddColumn \"E2\": 14.4\n" +
                "Sheet \"T-shirt Size\" Row \"2\" AddColumn \"F2\": PROJ\n" +
                "Sheet \"T-shirt Size\" Row Create: 3\n" +
                "Sheet \"T-shirt Size\" Row \"3\" AddColumn \"A3\": Alpha Test\n" +
                "Sheet \"T-shirt Size\" Row \"3\" AddColumn \"B3\": M\n" +
                "Sheet \"T-shirt Size\" Row \"3\" AddColumn \"C3\": Hours\n" +
                "Sheet \"T-shirt Size\" Row \"3\" AddColumn \"D3\": 6.0\n" +
                "Sheet \"T-shirt Size\" Row \"3\" AddColumn \"E3\": 7.2\n" +
                "Sheet \"T-shirt Size\" Row \"3\" AddColumn \"F3\": PROJ\n" +
                "Sheet \"T-shirt Size\" Row Create: 4\n" +
                "Sheet \"T-shirt Size\" Row \"4\" AddColumn \"A4\": UAT\n" +
                "Sheet \"T-shirt Size\" Row \"4\" AddColumn \"B4\": S\n" +
                "Sheet \"T-shirt Size\" Row \"4\" AddColumn \"C4\": Hours\n" +
                "Sheet \"T-shirt Size\" Row \"4\" AddColumn \"D4\": 4.0\n" +
                "Sheet \"T-shirt Size\" Row \"4\" AddColumn \"E4\": 4.8\n" +
                "Sheet \"T-shirt Size\" Row \"4\" AddColumn \"F4\": PROJ\n" +
                "Sheet \"T-shirt Size\" Save\n" +
                "Spreadsheet Close\n";

        assertEquals("T-shirt Size Sheet", expectedEditorLogger, editor.loggerString());
    }
    
    @Test
    public void whenClustersTableRangeIsSmallerThanItemsSize_thenShouldThrowAnException() {
        editor.addTable("T-shirt Size", "Clusters", SpreadsheetA1Range.parse("A1:D3"));
        
        ProjectFilterConfiguration project = Mockito.mock(ProjectFilterConfiguration.class);
        doReturn("PROJ").when(project).getProjectKey();

        FollowupCluster cluster = new FollowupClusterImpl(asList(
            new FollowUpClusterItem(project, "Alpha Bug", "notused", "L", 12.0, 14.4),
            new FollowUpClusterItem(project, "Alpha Test", "notused", "M", 6.0, 7.2),
            new FollowUpClusterItem(project, "UAT", "notused", "S", 4.0, 4.8)));

        subject.getEditor().open();
        try {
            subject.generateTShirtSizeSheet(cluster);
            fail();
        } catch (InvalidTableRangeException e) {
            assertEquals("T-shirt Size", e.getSheetName());
            assertEquals("Clusters", e.getTableName());
            assertEquals(4, e.getMinRows());
        }
    }

    @Test
    public void givenNoFollowUpClusterItems_whenGenerateTShirtSize_thenShouldNotGenerateSheet() throws IOException {
        FollowupCluster cluster = new EmptyFollowupCluster();

        subject.getEditor().open();
        subject.generateTShirtSizeSheet(cluster);
        subject.getEditor().close();

        String expectedEditorLogger =
                "Spreadsheet Open\n" + 
                "Sheet Create: T-shirt Size\n" + 
                "Sheet \"T-shirt Size\" Row Create: 1\n" + 
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"A1\": Cluster Name\n" + 
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"B1\": T-Shirt Size\n" + 
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"C1\": Type\n" + 
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"D1\": Effort\n" + 
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"E1\": Cycle\n" + 
                "Sheet \"T-shirt Size\" Row \"1\" AddColumn \"F1\": Project\n" + 
                "Sheet \"T-shirt Size\" Save\n" + 
                "Spreadsheet Close\n";

        assertEquals("T-shirt Size Sheet", expectedEditorLogger, editor.loggerString());
    }
    
    @Test
    public void whenThereAreWorklogs_ShouldGenerateWorklogSheet() {
        FollowUpData followupData = getDefaultFollowupData();
        List<Worklog> worklogs = new LinkedList<>();
        worklogs.add(new Worklog("john.doe", DateTimeUtils.parseStringToDate("2018-12-12"), 18000));
        followupData.fromJiraDs.rows.get(0).worklogs = worklogs;
        subject.generateWorklogSheet(followupData, ZoneId.of("Z"));
        
        String loggerString = editor.loggerString();
        
        String expected = "Sheet Create: Worklogs\n" + 
                "Sheet \"Worklogs\" Row Create: 1\n" + 
                "Sheet \"Worklogs\" Row \"1\" AddColumn \"A1\": AUTHOR\n" + 
                "Sheet \"Worklogs\" Row \"1\" AddColumn \"B1\": ISSUE\n" + 
                "Sheet \"Worklogs\" Row \"1\" AddColumn \"C1\": STARTED\n" + 
                "Sheet \"Worklogs\" Row \"1\" AddColumn \"D1\": TIMESPENT\n" + 
                "Sheet \"Worklogs\" Row Create: 2\n" + 
                "Sheet \"Worklogs\" Row \"2\" AddColumn \"A2\": john.doe\n" + 
                "Sheet \"Worklogs\" Row \"2\" AddColumn \"B2\": I-3\n" + 
                "Sheet \"Worklogs\" Row \"2\" AddColumn \"C2\": 2018-12-12T02:00Z\n" + 
                "Sheet \"Worklogs\" Row \"2\" AddColumn \"D2\": 5.0\n" + 
                "Sheet \"Worklogs\" Save\n";
        assertEquals(expected, loggerString);
    }

    private FollowUpSnapshot followUpDataEntry(LocalDate date, List<FromJiraDataRow> rows, List<EffortHistoryRow> effortHistory) {
        return followUpDataEntry(date, rows, followupCluster, effortHistory);
    }

    private FollowUpSnapshot followUpDataEntry(LocalDate date, List<FromJiraDataRow> rows, FollowupCluster followupCluster, List<EffortHistoryRow> effortHistory) {
        FromJiraDataSet dataSet = new FromJiraDataSet(FROMJIRA_HEADERS, rows);
        FollowUpData followupData = new FollowUpData(dataSet, emptyList(), emptyList());
        return new FollowUpSnapshot(new FollowUpTimeline(date), followupData, followupCluster, effortHistory);
    }

    private String txtResourceAsString(String pathResource) {
        return IOUtilities.resourceToString(pathResource);
    }

    private static Resource resolve(String resourceName) {
        return IOUtilities.asResource(FollowUpReportGenerator.class.getClassLoader().getResource(resourceName));
    }
}
