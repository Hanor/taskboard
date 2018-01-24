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

import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toList;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.convertWorklog;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractAdditionalEstimatedHours;
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
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractRealParent;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractReleaseId;
import static objective.taskboard.domain.converter.IssueFieldsExtractor.extractTShirtSizes;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.cycletime.CycleTime;
import objective.taskboard.data.IssueScratch;
import objective.taskboard.data.TaskboardTimeTracking;
import objective.taskboard.database.IssuePriorityService;
import objective.taskboard.domain.IssueColorService;
import objective.taskboard.domain.IssueStateHashCalculator;
import objective.taskboard.domain.ParentIssueLink;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.jira.client.JiraIssueDto;
import objective.taskboard.repository.FilterCachedRepository;
import objective.taskboard.repository.ParentIssueLinkRepository;

@Service
public class JiraIssueToIssueConverter {
    public static final String INVALID_TEAM = "NO PROJECT TEAM";

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

    @Autowired
    private IssueStateHashCalculator issueStateHashCalculator;

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
    
    public objective.taskboard.data.Issue convertSingleIssue(JiraIssueDto jiraIssue, ParentProvider provider) {
        List<IssueCoAssignee> coAssignees = extractCoAssignees(jiraProperties, jiraIssue);
        
        String avatarCoAssignee1 = jiraIssue.getAssignee() != null ? jiraIssue.getAssignee().getAvatarUri("24x24").toString() : "";
        String avatarCoAssignee2 = coAssignees.stream()
                .map(c -> c.getAvatarUrl())
                .filter(url -> !url.equals(avatarCoAssignee1))
                .findFirst().orElse("");
        
        Long priorityOrder = priorityService.determinePriority(jiraIssue);
        
        Optional<Date> priorityUpdateDate = priorityService.priorityUpdateDate(jiraIssue);
        
        Date remoteIssueUpdatedDate = jiraIssue.getUpdateDate() != null ? jiraIssue.getUpdateDate().toDate() : jiraIssue.getCreationDate().toDate();
        IssueScratch converted = new IssueScratch(
                jiraIssue.getId(),
                jiraIssue.getKey(),
                jiraIssue.getProject().getKey(),
                jiraIssue.getProject().getName(),
                jiraIssue.getIssueType().getId(),
                jiraIssue.getIssueType().getIconUri().toASCIIString(),
                defaultIfNull(jiraIssue.getSummary(),""),
                jiraIssue.getStatus().getId(),
                startDateStepService.get(jiraIssue),
                avatarCoAssignee1,
                avatarCoAssignee2,
                extractParentKey(jiraProperties, jiraIssue, parentIssueLinks),
                getParentTypeId(jiraIssue),
                getParentTypeIconUri(jiraIssue),
                extractDependenciesIssues(jiraProperties, jiraIssue),
                String.join(",", coAssignees.stream().map(x -> x.getName()).collect(toList())),
                jiraIssue.getAssignee() != null ? jiraIssue.getAssignee().getName() : "",
                jiraIssue.getPriority() != null ? jiraIssue.getPriority().getId() : 0l,
                jiraIssue.getDueDate() != null ? jiraIssue.getDueDate().toDate() : null,
                jiraIssue.getCreationDate().getMillis(),
                priorityUpdateDate.orElse(new Date()),
                remoteIssueUpdatedDate,
                defaultIfNull(jiraIssue.getDescription(), ""),
                getComments(jiraIssue),
                extractLabels(jiraIssue),
                extractComponents(jiraIssue),
                getCustomFields(jiraIssue),
                priorityOrder,
                TaskboardTimeTracking.fromJira(jiraIssue.getTimeTracking()),
                jiraIssue.getReporter() == null ? null : jiraIssue.getReporter().getName(),
                coAssignees,
                extractClassOfService(jiraProperties, jiraIssue),
                extractReleaseId(jiraProperties, jiraIssue),
                extractChangelog(jiraIssue),
                convertWorklog(jiraIssue.getWorklogs()));
        
        return createIssueFromScratch(converted, provider);
    }

    public objective.taskboard.data.Issue createIssueFromScratch(IssueScratch scratch, ParentProvider provider) {
        objective.taskboard.data.Issue converted = new objective.taskboard.data.Issue(scratch, 
                jiraProperties, 
                metadataService, 
                issueTeamService, 
                filterRepository,
                cycleTime,
                cardVisibilityEvalService,
                projectService,
                issueStateHashCalculator,
                issueColorService);
        
    	if (!isEmpty(converted.getParent())) {
    	    Optional<objective.taskboard.data.Issue> parentCard = provider.get(converted.getParent());
    	    if (!parentCard.isPresent())
    	        throw new IncompleteIssueException(scratch, converted.getParent());
        }

        return converted;
    }

    private long getParentTypeId(JiraIssueDto issue) {
    	IssueParent realParent = extractRealParent(issue);
        if (realParent == null)
            return 0;
        return realParent.getTypeId();
    }

    private String getParentTypeIconUri(JiraIssueDto issue) {
    	IssueParent realParent = extractRealParent(issue);
        if (realParent == null)
            return "";
        return realParent.getTypeIconUrl();
    }

    private String getComments(JiraIssueDto issue) {
    	List<String> comments = extractComments(issue);
        if (comments.isEmpty())
            return "";
        return comments.get(0);
    }
    
    private Map<String, Serializable> getCustomFields(JiraIssueDto issue) {
        Map<String, Serializable> customFields = newHashMap();
        
        customFields.put(jiraProperties.getCustomfield().getBlocked().getId(), extractBlocked(jiraProperties, issue));
        customFields.put(jiraProperties.getCustomfield().getLastBlockReason().getId(), extractLastBlockReason(jiraProperties, issue));
        customFields.putAll(extractTShirtSizes(jiraProperties, issue));
        customFields.putAll(extractAdditionalEstimatedHours(jiraProperties, issue));
        return customFields;
    }
}
