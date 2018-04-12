package objective.taskboard.jira;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static objective.taskboard.config.CacheConfiguration.ALL_PROJECTS;
import static objective.taskboard.jira.AuthorizedJiraEndpointTest.JIRA_MASTER_PASSWORD;
import static objective.taskboard.jira.AuthorizedJiraEndpointTest.JIRA_MASTER_USERNAME;
import static objective.taskboard.repository.PermissionRepository.ADMINISTRATIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.atlassian.jira.rest.client.api.domain.CimProject;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.domain.Project;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.jira.data.Version;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.repository.ProjectFilterConfigurationRepository;
import objective.taskboard.testUtils.JiraMockServer;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        AuthorizedJiraEndpointTest.Configuration.class
        , ProjectServiceTest.Configuration.class})
public class ProjectServiceTest {

    private final static String PROJECT_ARCHIVED = "PROJ1";
    private final static String PROJECT_REGULAR_1 = "PROJ2";
    private final static String PROJECT_REGULAR_2 = "PROJ3";
    private final static String PROJECT_WITHOUT_ACCESS = "PROJ4";
    private final static String PROJECT_WITHOUT_METADATA = "PROJ5";
    private final static String PROJECT_INVALID = "PROJECTINVALID";

    public static class Configuration {
        @Bean
        public ProjectFilterConfigurationCachedRepository projectRepository() {
            return spy(new ProjectFilterConfigurationCachedRepository());
        }
        @Bean
        public JiraProjectService jiraProjectService() {
            return new JiraProjectService() {
                @Override
                public Map<String, Project> getUserProjects() {
                    return generateUserProjects();
                }
                @Override
                public Iterable<CimProject> getCreateIssueMetadata(String projectKey) {
                    if (PROJECT_WITHOUT_METADATA.equals(projectKey))
                        return asList();
                    return asList(new CimProject(null, null, null, null, null, null));
                }
            };
        }
        @Bean
        public ProjectService projectService() {
            return new ProjectService();
        }
    }

    @Autowired
    private JiraMockServer jira;

    @MockBean
    private JiraProperties jiraProperties;

    @MockBean
    private ProjectFilterConfigurationRepository projectFilterRepository;

    @MockBean
    private Authorizer authorizer;

    @Autowired
    private ProjectFilterConfigurationCachedRepository projectRepository;

    @Autowired
    private ProjectService subject;

    @Autowired
    private CacheManager cacheManager;

    @Before
    public void setupSecurity() {
        // master user
        JiraProperties.Lousa lousa = new JiraProperties.Lousa();
        lousa.setUsername(JIRA_MASTER_USERNAME);
        lousa.setPassword(JIRA_MASTER_PASSWORD);
        doReturn(lousa).when(jiraProperties).getLousa();
    }

    @Before
    public void setupProperties() {
        doReturn("http://localhost:" + jira.port()).when(jiraProperties).getUrl();
    }

    @Before
    public void mockRepository() {
        ProjectFilterConfiguration taskb = new ProjectFilterConfiguration("TASKB");
        ProjectFilterConfiguration proj1 = new ProjectFilterConfiguration(PROJECT_ARCHIVED);
        proj1.setArchived(true);
        ProjectFilterConfiguration proj2 = new ProjectFilterConfiguration(PROJECT_REGULAR_1);
        proj2.setArchived(false);
        ProjectFilterConfiguration proj3 = new ProjectFilterConfiguration(PROJECT_REGULAR_2);
        proj3.setArchived(false);
        ProjectFilterConfiguration proj4 = new ProjectFilterConfiguration(PROJECT_WITHOUT_ACCESS);
        proj4.setArchived(false);
        ProjectFilterConfiguration proj5 = new ProjectFilterConfiguration(PROJECT_WITHOUT_METADATA);
        proj5.setArchived(false);

        List<ProjectFilterConfiguration> projectList = asList(taskb, proj1, proj2, proj3, proj4, proj5);
        doReturn(projectList).when(projectFilterRepository).findAll();
        projectRepository.loadCache();
    }

