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

import static com.google.common.collect.Maps.newHashMap;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;

import objective.taskboard.config.SpringContextBridge;
import objective.taskboard.domain.converter.CardVisibilityEvalService;
import objective.taskboard.domain.converter.IssueCoAssignee;
import objective.taskboard.domain.converter.IssueTeamService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.BallparkMapping;
import objective.taskboard.jira.MetadataService;
import objective.taskboard.repository.FilterCachedRepository;


@JsonIgnoreProperties({"jiraProperties", "metaDataService"})
public class Issue extends IssueScratch implements Serializable {
    private static final long serialVersionUID = 8513934402068368820L;

    @JsonIgnore
    private transient Issue parentCard;
    
    private transient Set<Issue> subtasks = new LinkedHashSet<>();

    private transient JiraProperties jiraProperties;
    
    private transient MetadataService metaDataService;
    
    private transient IssueTeamService issueTeamService;
    
    private transient FilterCachedRepository filterRepository;
    
    private transient CardVisibilityEvalService cardVisibilityEvalService;
    
    @JsonIgnore
    private List<IssueCoAssignee> coAssignees = new LinkedList<>(); //NOSONAR
    
    private String color;
    
    public Issue(IssueScratch scratch, 
            JiraProperties properties, 
            MetadataService metadataService, 
            IssueTeamService issueTeamService, 
            FilterCachedRepository filterRepository,
            CardVisibilityEvalService cardVisibilityEvalService) {
        this.id = scratch.id;
        this.issueKey = scratch.issueKey;
        this.projectKey = scratch.projectKey;
        this.project = scratch.project;
        this.type = scratch.type;
        this.typeIconUri = scratch.typeIconUri;
        this.summary = scratch.summary;
        this.status = scratch.status;
        this.startDateStepMillis = scratch.startDateStepMillis;
        this.subresponsavel1 = scratch.subresponsavel1;
        this.subresponsavel2 = scratch.subresponsavel2;
        this.parent = scratch.parent;
        this.parentType = scratch.parentType;
        this.parentTypeIconUri = scratch.parentTypeIconUri;
        this.dependencies = scratch.dependencies;
        this.subResponsaveis = scratch.subResponsaveis;
        this.assignee = scratch.assignee;
        this.priority = scratch.priority;
        this.dueDate = scratch.dueDate;
        this.created = scratch.created;
        this.description = scratch.description;
        this.comments = scratch.comments;
        this.labels = scratch.labels;
        this.components = scratch.components;
        this.customFields = scratch.customFields;
        this.priorityOrder = scratch.priorityOrder;
        this.timeTracking = scratch.timeTracking;
        this.reporter = scratch.reporter;
        this.coAssignees = scratch.coAssignees;
        this.classOfService = scratch.classOfService;
        this.release = scratch.release;
        this.changelog = scratch.changelog;
        this.priorityUpdatedDate = scratch.priorityUpdatedDate;
        this.remoteIssueUpdatedDate = scratch.remoteIssueUpdatedDate;
        
        this.metaDataService = metadataService;
        this.jiraProperties = properties;
        this.issueTeamService = issueTeamService;
        this.filterRepository = filterRepository;
        this.cardVisibilityEvalService = cardVisibilityEvalService;        
        this.render = false;
        this.favorite = false;
        this.hidden = false;        
        this.color = null;
    }
    
    @JsonAnyGetter
    public Map<String, Serializable> getCustomFields() {
        return customFields;
    }

    public Issue() {
        jiraProperties = SpringContextBridge.getBean(JiraProperties.class);
        metaDataService = SpringContextBridge.getBean(MetadataService.class);
        issueTeamService = SpringContextBridge.getBean(IssueTeamService.class);
        filterRepository = SpringContextBridge.getBean(FilterCachedRepository.class);
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
        if (customFields.get(mainTShirtSizeFieldId) == null)
            return null;

        CustomField customField = (CustomField)customFields.get(mainTShirtSizeFieldId);
        if (customField == null || customField.getValue() == null)
            return null;
        return customField.getValue().toString();
    }

    @JsonIgnore
    public void setTShirtSize(String value) {
        String mainTShirtSizeFieldId = jiraProperties.getCustomfield().getTShirtSize().getMainTShirtSizeFieldId();
        if (customFields.get(mainTShirtSizeFieldId) == null)
            return;
        CustomField customField = (CustomField)customFields.get(mainTShirtSizeFieldId);
        if (customField == null) {
            customField = new CustomField(mainTShirtSizeFieldId, value);
            customFields.put(mainTShirtSizeFieldId, customField);
        }
        customField.setValue(value);
    }

