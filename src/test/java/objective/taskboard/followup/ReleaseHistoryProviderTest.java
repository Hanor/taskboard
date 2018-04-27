package objective.taskboard.followup;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import objective.taskboard.followup.ReleaseHistoryProvider.ProjectRelease;
import objective.taskboard.jira.data.ProjectVersion;
import objective.taskboard.jira.endpoint.JiraEndpointAsMaster;

public class ReleaseHistoryProviderTest {
    
    private JiraEndpointAsMaster jiraEndpoint = mock(JiraEndpointAsMaster.class);
    private ProjectVersion.Service projectVersionService = mock(ProjectVersion.Service.class);
    private ReleaseHistoryProvider subject = new ReleaseHistoryProvider(jiraEndpoint);

    @Before
    public void setup() {
        when(jiraEndpoint.request(ProjectVersion.Service.class)).thenReturn(projectVersionService);
    }
    
    @Test
    public void getShouldReturnValues() {
        when(projectVersionService.list("PX")).thenReturn(asList(
                projectVersion("Release 1", LocalDate.parse("2018-04-01")),
                projectVersion("Release 2", LocalDate.parse("2018-04-15"))));
        
        assertReleases(subject.get("PX"), 
                "Release 1 | 2018-04-01",
                "Release 2 | 2018-04-15");
    }
    
    @Test
    public void getShouldSortByDate() {
        when(projectVersionService.list("PX")).thenReturn(asList(
                projectVersion("Release 2", LocalDate.parse("2018-04-15")),
                projectVersion("Release 1", LocalDate.parse("2018-04-01"))));
        
        assertReleases(subject.get("PX"), 
                "Release 1 | 2018-04-01",
                "Release 2 | 2018-04-15");
    }
    
    @Test
    public void getShouldIgnoreArchivedVersions() {
        when(projectVersionService.list("PX")).thenReturn(asList(
                projectVersion("Release 1", LocalDate.parse("2018-04-01")),
                archivedProjectVersion("Release 2", LocalDate.parse("2018-04-15")),
                projectVersion("Release 3", LocalDate.parse("2018-04-29"))));
        
        assertReleases(subject.get("PX"), 
                "Release 1 | 2018-04-01",
                "Release 3 | 2018-04-29");
    }
    
    @Test
    public void getShouldIgnoreVersionsWithoutReleaseDate() {
        when(projectVersionService.list("PX")).thenReturn(asList(
                projectVersion("Release 1", LocalDate.parse("2018-04-01")),
                projectVersion("Release 2", null),
                projectVersion("Release 3", LocalDate.parse("2018-04-29"))));
        
        assertReleases(subject.get("PX"), 
                "Release 1 | 2018-04-01",
                "Release 3 | 2018-04-29");
    }
    
    private static void assertReleases(List<ProjectRelease> actual, String... expected) {
        assertEquals(StringUtils.join(expected, "\n"), actual.stream().map(r -> r.getName() + " | " + r.getDate()).collect(joining("\n")));
    }

    private static ProjectVersion projectVersion(String name, LocalDate releaseDate) {
        ProjectVersion result = new ProjectVersion();
        result.name = name;
        result.releaseDate = releaseDate;
        result.archived = false;
        result.released = false;
        
        return result;
    }
    
    private static ProjectVersion archivedProjectVersion(String name, LocalDate releaseDate) {
        ProjectVersion result = projectVersion(name, releaseDate);
        result.archived = true;
        return result;
    }
}
