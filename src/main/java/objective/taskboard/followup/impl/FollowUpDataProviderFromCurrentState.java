package objective.taskboard.followup.impl;

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

import static org.apache.commons.lang.ObjectUtils.defaultIfNull;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import objective.taskboard.Constants;
import objective.taskboard.data.Issue;
import objective.taskboard.followup.AnalyticsTransitionsDataSet;
import objective.taskboard.followup.FollowupData;
import objective.taskboard.followup.FollowupDataProvider;
import objective.taskboard.followup.FromJiraDataRow;
import objective.taskboard.followup.FromJiraDataSet;
import objective.taskboard.followup.SyntheticTransitionsDataSet;
import objective.taskboard.issueBuffer.IssueBufferService;
import objective.taskboard.issueBuffer.IssueBufferState;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.BallparkMapping;
import objective.taskboard.jira.MetadataService;

@Service
public class FollowUpDataProviderFromCurrentState implements FollowupDataProvider {

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private IssueBufferService issueBufferService;

    private Map<String, Issue> demandsByKey;
    private Map<String, Issue> featuresByKey;
    private Map<String, FromJiraDataRow> followUpBallparks;

    @Override
    public IssueBufferState getFollowupState() {
        return issueBufferService.getState();
    }

    @Override
    public FollowupData getJiraData(String[] includeProjects, ZoneId timezone) {
        List<String> i = Arrays.asList(includeProjects);

        List<Issue> issuesVisibleToUser = issueBufferService.getAllIssues().stream()
                .filter(issue -> isAllowedStatus(issue.getStatus()))
                .filter(issue -> i.contains(issue.getProjectKey()))
                .collect(Collectors.toList());

        FromJiraDataSet fromJiraDs = getFromJiraDs(issuesVisibleToUser);

        FollowUpTransitionsDataProvider transitions = new FollowUpTransitionsDataProvider(jiraProperties, metadataService);
        List<AnalyticsTransitionsDataSet> analyticsTransitionsDsList = transitions.getAnalyticsTransitionsDsList(issuesVisibleToUser, timezone);
        List<SyntheticTransitionsDataSet> syntheticsTransitionsDsList = transitions.getSyntheticTransitionsDsList(analyticsTransitionsDsList);

        return new FollowupData(fromJiraDs, analyticsTransitionsDsList, syntheticsTransitionsDsList);
    }

    private FromJiraDataSet getFromJiraDs(List<Issue> issuesVisibleToUser) {
        LinkedList<Issue> issues = new LinkedList<>(issuesVisibleToUser);

        followUpBallparks = new LinkedHashMap<String, FromJiraDataRow>();
        demandsByKey = makeDemandBallparks(issues);
        featuresByKey = makeFeatureBallparks(issues);

        final List<FromJiraDataRow> result = makeSubtasks(issues);

        result.addAll(followUpBallparks.values());

        result.sort(Comparator.comparingInt((FromJiraDataRow f) -> f.demandStatusPriority)
                .thenComparingLong(f -> f.demandPriorityOrder)
                .thenComparingInt(f -> f.taskStatusPriority)
                .thenComparingLong(f -> f.taskPriorityOrder)
                .thenComparingInt(f -> f.subtaskStatusPriority)
                .thenComparingLong(f -> f.subtaskPriorityOrder));

        return new FromJiraDataSet(Constants.FROMJIRA_HEADERS, result);
    }

    private boolean isAllowedStatus(long status) {
        return !jiraProperties.getFollowup().getStatusExcludedFromFollowup().contains(status);
    }

    private Map<String, Issue> makeDemandBallparks(LinkedList<Issue> issues) {
        Map<String, Issue> demands = new LinkedHashMap<>();
        Iterator<Issue> it = issues.iterator();
        while(it.hasNext()) {
            Issue issue = it.next();
            if (issue.isDemand()) {
                followUpBallparks.put(issue.getIssueKey(), createBallparkDemand(issue));
                demands.put(issue.getIssueKey(), issue);
                it.remove();
            }
        }
        return demands;
    }

