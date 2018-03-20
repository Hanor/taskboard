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
package objective.taskboard.data;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;

import objective.taskboard.config.SpringContextBridge;
import objective.taskboard.cycletime.CycleTime;
import objective.taskboard.database.IssuePriorityService;
import objective.taskboard.domain.IssueColorService;
import objective.taskboard.domain.IssueStateHashCalculator;
import objective.taskboard.domain.converter.CardVisibilityEvalService;
import objective.taskboard.domain.converter.IssueTeamService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.BallparkMapping;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.jira.data.Version;
import objective.taskboard.repository.FilterCachedRepository;
import objective.taskboard.utils.DateTimeUtils;

public class Issue extends IssueScratch implements Serializable {
    private static final long serialVersionUID = 8513934402068368820L;

    private transient Issue parentCard;

    private transient Set<Issue> subtasks = new LinkedHashSet<>();

    private transient JiraProperties jiraProperties;

    private transient MetadataService metaDataService;

    private transient IssueTeamService issueTeamService;

    private transient FilterCachedRepository filterRepository;

    private transient CardVisibilityEvalService cardVisibilityEvalService;

    private transient ProjectService projectService;

    private transient IssueStateHashCalculator issueStateHashCalculator;

    private transient IssueColorService issueColorService;

    private transient CycleTime cycleTime;

    private transient IssuePriorityService issuePriorityService;

    public Issue(IssueScratch scratch,
            JiraProperties properties, 
            MetadataService metadataService, 
            IssueTeamService issueTeamService, 
            FilterCachedRepository filterRepository,
            CycleTime cycleTime,
            CardVisibilityEvalService cardVisibilityEvalService,
            ProjectService projectService,
            IssueStateHashCalculator issueStateHashCalculator,
            IssueColorService issueColorService,
            IssuePriorityService issuePriorityService) {
        this.id = scratch.id;
        this.issueKey = scratch.issueKey;
        this.projectKey = scratch.projectKey;
        this.project = scratch.project;
        this.type = scratch.type;
        this.summary = scratch.summary;
        this.status = scratch.status;
        this.startDateStepMillis = scratch.startDateStepMillis;
        this.parent = scratch.parent;
        this.dependencies = scratch.dependencies;
        this.assignee = scratch.assignee;
        this.priority = scratch.priority;
        this.dueDate = scratch.dueDate;
        this.created = scratch.created;
        this.description = scratch.description;
        this.comments = scratch.comments;
        this.labels = scratch.labels;
        this.components = scratch.components;
        this.blocked = scratch.blocked;
        this.lastBlockReason = scratch.lastBlockReason;
        this.tshirtSizes = scratch.tshirtSizes;
        this.additionalEstimatedHours = scratch.additionalEstimatedHours;
        this.timeTracking = scratch.timeTracking;
        this.reporter = scratch.reporter;
        this.classOfService = scratch.classOfService;
        this.releaseId = scratch.releaseId;
        this.changelog = scratch.changelog;
        this.remoteIssueUpdatedDate = scratch.remoteIssueUpdatedDate;
        this.coAssignees = scratch.coAssignees;

        this.metaDataService = metadataService;
        this.jiraProperties = properties;
        this.issueTeamService = issueTeamService;
        this.filterRepository = filterRepository;
        this.cycleTime = cycleTime;
        this.cardVisibilityEvalService = cardVisibilityEvalService;
        this.projectService = projectService;
        this.issueStateHashCalculator = issueStateHashCalculator;
        this.issueColorService = issueColorService;
        this.issuePriorityService = issuePriorityService;
        this.worklogs = scratch.worklogs;
        this.assignedTeamsIds = scratch.assignedTeamsIds;
    }
    
    public Issue() {
        jiraProperties = SpringContextBridge.getBean(JiraProperties.class);
        metaDataService = SpringContextBridge.getBean(MetadataService.class);
        issueTeamService = SpringContextBridge.getBean(IssueTeamService.class);
        filterRepository = SpringContextBridge.getBean(FilterCachedRepository.class);
        cycleTime = SpringContextBridge.getBean(CycleTime.class);
        cardVisibilityEvalService = SpringContextBridge.getBean(CardVisibilityEvalService.class);
        projectService = SpringContextBridge.getBean(ProjectService.class);
        issueStateHashCalculator = SpringContextBridge.getBean(IssueStateHashCalculator.class);
        issueColorService = SpringContextBridge.getBean(IssueColorService.class);
        issuePriorityService = SpringContextBridge.getBean(IssuePriorityService.class);
    }

