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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

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
import objective.taskboard.jira.client.JiraTimeTrackingDto;
import objective.taskboard.jira.data.Transition;
import objective.taskboard.linkgraph.LinkGraphProperties;

@RestController
@RequestMapping("/ws/issues")
public class IssueController 
{
    private static final Logger log = LoggerFactory.getLogger(IssueController.class);
    
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

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private LinkGraphProperties linkGraphProperties;
    
    @Autowired
    private UserTeamService userTeamService;

    @RequestMapping(path = "/", method = RequestMethod.POST)
    public List<CardDto> issues() {
        return toCardDto(issueBufferService.getIssues());
    }
    
    @RequestMapping(path = "/byids", method = RequestMethod.POST)
    public List<CardDto> byids(@RequestBody List<Long> issuesIds) {
        return toCardDto(issueBufferService.getIssuesByIds(issuesIds));
    }

    @RequestMapping(path = "addMeAsAssignee", method = RequestMethod.POST)
    public CardDto addMeAsAssignee(@RequestBody String issueKey) {
        return toCardDto(issueBufferService.addMeAsAssignee(issueKey));
    }

    @RequestMapping(path = "addAssigneeToIssue/{issue}", method = RequestMethod.POST)
    public CardDto addAssigneeToIssue(@PathVariable("issue") String issueKey, @RequestBody UserRequestDTO user) {
        return toCardDto(issueBufferService.addAssigneeToIssue(issueKey, user.username));
    }

    @RequestMapping(path = "removeAssigneeFromIssue/{issue}", method = RequestMethod.POST)
    public CardDto removeAssigneeFromIssue(@PathVariable("issue") String issueKey, @RequestBody UserRequestDTO user) {
        return toCardDto(issueBufferService.removeAssigneeFromIssue(issueKey, user.username));
    }

    @RequestMapping(path = "addTeamToIssue/{issue}", method = RequestMethod.POST)
    public CardDto addTeamToIssue(@PathVariable("issue") String issueKey, @RequestBody TeamRequestDTO team) {
        return toCardDto(issueBufferService.addTeamToIssue(issueKey, team.id));
    }

    @RequestMapping(path = "replaceTeamInIssue/{issue}", method = RequestMethod.POST)
    public CardDto replaceTeamInIssue(@PathVariable("issue") String issueKey, @RequestBody ReplaceTeamRequestDTO replaceTeamRequest) {
        return toCardDto(issueBufferService.replaceTeamInIssue(
                issueKey,
                replaceTeamRequest.teamToReplace,
                replaceTeamRequest.replacementTeam));
    }

    @RequestMapping(path = "removeTeamFromIssue/{issue}", method = RequestMethod.POST)
    public CardDto removeTeamFromIssue(@PathVariable("issue") String issueKey, @RequestBody TeamRequestDTO team) {
        return toCardDto(issueBufferService.removeTeamFromIssue(
                issueKey,
                team.id));
    }

    @RequestMapping(path = "transition", method = RequestMethod.POST)
    public CardDto transition(@RequestBody TransitionRequestDTO tr) throws JSONException {
        Map<String, Object> fields = tr.fields == null ? Collections.emptyMap() : tr.fields;
        return toCardDto(issueBufferService.doTransition(tr.issueKey, tr.transitionId, fields));
    }

    @RequestMapping(path = "transitions", method = RequestMethod.POST)
    public List<Transition> transitions(@RequestBody String issueKey) {
        try {
            return jiraBean.getTransitions(issueKey);
        } catch (PermissaoNegadaException e) {
            log.debug("Could not fetch transitions", e);
            return Lists.newLinkedList();
        }
    }

    @RequestMapping(path = "resolutions/{transition}", method = RequestMethod.GET)
    public String resolutions(@PathVariable String transition) {
        return jiraBean.getResolutions(transition);
    }