    private Map<String, Issue> makeFeatureBallparks(LinkedList<Issue> issues) {
        Map<String, Issue> features = new LinkedHashMap<String, Issue>();

        Iterator<Issue> it = issues.iterator();
        while(it.hasNext()) {
            Issue feature = it.next();
            if (!feature.isFeature())
                continue;
            it.remove();

            Issue demand = demandsByKey.get(feature.getParent());
            if (demand != null)
                followUpBallparks.remove(demand.getIssueKey());

            features.put(feature.getIssueKey(), feature);

            if (jiraProperties.getFollowup().getFeatureStatusThatDontGenerateBallpark().contains(feature.getStatus()))
                continue;

            List<BallparkMapping> mappingList = getBallparksOrCry(feature);

            for (BallparkMapping mapping : mappingList) {
                String issueKeyAndTShirtSize = feature.getIssueKey()+mapping.getTshirtCustomFieldId();
                followUpBallparks.put( issueKeyAndTShirtSize, createBallparkFeature(demand, feature, mapping));
            }
        }
        return features;
    }

    private List<FromJiraDataRow> makeSubtasks(LinkedList<Issue> issues) {

        final List<FromJiraDataRow> subtasksFollowups = new LinkedList<FromJiraDataRow>();
        Iterator<Issue> it = issues.iterator();
        while(it.hasNext()) {
            Issue issue = it.next();
            Issue feature = featuresByKey.get(issue.getParent());
            if (feature == null)
                continue;

            Issue demand  = demandsByKey.get(feature.getParent());
            if (demand != null)
                followUpBallparks.remove(demand.getIssueKey());

            subtasksFollowups.add(createSubTaskFollowup(demand, feature, issue));

            if (jiraProperties.getFollowup().getSubtaskStatusThatDontPreventBallparkGeneration().contains(issue.getStatus()))
                continue;

            String featureTshirtForThisSubTask = "";
            List<BallparkMapping> mappingList = getBallparksOrCry(feature);
            for (BallparkMapping mapping : mappingList) {
                if (mapping.getJiraIssueTypes().contains(issue.getType())) {
                    featureTshirtForThisSubTask = mapping.getTshirtCustomFieldId();
                    followUpBallparks.remove(feature.getIssueKey()+featureTshirtForThisSubTask);
                }
            }
        }
        return subtasksFollowups;
    }

    private List<BallparkMapping> getBallparksOrCry(Issue issue) {
        List<BallparkMapping> mappings = issue.getActiveBallparkMappings();
        if (mappings == null) {
            throw new IllegalStateException(
                    "Ballpark mapping for issue type '"+issue.getIssueTypeName()+"' (id "+issue.getType()+") missing in configuration");
        }
        return mappings;
    }

    private FromJiraDataRow createBallparkDemand(Issue demand) {
        FromJiraDataRow followUpData = new FromJiraDataRow();
        followUpData.planningType = "Ballpark";
        followUpData.project = demand.getProject();

        followUpData.demandId = demand.getId();
        followUpData.demandType = demand.getIssueTypeName();
        followUpData.demandStatus= demand.getStatusName();
        followUpData.demandNum = demand.getIssueKey();
        followUpData.demandSummary = demand.getSummary();
        followUpData.demandDescription = issueDescription("M", demand);
        followUpData.demandStatusPriority = demand.getStatusPriority();
        followUpData.demandPriorityOrder = demand.getPriorityOrder();

        followUpData.taskType = "BALLPARK - Demand";
        followUpData.taskStatus = getBallparkStatus();
        followUpData.taskId = 0L;
        followUpData.taskNum = followUpData.demandNum;
        followUpData.taskSummary="Dummy Feature";
        followUpData.taskDescription = issueDescription(0, demand.getSummary());
        followUpData.taskFullDescription = issueFullDescription("BALLPARK - Demand", "M", 0, demand.getSummary());
        followUpData.taskRelease = (String) defaultIfNull(getRelease(demand), "No release set");

        followUpData.subtaskType = "BALLPARK - Demand";
        followUpData.subtaskStatus = followUpData.demandStatus;
        followUpData.subtaskId = 0L;
        followUpData.subtaskNum = demand.getProjectKey()+"-0";
        followUpData.subtaskSummary = followUpData.demandSummary;
        followUpData.subtaskDescription = issueDescription("M", 0, demand.getSummary());
        followUpData.subtaskFullDescription = issueFullDescription("BALLPARK - Demand", "M", 0, demand.getSummary());
        followUpData.tshirtSize = "M";
        followUpData.worklog = 0.0;
        followUpData.wrongWorklog = timeSpentInHour(demand);
        followUpData.demandBallpark = originalEstimateInHour(demand);
        followUpData.taskBallpark = 0.0;
        followUpData.queryType = "DEMAND BALLPARK";
        return followUpData;
    }

