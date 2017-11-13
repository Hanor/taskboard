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
import static java.util.Collections.singletonList;
import static objective.taskboard.followup.impl.FollowUpTransitionsDataProvider.TYPE_DEMAND;
import static objective.taskboard.followup.impl.FollowUpTransitionsDataProvider.TYPE_FEATURES;
import static objective.taskboard.followup.impl.FollowUpTransitionsDataProvider.TYPE_SUBTASKS;
import static objective.taskboard.utils.IOUtilities.resourceToString;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import objective.taskboard.Constants;
import objective.taskboard.followup.impl.FollowUpDataHistoryGeneratorJSONFilesTest;
import objective.taskboard.utils.DateTimeUtils;
import objective.taskboard.utils.DateTimeUtils.ZonedDateTimeAdapter;

public class FollowUpHelper {

    public static FromJiraDataRow getDefaultFromJiraDataRow() {
        FromJiraDataRow followUpData = new FromJiraDataRow();
        followUpData.planningType = "Ballpark";
        followUpData.project = "PROJECT TEST";
        followUpData.demandType = "Demand";
        followUpData.demandStatus = "Doing";
        followUpData.demandId = 1L;
        followUpData.demandNum = "I-1";
        followUpData.demandSummary = "Summary Demand";
        followUpData.demandDescription = "Description Demand";
        followUpData.taskType = "Feature";
        followUpData.taskStatus = "Doing";
        followUpData.taskId = 2L;
        followUpData.taskNum = "I-2";
        followUpData.taskSummary = "Summary Feature";
        followUpData.taskDescription = "Description Feature";
        followUpData.taskFullDescription = "Full Description Feature";
        followUpData.taskRelease = "Release";
        followUpData.subtaskType = "Sub-task";
        followUpData.subtaskStatus = "Doing";
        followUpData.subtaskId = 3L;
        followUpData.subtaskNum = "I-3";
        followUpData.subtaskSummary = "Summary Sub-task";
        followUpData.subtaskDescription = "Description Sub-task";
        followUpData.subtaskFullDescription = "Full Description Sub-task";
        followUpData.tshirtSize = "M";
        followUpData.worklog = 1D;
        followUpData.wrongWorklog = 1D;
        followUpData.demandBallpark = 1D;
        followUpData.taskBallpark = 1D;
        followUpData.queryType = "Type";
        return followUpData;
    }

    public static FollowupData getDefaultFollowupData() {
        return new FollowupData(new FromJiraDataSet(Constants.FROMJIRA_HEADERS, getDefaultFromJiraDataRowList()),
                getDefaultAnalyticsTransitionsDataSet(), getDefaultSyntheticTransitionsDataSet());
    }

    public static FollowupData getEmptyFollowupData() {
        return new FollowupData(new FromJiraDataSet(Constants.FROMJIRA_HEADERS, emptyList()), emptyList(), emptyList());
    }

    public static List<FromJiraDataRow> getDefaultFromJiraDataRowList() {
        return singletonList(getDefaultFromJiraDataRow());
    }

    public static FollowupData getFromFile() {
        Gson gson = new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter()).create();

