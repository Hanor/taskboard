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

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.size;
import static objective.taskboard.followup.FollowUpDataRepositoryByFile.EXTENSION_JSON;
import static objective.taskboard.followup.FollowUpDataRepositoryByFile.EXTENSION_ZIP;
import static objective.taskboard.followup.FollowUpDataRepositoryByFile.FILE_NAME_FORMATTER;
import static objective.taskboard.followup.FollowUpHelper.assertFollowUpDataV0;
import static objective.taskboard.followup.FollowUpHelper.followupEmptyV2;
import static objective.taskboard.followup.FollowUpHelper.followupExpectedV2;
import static objective.taskboard.followup.FollowUpHelper.getDefaultFollowupData;
import static objective.taskboard.followup.FollowUpHelper.getEmptyFollowupData;
import static objective.taskboard.utils.IOUtilities.ENCODE_UTF_8;
import static objective.taskboard.utils.IOUtilities.asResource;
import static objective.taskboard.utils.ZipUtils.unzip;
import static objective.taskboard.utils.ZipUtils.zip;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import objective.taskboard.database.directory.DataBaseDirectory;
import objective.taskboard.rules.TimeZoneRule;
import objective.taskboard.utils.DateTimeUtils;

public class FollowUpDataRepositoryByFileTest {
    private static final ZoneId TIMEZONE = ZoneId.systemDefault();
    private static final String PROJECT_TEST = "PROJECT TEST";
    private static final String PROJECT_TEST_2 = "PROJECT TEST 2";
    private static final LocalDate TODAY_DATE = LocalDate.now();
    private static final LocalDate TODAY = TODAY_DATE;
    private static final LocalDate YESTERDAY = TODAY_DATE.minusDays(1);

    private Path dataPath;
    private FollowUpDataRepository subject;

    @Rule
    public TimeZoneRule timeZoneRule = new TimeZoneRule("America/Sao_Paulo");
    
    @Before
    public void before() throws IOException {
        dataPath = createTempDirectory(getClass().getSimpleName());
        
        DataBaseDirectory dataBaseDirectory = mock(DataBaseDirectory.class);
        when(dataBaseDirectory.path(anyString())).thenReturn(dataPath);

        subject = new FollowUpDataRepositoryByFile(dataBaseDirectory);    
    }
    
    @After
    public void after() {
        deleteQuietly(dataPath.toFile());
    }

    @Test
    public void whenSaveOneProject_thenOneFileShouldBeGenerated() throws IOException {
        subject.save(PROJECT_TEST, TODAY_DATE, getDefaultFollowupData());

        assertGeneratedFile(PROJECT_TEST, followupExpectedV2());
    }

    @Test
    public void whenSaveEmptyData_thenNoDataShouldBeGenerated() throws IOException {
        subject.save(PROJECT_TEST, TODAY_DATE, getEmptyFollowupData());

        assertGeneratedFile(PROJECT_TEST, followupEmptyV2());
    }

    @Test
    public void whenSaveTwoProjects_thenTwoFilesShouldBeGenerated() throws IOException {
        subject.save(PROJECT_TEST, TODAY_DATE, getDefaultFollowupData());
        subject.save(PROJECT_TEST_2, TODAY_DATE, getDefaultFollowupData());

        assertGeneratedFile(PROJECT_TEST, followupExpectedV2());
        assertGeneratedFile(PROJECT_TEST_2, followupExpectedV2());
    }

    @Test
    public void givenProjectWithNoHistory_whenGetHistoryGivenProjects_thenReturnNoData() {
        List<LocalDate> history = subject.getHistoryByProject(PROJECT_TEST);
        assertTrue("History should be empty", history.isEmpty());
    }

    @Test
    public void givenProjectWithTodaysHistory_whenGetHistoryGivenProjects_thenReturnNoData() {
        createProjectZip(PROJECT_TEST, TODAY);

        List<LocalDate> history = subject.getHistoryByProject(PROJECT_TEST);
        assertTrue("History should be empty", history.isEmpty());
    }

    @Test
    public void givenProjectWithYesterdaysHistory_whenGetHistoryGivenProjects_thenReturnData() throws IOException {
        createProjectZip(PROJECT_TEST, YESTERDAY);

        List<LocalDate> history = subject.getHistoryByProject(PROJECT_TEST);
        assertEquals("History size", 1, history.size());
        assertEquals("First history", YESTERDAY, history.get(0));
    }