    private Double timeSpentInHour(Issue issue) {
        if (issue.getTimeTracking() == null)
            return 0.0;
        if (issue.getTimeTracking().getTimeSpentMinutes() == null)
            return 0.0;
        return issue.getTimeTracking().getTimeSpentMinutes()/60.0;
    }

    private Double originalEstimateInHour(Issue issue) {
        if (issue == null)
            return 0.0;
        if (issue.getTimeTracking() == null)
            return 0.0;
        if (issue.getTimeTracking().getOriginalEstimateMinutes() == null)
            return 0.0;
        return issue.getTimeTracking().getOriginalEstimateMinutes()/60.0;
    }

    private FromJiraDataRow createBallparkFeature(Issue demand, Issue task, BallparkMapping ballparkMapping) {
        FromJiraDataRow followUpData = new FromJiraDataRow();
        followUpData.planningType = "Ballpark";
        followUpData.project = task.getProject();

        if (demand != null) {
            followUpData.demandType = demand.getIssueTypeName();
            followUpData.demandStatus= demand.getStatusName();
            followUpData.demandId = demand.getId();
            followUpData.demandNum = demand.getIssueKey();
            followUpData.demandSummary = demand.getSummary();
            followUpData.demandDescription = issueDescription("", demand);
            followUpData.demandStatusPriority = demand.getStatusPriority();
            followUpData.demandPriorityOrder = demand.getPriorityOrder();
        }

        followUpData.taskType = task.getIssueTypeName();
        followUpData.taskStatus = task.getStatusName();
        followUpData.taskId = task.getId();
        followUpData.taskNum = task.getIssueKey();
        followUpData.taskSummary=task.getSummary();
        followUpData.taskDescription = issueDescription(task);
        followUpData.taskFullDescription = issueFullDescription(task);
        followUpData.taskRelease = coalesce(getRelease(task), getRelease(demand) ,"No release set");
        followUpData.taskStatusPriority = task.getStatusPriority();
        followUpData.taskPriorityOrder = task.getPriorityOrder();

        followUpData.subtaskType = ballparkMapping.getIssueType();
        followUpData.subtaskStatus = getBallparkStatus();
        followUpData.subtaskId = 0L;
        followUpData.subtaskNum = task.getProjectKey()+"-0";
        followUpData.subtaskSummary = ballparkMapping.getIssueType();
        followUpData.subtaskDescription = issueDescription(0, task.getSummary());
        followUpData.subtaskFullDescription = issueFullDescription(ballparkMapping.getIssueType(), "", 0, task.getSummary());
        followUpData.tshirtSize = task.getTshirtSizeOfSubtaskForBallpark(ballparkMapping);
        followUpData.worklog = 0.0;
        followUpData.wrongWorklog = timeSpentInHour(task);
        followUpData.demandBallpark = originalEstimateInHour(demand);
        followUpData.taskBallpark = originalEstimateInHour(task);
        followUpData.queryType = "FEATURE BALLPARK";
        return followUpData;
    }

    private String getBallparkStatus() {
        return metadataService.getStatusById(jiraProperties.getFollowup().getBallparkDefaultStatus()).name;
    }

