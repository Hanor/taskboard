package objective.taskboard.domain.converter;

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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.atlassian.jira.rest.client.api.domain.Issue;

import lombok.extern.slf4j.Slf4j;
import objective.taskboard.data.CustomField;
import objective.taskboard.data.Issue.TaskboardTimeTracking;
import objective.taskboard.database.IssuePriorityService;
import objective.taskboard.domain.IssueColorService;
import objective.taskboard.domain.ParentIssueLink;
import objective.taskboard.domain.converter.IssueTeamService.InvalidTeamException;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraService;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.repository.ParentIssueLinkRepository;

@Slf4j
@Service
public class JiraIssueToIssueConverter {

    public static final String INVALID_TEAM = "INVALID_TEAM";

    @Autowired
    private ParentIssueLinkRepository parentIssueLinkRepository;

    @Autowired
    private IssueTeamService issueTeamService;

    @Autowired
    private JiraService jiraService;

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

    private List<String> parentIssueLinks;

    @PostConstruct
    private void loadParentIssueLinks() {
        parentIssueLinks = parentIssueLinkRepository.findAll().stream()
                               .map(ParentIssueLink::getDescriptionIssueLink)
                               .collect(toList());
    }
    
    public List<objective.taskboard.data.Issue> convertIssues(List<Issue> issueList, Map<String, IssueMetadata> issuesMetadataByKey) {
        loadParentIssueLinks();

        for (Issue issue : issueList)
            issuesMetadataByKey.put(issue.getKey(), new IssueMetadata(issue, jiraProperties, parentIssueLinks, log));

        List<objective.taskboard.data.Issue> converted = new ArrayList<>();
        Iterator<Issue> it = issueList.iterator();
        while (it.hasNext()) {
            converted.add(convert(it.next(), issuesMetadataByKey));
            it.remove();
        }
        System.out.println("Converted issues: " + converted.size());
        return converted;
    }
    
    public objective.taskboard.data.Issue convertSingleIssue(Issue jiraIssue, Map<String, IssueMetadata> issuesMetadataByKey) {
        issuesMetadataByKey.put(jiraIssue.getKey(), new IssueMetadata(jiraIssue, jiraProperties, parentIssueLinks, log));
        return convert(jiraIssue, issuesMetadataByKey);
    }

    private objective.taskboard.data.Issue convert(Issue jiraIssue, Map<String, IssueMetadata> issuesMetadataByKey) {
        IssueMetadata metadata = issuesMetadataByKey.get(jiraIssue.getKey());

        String avatarCoAssignee1 = jiraIssue.getAssignee() != null ? jiraIssue.getAssignee().getAvatarUri("24x24").toString() : "";
        String avatarCoAssignee2 = metadata.getCoAssignees().stream()
                .map(c -> c.getAvatarUrl())
                .filter(url -> !url.equals(avatarCoAssignee1))
                .findFirst().orElse("");

        List<String> issueTeams = newArrayList();
        List<String> usersTeam = newArrayList();
        try {
            IssueMetadata parentMetadata = getParentMetadata(metadata, issuesMetadataByKey);

            Map<String, List<String>> mapUsersTeams = issueTeamService.getIssueTeams(metadata, parentMetadata);

            for (List<String> teams : mapUsersTeams.values())
                issueTeams.addAll(teams);

            issueTeams = issueTeams.stream()
                    .distinct()
                    .collect(toList());

            usersTeam.addAll(mapUsersTeams.keySet());
        } catch (InvalidTeamException e) {
            issueTeams.add(INVALID_TEAM);
            usersTeam.addAll(e.getUsersInInvalidTeam());
        }
        Long priorityOrder = priorityService.determinePriority(jiraIssue);
        
        Date issueUpdatedDate = jiraIssue.getUpdateDate() == null? null : jiraIssue.getUpdateDate().toDate();
        Optional<Date> priorityUpdateDate = priorityService.priorityUpdateDate(jiraIssue);
        if (priorityUpdateDate.isPresent()) {
            if (issueUpdatedDate == null || priorityUpdateDate.get().after(issueUpdatedDate))
                issueUpdatedDate = priorityUpdateDate.get();
        }
        objective.taskboard.data.Issue i = objective.taskboard.data.Issue.from(
                jiraIssue.getId(),
                jiraIssue.getKey(),
                jiraIssue.getProject().getKey(),
                jiraIssue.getProject().getName(),
                jiraIssue.getIssueType().getId(),
                jiraIssue.getIssueType().getIconUri().toASCIIString(),
                coalesce(jiraIssue.getSummary(),""),
                jiraIssue.getStatus().getId(),
                startDateStepService.get(jiraIssue),
                avatarCoAssignee1,
                avatarCoAssignee2,
                metadata.getParentKey(),
                getParentTypeId(metadata),
                getParentTypeIconUri(metadata),
                metadata.getDependenciesIssuesKey(),
                issueColorService.getColor(getClassOfServiceId(metadata, issuesMetadataByKey)),
                String.join(",", getCoAssigneesName(metadata)),
                jiraIssue.getAssignee() != null ? jiraIssue.getAssignee().getName() : "",
                String.join(",", usersTeam),
                jiraIssue.getPriority() != null ? jiraIssue.getPriority().getId() : 0l,
                jiraIssue.getDueDate() != null ? jiraIssue.getDueDate().toDate() : null,
                jiraIssue.getCreationDate().getMillis(),
                issueUpdatedDate,
                coalesce(jiraIssue.getDescription(), ""),
                issueTeams,
                getComments(metadata),
                metadata.getLabels(),
                metadata.getComponents(),
                getCustomFields(metadata, issuesMetadataByKey),
                priorityOrder,
                TaskboardTimeTracking.fromJira(jiraIssue.getTimeTracking()),
                jiraProperties,
                metadataService
        );
        return i;
    }
    
