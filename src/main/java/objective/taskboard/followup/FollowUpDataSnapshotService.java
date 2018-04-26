package objective.taskboard.followup;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.domain.FollowupDailySynthesis;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.followup.impl.FollowUpDataProviderFromCurrentState;
import objective.taskboard.repository.FollowupDailySynthesisRepository;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.utils.Clock;

@Service
public class FollowUpDataSnapshotService {

    private final static Logger log = LoggerFactory.getLogger(FollowUpDataSnapshotService.class);

    private final Clock clock;
    private final FollowUpDataRepository historyRepository;
    private final ProjectFilterConfigurationCachedRepository projectRepository;
    private final FollowupDailySynthesisRepository dailySynthesisRepository;
    private final FollowUpDataProviderFromCurrentState dataGenerator;
    private final FollowupClusterProvider clusterProvider;
    
    @Autowired
    public FollowUpDataSnapshotService(
            Clock clock,
            FollowUpDataRepository historyRepository,
            ProjectFilterConfigurationCachedRepository projectRepository,
            FollowupDailySynthesisRepository dailySynthesisRepository, 
            FollowUpDataProviderFromCurrentState dataGenerator,
            FollowupClusterProvider clusterProvider) {
        this.clock = clock;
        this.historyRepository = historyRepository;
        this.projectRepository = projectRepository;
        this.dailySynthesisRepository = dailySynthesisRepository;
        this.dataGenerator = dataGenerator;
        this.clusterProvider = clusterProvider;
    }

    public FollowUpDataSnapshot getFromCurrentState(ZoneId timezone, String projectKey) {
        return createSnapshot(LocalDate.now(), projectKey,
                (cluster) -> dataGenerator.generate(timezone, cluster, projectKey));
    }

    public FollowUpDataSnapshot getFromHistory(LocalDate date, ZoneId timezone, String projectKey) {
        return createSnapshot(date, projectKey, 
                (cluster) -> historyRepository.get(date, timezone, projectKey));
    }

    public List<LocalDate> getAvailableHistory(String projectKey) {
        return historyRepository.getHistoryByProject(projectKey);
    }
    
    public FollowUpDataSnapshot get(Optional<LocalDate> date, ZoneId timezone, String projectKey) {
        return date.isPresent() 
                ? getFromHistory(date.get(), timezone, projectKey) 
                : getFromCurrentState(timezone, projectKey);
    }

    private FollowUpDataSnapshot createSnapshot(LocalDate date, String projectKey, FollowupDataSupplier dataSupplier) {
        ProjectFilterConfiguration project = projectRepository.getProjectByKeyOrCry(projectKey);
        FollowUpTimeline timeline = FollowUpTimeline.getTimeline(date, Optional.of(project));
        FollowupCluster cluster = clusterProvider.getFor(project);
        FollowupData data = dataSupplier.get(cluster);

        List<EffortHistoryRow> effortHistory = dailySynthesisRepository.listAllBefore(project.getId(), date).stream()
                .map(EffortHistoryRow::from)
                .collect(toList());

        return new FollowUpDataSnapshot(timeline, data, cluster, effortHistory);
    }
    
    public void generateHistory(ZoneId timezone) {
        log.info("History generation started...");
        for (ProjectFilterConfiguration pf : projectRepository.getProjects()) {
            String projectKey = pf.getProjectKey();

            log.info("History generation of project " + projectKey + " started...");
            storeSnapshot(timezone, projectKey);
            log.info("History generation of project " + projectKey + " completed.");
        }
        log.info("History generation completed.");
    }

    private void storeSnapshot(ZoneId timezone, String projectKey) {
        LocalDate date = LocalDateTime.ofInstant(clock.now(), timezone).toLocalDate();
        FollowupData data = dataGenerator.generate(timezone, projectKey);

        historyRepository.save(projectKey, date, data);
        syncSynthesis(projectKey, date, timezone, true);
    }

    public void syncSynthesis(ZoneId timezone) {
        log.info("Synthesis sync started...");

        for (ProjectFilterConfiguration pf : projectRepository.getProjects()) {
            String projectKey = pf.getProjectKey();

            log.info("Synthesis sync of project " + projectKey + " started...");
            getAvailableHistory(projectKey).forEach(date -> syncSynthesis(projectKey, date, timezone, false));
            log.info("Synthesis sync of project " + projectKey + " completed.");
        }

        log.info("Synthesis sync completed.");
    }

    private synchronized void syncSynthesis(String projectKey, LocalDate date, ZoneId timezone, boolean override) {
        ProjectFilterConfiguration project = projectRepository.getProjectByKeyOrCry(projectKey);

        if (dailySynthesisRepository.exists(project.getId(), date)) {
            if (override) {
                dailySynthesisRepository.remove(project.getId(), date);
            } else {
                return;
            }
        }

        FollowUpDataSnapshot snapshot = getFromHistory(date, timezone, projectKey);
        EffortHistoryRow effortHistoryRow = snapshot.getEffortHistoryRow();

        dailySynthesisRepository.add(new FollowupDailySynthesis(
                project.getId(), 
                date, 
                effortHistoryRow.sumEffortDone, 
                effortHistoryRow.sumEffortBacklog));
    }
    
    private interface FollowupDataSupplier {
        FollowupData get(FollowupCluster cluster);
    }
}
