/*-
 * [LICENSE]
 * Taskboard
 * - - -
 * Copyright (C) 2015 - 2016 Objective Solutions
 * - - -
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
package objective.taskboard.controller;

import static java.util.stream.Collectors.toList;
import static objective.taskboard.domain.converter.JiraIssueToIssueConverter.INVALID_TEAM;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.codehaus.jettison.json.JSONException;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.atlassian.jira.rest.client.api.domain.Subtask;
import com.atlassian.jira.rest.client.api.domain.TimeTracking;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;
import objective.taskboard.data.AspectItemFilter;
import objective.taskboard.data.AspectSubitemFilter;
import objective.taskboard.data.Issue;
import objective.taskboard.data.Team;
import objective.taskboard.database.TaskboardDatabaseService;
import objective.taskboard.filterConfiguration.TeamFilterConfigurationService;
import objective.taskboard.filterPreferences.UserPreferencesService;
import objective.taskboard.issueBuffer.IssueBufferService;
import objective.taskboard.issueBuffer.IssueBufferState;
import objective.taskboard.issueTypeVisibility.IssueTypeVisibilityService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraService;
import objective.taskboard.jira.JiraService.PermissaoNegadaException;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.linkgraph.LinkGraphProperties;

@Slf4j
@RestController
@RequestMapping("/ws/issues")
public class IssueController {

    @Autowired
    private TaskboardDatabaseService taskService;

    @Autowired
    private JiraService jiraBean;

    @Autowired
    private IssueTypeVisibilityService issueTypeVisibilityService;

    @Autowired
    private TeamFilterConfigurationService teamFilterConfigurationService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private IssueBufferService issueBufferService;
    
    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserPreferencesService userPreferencesService;

    @Value("${spring.datasource.username}")
    private String datasourceUserName;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private LinkGraphProperties linkGraphProperties;
    
    @RequestMapping(path = "/", method = RequestMethod.POST)
    public List<Issue> issues() {
        return issueBufferService.getIssues();
    }
    
    @RequestMapping(path = "assign", method = RequestMethod.POST)
    public Issue assign(@RequestBody Issue issue) throws JSONException {
        return issueBufferService.toggleAssignAndSubresponsavelToUser(issue.getIssueKey());
    }

    @SuppressWarnings("deprecation")
    @RequestMapping(path = "create-issue", method = RequestMethod.POST)
    public Issue createIssue(@RequestBody Issue issue) throws JSONException {
        com.atlassian.jira.rest.client.api.domain.Issue parent = jiraBean.getIssueByKey(issue.getParent());
        IssueInputBuilder issueBuilder = new IssueInputBuilder(parent.getProject().getKey(), issue.getType());
        issueBuilder.setPriorityId(issue.getPriority());
        issueBuilder.setDueDate(new DateTime(issue.getDueDate().getTime()));
        issueBuilder.setDescription(issue.getDescription());
        issueBuilder.setSummary(issue.getSummary());
        issueBuilder.setFieldValue("parent", ComplexIssueInputFieldValue.with("key", parent.getKey()));

        List<String> tSizeIds = jiraProperties.getCustomfield().getTShirtSize().getIds();
        for (String tSizeId : tSizeIds)
            issueBuilder.setFieldValue(tSizeId, ComplexIssueInputFieldValue.with("id", issue.getCustomFields().get(tSizeId).toString()));

        String classOfServiceId = jiraProperties.getCustomfield().getClassOfService().getId();
        issueBuilder.setFieldValue(classOfServiceId, ComplexIssueInputFieldValue.with("id", issue.getCustomFields().get(classOfServiceId).toString()));
        log.info("Creating issue: " + issue);
        String issueKey = jiraBean.createIssue(issueBuilder.build());
        log.info("Created issue " + issueKey);
        com.atlassian.jira.rest.client.api.domain.Issue created = jiraBean.getIssueByKey(issueKey);
        return Issue.from(created.getKey(), created.getSummary());
    }

    @RequestMapping(path = "transition", method = RequestMethod.POST)
    public Issue transition(@RequestBody TransitionDTO params) throws JSONException {
        return issueBufferService.doTransitionByName(params.issue, params.transition, params.resolution);
    }

    @RequestMapping(path = "transitions", method = RequestMethod.POST)
    public List<Transition> transitions(@RequestBody Issue issue) {
        try {
            List<Transition> transitionsByIssueKey = jiraBean.getTransitionsByIssueKey(issue.getIssueKey());
            return transitionsByIssueKey;
        } catch (PermissaoNegadaException e) {
            return Lists.newLinkedList();
        }
    }

    @RequestMapping(path = "resolutions/{transition}", method = RequestMethod.GET)
    public String resolutions(@PathVariable String transition) {
        return jiraBean.getResolutions(transition);
    }

    @RequestMapping(path = "subtasks", method = RequestMethod.POST)
    public List<Issue> subtasks(@RequestBody Issue issue) throws SQLException {
        return issueBufferService.getIssueSubTasks(issue);
    }

    @RequestMapping(path = "timetracking", method = RequestMethod.POST)
    public TimeTracking timetracking(@RequestBody Issue issue) throws JSONException {
        Integer timeEstimateMinutes = 0;
        Integer timeSpentMinutes = 0;

        com.atlassian.jira.rest.client.api.domain.Issue issueJira = jiraBean.getIssueByKey(issue.getIssueKey());

        timeEstimateMinutes += issueJira.getTimeTracking().getOriginalEstimateMinutes() != null ? issueJira.getTimeTracking().getOriginalEstimateMinutes() : 0;
        timeSpentMinutes += issueJira.getTimeTracking().getTimeSpentMinutes() != null ? issueJira.getTimeTracking().getTimeSpentMinutes() : 0;

        for (Subtask subTask : issueJira.getSubtasks()) {
            com.atlassian.jira.rest.client.api.domain.Issue subTaskJira = jiraBean.getIssueByKey(subTask.getIssueKey());

            timeEstimateMinutes += subTaskJira.getTimeTracking().getOriginalEstimateMinutes() != null ? subTaskJira.getTimeTracking().getOriginalEstimateMinutes() : 0;
            timeSpentMinutes += subTaskJira.getTimeTracking().getTimeSpentMinutes() != null ? subTaskJira.getTimeTracking().getTimeSpentMinutes() : 0;
        }

        return new TimeTracking(timeEstimateMinutes, null, timeSpentMinutes);
    }
    
    @RequestMapping(path = "cacheState")
    public IssueBufferState issueCacheState() {
        return issueBufferService.getState();
    }
    
    @RequestMapping("configuration")
    public Map<String, Object> configuration() throws SQLException, InterruptedException, ExecutionException {
        Map<String, Object> map = new HashMap<>();
        map.put("laneConfiguration", taskService.laneConfiguration());
        map.put("issueTypes", metadataService.getIssueTypeMetadata());
        map.put("issueTypesConfig", issueTypeVisibilityService.getIssueTypeVisibility());
        map.put("priorities", metadataService.getPrioritiesMetadata());
        map.put("statuses", metadataService.getStatusesMetadata());
        map.put("userPreferences", userPreferencesService.getUserPreferences());
        map.put("urlJira", jiraProperties.getUrl());
        map.put("urlLinkGraph", linkGraphProperties.getUrl());
        return map;
    }

    @RequestMapping("aspects-filter")
    public List<AspectItemFilter> aspectsFilter() throws InterruptedException, ExecutionException {
        List<AspectItemFilter> aspectsItem = new ArrayList<>();
        aspectsItem.addAll(getDefaultFieldFilterList());
        return aspectsItem;
    }

    @RequestMapping(path = "block-task/{issue}", method = RequestMethod.POST)
    public Issue blockTask(@PathVariable("issue") String issue, @RequestBody String lastBlockReason) {
        jiraBean.block(issue, lastBlockReason);
        return issueBufferService.updateIssueBuffer(issue);
    }

    @RequestMapping("unblock-task/{issue}")
    public void unblockTask(@PathVariable("issue") String issue) {
        jiraBean.unblock(issue);
        issueBufferService.updateIssueBuffer(issue);
    }
    
    @RequestMapping("reorder")
    public List<Issue> reorder(@RequestBody String [] issues) {
        return issueBufferService.reorder(issues);
    }
    
    @RequestMapping("issue-buffer-state")
    public String getState() {
        return issueBufferService.getState().name();
    }

    private List<AspectItemFilter> getDefaultFieldFilterList() throws InterruptedException, ExecutionException {
        List<AspectItemFilter> defaultFieldFilters = new ArrayList<>();
        defaultFieldFilters.add(AspectItemFilter.from("Issue Type", "type", getIssueTypeFilterItems()));
        defaultFieldFilters.add(AspectItemFilter.from("Project", "projectKey", getProjectFilterItems()));
        defaultFieldFilters.add(AspectItemFilter.from("Team", "teams", getTeamFilterItems()));
        return defaultFieldFilters;
    }

    private List<AspectSubitemFilter> getIssueTypeFilterItems() throws InterruptedException, ExecutionException {
        return issueTypeVisibilityService.getVisibleIssueTypes().stream()
                .sorted((t1, t2) -> {
                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        if (t1.isSubtask() == t2.isSubtask()) return t1.getName().compareTo(t2.getName());
                        return Boolean.compare(t1.isSubtask(), t2.isSubtask());
                })
                .map(t -> AspectSubitemFilter.from(t.getName(), t, true))
                .collect(toList());
    }

    private List<AspectSubitemFilter> getProjectFilterItems() {
        List<Team> teamsVisibleToUser = teamFilterConfigurationService.getTeamsVisibleToUser();
        return projectService.getVisibleProjects().stream()
                .map(p -> AspectSubitemFilter.from(p.getName(), p.getKey(), true,
                                                   teamsVisibleToUser.stream()
                                                       .filter(t -> p.getTeamsIds().contains(t.getId()))
                                                       .map(t -> t.getName())
                                                       .collect(toList()),
                                                   p.getVersions()))
                .sorted(this::compareFilter)
                .collect(toList());
    }

    private List<AspectSubitemFilter> getTeamFilterItems() {
        List<AspectSubitemFilter> teamsFilter = teamFilterConfigurationService.getTeamsVisibleToUser().stream()
                .map(t -> AspectSubitemFilter.from(t.getName(), t.getName(), true))
                .sorted(this::compareFilter)
                .collect(toList());
        teamsFilter.add(AspectSubitemFilter.from(INVALID_TEAM, INVALID_TEAM, true));
        return teamsFilter;
    }

    private int compareFilter(AspectSubitemFilter f1, AspectSubitemFilter f2) {
        if (f1 == null && f2 == null) return 0;
        if (f1 == null) return 1;
        if (f2 == null) return -1;
        return f1.getName().compareTo(f2.getName());
    }

    public static class TransitionDTO {
        public String transition;
        public String resolution;
        public Issue issue;
    }
}
