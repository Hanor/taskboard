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
import static objective.taskboard.utils.DateTimeUtils.parseDateList;
import static objective.taskboard.utils.IOUtilities.resourceToString;
import static org.apache.commons.lang.ObjectUtils.defaultIfNull;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import objective.taskboard.Constants;
import objective.taskboard.followup.impl.FollowUpTransitionsDataProvider;
import objective.taskboard.utils.DateTimeUtils;
import objective.taskboard.utils.DateTimeUtils.ZonedDateTimeAdapter;

public class FollowUpHelper {

    private static final String TIMEZONE_ID = "America/Sao_Paulo";
    
    public static FromJiraDataRow getDefaultFromJiraDataRow() {
        return getDefaultFromJiraDataRow("Doing", 1D, "M", "Type");
    }
    
    public static FromJiraDataRow getDefaultFromJiraDataRow(String subtaskStatus, Double taskballpark, String tshirtSize, String queryPlan) {
        FromJiraDataRow followUpData = new FromJiraDataRow();
        followUpData.planningType = "Ballpark";
        followUpData.project = "PROJECT TEST";
        followUpData.demandType = "Demand";
        followUpData.demandStatus = "Doing";
        followUpData.demandId = 1L;
        followUpData.demandNum = "I-1";
        followUpData.demandSummary = "Summary Demand";
        followUpData.demandDescription = "Description Demand";
        followUpData.demandStatusPriority = 0;
        followUpData.demandPriorityOrder = 0l;
        followUpData.demandStartDateStepMillis = 0L;
        followUpData.demandAssignee = "assignee.demand.test";
        followUpData.demandDueDate = DateTimeUtils.get(DateTimeUtils.parseDate("2025-05-25"), ZoneId.of(TIMEZONE_ID));
        followUpData.demandCreated = DateTimeUtils.get(DateTimeUtils.parseDate("2012-01-01"), ZoneId.of(TIMEZONE_ID));
        followUpData.demandLabels = "";
        followUpData.demandComponents = "";
        followUpData.demandReporter = "reporter.demand.test";
        followUpData.demandCoAssignees = "";
        followUpData.demandClassOfService = "Standard";
        followUpData.demandUpdatedDate = DateTimeUtils.get(DateTimeUtils.parseDate("2012-02-01"), ZoneId.of(TIMEZONE_ID));
        followUpData.demandCycletime = 1.111111;
        followUpData.demandIsBlocked = false;
        followUpData.demandLastBlockReason = "Demand last block reason";

        followUpData.taskType = "Feature";
        followUpData.taskStatus = subtaskStatus;
        followUpData.taskId = 2L;
        followUpData.taskNum = "I-2";
        followUpData.taskSummary = "Summary Feature";
        followUpData.taskDescription = "Description Feature";
        followUpData.taskFullDescription = "Full Description Feature";
        followUpData.taskAdditionalEstimatedHours = 80.0;
        followUpData.taskRelease = "Release";
        followUpData.taskStatusPriority = 0;
        followUpData.taskPriorityOrder = 0l;
        followUpData.taskStartDateStepMillis = 0L;
        followUpData.taskAssignee = "assignee.task.test";
        followUpData.taskDueDate = DateTimeUtils.get(DateTimeUtils.parseDate("2025-05-24"), ZoneId.of(TIMEZONE_ID));
        followUpData.taskCreated = DateTimeUtils.get(DateTimeUtils.parseDate("2012-01-02"), ZoneId.of(TIMEZONE_ID));
        followUpData.taskLabels = "";
        followUpData.taskComponents = "";
        followUpData.taskReporter = "reporter.demand.test";
        followUpData.taskCoAssignees = "";
        followUpData.taskClassOfService = "Standard";
        followUpData.taskUpdatedDate = DateTimeUtils.get(DateTimeUtils.parseDate("2012-02-02"), ZoneId.of(TIMEZONE_ID));
        followUpData.taskCycletime = 2.222222;
        followUpData.taskIsBlocked = false;
        followUpData.taskLastBlockReason = "Task last block reason";

        followUpData.subtaskType = "Sub-task";
        followUpData.subtaskStatus = subtaskStatus;
        followUpData.subtaskId = 3L;
        followUpData.subtaskNum = "I-3";
        followUpData.subtaskSummary = "Summary Sub-task";
        followUpData.subtaskDescription = "Description Sub-task";
        followUpData.subtaskFullDescription = "Full Description Sub-task";
        followUpData.subtaskStatusPriority = 0;
        followUpData.subtaskPriorityOrder = 0l;
        followUpData.subtaskStartDateStepMillis = 0L;
        followUpData.subtaskAssignee = "assignee.subtask.test";
        followUpData.subtaskDueDate = DateTimeUtils.get(DateTimeUtils.parseDate("2025-05-23"), ZoneId.of(TIMEZONE_ID));
        followUpData.subtaskCreated = DateTimeUtils.get(DateTimeUtils.parseDate("2012-01-03"), ZoneId.of(TIMEZONE_ID));
        followUpData.subtaskLabels = "";
        followUpData.subtaskComponents = "";
        followUpData.subtaskReporter = "reporter.subtask.test";
        followUpData.subtaskCoAssignees = "";
        followUpData.subtaskClassOfService = "Standard";
        followUpData.subtaskUpdatedDate = DateTimeUtils.get(DateTimeUtils.parseDate("2012-02-03"), ZoneId.of(TIMEZONE_ID));
        followUpData.subtaskCycletime = 3.333333;
        followUpData.subtaskIsBlocked = false;
        followUpData.subtaskLastBlockReason = "Subtask last block reason";

        followUpData.tshirtSize = tshirtSize;
        followUpData.worklog = 1D;
        followUpData.wrongWorklog = 1D;
        followUpData.demandBallpark = 1D;
        followUpData.taskBallpark = taskballpark;
        followUpData.queryType = queryPlan;

        return followUpData;
    }