    private static Map<String, Project> generateUserProjects() {
        Map<String, Project> projectsMap = new HashMap<String, Project>();

        Project p1 = new Project();
        p1.setKey(PROJECT_ARCHIVED);
        p1.setName(PROJECT_ARCHIVED);

        Project p2 = new Project();
        p2.setKey(PROJECT_REGULAR_1);
        p2.setName(PROJECT_REGULAR_1);

        Project p3 = new Project();
        p3.setKey(PROJECT_REGULAR_2);
        p3.setName(PROJECT_REGULAR_2);

        Project p4 = new Project();
        p4.setKey(PROJECT_WITHOUT_METADATA);
        p4.setName(PROJECT_WITHOUT_METADATA);

        projectsMap.put(p1.getKey(), p1);
        projectsMap.put(p2.getKey(), p2);
        projectsMap.put(p3.getKey(), p3);
        projectsMap.put(p4.getKey(), p4);

        return projectsMap;
    }

    @Before
    public void setupAuthorizer() {
        doReturn(asList(PROJECT_ARCHIVED, PROJECT_REGULAR_1, PROJECT_REGULAR_2, PROJECT_WITHOUT_METADATA)).when(authorizer).getAllowedProjectsForPermissions(any());
    }

    @Test
    public void getVersion() {
        Version expected = new Version("12550", "1.0");
        Version actual = subject.getVersion("12550");

        assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void cacheGetVersion() {
        Version version1 = subject.getVersion("12550");
        Version version2 = subject.getVersion("12550");

        assertTrue(version1 == version2);
    }

    @Test
    public void evict() {
        Version version1 = subject.getVersion("12550");
        cacheManager.getCache(ALL_PROJECTS).clear();
        Version version2 = subject.getVersion("12550");

        assertTrue(version1 != version2);
    }

    @Test
    public void getNonArchivedJiraProjectsForUser_dontShowArchivedProjects_dontShowProjectsThatUserHasNoAccess() {
        List<Project> visibleProjectsOnTaskboard = subject.getNonArchivedJiraProjectsForUser();
        assertTrue(visibleProjectsOnTaskboard.stream().anyMatch(p -> p.getKey().equals(PROJECT_REGULAR_1)));
        assertTrue(visibleProjectsOnTaskboard.stream().anyMatch(p -> p.getKey().equals(PROJECT_REGULAR_2)));
        assertFalse(visibleProjectsOnTaskboard.stream().anyMatch(p -> p.getKey().equals(PROJECT_WITHOUT_ACCESS)));
        assertFalse(visibleProjectsOnTaskboard.stream().anyMatch(p -> p.getKey().equals(PROJECT_ARCHIVED)));
    }

    @Test
    public void getTaskboardProjects_withoutParams_showAllTaskboardProjects_sortedByProjectKey() {
        List<ProjectFilterConfiguration> projects = subject.getTaskboardProjects();
        assertTrue(projectRepository.getProjects().size() == projects.size());
        assertIsSortedByProjectkey(projects);
    }

    @Test
    public void getTaskboardProjects_withPermissionsParam_showOnlyProjectsThatUserHasPermission_sortedByProjectKey() {
        List<ProjectFilterConfiguration> projects = subject.getTaskboardProjects(ADMINISTRATIVE);
        assertTrue(projectRepository.getProjects().size() != projects.size() && projects.size() == 4);
        assertTrue(projects.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_REGULAR_1)));
        assertTrue(projects.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_REGULAR_2)));
        assertTrue(projects.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_ARCHIVED)));
        assertFalse(projects.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_WITHOUT_ACCESS)));
        assertIsSortedByProjectkey(projects);
    }

    @Test
    public void getTaskboardProjects_withPermissionsAndFilterParams_showOnlyProjectsThatUserHasPermission_respectingTheFilter_sortedByProjectKey() {
        List<ProjectFilterConfiguration> projectsNonArchivedAdmin = subject.getTaskboardProjects(subject::isNonArchivedAndUserHasAccess, ADMINISTRATIVE);
        assertTrue(projectRepository.getProjects().size() != projectsNonArchivedAdmin.size() && projectsNonArchivedAdmin.size() == 3);
        assertTrue(projectsNonArchivedAdmin.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_REGULAR_1)));
        assertTrue(projectsNonArchivedAdmin.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_REGULAR_2)));
        assertFalse(projectsNonArchivedAdmin.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_ARCHIVED)));
        assertFalse(projectsNonArchivedAdmin.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_WITHOUT_ACCESS)));
        assertIsSortedByProjectkey(projectsNonArchivedAdmin);

        List<ProjectFilterConfiguration> projectsNonArchived = subject.getTaskboardProjects(subject::isNonArchivedAndUserHasAccess);
        assertTrue(projectRepository.getProjects().size() != projectsNonArchived.size() && projectsNonArchived.size() == 3);
        assertTrue(projectsNonArchived.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_REGULAR_1)));
        assertTrue(projectsNonArchived.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_REGULAR_2)));
        assertFalse(projectsNonArchived.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_ARCHIVED)));
        assertFalse(projectsNonArchived.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_WITHOUT_ACCESS)));
        assertIsSortedByProjectkey(projectsNonArchived);

        List<ProjectFilterConfiguration> projects = subject.getTaskboardProjects(projectKey -> projectKey.equals(PROJECT_REGULAR_1), ADMINISTRATIVE);
        assertTrue(projectRepository.getProjects().size() != projects.size() && projects.size() == 1);
        assertTrue(projects.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_REGULAR_1)));
        assertFalse(projects.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_REGULAR_2)));
        assertFalse(projects.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_ARCHIVED)));
        assertFalse(projects.stream().anyMatch(p -> p.getProjectKey().equals(PROJECT_WITHOUT_ACCESS)));
        assertIsSortedByProjectkey(projects);
    }

    @Test
    public void getProjectMetadata_ifProjectIsNonArchivedAndUserHasAccessAndProjectHasMetadata_returnTheValue() {
        Optional<CimProject> metadataForRegular = subject.getProjectMetadata(PROJECT_REGULAR_1);
        assertTrue(metadataForRegular.isPresent());

        Optional<CimProject> metadataForWithoutAccess = subject.getProjectMetadata(PROJECT_WITHOUT_ACCESS);
        assertFalse(metadataForWithoutAccess.isPresent());

        Optional<CimProject> metadataForArchived = subject.getProjectMetadata(PROJECT_ARCHIVED);
        assertFalse(metadataForArchived.isPresent());

        Optional<CimProject> metadataForProjectWithoutMetadata = subject.getProjectMetadata(PROJECT_WITHOUT_METADATA);
        assertFalse(metadataForProjectWithoutMetadata.isPresent());
    }

    @Test
    public void isNonArchivedAndUserHasAccess_ifProjectIsArchivedOrUserHasNoAccess_returnFalse() {
        assertTrue(subject.isNonArchivedAndUserHasAccess(PROJECT_REGULAR_1));
        assertTrue(subject.isNonArchivedAndUserHasAccess(PROJECT_REGULAR_2));
        assertFalse(subject.isNonArchivedAndUserHasAccess(PROJECT_ARCHIVED));
        assertFalse(subject.isNonArchivedAndUserHasAccess(PROJECT_WITHOUT_ACCESS));
    }

    @Test
    public void jiraProjectExistsAndUserHasAccess_ifUserHasNoAccess_returnFalse_ifProjectIsArchived_returnTrue() {
        assertTrue(subject.jiraProjectExistsAndUserHasAccess(PROJECT_REGULAR_1));
        assertTrue(subject.jiraProjectExistsAndUserHasAccess(PROJECT_REGULAR_2));
        assertTrue(subject.jiraProjectExistsAndUserHasAccess(PROJECT_ARCHIVED));
        assertFalse(subject.jiraProjectExistsAndUserHasAccess(PROJECT_WITHOUT_ACCESS));
        assertFalse(subject.jiraProjectExistsAndUserHasAccess(PROJECT_INVALID));
    }

    @Test
    public void taskboardProjectExists_ifProjectExists_returnTrue() {
        assertTrue(subject.taskboardProjectExists(PROJECT_REGULAR_1));
        assertTrue(subject.taskboardProjectExists(PROJECT_REGULAR_2));
        assertTrue(subject.taskboardProjectExists(PROJECT_ARCHIVED));
        assertTrue(subject.taskboardProjectExists(PROJECT_WITHOUT_ACCESS));
        assertFalse(subject.taskboardProjectExists(PROJECT_INVALID));
    }

    @Test
    public void getTaskboardProject_withoutPermissionsParam_ifProjectExists_returnValue() {
        Optional<ProjectFilterConfiguration> projectRegular1 = subject.getTaskboardProject(PROJECT_REGULAR_1);
        assertTrue(projectRegular1.isPresent());

        Optional<ProjectFilterConfiguration> projectRegular2 = subject.getTaskboardProject(PROJECT_REGULAR_2);
        assertTrue(projectRegular2.isPresent());

        Optional<ProjectFilterConfiguration> projectArchived = subject.getTaskboardProject(PROJECT_ARCHIVED);
        assertTrue(projectArchived.isPresent());

        Optional<ProjectFilterConfiguration> projectWithoutAccess = subject.getTaskboardProject(PROJECT_WITHOUT_ACCESS);
        assertTrue(projectWithoutAccess.isPresent());

        Optional<ProjectFilterConfiguration> projectInvalid = subject.getTaskboardProject(PROJECT_INVALID);
        assertFalse(projectInvalid.isPresent());
    }

    @Test
    public void getTaskboardProject_withPermissionsParam_ifProjectExistsAndUserHasPermission_returnValue() {
        Optional<ProjectFilterConfiguration> projectRegular1 = subject.getTaskboardProject(PROJECT_REGULAR_1, ADMINISTRATIVE);
        assertTrue(projectRegular1.isPresent());

        Optional<ProjectFilterConfiguration> projectRegular2 = subject.getTaskboardProject(PROJECT_REGULAR_2, ADMINISTRATIVE);
        assertTrue(projectRegular2.isPresent());

        Optional<ProjectFilterConfiguration> projectArchived = subject.getTaskboardProject(PROJECT_ARCHIVED, ADMINISTRATIVE);
        assertTrue(projectArchived.isPresent());

        Optional<ProjectFilterConfiguration> projectWithoutAccess = subject.getTaskboardProject(PROJECT_WITHOUT_ACCESS, ADMINISTRATIVE);
        assertFalse(projectWithoutAccess.isPresent());

        Optional<ProjectFilterConfiguration> projectInvalid = subject.getTaskboardProject(PROJECT_INVALID, ADMINISTRATIVE);
        assertFalse(projectInvalid.isPresent());
    }

    @Test
    public void saveTaskboardProject() {
        ProjectFilterConfiguration newProject = new ProjectFilterConfiguration(PROJECT_INVALID);
        subject.saveTaskboardProject(newProject);

        verify(projectRepository, atLeast(1)).save(newProject);
    }

    private void assertIsSortedByProjectkey(List<ProjectFilterConfiguration> projects) {
        List<ProjectFilterConfiguration> sortedProjects = projects.stream()
                .sorted((p1, p2) -> p1.getProjectKey().compareTo(p2.getProjectKey()))
                .collect(toList());

        for (int i = 0; i < projects.size(); i++)
            assertThat(projects.get(i)).isEqualToComparingFieldByField(sortedProjects.get(i));
    }
}