    @Test
    public void whenHasDataHistory_GetShouldReturnSomeData() throws IOException, URISyntaxException {
        createProjectZipV0(PROJECT_TEST);

        List<FromJiraDataRow> jiraData = subject.get(YESTERDAY, TIMEZONE, PROJECT_TEST).fromJiraDs.rows;

        assertEquals("Jira data size", jiraData.size(), 1);
        assertFollowUpDataV0(jiraData.get(0));
    }

    @Test
    public void whenDoesNotHaveDataHistory_GetShouldThrowAnIllegalStateException() {
        try {
            subject.get(YESTERDAY, TIMEZONE, PROJECT_TEST);
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertNotNull("IllegalStateException shouldn't be null", e);
        }
    }

    @Test
    public void whenHasInvalidDataHistory_GetShouldThrowAnIllegalStateException() throws URISyntaxException {
        try {
            Path pathJSON = Paths.get(getClass().getResource("impl/V1_followUpDataHistoryExpected.json").toURI());
            Path zipFile = dataPath.resolve(PROJECT_TEST).resolve(YESTERDAY + EXTENSION_JSON + EXTENSION_ZIP);
            zip(pathJSON, zipFile);

            subject.get(YESTERDAY, TIMEZONE, PROJECT_TEST);
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertNotNull("IllegalStateException shouldn't be null", e);
        }
    }

    @Test
    public void whenHasDataHistoryWithCfd_GetShouldRestoreSyntheticsDataSources() throws IOException, URISyntaxException {
        // given
        createProjectZipV2(PROJECT_TEST);

        // when
        FollowUpData data = subject.get(YESTERDAY, TIMEZONE, PROJECT_TEST);

        // then
        assertThat(data).isEqualToComparingFieldByFieldRecursively(getDefaultFollowupData());
    }

    @Test
    public void testVersion2() throws IOException, URISyntaxException {
        // given
        createProjectZipV2(PROJECT_TEST);

        // when
        FollowUpData data = subject.get(YESTERDAY, TIMEZONE, PROJECT_TEST);

        // then
        assertThat(data).isEqualToComparingFieldByFieldRecursively(getDefaultFollowupData());
    }