    private FromJiraDataRow createSubTaskFollowup(Issue demand, Issue task, Issue subtask) {
        FromJiraDataRow followUpData = new FromJiraDataRow();
        followUpData.planningType = "Plan";
        followUpData.project = task.getProject();

        if (demand != null) {
            followUpData.demandId = demand.getId();
            followUpData.demandType = demand.getIssueTypeName();
            followUpData.demandStatus= demand.getStatusName();
            followUpData.demandNum = demand.getIssueKey();
            followUpData.demandSummary = demand.getSummary();
            followUpData.demandDescription = issueDescription("",demand);
            followUpData.demandBallpark = originalEstimateInHour(demand);
            followUpData.demandStatusPriority = demand.getStatusPriority();
            followUpData.demandPriorityOrder = demand.getPriorityOrder();
        }

        followUpData.taskType = task.getIssueTypeName();
        followUpData.taskStatus = task.getStatusName();
        followUpData.taskId = task.getId();
        followUpData.taskNum = task.getIssueKey();
        followUpData.taskSummary=task.getSummary();
        followUpData.taskDescription = issueDescription(task);
        followUpData.taskFullDescription = issueFullDescription(task);
        followUpData.taskRelease = coalesce(getRelease(subtask), getRelease(task), getRelease(demand), "No release set");
        followUpData.taskStatusPriority = task.getStatusPriority();
        followUpData.taskPriorityOrder = task.getPriorityOrder();

        followUpData.subtaskType = subtask.getIssueTypeName();
        followUpData.subtaskStatus = subtask.getStatusName();
        followUpData.subtaskId = subtask.getId();
        followUpData.subtaskNum = subtask.getIssueKey();
        followUpData.subtaskSummary = subtask.getSummary();
        followUpData.subtaskStatusPriority = subtask.getStatusPriority();
        followUpData.subtaskPriorityOrder = subtask.getPriorityOrder();

        followUpData.subtaskDescription = issueDescription(subtask);
        followUpData.subtaskFullDescription = subtask.getStatusName() + " > " + issueFullDescription(subtask);
        followUpData.tshirtSize = subtask.getTShirtSize() == null? "": subtask.getTShirtSize();
        followUpData.worklog = timeSpentInHour(subtask);
        followUpData.wrongWorklog = 0.0;
        
        if (StringUtils.isEmpty(followUpData.tshirtSize)) { 
            followUpData.taskBallpark = originalEstimateInHour(subtask);
            if (followUpData.taskBallpark == 0)
                followUpData.taskBallpark = originalEstimateInHour(task);
        }

        followUpData.queryType = "SUBTASK PLAN";
        return followUpData;
    }

    private String getTshirtSize(Issue i) {
        if (i.isFeature()) return "";
        if (i.isDemand()) return "M";
        return i.getTShirtSize();
    }

    private String getRelease(Issue i) {
        if (i == null || i.getRelease() == null)
            return null;
        
        return i.getRelease().name; 
    }

    private String issueDescription(Issue issue) {
        return issueDescription(getTshirtSize(issue), issue.getIssueKeyNum(), issue.getSummary());
    }

    private String issueDescription(String size, Issue issue) {
        return issueDescription(size, issue.getIssueKeyNum(), issue.getSummary());
    }

    private String issueDescription(Integer issueNum, String description) {
        return issueDescription(null, issueNum, description);
    }

    private String issueDescription(String size, Integer issueNum, String description) {
        String sizePart = StringUtils.isEmpty(size)?"":size+" | ";
        return String.format("%s%05d - %s", sizePart, issueNum, description);
    }

    private String issueFullDescription(Issue issue) {
        return issue.getIssueTypeName() +
                " | " + issueDescription(getTshirtSize(issue), issue.getIssueKeyNum(), issue.getSummary());
    }

    private String issueFullDescription(String issueType, String size, Integer issueNum, String description) {
        return issueType + " | " +issueDescription(size, issueNum, description);
    }

    private static <T> T coalesce(@SuppressWarnings("unchecked") T ...items) {
        for(T i : items) if(i != null) return i;
        return null;
    }

}
