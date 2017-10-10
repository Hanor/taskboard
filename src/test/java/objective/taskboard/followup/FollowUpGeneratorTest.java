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

import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor.Sheet;
import objective.taskboard.utils.IOUtilities;
import objective.taskboard.utils.XmlUtils;

@RunWith(MockitoJUnitRunner.class)
public class FollowUpGeneratorTest {

    private FollowUpGenerator subject;

    @Mock
    private FollowupDataProvider provider;

    @Before
    public void setup() {
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

        String jiraDataSheetExpected = normalizedXmlResource("followup/emptyFromJiraOnlyWithHeaders.xml");
        assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);
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
        assertEquals("Transitions sheets quantity", 2, transitionsSheets.size());

        String analyticTransitionsSheetExpected = normalizedXmlResource("followup/analyticTransitionsWithIssues.xml");
        assertEquals("Analytic transitions sheet", analyticTransitionsSheetExpected, transitionsSheets.get(0).stringValue());
        String syntheticTransitionsSheetExpected = normalizedXmlResource("followup/syntheticTransitionsWithIssues.xml");
        assertEquals("Synthetic transitions sheet", syntheticTransitionsSheetExpected, transitionsSheets.get(1).stringValue());
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
        return new FollowUpGenerator(provider, new SimpleSpreadsheetEditor(genericTemplate));
    }

    private FollowUpGenerator getFollowUpGeneratorUsingTestTemplate() {
        FollowUpTemplate testTemplate = new FollowUpTemplate(resolve("followup/Followup-template.xlsm"));
        return new FollowUpGenerator(provider, new SimpleSpreadsheetEditor(testTemplate));
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
