package objective.taskboard.followup;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.jira.FrontEndMessageException;
import objective.taskboard.jira.ProjectService;

@Service
public class FollowUpDateRangeProvider {

    @Autowired
    private ProjectService projectService;

    public FollowUpProjectDataRangeDTO getDateRangeData(String projectKey) {
        Optional<ProjectFilterConfiguration> taskboardProjectOpt = projectService.getTaskboardProject(projectKey);
        if (!taskboardProjectOpt.isPresent())
            throw new FrontEndMessageException("Project " + projectKey + " doesn't exist");

        ProjectFilterConfiguration taskboardProject = taskboardProjectOpt.get();
        if (!taskboardProject.getStartDate().isPresent() || !taskboardProject.getDeliveryDate().isPresent())
            throw new FrontEndMessageException("No \"Start Date\" or \"Delivery Date\" configuration found for project " + projectKey + ".");

        return new FollowUpProjectDataRangeDTO(taskboardProject);
    }

    public static class FollowUpProjectDataRangeDTO {
        public String projectKey;
        public LocalDate startDate;
        public LocalDate deliveryDate;
        public FollowUpProjectDataRangeDTO(ProjectFilterConfiguration taskboardProject) {
            this.projectKey = taskboardProject.getProjectKey();
            this.startDate = taskboardProject.getStartDate().orElse(null);
            this.deliveryDate = taskboardProject.getDeliveryDate().orElse(null);
        }
    }

}
