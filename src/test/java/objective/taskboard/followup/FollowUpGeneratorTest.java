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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import objective.taskboard.followup.impl.DefaultFollowUpTemplateStorage;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.xml.sax.SAXException;

import objective.taskboard.utils.IOUtilities;

public class FollowUpGeneratorTest {

    private static final String PATH_SHARED_STRINGS_INITIAL = "followup/sharedStrings-initial.xml";
    private static final String PATH_SHARED_STRINGS_TEMPLATE = "followup/sharedStrings-template.xml";
    private static final String PATH_SHARED_STRINGS_SI_TEMPLATE = "followup/sharedStrings-si-template.xml";
    private static final String PATH_SHEET7_TEMPLATE = "followup/sheet7-template.xml";
    private static final String PATH_SHEET7_ROW_TEMPLATE = "followup/sheet7-row-template.xml";
    private static final String PATH_FOLLOWUP_TEMPLATE = "followup/Followup-template.xlsm";
    private static final String PATH_TABLE7_TEMPLATE = "followup/table7-template.xml";

    private static final String MSG_ASSERT_SHARED_STRINGS_SIZE = "Shared strings size";

    @Test
    public void getSharedStringsInitialTest() throws ParserConfigurationException, SAXException, IOException {
        FollowUpGenerator subject = getFollowUpGeneratorUsingTestTemplates(null);
        Map<String, Long> sharedStrings = subject.getSharedStringsInitial();
        assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 204, sharedStrings.size());
        assertEquals("First shared string", 0, sharedStrings.get("project").longValue());
        assertEquals("Some special character shared string", 44, sharedStrings.get("Demand Status > Demand > Task Status > Task > Subtask").longValue());
        assertEquals("Any shared string", 126, sharedStrings.get("Group %").longValue());
        assertEquals("Last shared string", 203, sharedStrings.get("BALLPARK").longValue());
    }

    @Test
    public void addNewSharedStringsInTheEndTest() throws ParserConfigurationException, SAXException, IOException {
        FollowupDataProvider provider = getFollowupDataProvider(FollowUpHelper.getFollowUpDataDefaultList());
        FollowUpGenerator subject = getFollowUpGeneratorUsingTestTemplates(provider);

        Map<String, Long> sharedStrings = subject.getSharedStringsInitial();
        assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 204, sharedStrings.size());
        assertEquals("First shared string", 0, sharedStrings.get("project").longValue());
        assertEquals("Last shared string", 203, sharedStrings.get("BALLPARK").longValue());

        subject.generateJiraDataSheet(sharedStrings, emptyArray());

        assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 218, sharedStrings.size());
        assertEquals("First new shared string", 204, sharedStrings.get("PROJECT TEST").longValue());
        assertEquals("Any new shared string", 210, sharedStrings.get("Summary Feature").longValue());
        assertEquals("Last new shared string", 217, sharedStrings.get("Full Description Sub-task").longValue());
    }

    @Test
    public void generateSharedStringsInOrderTest() throws ParserConfigurationException, SAXException, IOException {
        FollowUpGenerator subject = getFollowUpGeneratorUsingTestTemplates(null);

        Map<String, Long> sharedStrings = subject.getSharedStringsInitial();
        String sharedStringsGenerated = subject.generateSharedStrings(sharedStrings);

        String sharedStringsExpected = getStringExpected("followup/generateSharedStringsInOrderTest.xml");
        assertEquals("Shared strings", sharedStringsExpected, sharedStringsGenerated);
    }

    @Test
    public void generateSharedStringsInOrderAfterAddNewSharedStringTest() throws ParserConfigurationException, SAXException, IOException {
        FollowupDataProvider provider = getFollowupDataProvider(FollowUpHelper.getFollowUpDataDefaultList());
        FollowUpGenerator subject = getFollowUpGeneratorUsingTestTemplates(provider);

        Map<String, Long> sharedStrings = subject.getSharedStringsInitial();

        assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 204, sharedStrings.size());
        subject.generateJiraDataSheet(sharedStrings, emptyArray());
        assertEquals(MSG_ASSERT_SHARED_STRINGS_SIZE, 218, sharedStrings.size());

        String sharedStringsGenerated = subject.generateSharedStrings(sharedStrings);

        String sharedStringsExpected = getStringExpected("followup/generateSharedStringsInOrderAfterAddNewSharedStringTest.xml");
        assertEquals("Shared strings", sharedStringsExpected, sharedStringsGenerated);
    }

    @Test
    public void generateJiraDataSheetTest() throws ParserConfigurationException, SAXException, IOException {
        FollowupDataProvider provider = getFollowupDataProvider(FollowUpHelper.getFollowUpDataDefaultList());
        FollowUpGenerator subject = getFollowUpGeneratorUsingTestTemplates(provider);

        Map<String, Long> sharedStrings = subject.getSharedStringsInitial();
        String jiraDataSheet = subject.generateJiraDataSheet(sharedStrings, emptyArray());

        String jiraDataSheetExpected = getStringExpected("followup/generateJiraDataSheetTest.xml");
        assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);
    }

    @Test
    public void generateJiraDataSheetWithEmptyDataTest() throws ParserConfigurationException, SAXException, IOException {
        FollowupDataProvider provider = getFollowupDataProvider(Collections.emptyList());
        FollowUpGenerator subject = getFollowUpGeneratorUsingTestTemplates(provider);

        Map<String, Long> sharedStrings = subject.getSharedStringsInitial();
        String jiraDataSheet = subject.generateJiraDataSheet(sharedStrings, emptyArray());

        String jiraDataSheetExpected = getStringExpected("followup/generateJiraDataSheetWithEmptyDataTest.xml");
        assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);
    }

    @Test
    public void generateJiraDataSheetWithSomeEmptyAndNullAttributesJiraDataTest() throws ParserConfigurationException, SAXException, IOException {
        FollowUpData followupDefault = FollowUpHelper.getFollowUpDataDefault();
        followupDefault.project = "";
        followupDefault.demandType = null;
        followupDefault.taskId = 0L;
        followupDefault.subtaskId = null;
        followupDefault.worklog = 0.0;
        followupDefault.wrongWorklog = null;
        FollowupDataProvider provider = getFollowupDataProvider(asList(followupDefault));
        FollowUpGenerator subject = getFollowUpGeneratorUsingTestTemplates(provider);

        Map<String, Long> sharedStrings = subject.getSharedStringsInitial();
        String jiraDataSheet = subject.generateJiraDataSheet(sharedStrings, emptyArray());

        String jiraDataSheetExpected = getStringExpected("followup/generateJiraDataSheetWithSomeEmptyAndNullAttributesJiraDataTest.xml");
        assertEquals("Jira data sheet", jiraDataSheetExpected, jiraDataSheet);
    }

    @Test
    public void generateTest() throws Exception {
        FollowupDataProvider provider = getFollowupDataProvider(FollowUpHelper.getFollowUpDataDefaultList());
        FollowUpGenerator subject = getFollowUpGeneratorUsingTestTemplates(provider);
        Resource resource = subject.generate(emptyArray());
        assertNotNull("Resource shouldn't be null", resource);
    }

    @Test
    public void generateUsingDefaultTemplatesTest() throws Exception {
        FollowupDataProvider provider = getFollowupDataProvider(FollowUpHelper.getFollowUpDataDefaultList());
        FollowUpGenerator subject = getDefaultFollowUpGenerator(provider);
        Resource resource = subject.generate(emptyArray());
        assertNotNull("Resource shouldn't be null", resource);
    }

    private FollowUpGenerator getDefaultFollowUpGenerator(FollowupDataProvider provider) {
        return new FollowUpGenerator(provider, new FollowUpTemplate(
                resolve("followup-template/sharedStrings-initial.xml")
                , resolve("followup-template/sharedStrings-template.xml")
                , resolve("followup-template/sharedStrings-si-template.xml")
                , resolve("followup-template/sheet7-template.xml")
                , resolve("followup-template/sheet7-row-template.xml")
                , resolve("followup-template/Followup-template.xlsm")
                , resolve("followup-template/table7-template.xml")
        ));
    }

    private FollowUpGenerator getFollowUpGeneratorUsingTestTemplates(FollowupDataProvider provider) {
        return new FollowUpGenerator(provider, new FollowUpTemplate(
                resolve(PATH_SHARED_STRINGS_INITIAL)
                , resolve(PATH_SHARED_STRINGS_TEMPLATE)
                , resolve(PATH_SHARED_STRINGS_SI_TEMPLATE)
                , resolve(PATH_SHEET7_TEMPLATE)
                , resolve(PATH_SHEET7_ROW_TEMPLATE)
                , resolve(PATH_FOLLOWUP_TEMPLATE)
                , resolve(PATH_TABLE7_TEMPLATE)));
    }

    private FollowupDataProvider getFollowupDataProvider(List<FollowUpData> jiraData) {
        FollowupDataProvider provider = mock(FollowupDataProvider.class);
        when(provider.getJiraData(emptyArray())).thenReturn(jiraData);
        return provider;
    }

    private String getStringExpected(String pathResource) {
        return IOUtilities.resourceToString(pathResource);
    }

    private String[] emptyArray() {
        return new String[0];
    }

    private static Resource resolve(String resourceName) {
        return IOUtilities.asResource(DefaultFollowUpTemplateStorage.class.getClassLoader().getResource(resourceName));
    }
}
