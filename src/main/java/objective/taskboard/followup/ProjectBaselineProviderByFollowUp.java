package objective.taskboard.followup;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import objective.taskboard.project.ProjectBaselineProvider;

@Component
class ProjectBaselineProviderByFollowUp implements ProjectBaselineProvider {
    
    private final FollowUpDataRepository dataRepository;

    @Autowired
    public ProjectBaselineProviderByFollowUp(FollowUpDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public List<LocalDate> getAvailableDates(String projectKey) {
        return dataRepository.getHistoryByProject(projectKey);
    }

}