    private <T> T coalesce(T value, T def) {
        return value == null? def: value;
    }

    private long getParentTypeId(IssueMetadata metadata) {
        if (metadata.getRealParent() == null)
            return 0;
        return metadata.getRealParent().getTypeId();
    }

    private String getParentTypeIconUri(IssueMetadata metadata) {
        if (metadata.getRealParent() == null)
            return "";
        return metadata.getRealParent().getTypeIconUrl();
    }

    private List<String> getCoAssigneesName(IssueMetadata metadata) {
        return metadata.getCoAssignees().stream()
                .map(x -> x.getName())
                .collect(toList());
    }

    private String getComments(IssueMetadata metadata) {
        if (metadata.getComments().isEmpty())
            return "";
        return metadata.getComments().get(0);
    }

    private Map<String, Object> getCustomFields(IssueMetadata metadata, Map<String, IssueMetadata> issuesMetadataByKey) {
        Map<String, Object> customFields = newHashMap();
        customFields.put(jiraProperties.getCustomfield().getClassOfService().getId(), getClassOfServiceValue(metadata, issuesMetadataByKey));
        customFields.put(jiraProperties.getCustomfield().getBlocked().getId(), metadata.getBlocked());
        customFields.put(jiraProperties.getCustomfield().getLastBlockReason().getId(), metadata.getLastBlockReason());
        customFields.putAll(metadata.getTShirtSizes());
        customFields.putAll(metadata.getAdditionalEstimatedHours());
        customFields.putAll(getRelease(metadata, issuesMetadataByKey));
        return customFields;
    }

    private String getClassOfServiceValue(IssueMetadata metadata, Map<String, IssueMetadata> issuesMetadataByKey) {
        String defaultClassOfService = jiraProperties.getCustomfield().getClassOfService().getDefaultValue();
        CustomField classOfService = getClassOfService(metadata, issuesMetadataByKey);
        return classOfService == null ? defaultClassOfService : (String)classOfService.getValue();
    }

    private Long getClassOfServiceId(IssueMetadata metadata, Map<String, IssueMetadata> issuesMetadataByKey) {
        CustomField classOfService = getClassOfService(metadata, issuesMetadataByKey);
        return classOfService == null ? 0L : classOfService.getOptionId();
    }

    private CustomField getClassOfService(IssueMetadata metadata, Map<String, IssueMetadata> issuesMetadataByKey) {
        String defaultClassOfService = jiraProperties.getCustomfield().getClassOfService().getDefaultValue();
        CustomField classOfService = metadata.getClassOfService();

        boolean isNotDefaultClassOfService = classOfService != null
                                             && classOfService.getValue() != null
                                             && !classOfService.getValue().toString().equals(defaultClassOfService);
        if (isNotDefaultClassOfService)
            return classOfService;

        IssueMetadata parentMetadata = getParentMetadata(metadata, issuesMetadataByKey);
        if (parentMetadata == null)
            return classOfService;

        return getClassOfService(parentMetadata, issuesMetadataByKey);
    }

    private Map<String, CustomField> getRelease(IssueMetadata metadata, Map<String, IssueMetadata> issuesMetadataByKey) {
        Map<String, CustomField> release = metadata.getRelease();

        if (!release.isEmpty())
            return release;

        IssueMetadata parentMetadata = getParentMetadata(metadata, issuesMetadataByKey);
        if (parentMetadata == null)
            return newHashMap();

        return getRelease(parentMetadata, issuesMetadataByKey);
    }

    private IssueMetadata getParentMetadata(IssueMetadata metadata, Map<String, IssueMetadata> issuesMetadataByKey) {
        String parentKey = metadata.getParentKey();
        if (isNullOrEmpty(parentKey))
            return null;

        IssueMetadata parentMetadata = issuesMetadataByKey.get(parentKey);

        if (parentMetadata != null)
            return parentMetadata;

        Issue parent = jiraService.getIssueByKeyAsMaster(parentKey);
        parentMetadata = new IssueMetadata(parent, jiraProperties, parentIssueLinks, log);
        issuesMetadataByKey.put(parent.getKey(), parentMetadata);
        return parentMetadata;
    }
}
