package objective.taskboard.controller;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers.DateDeserializer;

import objective.taskboard.data.Changelog;
import objective.taskboard.data.CustomField;
import objective.taskboard.data.Issue;
import objective.taskboard.data.Issue.CardTeam;
import objective.taskboard.data.Subtask;
import objective.taskboard.data.TaskboardTimeTracking;
import objective.taskboard.data.User;
import objective.taskboard.jira.data.Version;

public class CardDto {
    public Long id;
    public String issueKey;
    public String projectKey;
    public String project;
    public long type;
    public String summary;
    public long status;
    public long startDateStepMillis;
    public String parent;
    public List<String> dependencies;
    public User assignee;
    public long priority;

    @JsonDeserialize(using = DateDeserializer.class)
    public Date dueDate;

    public long created;
    public String description;
    public String comments;
    public List<String> labels;
    public List<String> components;
    public boolean blocked;
    public String lastBlockReason;
    public CustomField additionalEstimatedHours;
    public TaskboardTimeTracking timeTracking;
    public String releaseId;
    public List<Changelog> changelog;
    public String reporter;
    public String parentSummary;
    public String color;
    public Set<String> mismatchingUsers;
    public List<String> teamNames;
    public List<CardTeam> teams;
    public boolean usingDefaultTeam;
    public boolean usingParentTeam;
    public String classOfServiceValue;
    public boolean demand;
    public boolean feature;
    public boolean subTask;
    public CustomField additionalEstimatedHoursField;
    public boolean cancelled;
    public boolean completed;
    public List<CustomField> subtasksTshirtSizes;
    public String cardTshirtSize;
    public String typeIconUri;
    public long parentType;
    public String parentTypeIconUri;
    public List<User> assignees;
    public long priorityOrder;
    public List<Subtask> subtasks;
    public Version release;
    public int stateHash;
    
    public static CardDto fromIssue(Issue issue, List<Long> visibleTeams) {
        CardDto cardDto = new CardDto();
        cardDto.id = issue.getId();
        cardDto.issueKey = issue.getIssueKey();
        cardDto.projectKey = issue.getIssueKey();
        cardDto.project = issue.getProject();
        cardDto.projectKey = issue.getProjectKey();
        cardDto.type = issue.getType();
        cardDto.summary = issue.getSummary();
        cardDto.status = issue.getStatus();
        cardDto.startDateStepMillis = issue.getStartDateStepMillis();
        cardDto.dependencies = issue.getDependencies();
        cardDto.assignee = issue.getAssignee();
        cardDto.assignees = issue.getAssignees();
        cardDto.priority = issue.getPriority();
        cardDto.dueDate = issue.getDueDate();
        cardDto.created = issue.getCreated();
        cardDto.description = issue.getDescription();
        cardDto.comments = issue.getComments();
        cardDto.labels = issue.getLabels();
        cardDto.components = issue.getComponents();
        cardDto.blocked = issue.isBlocked();
        cardDto.lastBlockReason = issue.getLastBlockReason();
        cardDto.additionalEstimatedHours = issue.getAdditionalEstimatedHoursField();
        cardDto.timeTracking = issue.getTimeTracking();
        cardDto.releaseId = issue.getReleaseId();
        cardDto.changelog = issue.getChangelog();
        cardDto.reporter = issue.getReporter();
        cardDto.color = issue.getColor();
        cardDto.changelog = issue.getChangelog();
        cardDto.priorityOrder = issue.getPriorityOrder();
        
        cardDto.classOfServiceValue = issue.getClassOfServiceValue();
        cardDto.demand = issue.isDemand();
        cardDto.feature = issue.isFeature();
        cardDto.subTask = issue.isSubTask();
        cardDto.cancelled = issue.isCancelled();
        cardDto.completed = issue.isCompleted();
        cardDto.additionalEstimatedHoursField = issue.getAdditionalEstimatedHoursField();

        cardDto.subtasksTshirtSizes = issue.getSubtasksTshirtSizes();
        cardDto.cardTshirtSize = issue.getCardTshirtSize();
        cardDto.typeIconUri = issue.getTypeIconUri();
        cardDto.release = issue.getRelease();
        
        cardDto.parent = issue.getParent();
        cardDto.parentSummary = issue.getParentSummary();
        cardDto.parentType = issue.getParentType();
        cardDto.parentTypeIconUri = issue.getParentTypeIconUri();
        
        cardDto.subtasks = issue.getSubtasks();
        cardDto.teams = filterTeams(issue.getTeams(), visibleTeams);
        cardDto.teamNames = cardDto.teams.stream().map(t->t.name).distinct().collect(Collectors.toList()); 
        cardDto.usingDefaultTeam = issue.isUsingDefaultTeam();
        cardDto.usingParentTeam= issue.isUsingParentTeam();
        
        cardDto.stateHash = issue.getStateHash();
        cardDto.mismatchingUsers = issue.getMismatchingUsers();
        return cardDto;
    }

    private static List<CardTeam> filterTeams(Set<CardTeam> cardTeams, List<Long> visibleTeams) {
        return cardTeams.stream().filter(t->visibleTeams.contains(t.id)).collect(Collectors.toList());
    }
}