    public static FollowupData getDefaultFollowupData() {
        return new FollowupData(new FromJiraDataSet(Constants.FROMJIRA_HEADERS, getDefaultFromJiraDataRowList()),
                getDefaultAnalyticsTransitionsDataSet(), getDefaultSyntheticTransitionsDataSet());
    }

    public static FollowupData getBiggerFollowupData() {
        return new FollowupData(new FromJiraDataSet(Constants.FROMJIRA_HEADERS, getDefaultFromJiraDataRowList()),
                getBiggerAnalyticsTransitionsDataSet(), getBiggerSyntheticTransitionsDataSet());
    }

    public static FollowupData getEmptyFollowupData() {
        return new FollowupData(new FromJiraDataSet(Constants.FROMJIRA_HEADERS, emptyList()), emptyList(), emptyList());
    }

    public static FollowupData getFollowupData(FromJiraDataRow... rows) {
        return new FollowupData(new FromJiraDataSet(Constants.FROMJIRA_HEADERS, asList(rows)), emptyList(), emptyList());
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
        assertEquals("demandStatusPriority", expected.demandStatusPriority, actual.demandStatusPriority);
        assertEquals("demandPriorityOrder", expected.demandPriorityOrder, actual.demandPriorityOrder);
        assertEquals("demandStartDateStepMillis", expected.demandStartDateStepMillis, actual.demandStartDateStepMillis);
        assertEquals("demandAssignee", expected.demandAssignee, actual.demandAssignee);
        assertEquals("demandDueDate", expected.demandDueDate, actual.demandDueDate);
        assertEquals("demandCreated", expected.demandCreated, actual.demandCreated);
        assertEquals("demandLabels", expected.demandLabels, actual.demandLabels);
        assertEquals("demandComponents", expected.demandComponents, actual.demandComponents);
        assertEquals("demandReporter", expected.demandReporter, actual.demandReporter);
        assertEquals("demandCoAssignees", expected.demandCoAssignees, actual.demandCoAssignees);
        assertEquals("demandClassOfService", expected.demandClassOfService, actual.demandClassOfService);
        assertEquals("demandUpdatedDate", expected.demandUpdatedDate, actual.demandUpdatedDate);
        assertEquals("demandCycletime", expected.demandCycletime, actual.demandCycletime);
        assertEquals("demandIsBlocked", expected.demandIsBlocked, actual.demandIsBlocked);
        assertEquals("demandLastBlockReason", expected.demandLastBlockReason, actual.demandLastBlockReason);
        assertEquals("taskType", expected.taskType, actual.taskType);
        assertEquals("taskStatus", expected.taskStatus, actual.taskStatus);
        assertEquals("taskId", expected.taskId, actual.taskId);
        assertEquals("taskNum", expected.taskNum, actual.taskNum);
        assertEquals("taskSummary", expected.taskSummary, actual.taskSummary);
        assertEquals("taskDescription", expected.taskDescription, actual.taskDescription);
        assertEquals("taskFullDescription", expected.taskFullDescription, actual.taskFullDescription);
        assertEquals("taskAdditionalEstimatedHours", expected.taskAdditionalEstimatedHours, actual.taskAdditionalEstimatedHours);
        assertEquals("taskRelease", expected.taskRelease, actual.taskRelease);
        assertEquals("taskStatusPriority", expected.taskStatusPriority, actual.taskStatusPriority);
        assertEquals("taskPriorityOrder", expected.taskPriorityOrder, actual.taskPriorityOrder);
        assertEquals("taskStartDateStepMillis", expected.taskStartDateStepMillis, actual.taskStartDateStepMillis);
        assertEquals("taskAssignee", expected.taskAssignee, actual.taskAssignee);
        assertEquals("taskDueDate", expected.taskDueDate, actual.taskDueDate);
        assertEquals("taskCreated", expected.taskCreated, actual.taskCreated);
        assertEquals("taskLabels", expected.taskLabels, actual.taskLabels);
        assertEquals("taskComponents", expected.taskComponents, actual.taskComponents);
        assertEquals("taskReporter", expected.taskReporter, actual.taskReporter);
        assertEquals("taskCoAssignees", expected.taskCoAssignees, actual.taskCoAssignees);
        assertEquals("taskClassOfService", expected.taskClassOfService, actual.taskClassOfService);
        assertEquals("taskUpdatedDate", expected.taskUpdatedDate, actual.taskUpdatedDate);
        assertEquals("taskCycletime", expected.taskCycletime, actual.taskCycletime);
        assertEquals("taskIsBlocked", expected.taskIsBlocked, actual.taskIsBlocked);
        assertEquals("taskLastBlockReason", expected.taskLastBlockReason, actual.taskLastBlockReason);
        assertEquals("subtaskType", expected.subtaskType, actual.subtaskType);
        assertEquals("subtaskStatus", expected.subtaskStatus, actual.subtaskStatus);
        assertEquals("subtaskId", expected.subtaskId, actual.subtaskId);
        assertEquals("subtaskNum", expected.subtaskNum, actual.subtaskNum);
        assertEquals("subtaskSummary", expected.subtaskSummary, actual.subtaskSummary);
        assertEquals("subtaskDescription", expected.subtaskDescription, actual.subtaskDescription);
        assertEquals("subtaskFullDescription", expected.subtaskFullDescription, actual.subtaskFullDescription);
        assertEquals("subtaskStatusPriority", expected.subtaskStatusPriority, actual.subtaskStatusPriority);
        assertEquals("subtaskPriorityOrder", expected.subtaskPriorityOrder, actual.subtaskPriorityOrder);
        assertEquals("subtaskStartDateStepMillis", expected.subtaskStartDateStepMillis, actual.subtaskStartDateStepMillis);
        assertEquals("subtaskAssignee", expected.subtaskAssignee, actual.subtaskAssignee);
        assertEquals("subtaskDueDate", expected.subtaskDueDate, actual.subtaskDueDate);
        assertEquals("subtaskCreated", expected.subtaskCreated, actual.subtaskCreated);
        assertEquals("subtaskLabels", expected.subtaskLabels, actual.subtaskLabels);
        assertEquals("subtaskComponents", expected.subtaskComponents, actual.subtaskComponents);
        assertEquals("subtaskReporter", expected.subtaskReporter, actual.subtaskReporter);
        assertEquals("subtaskCoAssignees", expected.subtaskCoAssignees, actual.subtaskCoAssignees);
        assertEquals("subtaskClassOfService", expected.subtaskClassOfService, actual.subtaskClassOfService);
        assertEquals("subtaskUpdatedDate", expected.subtaskUpdatedDate, actual.subtaskUpdatedDate);
        assertEquals("subtaskCycletime", expected.subtaskCycletime, actual.subtaskCycletime);
        assertEquals("subtaskIsBlocked", expected.subtaskIsBlocked, actual.subtaskIsBlocked);
        assertEquals("subtaskLastBlockReason", expected.subtaskLastBlockReason, actual.subtaskLastBlockReason);
        assertEquals("tshirtSize", expected.tshirtSize, actual.tshirtSize);
        assertEquals("worklog", expected.worklog, actual.worklog);
        assertEquals("wrongWorklog", expected.wrongWorklog, actual.wrongWorklog);
        assertEquals("demandBallpark", expected.demandBallpark, actual.demandBallpark);
        assertEquals("taskBallpark", expected.taskBallpark, actual.taskBallpark);
        assertEquals("queryType", expected.queryType, actual.queryType);
    }