    @JsonIgnore
    public boolean isDeferred() {
        boolean isDeferredStatus = jiraProperties.getStatusesDeferredIds().contains(this.getStatus());
        if (!isDeferredStatus && parentCard != null)
            return parentCard.isDeferred();
        return isDeferredStatus;
    }

    @JsonIgnore
    public String getTShirtSize() {
        String mainTShirtSizeFieldId = jiraProperties.getCustomfield().getTShirtSize().getMainTShirtSizeFieldId();
        return Optional.ofNullable(tshirtSizes.get(mainTShirtSizeFieldId))
                .flatMap(customField -> Optional.ofNullable((String) customField.getValue()))
                .orElse(null);
    }

    @JsonIgnore
    public String getTshirtSizeOfSubtaskForBallpark(BallparkMapping mapping) {
        return Optional.ofNullable(tshirtSizes.get(mapping.getTshirtCustomFieldId()))
                .flatMap(customField -> Optional.ofNullable((String) customField.getValue()))
                .orElse(null);
    }

    @JsonIgnore
    public List<BallparkMapping> getBallparkMappings() {
        return jiraProperties.getFollowup().getBallparkMappings().get(getType());
    }

    @JsonIgnore
    public List<BallparkMapping> getActiveBallparkMappings() {
        List<BallparkMapping> list = jiraProperties.getFollowup().getBallparkMappings().get(getType());
        if (list == null)
            return null;

        return list.stream().filter(bm -> getTshirtSizeOfSubtaskForBallpark(bm)!=null).collect(Collectors.toList());
    }
    
    public Map<String, CustomField> getTshirtSizes(){
        return tshirtSizes;
    }
    
    public String getParentSummary() {
        if (this.parentCard != null)
            return this.parentCard.getSummary();
        return "";
    }

    public String getColor() {
        return issueColorService.getColor(getClassOfServiceId());
    }
    
    public Set<String> getMismatchingUsers() {
        return issueTeamService.getMismatchingUsers(this);
    }

    public Set<String> getTeamNames() {
        return getTeams().stream().map(t->t.name).collect(Collectors.toSet());
    }
    
    public Set<CardTeam> getTeams() {
        Set<CardTeam> issueTeams = new LinkedHashSet<>();
        issueTeams.addAll(issueTeamService.getTeamsForIds(getRawAssignedTeamsIds()));
        if (!issueTeams.isEmpty())
            return issueTeams;

        Optional<Issue> parentCardOpt = getParentCard();
        if (parentCardOpt.isPresent())
            return parentCardOpt.get().getTeams();

        issueTeams.add(issueTeamService.getDefaultTeam(getProjectKey()));

        return issueTeams;
    }
    
    /**
     * Returns the value of assigned teams id, *excluding* the default team. This is the value in jira field.
     * 
     * @return the list of team id values in the jira issue. Prefer using getTeams to find the *actual* teams. 
     */
    public List<Long> getRawAssignedTeamsIds() {
        return assignedTeamsIds;
    }

    public boolean isUsingDefaultTeam() {
        if (getRawAssignedTeamsIds().size() > 0)
            return false;
        Optional<Issue> parentCard = getParentCard();
        if (!parentCard.isPresent())
            return true;

        return parentCard.get().isUsingDefaultTeam();
    }

    public boolean isUsingParentTeam() {
        if (parentCard == null)
            return false;

        if (getRawAssignedTeamsIds().size() > 0)
            return false;

        return !parentCard.isUsingDefaultTeam();
    }

    public void setParentCard(Issue parentCard) {
        this.parentCard = parentCard;
        if (parentCard != null)
            parentCard.addsubtask(this);
    }
    
    private void addsubtask(Issue issue) {
        this.subtasks.remove(issue);
        this.subtasks.add(issue);
    }

    public void unlinkParent() {
        if(parentCard != null)
            parentCard.subtasks.remove(this);
        parentCard = null;
    }

    @JsonIgnore
    public Optional<Issue> getParentCard() {
        return Optional.ofNullable(parentCard);
    }

    public String getClassOfServiceValue() {
        String defaultClassOfService = jiraProperties.getCustomfield().getClassOfService().getDefaultValue();
        CustomField classOfService = getClassOfServiceCustomField();
        return classOfService == null ? defaultClassOfService : (String)classOfService.getValue();
    }