        try(InputStream file = FollowUpHelper.class.getResourceAsStream("jiradata.json")) {
            return gson.fromJson(new InputStreamReader(file), FollowupData.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void toJsonFile(FollowupData data, File file) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
                .setPrettyPrinting()
                .create();

        try(OutputStream stream = new FileOutputStream(file)) {
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            gson.toJson(data, writer);
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void assertFollowUpDataDefault(FromJiraDataRow actual) {
        FromJiraDataRow expected = getDefaultFromJiraDataRow();
        assertEquals("planningType", expected.planningType, actual.planningType);
        assertEquals("project", expected.project, actual.project);
        assertEquals("demandType", expected.demandType, actual.demandType);
        assertEquals("demandStatus", expected.demandStatus, actual.demandStatus);
        assertEquals("demandId", expected.demandId, actual.demandId);
        assertEquals("demandNum", expected.demandNum, actual.demandNum);
        assertEquals("demandSummary", expected.demandSummary, actual.demandSummary);
        assertEquals("demandDescription", expected.demandDescription, actual.demandDescription);
        assertEquals("taskType", expected.taskType, actual.taskType);
        assertEquals("taskStatus", expected.taskStatus, actual.taskStatus);
        assertEquals("taskId", expected.taskId, actual.taskId);
        assertEquals("taskNum", expected.taskNum, actual.taskNum);
        assertEquals("taskSummary", expected.taskSummary, actual.taskSummary);
        assertEquals("taskDescription", expected.taskDescription, actual.taskDescription);
        assertEquals("taskFullDescription", expected.taskFullDescription, actual.taskFullDescription);
        assertEquals("taskRelease", expected.taskRelease, actual.taskRelease);
        assertEquals("subtaskType", expected.subtaskType, actual.subtaskType);
        assertEquals("subtaskStatus", expected.subtaskStatus, actual.subtaskStatus);
        assertEquals("subtaskId", expected.subtaskId, actual.subtaskId);
        assertEquals("subtaskNum", expected.subtaskNum, actual.subtaskNum);
        assertEquals("subtaskSummary", expected.subtaskSummary, actual.subtaskSummary);
        assertEquals("subtaskDescription", expected.subtaskDescription, actual.subtaskDescription);
        assertEquals("subtaskFullDescription", expected.subtaskFullDescription, actual.subtaskFullDescription);
        assertEquals("tshirtSize", expected.tshirtSize, actual.tshirtSize);
        assertEquals("worklog", expected.worklog, actual.worklog);
        assertEquals("wrongWorklog", expected.wrongWorklog, actual.wrongWorklog);
        assertEquals("demandBallpark", expected.demandBallpark, actual.demandBallpark);
        assertEquals("taskBallpark", expected.taskBallpark, actual.taskBallpark);
        assertEquals("queryType", expected.queryType, actual.queryType);
    }

    public static List<AnalyticsTransitionsDataSet> getDefaultAnalyticsTransitionsDataSet() {
        List<String> headers = new LinkedList<>();
        headers.add("PKEY");
        headers.add("ISSUE_TYPE");
        headers.add("Done");
        headers.add("Doing");
        headers.add("To Do");

        AnalyticsTransitionsDataRow rowDemand = new AnalyticsTransitionsDataRow("I-1", "Demand"
                , asList(
                        DateTimeUtils.parseDate("2017-09-27")
                        , DateTimeUtils.parseDate("2017-09-26")
                        , DateTimeUtils.parseDate("2017-09-25")));

        AnalyticsTransitionsDataRow rowFeature = new AnalyticsTransitionsDataRow("I-2", "Feature"
                , asList(
                        null
                        , DateTimeUtils.parseDate("2017-09-26")
                        , DateTimeUtils.parseDate("2017-09-25")));

        AnalyticsTransitionsDataRow rowSubtask = new AnalyticsTransitionsDataRow("I-3", "Sub-task"
                , asList(
                        null
                        , null
                        , DateTimeUtils.parseDate("2017-09-25")));

        return asList(new AnalyticsTransitionsDataSet(TYPE_DEMAND, headers, asList(rowDemand)),
                new AnalyticsTransitionsDataSet(TYPE_FEATURES, headers, asList(rowFeature)),
                new AnalyticsTransitionsDataSet(TYPE_SUBTASKS, headers, asList(rowSubtask)));
    }

    public static List<AnalyticsTransitionsDataSet> getEmptyAnalyticsTransitionsDataSet() {
        return singletonList(new AnalyticsTransitionsDataSet(TYPE_DEMAND, emptyList(), emptyList()));
    }

    public static List<SyntheticTransitionsDataSet> getDefaultSyntheticTransitionsDataSet() {
        List<String> headers = new LinkedList<>();
        headers.add("Date");
        headers.add("Done");
        headers.add("Doing");
        headers.add("To Do");

        List<SyntheticTransitionsDataRow> rowsDemand = new LinkedList<>();
        rowsDemand.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-25"), Ints.asList(0, 0, 1)));
        rowsDemand.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-26"), Ints.asList(0, 1, 0)));
        rowsDemand.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-27"), Ints.asList(1, 0, 0)));

        List<SyntheticTransitionsDataRow> rowsFeature = new LinkedList<>();
        rowsFeature.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-25"), Ints.asList(0, 0, 1)));
        rowsFeature.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-26"), Ints.asList(0, 1, 0)));

        List<SyntheticTransitionsDataRow> rowsSubtask = new LinkedList<>();
        rowsSubtask.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-25"), Ints.asList(0, 0, 1)));

        return asList(new SyntheticTransitionsDataSet(TYPE_DEMAND, headers, rowsDemand),
                new SyntheticTransitionsDataSet(TYPE_FEATURES, headers, rowsFeature),
                new SyntheticTransitionsDataSet(TYPE_SUBTASKS, headers, rowsSubtask));
    }

    public static List<SyntheticTransitionsDataSet> getEmptySyntheticTransitionsDataSet() {
        return singletonList(new SyntheticTransitionsDataSet(TYPE_DEMAND, emptyList(), emptyList()));
    }

    public static String followupEmptyV0() {
        return "[]";
    }
    public static String followupExpectedV0() {
        return resourceToString(FollowUpDataHistoryGeneratorJSONFilesTest.class, "V0_followUpDataHistoryExpected.json");
    }
    public static String followupEmptyV1() {
        return resourceToString(FollowUpDataHistoryGeneratorJSONFilesTest.class, "V1_followupDataHistory_empty.json");
    }
    public static String followupExpectedV1() {
        return resourceToString(FollowUpDataHistoryGeneratorJSONFilesTest.class, "V1_followUpDataHistoryExpected.json");
    }
}