    @Test
    public void whenHasDataHistoryWithCfdAndPassesZoneId_GetShouldRecalculateAnalyticsAndSynthetics() throws IOException, URISyntaxException {
        // given
        createProjectZipV1(PROJECT_TEST);
        ZoneId saoPauloTZ = ZoneId.of("America/Sao_Paulo"); // -03:00, no conversion
        ZoneId torontoTZ = ZoneId.of("America/Toronto"); // -04:00, changes date and hours
        ZoneId sydneyTZ = ZoneId.of("Australia/Sydney"); // +10:00, same day, different hours

        // when
        FollowUpData dataSaoPaulo = subject.get(YESTERDAY, saoPauloTZ, PROJECT_TEST);
        FollowUpData dataToronto = subject.get(YESTERDAY, torontoTZ, PROJECT_TEST);
        FollowUpData dataSydney = subject.get(YESTERDAY, sydneyTZ, PROJECT_TEST);

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
        assertEquals(saoPauloSyntheticRows.get(0).date, DateTimeUtils.parseDateTime("2017-09-25", "00:00:00", saoPauloTZ));
        assertEquals(saoPauloSyntheticRows.get(1).date, DateTimeUtils.parseDateTime("2017-09-25", "00:00:00", saoPauloTZ));
        assertEquals(saoPauloSyntheticRows.get(2).date, DateTimeUtils.parseDateTime("2017-09-26", "00:00:00", saoPauloTZ));
        assertEquals(saoPauloSyntheticRows.get(3).date, DateTimeUtils.parseDateTime("2017-09-26", "00:00:00", saoPauloTZ));
        assertEquals(saoPauloSyntheticRows.get(4).date, DateTimeUtils.parseDateTime("2017-09-27", "00:00:00", saoPauloTZ));
        assertEquals(saoPauloSyntheticRows.get(5).date, DateTimeUtils.parseDateTime("2017-09-27", "00:00:00", saoPauloTZ));

        List<SyntheticTransitionsDataRow> torontoSyntheticRows = dataToronto.syntheticsTransitionsDsList.get(0).rows;
        assertEquals(torontoSyntheticRows.get(0).date, DateTimeUtils.parseDateTime("2017-09-24", "00:00:00", torontoTZ));
        assertEquals(torontoSyntheticRows.get(1).date, DateTimeUtils.parseDateTime("2017-09-24", "00:00:00", torontoTZ));
        assertEquals(torontoSyntheticRows.get(2).date, DateTimeUtils.parseDateTime("2017-09-25", "00:00:00", torontoTZ));
        assertEquals(torontoSyntheticRows.get(3).date, DateTimeUtils.parseDateTime("2017-09-25", "00:00:00", torontoTZ));
        assertEquals(torontoSyntheticRows.get(4).date, DateTimeUtils.parseDateTime("2017-09-26", "00:00:00", torontoTZ));
        assertEquals(torontoSyntheticRows.get(5).date, DateTimeUtils.parseDateTime("2017-09-26", "00:00:00", torontoTZ));

        List<SyntheticTransitionsDataRow> sydneySyntheticsRows = dataSydney.syntheticsTransitionsDsList.get(0).rows;
        assertEquals(sydneySyntheticsRows.get(0).date, DateTimeUtils.parseDateTime("2017-09-25", "00:00:00", sydneyTZ));
        assertEquals(sydneySyntheticsRows.get(1).date, DateTimeUtils.parseDateTime("2017-09-25", "00:00:00", sydneyTZ));
        assertEquals(sydneySyntheticsRows.get(2).date, DateTimeUtils.parseDateTime("2017-09-26", "00:00:00", sydneyTZ));
        assertEquals(sydneySyntheticsRows.get(3).date, DateTimeUtils.parseDateTime("2017-09-26", "00:00:00", sydneyTZ));
        assertEquals(sydneySyntheticsRows.get(4).date, DateTimeUtils.parseDateTime("2017-09-27", "00:00:00", sydneyTZ));
        assertEquals(sydneySyntheticsRows.get(5).date, DateTimeUtils.parseDateTime("2017-09-27", "00:00:00", sydneyTZ));
    }
  
    private void assertGeneratedFile(String project, String dataHistoryExpected) throws IOException {
        String dateString = TODAY.format(FILE_NAME_FORMATTER);
        Path source = dataPath.resolve(project).resolve(dateString + EXTENSION_JSON + EXTENSION_ZIP);

        assertTrue("File should be exist", exists(source));
        assertThat(size(source), greaterThan(0L));

        Path destiny = createTempDirectory("FollowUpDataHistoryAssertTest");
        try {
            unzip(source.toFile(), destiny);
            Path pathJSON = destiny.resolve(dateString + EXTENSION_JSON);
            String actual = IOUtils.toString(asResource(pathJSON).getInputStream(), ENCODE_UTF_8);

            assertEquals("Follow Up data history", dataHistoryExpected, actual);
        } finally {
            deleteQuietly(destiny.toFile());
        }
    }

    private Path createProjectZipV0(String project) throws IOException, URISyntaxException {
        return createProjectZip(project, YESTERDAY, "impl/V0_followUpDataHistoryExpected.json");
    }

    private Path createProjectZipV1(String project) throws IOException, URISyntaxException {
        return createProjectZip(project, YESTERDAY, "impl/V1_followUpDataHistoryExpected.json");
    }

    private Path createProjectZipV2(String project) throws IOException, URISyntaxException {
        return createProjectZip(project, YESTERDAY, "impl/V2_followUpDataHistoryExpected.json");
    }
    
    private Path createProjectZip(String project, LocalDate date) {
        return createProjectZip(project, date, "impl/V2_followUpDataHistoryExpected.json");
    }
    
    private Path createProjectZip(String projectKey, LocalDate date, String file) {
        try {
            Path pathProject = dataPath.resolve(projectKey);
            createDirectories(pathProject);

            Path pathInputJSON = Paths.get(getClass().getResource(file).toURI());
            Path pathOutputJSON = pathProject.resolve(date.format(FILE_NAME_FORMATTER) + EXTENSION_JSON);
            copy(pathInputJSON, pathOutputJSON);

            Path zipFile = Paths.get(pathOutputJSON.toString() + EXTENSION_ZIP);
            zip(pathOutputJSON, zipFile);
            return pathProject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