    @JsonIgnore
    public Long getClassOfServiceId() {
        CustomField classOfService = getClassOfServiceCustomField();
        return classOfService == null ? 0L : classOfService.getOptionId();
    }

    @JsonIgnore
    public CustomField getClassOfServiceCustomField() {
        String defaultClassOfService = jiraProperties.getCustomfield().getClassOfService().getDefaultValue();
        CustomField classOfService = getLocalClassOfServiceCustomField();

        boolean isNotDefaultClassOfService = classOfService != null
                                             && classOfService.getValue() != null
                                             && !classOfService.getValue().toString().equals(defaultClassOfService);
        if (isNotDefaultClassOfService)
            return classOfService;

        Optional<Issue> pc = getParentCard();
        if (pc.isPresent())
            return pc.get().getClassOfServiceCustomField();

        return classOfService;
    }

    @JsonIgnore
    public List<Changelog> getChangelog() {
        return changelog;
    }

    public boolean isDemand() {
        return jiraProperties.getIssuetype().getDemand().getId() == this.getType();
    }

    public boolean isFeature() {
        return jiraProperties.getIssuetype().getFeatures().stream().anyMatch(ft -> ft.getId() == this.getType());
    }

    public boolean isSubTask() {
        return !isDemand() && !isFeature();
    }

    @JsonIgnore
    public Integer getIssueKeyNum() {
        return Integer.parseInt(issueKey.replace(projectKey+"-", ""));
    }

    @JsonIgnore
    public String getIssueTypeName() {
        return metaDataService.getIssueTypeById(type).getName();
    }

    @JsonIgnore
    public String getStatusName() {
        return metaDataService.getStatusById(status).name;
    }

    @JsonIgnore
    public Integer getStatusPriority() {
        Integer r;
        if (this.isDemand())
            r = jiraProperties.getStatusPriorityOrder().getDemandPriorityByStatus(this.getStatusName());
        else
        if (this.isFeature())
            r = jiraProperties.getStatusPriorityOrder().getTaskPriorityByStatus(this.getStatusName());
        else
            r = jiraProperties.getStatusPriorityOrder().getSubtaskPriorityByStatus(this.getStatusName());
        
        if (r == null) r = 0;
        return r;
    }

    @JsonIgnore
    public Double getCycleTime(ZoneId timezone) {
        return cycleTime.getCycleTime(Instant.ofEpochMilli(startDateStepMillis), timezone, status);
    }
    
    public CustomField getAdditionalEstimatedHoursField() {
        return additionalEstimatedHours;
    }

    public Double getAdditionalEstimatedHours() {
        CustomField additionalEstimatedHours = getAdditionalEstimatedHoursField();
        if (additionalEstimatedHours != null)
            return (Double) additionalEstimatedHours.getValue();
        return null;
    }
    
    public boolean isCancelled() {
        return jiraProperties.getStatusesCanceledIds().stream().anyMatch(s -> s.equals(status));
    }
    
    public boolean isCompleted() {
        return jiraProperties.getStatusesCompletedIds().stream().anyMatch(s -> s.equals(status));
    }    

    public boolean isBlocked() {
        return blocked;
    }

    public String getLastBlockReason() {
        return lastBlockReason;
    }
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public List<CustomField> getSubtasksTshirtSizes() {
        return jiraProperties.getCustomfield().getTShirtSize().getIds()
                .stream()
                .filter(tshirtSizes::containsKey)
                .map(tshirtSizes::get)
                .collect(Collectors.toList());
    }
    
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getCardTshirtSize() {
        List<CustomField> tshirtSizes = getSubtasksTshirtSizes();
        if (tshirtSizes.size() == 1)
            return tshirtSizes.get(0).getValue().toString();
        return "";
    }

    public Long getId() {
        return this.id;
    }

    public String getIssueKey() {
        return this.issueKey;
    }

    public String getProjectKey() {
        return this.projectKey;
    }

    public String getProject() {
        return this.project;
    }

    public long getType() {
        return this.type;
    }

    public String getTypeIconUri() {
        return metaDataService.getIssueTypeById(type).getIconUri().toASCIIString();
    }

    public String getSummary() {
        return this.summary;
    }

    public long getStatus() {
        return this.status;
    }

    public long getStartDateStepMillis() {
        return this.startDateStepMillis;
    }

    public String getParent() {
        return this.parent;
    }

