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

package objective.taskboard.followup.impl;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempDirectory;
import static objective.taskboard.followup.FollowUpHelper.assertFollowUpDataDefault;
import static objective.taskboard.followup.FollowUpHelper.getDefaultFollowupData;
import static objective.taskboard.followup.impl.FollowUpDataHistoryGeneratorJSONFiles.EXTENSION_JSON;
import static objective.taskboard.followup.impl.FollowUpDataHistoryGeneratorJSONFiles.EXTENSION_ZIP;
import static objective.taskboard.followup.impl.FollowUpDataHistoryGeneratorJSONFiles.FILE_NAME_FORMAT;
import static objective.taskboard.utils.ZipUtils.zip;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import objective.taskboard.database.directory.DataBaseDirectory;
import objective.taskboard.followup.FollowupData;
import objective.taskboard.followup.FromJiraDataRow;
import objective.taskboard.followup.SyntheticTransitionsDataRow;
import objective.taskboard.rules.TimeZoneRule;
import objective.taskboard.utils.DateTimeUtils;

@RunWith(MockitoJUnitRunner.class)
public class FollowUpDataProviderFromHistoryTest {

    private static final String PROJECT_TEST = "PROJECT TEST";
    private static final String PROJECT_TEST_2 = "PROJECT TEST 2";
    private static final String YESTERDAY = DateTime.now().minusDays(1).toString(FILE_NAME_FORMAT);

    private FollowUpDataProviderFromHistory subject;

    @Rule
    public TimeZoneRule timeZoneRule = new TimeZoneRule("America/Sao_Paulo");

    @Mock
    private DataBaseDirectory dataBaseDirectory;

    @Before
    public void before() throws IOException {
        Path pathHistoryTemp = createTempDirectory(getClass().getSimpleName());
        when(dataBaseDirectory.path(anyString())).thenReturn(pathHistoryTemp);
        subject = new FollowUpDataProviderFromHistory(YESTERDAY, dataBaseDirectory);
    }

    @Test
    public void whenHasDataHistory_ShouldReturnSomeData() throws IOException, URISyntaxException {
        createProjectZipV0(PROJECT_TEST);

        List<FromJiraDataRow> jiraData = subject.getJiraData(PROJECT_TEST).fromJiraDs.rows;

        assertEquals("Jira data size", jiraData.size(), 1);
        assertFollowUpDataDefault(jiraData.get(0));
    }

    @Test
    public void whenDoesNotHaveDataHistory_ShouldThrowAnIllegalStateException() {
        try {
            subject.getJiraData(PROJECT_TEST);
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertNotNull("IllegalStateException shouldn't be null", e);
        }
    }

    @Test
    public void whenHasInvalidDataHistory_ShouldThrowAnIllegalStateException() throws URISyntaxException {
        try {
            Path pathJSON = Paths.get(getClass().getResource("V1_followUpDataHistoryExpected.json").toURI());
            Path zipFile = dataBaseDirectory.path(anyString()).resolve(PROJECT_TEST).resolve(YESTERDAY + EXTENSION_JSON + EXTENSION_ZIP);
            zip(pathJSON, zipFile);

            subject.getJiraData(PROJECT_TEST);
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertNotNull("IllegalStateException shouldn't be null", e);
        }
    }

    @Test
    public void whenTwoProjectsHasDataHistory_ShouldReturnSomeData() throws IOException,  URISyntaxException {
        createProjectZipV0(PROJECT_TEST);
        createProjectZipV0(PROJECT_TEST_2);

        List<FromJiraDataRow> jiraData = subject.getJiraData(PROJECT_TEST, PROJECT_TEST_2).fromJiraDs.rows;

        assertEquals("Jira data size", jiraData.size(), 2);
        assertFollowUpDataDefault(jiraData.get(0));
        assertFollowUpDataDefault(jiraData.get(1));
    }

    @Test
    public void whenHasDataHistoryWithCfd_shouldRestoreSyntheticsDataSources() throws IOException, URISyntaxException {
        // given
        createProjectZipV1(PROJECT_TEST);

        // when
        FollowupData data = subject.getJiraData(PROJECT_TEST);

        // then
        assertThat(data).isEqualToComparingFieldByFieldRecursively(getDefaultFollowupData());
    }

