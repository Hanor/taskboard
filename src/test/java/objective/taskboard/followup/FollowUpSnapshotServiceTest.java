package objective.taskboard.followup;

import static java.util.Arrays.asList;
import static objective.taskboard.followup.FollowUpHelper.getDefaultFollowupData;
import static objective.taskboard.followup.FollowUpHelper.getEmptyFollowupData;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import objective.taskboard.domain.FollowupDailySynthesis;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.followup.cluster.EmptyFollowupCluster;
import objective.taskboard.followup.cluster.FollowupClusterProvider;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.testUtils.FixedClock;

public class FollowUpSnapshotServiceTest {

    private FixedClock clock = new FixedClock();
    private FollowUpDataRepositoryMock historyRepository = new FollowUpDataRepositoryMock();
    private ProjectFilterConfigurationCachedRepository projectRepository = mock(ProjectFilterConfigurationCachedRepository.class);
    private FollowupDailySynthesisRepositoryMock dailySynthesisRepository = new FollowupDailySynthesisRepositoryMock();
    private FollowUpDataGenerator dataGenerator = mock(FollowUpDataGenerator.class);
    private FollowupClusterProvider clusterProvider = mock(FollowupClusterProvider.class);
    private ReleaseHistoryProvider releaseHistoryProvider = mock(ReleaseHistoryProvider.class);
    
    private ZoneId timezone = ZoneId.of("UTC");
    private ProjectFilterConfiguration project1 = mock(ProjectFilterConfiguration.class);
    private ProjectFilterConfiguration project2 = mock(ProjectFilterConfiguration.class);
    
    private FollowUpSnapshotService subject = new FollowUpSnapshotService(
            clock, 
            historyRepository, 
            projectRepository, 
            dailySynthesisRepository, 
            dataGenerator, 
            clusterProvider, 
            releaseHistoryProvider);

    @Before
    public void setup() {
        when(project1.getProjectKey()).thenReturn("PROJ1");
        when(project1.getId()).thenReturn(91);
        when(project1.getBaselineDate()).thenReturn(Optional.empty());
        
        when(project2.getProjectKey()).thenReturn("PROJ2");
        when(project2.getId()).thenReturn(92);
        when(project2.getBaselineDate()).thenReturn(Optional.empty());
        
        when(projectRepository.getProjects()).thenReturn(asList(project1, project2));
        when(projectRepository.getProjectByKeyOrCry(eq("PROJ1"))).thenReturn(project1);
        when(projectRepository.getProjectByKeyOrCry(eq("PROJ2"))).thenReturn(project2);
        
        when(dataGenerator.generate(any(), any())).thenReturn(getEmptyFollowupData());
        
        when(clusterProvider.getForProject(any())).thenReturn(new EmptyFollowupCluster());
    }
    
    @Test
    public void shouldGenerateHistory() {
        clock.setNow("2018-04-10T12:00:00.00Z");
        subject.storeSnapshots(timezone);
        
        clock.setNow("2018-04-11T12:00:00.00Z");
        subject.storeSnapshots(timezone);
        
        historyRepository.assertValues(
                "PROJ1",
                "  2018-04-10 (rows: 0)",
                "  2018-04-11 (rows: 0)",
                "PROJ2",
                "  2018-04-10 (rows: 0)",
                "  2018-04-11 (rows: 0)");
        
        dailySynthesisRepository.assertValues(
                "2018-04-10 | 91 | 0.0 | 0.0",
                "2018-04-10 | 92 | 0.0 | 0.0",
                "2018-04-11 | 91 | 0.0 | 0.0",
                "2018-04-11 | 92 | 0.0 | 0.0");
    }
    
    @Test
    public void generateHistoryShouldOverrideExistingValues() {
        historyRepository.save("PROJ1", LocalDate.parse("2018-04-10"), getDefaultFollowupData());
        dailySynthesisRepository.add(new FollowupDailySynthesis(91, LocalDate.parse("2018-04-10"), 9.0, 2.0));

        when(dataGenerator.generate(any(), any())).thenReturn(getEmptyFollowupData());
        when(projectRepository.getProjects()).thenReturn(asList(project1));
        
        clock.setNow("2018-04-10T12:00:00.00Z");
        subject.storeSnapshots(timezone);
        
        historyRepository.assertValues(
                "PROJ1",
                "  2018-04-10 (rows: 0)");
        
        dailySynthesisRepository.assertValues(
                "2018-04-10 | 91 | 0.0 | 0.0");
    }
    
    @Test
    public void shouldSyncSynthesis() {
        historyRepository.save("PROJ1", LocalDate.parse("2018-04-10"), getEmptyFollowupData());
        historyRepository.save("PROJ1", LocalDate.parse("2018-04-11"), getEmptyFollowupData());
        historyRepository.save("PROJ2", LocalDate.parse("2018-04-11"), getEmptyFollowupData());
        
        subject.syncSynthesis(timezone);
        
        dailySynthesisRepository.assertValues(
                "2018-04-10 | 91 | 0.0 | 0.0",
                "2018-04-11 | 91 | 0.0 | 0.0",
                "2018-04-11 | 92 | 0.0 | 0.0");
    }
    
    @Test
    public void syncSynthesisShouldNotOverrideExistingValues() {
        historyRepository.save("PROJ1", LocalDate.parse("2018-04-10"), getEmptyFollowupData());
        dailySynthesisRepository.add(new FollowupDailySynthesis(91, LocalDate.parse("2018-04-10"), 9.0, 2.0));
        
        subject.syncSynthesis(timezone);
        
        dailySynthesisRepository.assertValues(
                "2018-04-10 | 91 | 9.0 | 2.0");
    }

}