    public long getParentType() {
        return getParentCard()
                .map(parent -> parent.type)
                .orElse(0L);
    }

    public String getParentTypeIconUri() {
        return getParentCard()
                .map(Issue::getTypeIconUri)
                .orElse("");
    }

    public List<String> getDependencies() {
        return this.dependencies;
    }

    public List<User> getCoAssignees() {
        return this.coAssignees;
    }
    
    public List<User> getAssignees() {
        LinkedList<User> assigneeSet = new LinkedList<>();
        if (getAssignee().isAssigned())
            assigneeSet.add(getAssignee());
        assigneeSet.addAll(getCoAssignees());
        return assigneeSet;
    }

    public User getAssignee() {
        return this.assignee;
    }
    
    public long getPriority() {
        return this.priority;
    }

    @JsonDeserialize(using = DateDeserializer.class)
    public Date getDueDate() {
        return this.dueDate;
    }

    @JsonIgnore
    public ZonedDateTime getDueDateByTimezoneId(ZoneId timezone) {
        return DateTimeUtils.get(dueDate, timezone);
    }

    @JsonDeserialize(using = DateDeserializer.class)
    public Date getUpdatedDate() {
        Date priorityUpdatedDate = getPriorityUpdatedDate();

        if (remoteIssueUpdatedDate == null
                || priorityUpdatedDate.after(remoteIssueUpdatedDate))
            return priorityUpdatedDate;
        
        return remoteIssueUpdatedDate;
    }

    @JsonIgnore
    public ZonedDateTime getUpdatedDateByTimezoneId(ZoneId timezone) {
        return DateTimeUtils.get(getUpdatedDate(), timezone);
    }

    public long getCreated() {
        return this.created;
    }

    @JsonIgnore
    public ZonedDateTime getCreatedDateByTimezoneId(ZoneId timezone) {
        return DateTimeUtils.get(this.created, timezone);
    }

    public String getDescription() {
        return this.description;
    }

    public String getComments() {
        return this.comments;
    }

    public List<String> getLabels() {
        return this.labels;
    }

    public List<String> getComponents() {
        return this.components;
    }

    public long getPriorityOrder() {
        return issuePriorityService.determinePriority(this);
    }