    @Test
    public void whenHasDataHistoryWithCfdAndPassesZoneId_shouldRecalculateAnalyticsAndSynthetics() throws IOException, URISyntaxException {
        // given
        createProjectZipV1(PROJECT_TEST);
        ZoneId saoPauloTZ = ZoneId.of("America/Sao_Paulo"); // -03:00, no conversion
        ZoneId torontoTZ = ZoneId.of("America/Toronto"); // -04:00, changes date and hours
        ZoneId sydneyTZ = ZoneId.of("Australia/Sydney"); // +10:00, same day, different hours

        // when
        FollowupData dataSaoPaulo = subject.getJiraData(PROJECT_TEST.split(","), saoPauloTZ);
        FollowupData dataToronto = subject.getJiraData(PROJECT_TEST.split(","), torontoTZ);
        FollowupData dataSydney = subject.getJiraData(PROJECT_TEST.split(","), sydneyTZ);

        // then
        List<ZonedDateTime> saoPauloAnalyticsDates = dataSaoPaulo.analyticsTransitionsDsList.get(0).rows.get(0).transitionsDates;
        assertEquals(saoPauloAnalyticsDates.get(0), DateTimeUtils.parseDateTime("2017-09-27", "00:00:00", saoPauloTZ));
        assertEquals(saoPauloAnalyticsDates.get(1), DateTimeUtils.parseDateTime("2017-09-26", "00:00:00", saoPauloTZ));
        assertEquals(saoPauloAnalyticsDates.get(2), DateTimeUtils.parseDateTime("2017-09-25", "00:00:00", saoPauloTZ));

        List<ZonedDateTime> torontoAnalyticsDates = dataToronto.analyticsTransitionsDsList.get(0).rows.get(0).transitionsDates;
        assertEquals(torontoAnalyticsDates.get(0), DateTimeUtils.parseDateTime("2017-09-26", "23:00:00", torontoTZ));
        assertEquals(torontoAnalyticsDates.get(1), DateTimeUtils.parseDateTime("2017-09-25", "23:00:00", torontoTZ));
        assertEquals(torontoAnalyticsDates.get(2), DateTimeUtils.parseDateTime("2017-09-24", "23:00:00", torontoTZ));

        List<ZonedDateTime> sydneyAnalyticsDates = dataSydney.analyticsTransitionsDsList.get(0).rows.get(0).transitionsDates;
        assertEquals(sydneyAnalyticsDates.get(0), DateTimeUtils.parseDateTime("2017-09-27", "13:00:00", sydneyTZ));
        assertEquals(sydneyAnalyticsDates.get(1), DateTimeUtils.parseDateTime("2017-09-26", "13:00:00", sydneyTZ));
        assertEquals(sydneyAnalyticsDates.get(2), DateTimeUtils.parseDateTime("2017-09-25", "13:00:00", sydneyTZ));

        List<SyntheticTransitionsDataRow> saoPauloSyntheticRows = dataSaoPaulo.syntheticsTransitionsDsList.get(0).rows;
        assertEquals(saoPauloSyntheticRows.get(0).date, DateTimeUtils.parseDate("2017-09-25", saoPauloTZ));
        assertEquals(saoPauloSyntheticRows.get(1).date, DateTimeUtils.parseDate("2017-09-26", saoPauloTZ));
        assertEquals(saoPauloSyntheticRows.get(2).date, DateTimeUtils.parseDate("2017-09-27", saoPauloTZ));

        List<SyntheticTransitionsDataRow> torontoSyntheticRows = dataToronto.syntheticsTransitionsDsList.get(0).rows;
        assertEquals(torontoSyntheticRows.get(0).date, DateTimeUtils.parseDate("2017-09-24", torontoTZ));
        assertEquals(torontoSyntheticRows.get(1).date, DateTimeUtils.parseDate("2017-09-25", torontoTZ));
        assertEquals(torontoSyntheticRows.get(2).date, DateTimeUtils.parseDate("2017-09-26", torontoTZ));

        List<SyntheticTransitionsDataRow> sydneySyntheticsRows = dataSydney.syntheticsTransitionsDsList.get(0).rows;
        assertEquals(sydneySyntheticsRows.get(0).date, DateTimeUtils.parseDate("2017-09-25", sydneyTZ));
        assertEquals(sydneySyntheticsRows.get(1).date, DateTimeUtils.parseDate("2017-09-26", sydneyTZ));
        assertEquals(sydneySyntheticsRows.get(2).date, DateTimeUtils.parseDate("2017-09-27", sydneyTZ));
    }

    @After
    public void after() {
        deleteQuietly(dataBaseDirectory.path(anyString()).toFile());
    }

    private Path createProjectZipV0(String project) throws IOException, URISyntaxException {
        return createProjectZip(project, "V0_followUpDataHistoryExpected.json");
    }

    private Path createProjectZipV1(String project) throws IOException, URISyntaxException {
        return createProjectZip(project, "V1_followUpDataHistoryExpected.json");
    }

    private Path createProjectZip(String project, String contentFile) throws IOException, URISyntaxException {
        Path pathProject = dataBaseDirectory.path(anyString()).resolve(project);
        createDirectories(pathProject);

        Path pathInputJSON = Paths.get(getClass().getResource(contentFile).toURI());
        Path pathOutputJSON = pathProject.resolve(YESTERDAY + EXTENSION_JSON);
        copy(pathInputJSON, pathOutputJSON);

        Path zipFile = Paths.get(pathOutputJSON.toString() + EXTENSION_ZIP);
        zip(pathOutputJSON, zipFile);
        return pathProject;
    }
}
