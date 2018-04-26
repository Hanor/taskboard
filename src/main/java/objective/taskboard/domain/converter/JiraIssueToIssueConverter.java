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
package objective.taskboard.domain.converter;

import static java.util.stream.Collectors.toList;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.convertWorklog;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractAdditionalEstimatedHours;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractAssignedTeamsIds;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractBlocked;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractChangelog;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractClassOfService;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractCoAssignees;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractComments;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractComponents;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractDependenciesIssues;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractLabels;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractLastBlockReason;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractParentKey;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractReleaseId;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractTShirtSizes;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.cycletime.CycleTime;
import objective.taskboard.data.Issue;
import objective.taskboard.data.IssueScratch;
import objective.taskboard.data.TaskboardTimeTracking;
import objective.taskboard.data.User;
import objective.taskboard.database.IssuePriorityService;
import objective.taskboard.domain.IssueColorService;
import objective.taskboard.domain.ParentIssueLink;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.repository.FilterCachedRepository;
import objective.taskboard.repository.ParentIssueLinkRepository;

@Service
public class JiraIssueToIssueConverter {
    @Autowired
    private ParentIssueLinkRepository parentIssueLinkRepository;

    @Autowired
    private IssueTeamService issueTeamService;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private IssueColorService issueColorService;

    @Autowired
    private StartDateStepService startDateStepService;

    @Autowired
    private IssuePriorityService priorityService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private FilterCachedRepository filterRepository;

    @Autowired
    private CycleTime cycleTime;

    @Autowired
    private CardVisibilityEvalService cardVisibilityEvalService;

    @Autowired
    private ProjectService projectService;

    private List<String> parentIssueLinks = new ArrayList<>();
    
    public void setParentIssueLinks(List<String> parentIssueLinks) {
        this.parentIssueLinks = parentIssueLinks;
    }

    @PostConstruct
    private void loadParentIssueLinks() {
        parentIssueLinks = parentIssueLinkRepository.findAll().stream()
                               .map(ParentIssueLink::getDescriptionIssueLink)
                               .collect(toList());
    }
    

    public Issue convertSingleIssue(JiraIssueDto jiraIssue, ParentProvider provider) {
        List<IssueCoAssignee> coAssignees = extractCoAssignees(jiraProperties, jiraIssue);
        
        String assignedUid;
        if (jiraIssue.getAssignee() == null)
            assignedUid = "";
        else
            assignedUid = jiraIssue.getAssignee().getName();       

        IssueScratch converted = new IssueScratch(
                jiraIssue.getId(),
                jiraIssue.getKey(),
                jiraIssue.getProject().getKey(),
                jiraIssue.getProject().getName(),
                jiraIssue.getIssueType().getId(),
                defaultIfNull(jiraIssue.getSummary(),""),
                jiraIssue.getStatus().getId(),
                startDateStepService.get(jiraIssue),
                extractParentKey(jiraProperties, jiraIssue, parentIssueLinks),
                extractDependenciesIssues(jiraProperties, jiraIssue),
                coAssignees.stream().filter(c -> !c.getName().equals(assignedUid)).map(x -> User.from(x)).collect(toList()),
                User.from(jiraIssue.getAssignee()),
                jiraIssue.getPriority() != null ? jiraIssue.getPriority().getId() : 0l,
                jiraIssue.getDueDate() != null ? jiraIssue.getDueDate().toDate() : null,
                jiraIssue.getCreationDate().getMillis(),
                jiraIssue.getUpdateDate() != null ? jiraIssue.getUpdateDate().toDate() : jiraIssue.getCreationDate().toDate(),
                defaultIfNull(jiraIssue.getDescription(), ""),
                getComments(jiraIssue),
                extractLabels(jiraIssue),
                extractComponents(jiraIssue),
                extractBlocked(jiraProperties, jiraIssue),
                extractLastBlockReason(jiraProperties, jiraIssue),
                extractTShirtSizes(jiraProperties, jiraIssue),
                extractAdditionalEstimatedHours(jiraProperties, jiraIssue),
                TaskboardTimeTracking.fromJira(jiraIssue.getTimeTracking()),
                jiraIssue.getReporter() == null ? null : jiraIssue.getReporter().getName(),
                extractClassOfService(jiraProperties, jiraIssue),
                extractReleaseId(jiraProperties, jiraIssue),
                extractChangelog(jiraIssue),
                convertWorklog(jiraIssue.getWorklogs()),
                extractAssignedTeamsIds(jiraProperties, jiraIssue));
        
        return createIssueFromScratch(converted, provider);
    }

    public Issue createIssueFromScratch(IssueScratch scratch, ParentProvider provider) {
        Issue converted = new Issue(scratch, 
                getJiraProperties(), 
                getMetadataService(), 
                getIssueTeamService(), 
                filterRepository,
                cycleTime,
                cardVisibilityEvalService,
                projectService,
                getIssueColorService(),
                getIssuePriorityService());
        
    	if (!isEmpty(converted.getParent())) {
    	    Optional<Issue> parentCard = provider.get(converted.getParent());
    	    if (!parentCard.isPresent())
    	        throw new IncompleteIssueException(scratch, converted.getParent());
        }

        return converted;
    }

    public JiraProperties getJiraProperties() {
        return jiraProperties;
    }

    public MetadataService getMetadataService() {
        return metadataService;
    }

    public IssueTeamService getIssueTeamService() {
        return issueTeamService;
    }

    private String getComments(JiraIssueDto issue) {
    	List<String> comments = extractComments(issue);
        if (comments.isEmpty())
            return "";
        return comments.get(0);
    }

    public IssueColorService getIssueColorService() {
        return issueColorService;
    }

    public IssuePriorityService getIssuePriorityService() {
        return priorityService;
    }
}