    public TaskboardTimeTracking getTimeTracking() {
        return this.timeTracking;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setIssueKey(final String issueKey) {
        this.issueKey = issueKey;
    }

    public void setProjectKey(final String projectKey) {
        this.projectKey = projectKey;
    }

    public void setProject(final String project) {
        this.project = project;
    }

    public void setType(final long type) {
        this.type = type;
    }

    public void setSummary(final String summary) {
        this.summary = summary;
    }

    public void setStatus(final long status) {
        this.status = status;
    }

    public void setStartDateStepMillis(final long startDateStepMillis) {
        this.startDateStepMillis = startDateStepMillis;
    }

    public void setParent(final String parent) {
        this.parent = parent;
    }

    public void setDependencies(final List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public void setAssignee(final User assignee) {
        this.assignee = assignee;
    }

    public void setPriority(final long priority) {
        this.priority = priority;
    }

    public void setDueDate(final Date dueDate) {
        this.dueDate = dueDate;
    }

    public void setRemoteIssueUpdatedDate(final Date remoteIssueUpdatedDate) {
        this.remoteIssueUpdatedDate = remoteIssueUpdatedDate;
    }
    
    @JsonDeserialize(using = DateDeserializer.class)
    public Date getRemoteIssueUpdatedDate() {
        return remoteIssueUpdatedDate;
    }
    
    @JsonDeserialize(using = DateDeserializer.class)
    public Date getPriorityUpdatedDate() {
        return issuePriorityService.priorityUpdateDate(this);
    }
    
    public void setCreated(final long created) {
        this.created = created;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setComments(final String comments) {
        this.comments = comments;
    }

    public void setLabels(final LinkedList<String> labels) {
        this.labels = labels;
    }

    public void setComponents(final List<String> components) {
        this.components = components;
    }

    public void setReleaseId(final String releaseId) {
        this.releaseId = releaseId;
    }

    public void setTimeTracking(final TaskboardTimeTracking timeTracking) {
        this.timeTracking = timeTracking;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String nameReporter) {
        this.reporter = nameReporter;
    }
    
    @JsonIgnore
    public CustomField getLocalClassOfServiceCustomField() {
        return classOfService;
    }

    public boolean isVisible() {
        if (isDeferred())
            return false;
        
        boolean isVisible =  filterRepository.getCache().stream().filter(f -> f.isApplicable(this)).count() != 0;
        if (!isVisible)
            return false;
        
        return cardVisibilityEvalService.isStillInVisibleRange(
                status, 
                getUpdatedDate().toInstant(), 
                changelog);
    }

    public Set<Issue> getSubtaskCards() {
        return subtasks;
    }
   
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public List<Subtask> getSubtasks() {
        return subtasks.stream().map(s->
            new Subtask(s.issueKey, 
                        s.summary, 
                        s.getStatusName(), 
                        s.getIssueTypeName(),
                        s.getTypeIconUri(), 
                        issueColorService.getStatusColor(s.type, s.status)))
                .collect(Collectors.toList());
    }

    public String getReleaseId() {
        if(releaseId != null)
            return releaseId;

        Optional<Issue> pc = getParentCard();
        if(pc.isPresent())
            return pc.get().releaseId;

        return null;
    }

    public Version getRelease() {
        return projectService.getVersion(getReleaseId());
    }

    public int getStateHash() {
        return issueStateHashCalculator.calculateHash(this);
    }
    
    public List<Worklog> getWorklogs() {
        return worklogs;
    }

    private Object readResolve() {
        this.subtasks = new LinkedHashSet<>();
        return this;
    }

    public void restoreServices(
            JiraProperties jiraProperties, 
            MetadataService metaDataService,
            IssueTeamService issueTeamService,
            FilterCachedRepository filterRepository,
            CycleTime cycleTime,
            CardVisibilityEvalService cardVisibilityEvalService,
            ProjectService projectService,
            IssueStateHashCalculator issueStateHashCalculator,
            IssueColorService issueColorService,
            IssuePriorityService issuePriorityService) {
        this.jiraProperties = jiraProperties;
        this.metaDataService = metaDataService;
        this.issueTeamService = issueTeamService;
        this.filterRepository = filterRepository;
        this.cycleTime = cycleTime;
        this.cardVisibilityEvalService = cardVisibilityEvalService;
        this.projectService = projectService;
        this.issueStateHashCalculator = issueStateHashCalculator;
        this.issueColorService = issueColorService;
        this.issuePriorityService = issuePriorityService;
    }
    
    @Override
    public int hashCode() {
        if (this.issueKey == null)
            return 0;
        return this.issueKey.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Issue) {
            Issue other = (Issue)obj;
            
            return (issueKey+"").equals(other.issueKey+"");
        }
        return false;
    }

    public void addTeam(Team teamToAdd) {
        if (isUsingDefaultTeam())
            assignedTeamsIds.add(issueTeamService.getDefaultTeamId(this));

        if (isUsingParentTeam())
            assignedTeamsIds.addAll(parentCard.getRawAssignedTeamsIds());

        assignedTeamsIds.add(teamToAdd.getId());
    }

    public void removeTeam(Team teamToRemove) {
        assignedTeamsIds.remove(teamToRemove.getId());
    }
    
    public void replaceTeam(Optional<Team> teamToReplace, Team replacementTeam) {
        if (teamToReplace.isPresent()) {
            int previousPos = assignedTeamsIds.indexOf(teamToReplace.get().getId());
            if (previousPos > -1) {
                assignedTeamsIds.set(previousPos, replacementTeam.getId());
                return;
            }
        }
        assignedTeamsIds.add(replacementTeam.getId());
    }
    
    public static class CardTeam {
        public String name;
        public Long id;
        
        public CardTeam() {}

        public CardTeam(Long i) {
            id = i;
        }

        public CardTeam(String name, Long i) {
            this.name = name;
            id = i;
        }

        public static CardTeam from(Team team) {
            CardTeam card = new CardTeam();
            card.name = team.getName();
            card.id = team.getId();
            return card;
        }
    }

    public Issue copy() {
        Issue copy = SerializationUtils.clone(this);
        copy.restoreServices(
                jiraProperties,
                metaDataService,
                issueTeamService,
                filterRepository,
                cycleTime,
                cardVisibilityEvalService,
                projectService,
                issueStateHashCalculator,
                issueColorService,
                issuePriorityService);

        if (this.parentCard != null) {
            Issue parentCopy = SerializationUtils.clone(this.parentCard);
            copy.setParentCard(parentCopy);
        }

        return copy;
    }
}