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
import static objective.taskboard.followup.FollowUpHelper.getDefaultAnalyticsTransitionsDataSet;
import static objective.taskboard.followup.FollowUpHelper.getDefaultFollowupData;
import static objective.taskboard.followup.FollowUpHelper.getDefaultFromJiraDataRow;
import static objective.taskboard.followup.FollowUpHelper.getDefaultSyntheticTransitionsDataSet;
import static objective.taskboard.followup.FollowUpHelper.getEmptyAnalyticsTransitionsDataSet;
import static objective.taskboard.followup.FollowUpHelper.getEmptyFollowupData;
import static objective.taskboard.followup.FollowUpHelper.getEmptySyntheticTransitionsDataSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

import objective.taskboard.jira.JiraProperties;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor.Sheet;
import objective.taskboard.utils.IOUtilities;
import objective.taskboard.utils.XmlUtils;

@RunWith(MockitoJUnitRunner.class)
public class FollowUpGeneratorTest {

    private FollowUpGenerator subject;

    @Mock
    private FollowupDataProvider provider;

    @Mock
    private JiraProperties jiraProperties;

    @Before
    public void setup() {
        String[] statusOrder = new String[] { "Done", "Doing", "To Do" };
        JiraProperties.StatusPriorityOrder statusPriorityOrder = new JiraProperties.StatusPriorityOrder();
        statusPriorityOrder.setDemands(statusOrder);
        statusPriorityOrder.setTasks(statusOrder);
        statusPriorityOrder.setSubtasks(statusOrder);
        when(jiraProperties.getStatusPriorityOrder()).thenReturn(statusPriorityOrder);
        subject = getFollowUpGeneratorUsingTestTemplate();
    }

