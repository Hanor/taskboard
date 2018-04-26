package objective.taskboard.domain;

import com.google.common.base.Objects;

import objective.taskboard.data.Issue;

public class IssueStateHashCalculator {

    public int calculateHash(Issue issue) {
        return Objects.hashCode(
                  issue.getColor()
                , issue.getTeams()
                , issue.getId()
                , issue.getIssueKey()
                , issue.getProjectKey()
                , issue.getProject()
                , issue.getType()
                , issue.getSummary()
                , issue.getStatus()
                , issue.getStartDateStepMillis()
                , issue.getParent()
                , issue.getParentType()
                , issue.getDependencies()
                , issue.getCoAssignees()
                , issue.getAssignee()
                , issue.getPriority()
                , issue.getDueDate()
                , issue.getUpdatedDate()
                , issue.getCreated()
                , issue.getDescription()
                , issue.getComments()
                , issue.getLabels()
                , issue.getComponents()
                , issue.getPriorityOrder()
                , issue.getTimeTracking()
                , issue.getRemoteIssueUpdatedDate()
                , issue.getPriorityUpdatedDate()
                , issue.getSubtaskCards()
                , issue.getSubtasks()
                , issue.getReleaseId()
                , issue.getClassOfServiceValue()
                , issue.getAdditionalEstimatedHours()
                , issue.isCancelled()
                , issue.isCompleted()
                , issue.isBlocked()
                , issue.getLastBlockReason()
                , issue.getSubtasksTshirtSizes()
                , issue.getAdditionalEstimatedHoursField()
        );
    }
}
