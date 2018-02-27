package objective.taskboard.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cache.CacheManager;

import objective.taskboard.data.Team;
import objective.taskboard.domain.ProjectTeam;
import objective.taskboard.followup.FollowUpFacade;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.repository.ProjectTeamRepository;
import objective.taskboard.repository.TeamCachedRepository;
import objective.taskboard.repository.TeamFilterConfigurationCachedRepository;
import objective.taskboard.testUtils.AbstractJpaRepositoryMock;

@RunWith(MockitoJUnitRunner.class)
public class ProjectControllerTest {
    @Mock
    private ProjectFilterConfigurationCachedRepository projectRepository;

    @Spy
    private ProjectRepoMock projectTeamRepo = new ProjectRepoMock();;

    @Mock
    private TeamCachedRepository teamRepository;

    @Mock
    private TeamFilterConfigurationCachedRepository teamFilterConfigurationRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private FollowUpFacade followUpFacade;

    @Mock
    private CacheManager cacheManager;
    
    @InjectMocks
    private ProjectController subject;
    
    @Test
    public void updateProjectsTeams_ShouldAddNewTeamsAndRemovesTeamsNotPresentInRequest() {
        ProjectTeam e1 = new ProjectTeam();
        e1.setProjectKey("TOOLS");
        e1.setTeamId(new Long(12));
        projectTeamRepo.data.add(e1);
        
        Team teamT1 = new Team();
        teamT1.setId(new Long(12));
        when(teamRepository.findByName("DEVOPS TEAM")).thenReturn(teamT1);
        
        Team teamT2 = new Team();
        teamT2.setId(new Long(12));
        when(teamRepository.findByName("OTHER TEAM")).thenReturn(teamT2);
        
        ProjectData pdata1 = new ProjectData();
        pdata1.projectKey = "DEVOPS";
        pdata1.teams = new HashSet<>(Arrays.asList("DEVOPS TEAM"));
        
        ProjectData pdata2 = new ProjectData();
        pdata2.projectKey = "TOOLS";
        pdata2.teams = new HashSet<>(Arrays.asList("DEVOPS TEAM"));
        ProjectData[] projectsTeams = new ProjectData[]{pdata1,pdata2};
        
        subject.updateProjectsTeams(projectsTeams);
        
        List<ProjectTeam> actualProj1 = projectTeamRepo.findByIdProjectKey("DEVOPS");
        assertEquals(1, actualProj1.size());
        assertEquals((Long)12L, actualProj1.get(0).getTeamId()); 
        
        List<ProjectTeam> actualProj2 = projectTeamRepo.findByIdProjectKey("TOOLS");
        assertEquals((Long)12L, actualProj2.get(0).getTeamId());
    }
    
    static class ProjectRepoMock extends AbstractJpaRepositoryMock<ProjectTeam, String> implements ProjectTeamRepository{
        public final List<ProjectTeam> data = new LinkedList<>();

        @SuppressWarnings("unchecked")
        @Override
        public ProjectTeam save(ProjectTeam entity) {
            data.add(entity);
            return entity;
        }
        
        @Override
        public void delete(ProjectTeam entity) {
            Iterator<ProjectTeam> it = data.iterator();
            while (it.hasNext()) {
                ProjectTeam next = it.next();
                if (next.getProjectKey().equals(entity.getProjectKey()) && next.getTeamId().equals(entity.getTeamId())) {
                    it.remove();
                }
            }
        }

        @Override
        public List<ProjectTeam> findByIdProjectKey(String projectKey) {
            List<ProjectTeam> r = new LinkedList<>();
            Iterator<ProjectTeam> it = data.iterator();
            while (it.hasNext()) {
                ProjectTeam next = it.next();
                if (next.getProjectKey().equals(projectKey)) 
                    r.add(next);
            }
            return r;
        }

        @Override
        public List<ProjectTeam> findByIdTeamId(Long teamId) {
            List<ProjectTeam> r = new LinkedList<>();
            Iterator<ProjectTeam> it = data.iterator();
            while (it.hasNext()) {
                ProjectTeam next = it.next();
                if (next.getTeamId().equals(teamId)) 
                    r.add(next);
            }
            return r;
        }
    }
}