    public static void assertFollowUpDataV0(FromJiraDataRow actual) {
        FromJiraDataRow expected = getDefaultFromJiraDataRow();
        assertEquals("planningType", expected.planningType, actual.planningType);
        assertEquals("project", expected.project, actual.project);
        assertEquals("demandType", expected.demandType, actual.demandType);
        assertEquals("demandStatus", expected.demandStatus, actual.demandStatus);
        assertEquals("demandId", expected.demandId, actual.demandId);
        assertEquals("demandNum", expected.demandNum, actual.demandNum);
        assertEquals("demandSummary", expected.demandSummary, actual.demandSummary);
        assertEquals("demandDescription", expected.demandDescription, actual.demandDescription);
        assertEquals("demandStatusPriority", expected.demandStatusPriority, actual.demandStatusPriority);
        assertEquals("demandPriorityOrder", expected.demandPriorityOrder, actual.demandPriorityOrder);
        assertEquals("taskType", expected.taskType, actual.taskType);
        assertEquals("taskStatus", expected.taskStatus, actual.taskStatus);
        assertEquals("taskId", expected.taskId, actual.taskId);
        assertEquals("taskNum", expected.taskNum, actual.taskNum);
        assertEquals("taskSummary", expected.taskSummary, actual.taskSummary);
        assertEquals("taskDescription", expected.taskDescription, actual.taskDescription);
        assertEquals("taskFullDescription", expected.taskFullDescription, actual.taskFullDescription);
        assertEquals("taskRelease", expected.taskRelease, actual.taskRelease);
        assertEquals("taskStatusPriority", expected.taskStatusPriority, actual.taskStatusPriority);
        assertEquals("taskPriorityOrder", expected.taskPriorityOrder, actual.taskPriorityOrder);
        assertEquals("subtaskType", expected.subtaskType, actual.subtaskType);
        assertEquals("subtaskStatus", expected.subtaskStatus, actual.subtaskStatus);
        assertEquals("subtaskId", expected.subtaskId, actual.subtaskId);
        assertEquals("subtaskNum", expected.subtaskNum, actual.subtaskNum);
        assertEquals("subtaskSummary", expected.subtaskSummary, actual.subtaskSummary);
        assertEquals("subtaskDescription", expected.subtaskDescription, actual.subtaskDescription);
        assertEquals("subtaskFullDescription", expected.subtaskFullDescription, actual.subtaskFullDescription);
        assertEquals("subtaskStatusPriority", expected.subtaskStatusPriority, actual.subtaskStatusPriority);
        assertEquals("subtaskPriorityOrder", expected.subtaskPriorityOrder, actual.subtaskPriorityOrder);
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

        AnalyticsTransitionsDataRow rowOs = new AnalyticsTransitionsDataRow("I-4", "OS"
                , asList(
                        null
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

        return asList(new AnalyticsTransitionsDataSet(TYPE_DEMAND, headers, asList(rowDemand, rowOs)),
                new AnalyticsTransitionsDataSet(TYPE_FEATURES, headers, asList(rowFeature)),
                new AnalyticsTransitionsDataSet(TYPE_SUBTASKS, headers, asList(rowSubtask)));
    }

    public static List<AnalyticsTransitionsDataSet> getBiggerAnalyticsTransitionsDataSet() {
        List<String> demandHeaders = new LinkedList<>();
        demandHeaders.add("PKEY");
        demandHeaders.add("ISSUE_TYPE");
        demandHeaders.add("Done");
        demandHeaders.add("Doing");
        demandHeaders.add("To Do");

        List<String> featureHeaders = new LinkedList<>();
        featureHeaders.add("PKEY");
        featureHeaders.add("ISSUE_TYPE");
        featureHeaders.add("Done");
        featureHeaders.add("QA");
        featureHeaders.add("Doing");
        featureHeaders.add("To Do");

        List<String> subTaskHeaders = new LinkedList<>();
        subTaskHeaders.add("PKEY");
        subTaskHeaders.add("ISSUE_TYPE");
        subTaskHeaders.add("Done");
        subTaskHeaders.add("UAT");
        subTaskHeaders.add("Reviewing");
        subTaskHeaders.add("Doing");
        subTaskHeaders.add("To Do");

        AnalyticsTransitionsDataRow[] demands = new AnalyticsTransitionsDataRow[6];
        AnalyticsTransitionsDataRow[] features = new AnalyticsTransitionsDataRow[5];
        AnalyticsTransitionsDataRow[] subTasks = new AnalyticsTransitionsDataRow[12];
        demands[0] = new AnalyticsTransitionsDataRow("TASKB-1", "Demand"
                , parseDateList("2000-01-05", "2000-01-03", "2000-01-01"));
        demands[1] = new AnalyticsTransitionsDataRow("TASKB-2", "Demand"
                , parseDateList("2000-01-06", "2000-01-04", "2000-01-02"));
        demands[2] = new AnalyticsTransitionsDataRow("TASKB-3", "Demand"
                , parseDateList(null, "2000-01-04", "2000-01-01"));
        demands[3] = new AnalyticsTransitionsDataRow("TASKB-4", "OS"
                , parseDateList("2000-01-07", "2000-01-05", "2000-01-03"));
        demands[4] = new AnalyticsTransitionsDataRow("TASKB-5", "OS"
                , parseDateList("2000-01-08", "2000-01-06", "2000-01-04"));
        demands[5] = new AnalyticsTransitionsDataRow("TASKB-6", "OS"
                , parseDateList(null, "2000-01-06", "2000-01-03"));

        features[0] = new AnalyticsTransitionsDataRow("TASKB-7", "Feature"
                , parseDateList(null, "2000-01-04", "2000-01-03", "2000-01-02"));
        features[1] = new AnalyticsTransitionsDataRow("TASKB-8", "Feature"
                , parseDateList(null, null, "2000-01-04", "2000-01-03"));
        features[2] = new AnalyticsTransitionsDataRow("TASKB-9", "Feature"
                , parseDateList("2000-01-07", "2000-01-06", "2000-01-05", "2000-01-04"));
        features[3] = new AnalyticsTransitionsDataRow("TASKB-10", "Feature"
                , parseDateList(null, "2000-01-06", "2000-01-05", "2000-01-04"));
        features[4] = new AnalyticsTransitionsDataRow("TASKB-11", "Feature"
                , parseDateList("2000-01-08", "2000-01-07", "2000-01-06", "2000-01-05"));

        subTasks[0] = new AnalyticsTransitionsDataRow("TASKB-12", "Development"
                , parseDateList("2000-01-09", "2000-01-08", "2000-01-07", "2000-01-06", "2000-01-05"));
        subTasks[1] = new AnalyticsTransitionsDataRow("TASKB-13", "Development"
                , parseDateList("2000-01-11", "2000-01-10", "2000-01-09", "2000-01-07", "2000-01-05"));
        subTasks[2] = new AnalyticsTransitionsDataRow("TASKB-14", "Development"
                , parseDateList(null, null, "2000-01-08", "2000-01-08", "2000-01-06"));
        subTasks[3] = new AnalyticsTransitionsDataRow("TASKB-15", "Development"
                , parseDateList(null, null, null, null, "2000-01-06"));
        subTasks[4] = new AnalyticsTransitionsDataRow("TASKB-16", "Review"
                , parseDateList(null, null, null, "2000-01-09", "2000-01-07"));
        subTasks[5] = new AnalyticsTransitionsDataRow("TASKB-17", "Review"
                , parseDateList("2000-01-07", null, null, "2000-01-07", "2000-01-07"));
        subTasks[6] = new AnalyticsTransitionsDataRow("TASKB-18", "Review"
                , parseDateList("2000-01-10", null, null, "2000-01-09", "2000-01-08"));
        subTasks[7] = new AnalyticsTransitionsDataRow("TASKB-19", "Review"
                , parseDateList("2000-01-11", null, null, "2000-01-08", "2000-01-08"));
        subTasks[8] = new AnalyticsTransitionsDataRow("TASKB-20", "Sub-Task"
                , parseDateList("2000-01-08", null, "2000-01-07", "2000-01-06", "2000-01-04"));
        subTasks[9] = new AnalyticsTransitionsDataRow("TASKB-21", "Sub-Task"
                , parseDateList("2000-01-07", null, "2000-01-07", "2000-01-07", "2000-01-06"));
        subTasks[10] = new AnalyticsTransitionsDataRow("TASKB-22", "Sub-Task"
                , parseDateList(null, null, null, "2000-01-09", "2000-01-08"));
        subTasks[11] = new AnalyticsTransitionsDataRow("TASKB-23", "Sub-Task"
                , parseDateList("2000-01-12", null, "2000-01-11", "2000-01-11", "2000-01-10"));

        return asList(new AnalyticsTransitionsDataSet(TYPE_DEMAND, demandHeaders, asList(demands))
                , new AnalyticsTransitionsDataSet(TYPE_FEATURES, featureHeaders, asList(features))
                , new AnalyticsTransitionsDataSet(TYPE_SUBTASKS, subTaskHeaders, asList(subTasks)));
    }

    public static List<AnalyticsTransitionsDataSet> getAnalyticsTransitionsDataSetWitNoRow() {
        List<String> headers = new LinkedList<>();
        headers.add("PKEY");
        headers.add("ISSUE_TYPE");
        headers.add("Done");
        headers.add("Doing");
        headers.add("To Do");

        AnalyticsTransitionsDataRow rowSubtask = new AnalyticsTransitionsDataRow("I-4", "Sub-task"
                , asList(
                        null
                        , null
                        , DateTimeUtils.parseDate("2017-09-25")));

        return asList(new AnalyticsTransitionsDataSet(TYPE_DEMAND, headers, null),
                new AnalyticsTransitionsDataSet(TYPE_FEATURES, headers, emptyList()),
                new AnalyticsTransitionsDataSet(TYPE_SUBTASKS, headers, asList(rowSubtask)));
    }

    public static List<AnalyticsTransitionsDataSet> getEmptyAnalyticsTransitionsDataSet() {
        return singletonList(new AnalyticsTransitionsDataSet(TYPE_DEMAND, emptyList(), emptyList()));
    }

    public static List<SyntheticTransitionsDataSet> getDefaultSyntheticTransitionsDataSet() {
        List<String> headers = new LinkedList<>();
        headers.add("Date");
        headers.add("Type");
        headers.add("Done");
        headers.add("Doing");
        headers.add("To Do");

        List<SyntheticTransitionsDataRow> rowsDemand = new LinkedList<>();
        rowsDemand.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-25"), "Demand", Ints.asList(0, 0, 1)));
        rowsDemand.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-25"), "OS", Ints.asList(0, 0, 1)));
        rowsDemand.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-26"), "Demand", Ints.asList(0, 1, 0)));
        rowsDemand.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-26"), "OS", Ints.asList(0, 1, 0)));
        rowsDemand.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-27"), "Demand", Ints.asList(1, 0, 0)));
        rowsDemand.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-27"), "OS", Ints.asList(0, 1, 0)));

        List<SyntheticTransitionsDataRow> rowsFeature = new LinkedList<>();
        rowsFeature.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-25"), "Feature", Ints.asList(0, 0, 1)));
        rowsFeature.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-26"), "Feature", Ints.asList(0, 1, 0)));

        List<SyntheticTransitionsDataRow> rowsSubtask = new LinkedList<>();
        rowsSubtask.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-25"), "Sub-task", Ints.asList(0, 0, 1)));

        return asList(new SyntheticTransitionsDataSet(TYPE_DEMAND, headers, rowsDemand),
                new SyntheticTransitionsDataSet(TYPE_FEATURES, headers, rowsFeature),
                new SyntheticTransitionsDataSet(TYPE_SUBTASKS, headers, rowsSubtask));
    }

    public static List<SyntheticTransitionsDataSet> getBiggerSyntheticTransitionsDataSet() {
        List<String> demandHeanders = new LinkedList<>();
        demandHeanders.add("PKEY");
        demandHeanders.add("ISSUE_TYPE");
        demandHeanders.add("Done");
        demandHeanders.add("Doing");
        demandHeanders.add("To Do");
        String[] demandStatuses = new String[] {"Done", "Doing", "To Do"};

        List<String> featureHeaders = new LinkedList<>();
        featureHeaders.add("PKEY");
        featureHeaders.add("ISSUE_TYPE");
        featureHeaders.add("Done");
        featureHeaders.add("QA");
        featureHeaders.add("Doing");
        featureHeaders.add("To Do");
        String[] featureStatuses = new String[] {"Done", "QA", "Doing", "To Do"};

        List<String> subTaskHeaders = new LinkedList<>();
        subTaskHeaders.add("PKEY");
        subTaskHeaders.add("ISSUE_TYPE");
        subTaskHeaders.add("Done");
        subTaskHeaders.add("UAT");
        subTaskHeaders.add("Reviewing");
        subTaskHeaders.add("Doing");
        subTaskHeaders.add("To Do");
        String[] subTaskStatuses = new String[] {"Done", "UAT", "Reviewing", "Doing", "To Do"};

        List<String> doneStatuses = singletonList("Done");

        List<AnalyticsTransitionsDataSet> analytics = getBiggerAnalyticsTransitionsDataSet();
        AnalyticsTransitionsDataSet demands = analytics.get(0);
        AnalyticsTransitionsDataSet features = analytics.get(1);
        AnalyticsTransitionsDataSet subTasks = analytics.get(2);

        return asList(
                FollowUpTransitionsDataProvider.getSyntheticTransitionsDs(demandHeanders, demandStatuses, doneStatuses, demands)
                , FollowUpTransitionsDataProvider.getSyntheticTransitionsDs(featureHeaders, featureStatuses, doneStatuses, features)
                , FollowUpTransitionsDataProvider.getSyntheticTransitionsDs(subTaskHeaders, subTaskStatuses, doneStatuses, subTasks));
    }

    public static List<SyntheticTransitionsDataSet> getSyntheticTransitionsDataSetWithNoRow() {
        List<String> headers = new LinkedList<>();
        headers.add("Date");
        headers.add("Type");
        headers.add("Done");
        headers.add("Doing");
        headers.add("To Do");

        List<SyntheticTransitionsDataRow> rowsSubtask = new LinkedList<>();
        rowsSubtask.add(new SyntheticTransitionsDataRow(DateTimeUtils.parseDate("2017-09-25"), "Sub-task", Ints.asList(0, 0, 1)));

        return asList(new SyntheticTransitionsDataSet(TYPE_DEMAND, headers, null),
                new SyntheticTransitionsDataSet(TYPE_FEATURES, headers, emptyList()),
                new SyntheticTransitionsDataSet(TYPE_SUBTASKS, headers, rowsSubtask));
    }

    public static List<SyntheticTransitionsDataSet> getEmptySyntheticTransitionsDataSet() {
        return singletonList(new SyntheticTransitionsDataSet(TYPE_DEMAND, emptyList(), emptyList()));
    }

    public static String followupEmptyV0() {
        return "[]";
    }
    public static String followupExpectedV0() {
        return resourceToString(FollowUpFacadeTest.class, "impl/V0_followUpDataHistoryExpected.json");
    }
    public static String followupEmptyV1() {
        return resourceToString(FollowUpHelper.class, "impl/V1_followupDataHistory_empty.json");
    }
    public static String followupExpectedV1() {
        return resourceToString(FollowUpHelper.class, "impl/V1_followUpDataHistoryExpected.json");
    }
    public static String followupEmptyV2() {
        return resourceToString(FollowUpHelper.class, "impl/V2_followupDataHistory_empty.json");
    }
    public static String followupExpectedV2() {
        return resourceToString(FollowUpHelper.class, "impl/V2_followUpDataHistoryExpected.json");
    }

    public static String fromJiraRowstoString(List<FromJiraDataRow> rows, String separation) {
        List<String> stringRows = rows.stream().map(r -> fromJiraDataRowtoString(r)).collect(Collectors.toList());
        return StringUtils.join(stringRows, separation);
    }

    private static String fromJiraDataRowtoString(FromJiraDataRow row) {
        return
                " planningType                  : " + row.planningType
             +"\n project                       : " + row.project
             +"\n demandType                    : " + row.demandType
             +"\n demandStatus                  : " + row.demandStatus
             +"\n demandId                      : " + defaultIfNull(row.demandId, "")
             +"\n demandNum                     : " + row.demandNum
             +"\n demandSummary                 : " + row.demandSummary
             +"\n demandDescription             : " + row.demandDescription
             +"\n demandStatusPriority          : " + row.demandStatusPriority
             +"\n demandPriorityOrder           : " + row.demandPriorityOrder
             +"\n demandStartDateStepMillis     : " + row.demandStartDateStepMillis
             +"\n demandAssignee                : " + row.demandAssignee
             +"\n demandDueDate                 : " + row.demandDueDate
             +"\n demandCreated                 : " + row.demandCreated
             +"\n demandLabels                  : " + row.demandLabels
             +"\n demandComponents              : " + row.demandComponents
             +"\n demandReporter                : " + row.demandReporter
             +"\n demandCoAssignees             : " + row.demandCoAssignees
             +"\n demandClassOfService          : " + row.demandClassOfService
             +"\n demandUpdatedDate             : " + row.demandUpdatedDate
             +"\n demandCycletime               : " + row.demandCycletime
             +"\n demandIsBlocked               : " + row.demandIsBlocked
             +"\n demandLastBlockReason         : " + row.demandLastBlockReason
             +"\n taskType                      : " + row.taskType
             +"\n taskStatus                    : " + row.taskStatus
             +"\n taskId                        : " + row.taskId
             +"\n taskNum                       : " + row.taskNum
             +"\n taskSummary                   : " + row.taskSummary
             +"\n taskDescription               : " + row.taskDescription
             +"\n taskFullDescription           : " + row.taskFullDescription
             +"\n taskAdditionalEstimatedHours  : " + row.taskAdditionalEstimatedHours
             +"\n taskRelease                   : " + row.taskRelease
             +"\n taskStatusPriority            : " + row.taskStatusPriority
             +"\n taskPriorityOrder             : " + row.taskPriorityOrder
             +"\n taskStartDateStepMillis       : " + row.taskStartDateStepMillis
             +"\n taskAssignee                  : " + row.taskAssignee
             +"\n taskDueDate                   : " + row.taskDueDate
             +"\n taskCreated                   : " + row.taskCreated
             +"\n taskLabels                    : " + row.taskLabels
             +"\n taskComponents                : " + row.taskComponents
             +"\n taskReporter                  : " + row.taskReporter
             +"\n taskCoAssignees               : " + row.taskCoAssignees
             +"\n taskClassOfService            : " + row.taskClassOfService
             +"\n taskUpdatedDate               : " + row.taskUpdatedDate
             +"\n taskCycletime                 : " + row.taskCycletime
             +"\n taskIsBlocked                 : " + row.taskIsBlocked
             +"\n taskLastBlockReason           : " + row.taskLastBlockReason
             +"\n subtaskType                   : " + row.subtaskType
             +"\n subtaskStatus                 : " + row.subtaskStatus
             +"\n subtaskId                     : " + row.subtaskId
             +"\n subtaskNum                    : " + row.subtaskNum
             +"\n subtaskSummary                : " + row.subtaskSummary
             +"\n subtaskDescription            : " + row.subtaskDescription
             +"\n subtaskFullDescription        : " + row.subtaskFullDescription
             +"\n subtaskStatusPriority         : " + row.subtaskStatusPriority
             +"\n subtaskPriorityOrder          : " + row.subtaskPriorityOrder
             +"\n subtaskStartDateStepMillis    : " + row.subtaskStartDateStepMillis
             +"\n subtaskAssignee               : " + row.subtaskAssignee
             +"\n subtaskDueDate                : " + row.subtaskDueDate
             +"\n subtaskCreated                : " + row.subtaskCreated
             +"\n subtaskLabels                 : " + row.subtaskLabels
             +"\n subtaskComponents             : " + row.subtaskComponents
             +"\n subtaskReporter               : " + row.subtaskReporter
             +"\n subtaskCoAssignees            : " + row.subtaskCoAssignees
             +"\n subtaskClassOfService         : " + row.subtaskClassOfService
             +"\n subtaskUpdatedDate            : " + row.subtaskUpdatedDate
             +"\n subtaskCycletime              : " + row.subtaskCycletime
             +"\n subtaskIsBlocked              : " + row.subtaskIsBlocked
             +"\n subtaskLastBlockReason        : " + row.subtaskLastBlockReason
             +"\n tshirtSize                    : " + row.tshirtSize
             +"\n worklog                       : " + row.worklog
             +"\n wrongWorklog                  : " + row.wrongWorklog
             +"\n demandBallpark                : " + row.demandBallpark
             +"\n taskBallpark                  : " + row.taskBallpark
             +"\n queryType                     : " + row.queryType;
    }
}