    @RequestMapping(path = "timetracking", method = RequestMethod.POST)
    public JiraTimeTrackingDto timetracking(@RequestBody String issueKey) throws JSONException {
        Integer timeEstimateMinutes = 0;
        Integer timeSpentMinutes = 0;

        Issue main = issueBufferService.getIssueByKey(issueKey);

        timeEstimateMinutes += main.getTimeTracking().getOriginalEstimateMinutes() != null ? main.getTimeTracking().getOriginalEstimateMinutes() : 0;
        timeSpentMinutes += main.getTimeTracking().getTimeSpentMinutes() != null ? main.getTimeTracking().getTimeSpentMinutes() : 0;

        for (Issue subTaskJira : main.getSubtaskCards()) {
            timeEstimateMinutes += subTaskJira.getTimeTracking().getOriginalEstimateMinutes() != null ? subTaskJira.getTimeTracking().getOriginalEstimateMinutes() : 0;
            timeSpentMinutes += subTaskJira.getTimeTracking().getTimeSpentMinutes() != null ? subTaskJira.getTimeTracking().getTimeSpentMinutes() : 0;
        }

        return new JiraTimeTrackingDto(timeEstimateMinutes, timeSpentMinutes);
    }

    @RequestMapping(path = "cacheState")
    public IssueBufferState issueCacheState() {
        return issueBufferService.getState();
    }

    @RequestMapping("configuration")
    public Map<String, Object> configuration() {
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
    public CardDto blockTask(@PathVariable("issue") String issue, @RequestBody String lastBlockReason) {
        jiraBean.block(issue, lastBlockReason);
        return toCardDto(issueBufferService.updateIssueBuffer(issue));
    }

    @RequestMapping(path = "unblock-task/{issue}", method = RequestMethod.POST)
    public CardDto unblockTask(@PathVariable("issue") String issue) {
        jiraBean.unblock(issue);
        return toCardDto(issueBufferService.updateIssueBuffer(issue));
    }
    
    @RequestMapping("reorder")
    public List<CardDto> reorder(@RequestBody String [] issues) {
        return toCardDto(issueBufferService.reorder(issues));
    }

    @RequestMapping("issue-buffer-state")
    public String getState() {
        return issueBufferService.getState().name();
    }

    private CardDto toCardDto(Issue issue) {
        List<Long> teamsVisibleToUser = userTeamService.getIdsOfTeamsVisibleToUser();
        return CardDto.fromIssue(issue, teamsVisibleToUser);
    }

    private List<CardDto> toCardDto(List<Issue> issues) {
        List<Long> teamsVisibleToUser = userTeamService.getIdsOfTeamsVisibleToUser();
        return issues.stream()
                .map(i->CardDto.fromIssue(i, teamsVisibleToUser))
                .collect(Collectors.toList());
    }

    private List<AspectItemFilter> getDefaultFieldFilterList() throws InterruptedException, ExecutionException {
        List<AspectItemFilter> defaultFieldFilters = new ArrayList<>();
        defaultFieldFilters.add(AspectItemFilter.from("Issue Type", "type", getIssueTypeFilterItems()));
        defaultFieldFilters.add(AspectItemFilter.from("Project", "projectKey", getProjectFilterItems()));
        defaultFieldFilters.add(AspectItemFilter.from("Team", "teamNames", getTeamFilterItems()));
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
        List<Team> teamsVisibleToUser = teamFilterConfigurationService.getDefaultTeamsInProjectsVisibleToUser();
        return projectService.getNonArchivedJiraProjectsForUser().stream()
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
        List<AspectSubitemFilter> teamsFilter = 
                teamFilterConfigurationService.getDefaultTeamsInProjectsVisibleToUser().stream()
                .map(t -> AspectSubitemFilter.from(t.getName(), t.getName(), true))
                .sorted(this::compareFilter)
                .collect(toList());
        
        return teamsFilter;
    }

    private int compareFilter(AspectSubitemFilter f1, AspectSubitemFilter f2) {
        if (f1 == null && f2 == null) return 0;
        if (f1 == null) return 1;
        if (f2 == null) return -1;
        return f1.getName().compareTo(f2.getName());
    }

    private static class TransitionRequestDTO {
        public String issueKey;
        public Long transitionId;
        public Map<String, Object> fields;
    }
    
    private static class TeamRequestDTO {
        public Long id;
    }

    private static class ReplaceTeamRequestDTO {
        public Long teamToReplace;
        public Long replacementTeam;
    }

    private static class UserRequestDTO {
        public String username;
    }
}