    @JsonIgnore
    public String getTshirtSizeOfSubtaskForBallpark(BallparkMapping mapping) {
        CustomField customField = (CustomField)customFields.get(mapping.getTshirtCustomFieldId());
        if (customField == null || customField.getValue() == null) return null;
        return customField.getValue().toString();
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

    public String getColor() {
        return this.color;
    }

    public String getUsersTeam() {
        return issueTeamService.getUsersTeam(this);
    }

    public Set<String> getTeams() {
        return issueTeamService.getTeams(this);
    }

    public void setColor(final String color) {
        this.color = color;
    }

    public void setParentCard(Issue parentCard) {
        this.parentCard = parentCard;
        if (parentCard != null)
            parentCard.addsubtask(this);
    }
    
    private void addsubtask(Issue issue) {
        this.subtasks.add(issue);
    }

    @JsonIgnore
    public Optional<Issue> getParentCard() {
        return Optional.ofNullable(parentCard);
    }

    @JsonIgnore
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
    public Map<String, CustomField> getRelease() {
        Map<String, CustomField> release = getLocalRelease();

        if (!release.isEmpty())
            return release;

        Optional<Issue> pc = getParentCard();
        if (!pc.isPresent())
            return newHashMap();

        return pc.get().getRelease();
    }
    
    @JsonIgnore
    public List<Changelog> getChangelog() {
        return changelog;
    }

    @JsonIgnore
    public boolean isDemand() {
        return jiraProperties.getIssuetype().getDemand().getId() == this.getType();
    }

    @JsonIgnore
    public boolean isFeature() {
        return jiraProperties.getIssuetype().getFeatures().stream().anyMatch(ft -> ft.getId() == this.getType());
    }

    @JsonIgnore
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
        return this.typeIconUri;
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

    public String getSubresponsavel1() {
        return this.subresponsavel1;
    }

    public String getSubresponsavel2() {
        return this.subresponsavel2;
    }

    public String getParent() {
        return this.parent;
    }

    public long getParentType() {
        return this.parentType;
    }

    public String getParentTypeIconUri() {
        return this.parentTypeIconUri;
    }

    public List<String> getDependencies() {
        return this.dependencies;
    }

    public boolean isRender() {
        return this.render;
    }

    public boolean isFavorite() {
        return this.favorite;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public String getSubResponsaveis() {
        return this.subResponsaveis;
    }

    public String getAssignee() {
        return this.assignee;
    }

    public long getPriority() {
        return this.priority;
    }

    @JsonDeserialize(using = DateDeserializer.class)
    public Date getDueDate() {
        return this.dueDate;
    }

    @JsonDeserialize(using = DateDeserializer.class)
    public Date getUpdatedDate() {
        if (priorityUpdatedDate == null && remoteIssueUpdatedDate == null)
            return null;
        
        if (remoteIssueUpdatedDate == null)
            return priorityUpdatedDate;
                
        if (priorityUpdatedDate == null)
            return priorityUpdatedDate;
        
        if (priorityUpdatedDate.after(remoteIssueUpdatedDate))
            return priorityUpdatedDate;
        
        return remoteIssueUpdatedDate;
    }
    
    public long getCreated() {
        return this.created;
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

    public Long getPriorityOrder() {
        return this.priorityOrder;
    }

    public TaskboardTimeTracking getTimeTracking() {
        return this.timeTracking;
    }

    public JiraProperties getJiraProperties() {
        return this.jiraProperties;
    }

    public MetadataService getMetaDataService() {
        return this.metaDataService;
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

    public void setTypeIconUri(final String typeIconUri) {
        this.typeIconUri = typeIconUri;
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

    public void setSubresponsavel1(final String subresponsavel1) {
        this.subresponsavel1 = subresponsavel1;
    }

    public void setSubresponsavel2(final String subresponsavel2) {
        this.subresponsavel2 = subresponsavel2;
    }

    public void setParent(final String parent) {
        this.parent = parent;
    }

    public void setParentType(final long parentType) {
        this.parentType = parentType;
    }

    public void setParentTypeIconUri(final String parentTypeIconUri) {
        this.parentTypeIconUri = parentTypeIconUri;
    }

    public void setDependencies(final List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public void setRender(final boolean render) {
        this.render = render;
    }

    public void setFavorite(final boolean favorite) {
        this.favorite = favorite;
    }

    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
    }

    public void setSubResponsaveis(final String subResponsaveis) {
        this.subResponsaveis = subResponsaveis;
    }

    public void setAssignee(final String assignee) {
        this.assignee = assignee;
    }

    public void setPriority(final long priority) {
        this.priority = priority;
    }

    public void setDueDate(final Date dueDate) {
        this.dueDate = dueDate;
    }

    public void setPriorityUpdatedDate(final Date updatedDate) {
        this.priorityUpdatedDate = updatedDate;
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
        return priorityUpdatedDate;
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

    public void setPriorityOrder(final Long priorityOrder) {
        this.priorityOrder = priorityOrder;
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
    public List<IssueCoAssignee> getCoAssignees() {
        return coAssignees;
    }

    public void setCoAssignees(List<IssueCoAssignee> coAssigness) {
        this.coAssignees = coAssigness;
    }

    @JsonIgnore
    public CustomField getLocalClassOfServiceCustomField() {
        return classOfService;
    }

    @JsonIgnore
    public Map<String, CustomField> getLocalRelease() {
        return release;
    }

    public void setCustomFields(final Map<String, Serializable> customFields) {
        this.customFields = customFields;
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
   
    public List<Subtask> getSubtasks() {
        return subtasks.stream().map(s->new Subtask(s.issueKey, s.summary)).collect(Collectors.toList());
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
            CardVisibilityEvalService cardVisibilityEvalService) {
        this.jiraProperties = jiraProperties;
        this.metaDataService = metaDataService;
        this.issueTeamService = issueTeamService;
        this.filterRepository = filterRepository;
        this.cardVisibilityEvalService = cardVisibilityEvalService;
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
}