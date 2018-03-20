package objective.taskboard.domain;

import org.springframework.stereotype.Service;

import com.google.common.base.Objects;

import objective.taskboard.data.Issue;

@Service
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
                , issue.getTypeIconUri()
                , issue.getSummary()
                , issue.getStatus()
                , issue.getStartDateStepMillis()
                , issue.getParent()
                , issue.getParentType()
                , issue.getParentTypeIconUri()
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
                , issue.isVisible()
                , issue.getSubtaskCards()
                , issue.getSubtasks()
                , issue.getReleaseId()
                , issue.getRelease()
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