    @Test
    public void generateJiraDataSheetTest() {
        subject.getEditor().open();
        String jiraDataSheet = subject.generateFromJiraSheet(getDefaultFollowupData()).stringValue();

        String jiraDataSheetExpected = normalizedXmlResource("followup/generateJiraDataSheetTest.xml");
        assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);
    }

    @Test
    public void whenGeneratingWithoutData_generatesFromJiraKeepingOnlyHeaders() {
        subject.getEditor().open();
        String jiraDataSheet = subject.generateFromJiraSheet(getEmptyFollowupData()).stringValue();

        String jiraDataSheetExpected = normalizedXmlResource("followup/emptyFromJiraWithGeneratedHeaders.xml");
        assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);
    }

    @Test
    public void givenEmptyTransitions_whenGenerateFromJiraSheet_thenShouldGenerateEmptyTransitions() {
        FromJiraDataSet fromJiraDS = new FromJiraDataSet(FROMJIRA_HEADERS, FollowUpHelper.getDefaultFromJiraDataRowList());
        FollowupData followupData = new FollowupData(fromJiraDS, FollowUpHelper.getEmptyAnalyticsTransitionsDataSet(),
                FollowUpHelper.getEmptySyntheticTransitionsDataSet());

        subject.getEditor().open();
        String fromJiraSheet = subject.generateFromJiraSheet(followupData).stringValue();

        String fromJiraSheetExpected = normalizedXmlResource("followup/fromJiraWithEmptyTransitions.xml");
        assertEquals("From Jira sheet", fromJiraSheetExpected, fromJiraSheet);
    }

    @Test
    public void givenNoTransitions_whenGenerateFromJiraSheet_thenShouldNotGenerateTransitions() {
        FromJiraDataSet fromJiraDS = new FromJiraDataSet(FROMJIRA_HEADERS, FollowUpHelper.getDefaultFromJiraDataRowList());
        FollowupData followupData = new FollowupData(fromJiraDS, null, null);

        subject.getEditor().open();
        String fromJiraSheet = subject.generateFromJiraSheet(followupData).stringValue();

        String fromJiraSheetExpected = normalizedXmlResource("followup/fromJiraWithNoTransitions.xml");
        assertEquals("From Jira sheet", fromJiraSheetExpected, fromJiraSheet);
    }

    @Test
    public void generateJiraDataSheetWithSomeEmptyAndNullAttributesJiraDataTest() {
        FromJiraDataRow fromJiraDefault = getDefaultFromJiraDataRow();
        fromJiraDefault.project = "";
        fromJiraDefault.demandType = null;
        fromJiraDefault.taskId = 0L;
        fromJiraDefault.subtaskId = null;
        fromJiraDefault.worklog = 0.0;
        fromJiraDefault.wrongWorklog = null;

        FromJiraDataSet fromJiraDs = new FromJiraDataSet(FROMJIRA_HEADERS, asList(fromJiraDefault));
        FollowupData followupData = new FollowupData(fromJiraDs, emptyList(), emptyList());

        subject.getEditor().open();
        String jiraDataSheet = subject.generateFromJiraSheet(followupData).stringValue();

        String jiraDataSheetExpected = normalizedXmlResource("followup/generateJiraDataSheetWithSomeEmptyAndNullAttributesJiraDataTest.xml");
        assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);
    }

    @Test
    public void generateTest() {
        when(provider.getJiraData(emptyArray(), ZoneId.systemDefault())).thenReturn(getDefaultFollowupData());
        Resource resource = subject.generate(emptyArray(), ZoneId.systemDefault());
        assertNotNull("Resource shouldn't be null", resource);
    }

    @Test
    public void generateUsingGenericTemplateTest() {
        when(provider.getJiraData(emptyArray(), ZoneId.systemDefault())).thenReturn(getDefaultFollowupData());
        subject = getFollowUpGeneratorUsingGenericTemplate();
        Resource resource = subject.generate(emptyArray(), ZoneId.systemDefault());
        assertNotNull("Resource shouldn't be null", resource);
    }

    @Test
    public void generateLotsOfLines() {
        List<FromJiraDataRow> fromJiraDataRowList = new LinkedList<>();
        for (int i=0; i < 5000; i++)
            fromJiraDataRowList.add(getDefaultFromJiraDataRow());

        FromJiraDataSet fromJiraDs = new FromJiraDataSet(FROMJIRA_HEADERS, fromJiraDataRowList);
        FollowupData followupData = new FollowupData(fromJiraDs, getDefaultAnalyticsTransitionsDataSet(), getDefaultSyntheticTransitionsDataSet());
        when(provider.getJiraData(emptyArray(), ZoneId.systemDefault())).thenReturn(followupData);
        Resource resource = subject.generate(emptyArray(), ZoneId.systemDefault());
        assertNotNull("Resource shouldn't be null", resource);
    }

    @Test
    public void givenIssues_whenGenerateTransitionsSheets_thenSheetsShouldBeGenerated() {
        subject.getEditor().open();
        List<Sheet> transitionsSheets = subject.generateTransitionsSheets(getDefaultFollowupData());
        assertEquals("Transitions sheets quantity", 6, transitionsSheets.size());

        String analyticDemandExpected = normalizedXmlResource("followup/analyticTransitionsDemand.xml");
        assertEquals("Analytic transitions Demand", analyticDemandExpected, transitionsSheets.get(0).stringValue());
        String analyticFeatureExpected = normalizedXmlResource("followup/analyticTransitionsFeature.xml");
        assertEquals("Analytic transitions Feature", analyticFeatureExpected, transitionsSheets.get(1).stringValue());
        String analyticSubtaskExpected = normalizedXmlResource("followup/analyticTransitionsSubtask.xml");
        assertEquals("Analytic transitions Subtask", analyticSubtaskExpected, transitionsSheets.get(2).stringValue());
        String syntheticDemandExpected = normalizedXmlResource("followup/syntheticTransitionsDemand.xml");
        assertEquals("Synthetic transitions Demand", syntheticDemandExpected, transitionsSheets.get(3).stringValue());
        String syntheticFeatureExpected = normalizedXmlResource("followup/syntheticTransitionsFeature.xml");
        assertEquals("Synthetic transitions Feature", syntheticFeatureExpected, transitionsSheets.get(4).stringValue());
        String syntheticSubtaskExpected = normalizedXmlResource("followup/syntheticTransitionsSubtask.xml");
        assertEquals("Synthetic transitions Subtask", syntheticSubtaskExpected, transitionsSheets.get(5).stringValue());
    }

    @Test
    public void givenNoIssue_whenGenerateTransitionsSheets_thenSheetsShouldNotBeGenerated() {
        List<Sheet> transitionsSheets = subject.generateTransitionsSheets(getEmptyFollowupData());
        assertEquals("Transitions sheets quantity", 0, transitionsSheets.size());

        FollowupData followupData = new FollowupData(null, getEmptyAnalyticsTransitionsDataSet(), getEmptySyntheticTransitionsDataSet());
        transitionsSheets = subject.generateTransitionsSheets(followupData);
        assertEquals("Transitions sheets quantity", 0, transitionsSheets.size());
    }

    private FollowUpGenerator getFollowUpGeneratorUsingGenericTemplate() {
        FollowUpTemplate genericTemplate = new FollowUpTemplate(resolve("followup/generic-followup-template.xlsm"));
        return new FollowUpGenerator(provider, new SimpleSpreadsheetEditor(genericTemplate), jiraProperties);
    }

    private FollowUpGenerator getFollowUpGeneratorUsingTestTemplate() {
        FollowUpTemplate testTemplate = new FollowUpTemplate(resolve("followup/Followup-template.xlsm"));
        return new FollowUpGenerator(provider, new SimpleSpreadsheetEditor(testTemplate), jiraProperties);
    }

    private String normalizedXmlResource(String pathResource) {
        return XmlUtils.normalizeXml(IOUtilities.resourceToString(pathResource));
    }

    private static Resource resolve(String resourceName) {
        return IOUtilities.asResource(FollowUpGenerator.class.getClassLoader().getResource(resourceName));
    }

    private String[] emptyArray() {
        return new String[0];
    }
}
