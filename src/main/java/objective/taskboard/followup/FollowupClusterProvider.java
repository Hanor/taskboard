package objective.taskboard.followup;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.followup.cluster.FollowUpClusterItem;
import objective.taskboard.followup.cluster.FollowUpClusterItemRepository;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;

@Component
public class FollowupClusterProvider {

    @Autowired
    private FollowUpClusterItemRepository clusterItemRepository;
    @Autowired
    private ProjectFilterConfigurationCachedRepository projectRepository;

    public FollowupCluster getForProject(String projectKey) {
        Optional<ProjectFilterConfiguration> project = projectRepository.getProjectByKey(projectKey);
        return project.map(this::getFor).orElse(new EmptyFollowupCluster());
    }

    public FollowupCluster getFor(ProjectFilterConfiguration project) {
        List<FollowUpClusterItem> clusterItems = clusterItemRepository.findByProject(project);
        if(isEmpty(clusterItems))
            return new EmptyFollowupCluster();
        return new FollowupClusterImpl(clusterItems);
    }
}
