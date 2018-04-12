package objective.taskboard.auth;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import objective.taskboard.auth.Authorizer.ProjectPermissionsData;
import objective.taskboard.repository.PermissionRepository;

public class AuthorizerTest {

    private static final String PERMISSION_A = "permission.a";
    private static final String PERMISSION_B = "permission.b";
    private static final String PERMISSION_C = "permission.c";
    private static final String PERMISSION_D = "permission.d";

    Authorizer subject;

    LoggedUserDetails userDetails = mock(LoggedUserDetails.class);

    PermissionRepository permissionRepository = mock(PermissionRepository.class);

    @Before
    public void setup() {
        subject = new Authorizer(userDetails, permissionRepository);
    }

    @Test
    public void givenUserRoles_whenVerifyPermissionThatContainsAny_thenShouldReturnTrue() {
        when(userDetails.getUserRoles()).thenReturn(userRoles());
        when(permissionRepository.findByName(PERMISSION_A)).thenReturn(permissionA());
        assertTrue(subject.hasPermissionInAnyProject(PERMISSION_A));
    }

    @Test
    public void givenUserRoles_whenVerifyPermissionThatNotContainsAny_thenShouldReturnFalse() {
        when(userDetails.getUserRoles()).thenReturn(userRoles());
        when(permissionRepository.findByName(PERMISSION_D)).thenReturn(permissionD());
        assertTrue(!subject.hasPermissionInAnyProject(PERMISSION_D));
    }

    @Test
    public void givenUserHasNoRoles_whenVerifyPermission_thenShouldReturnFalse() {
        when(userDetails.getUserRoles()).thenReturn(Collections.emptyList());
        when(permissionRepository.findByName(PERMISSION_A)).thenReturn(permissionA());

        assertTrue(!subject.hasPermissionInAnyProject(PERMISSION_A));
    }

    @Test
    public void givenUserRoles_whenVerifyPermissionInProjectThatContainsAny_thenShouldReturnTrue() {
        when(userDetails.getUserRoles()).thenReturn(userRoles());
        when(permissionRepository.findByName(PERMISSION_B)).thenReturn(permissionB());

        assertTrue(subject.hasPermissionInProject(PERMISSION_B, "PROJ3"));
    }

    @Test
    public void givenUserRoles_whenVerifyPermissionInProjectThatNotContainsAny_thenShouldReturnFalse() {
        when(userDetails.getUserRoles()).thenReturn(userRoles());
        when(permissionRepository.findByName(PERMISSION_A)).thenReturn(permissionA());

        assertTrue(!subject.hasPermissionInProject(PERMISSION_A, "PROJ3"));
    }

    @Test
    public void givenUserHasNoRoles_whenVerifyPermissionInProject_thenShouldReturnFalse() {
        when(userDetails.getUserRoles()).thenReturn(Collections.emptyList());
        when(permissionRepository.findByName(PERMISSION_A)).thenReturn(permissionA());

        assertTrue(!subject.hasPermissionInProject(PERMISSION_A, "PROJ3"));
    }

    @Test
    public void givenSomePermissions_whenVerifyProjectsPermission_thenShouldReturnProjectsWithAllowedPermissions() {
        when(userDetails.getUserRoles()).thenReturn(userRoles());
        when(permissionRepository.findAll()).thenReturn(permissions());

        List<ProjectPermissionsData> projectsPermission = subject.getProjectsPermission();
        assertThat(projectsPermission).hasSize(3)
            .extracting(p -> p.projectKey).containsExactlyInAnyOrder("PROJ1", "PROJ2", "PROJ3");

        assertThatProjectHasPermissions(projectsPermission, "PROJ1", PERMISSION_A, PERMISSION_C);
        assertThatProjectHasPermissions(projectsPermission, "PROJ2", PERMISSION_A, PERMISSION_C);
        assertThatProjectHasPermissions(projectsPermission, "PROJ3", PERMISSION_B, PERMISSION_C);
    }

    @Test
    public void givenUserHasPermissionsInSomeProjects_whenGetAllowedProjectForAPermission_thenShouldReturnTheProjectThatHasPermission() {
        when(userDetails.getUserRoles()).thenReturn(userRoles());
        when(permissionRepository.findAll()).thenReturn(permissions());

        List<String> allowedProjects = subject.getAllowedProjectsForPermissions(PERMISSION_B);
        assertThat(allowedProjects).containsExactly("PROJ3");

        allowedProjects = subject.getAllowedProjectsForPermissions(PERMISSION_A, PERMISSION_B);
        assertThat(allowedProjects).containsExactlyInAnyOrder("PROJ1", "PROJ2", "PROJ3");

        allowedProjects = subject.getAllowedProjectsForPermissions(PERMISSION_D);
        assertThat(allowedProjects).isEmpty();
    }

    @Test
    public void givenUserRoles_whenVerifyIfHasAnyRoleInProjects_thenShouldReturnCorrectly() {
        when(userDetails.getUserRoles()).thenReturn(userRoles());

        assertTrue(subject.hasAnyRoleInProjects(asList("Administrators", "Developers"), asList("PROJ1")));
        assertTrue(subject.hasAnyRoleInProjects(asList("Administrators", "Developers"), asList("PROJ3")));
        assertFalse(subject.hasAnyRoleInProjects(asList("Developers", "KPI"), asList("PROJ1")));
        assertFalse(subject.hasAnyRoleInProjects(asList("Administrators", "KPI"), asList("PROJ3")));
        assertFalse(subject.hasAnyRoleInProjects(asList("KPI"), asList("PROJ1", "PROJ2", "PROJ3")));
    }

    private static List<LoggedUserDetails.Role> userRoles() {
        return asList(
                new LoggedUserDetails.Role(1L, "Administrators", "PROJ1"),
                new LoggedUserDetails.Role(1L, "Administrators", "PROJ2"),
                new LoggedUserDetails.Role(1L, "Developers", "PROJ3"));
    }

    private List<PermissionRoles> permissions() {
        return asList(
                permissionA(),
                permissionB(),
                permissionC(),
                permissionD());
    }

    private PermissionRoles permissionD() {
        return new PermissionRoles(PERMISSION_D, "Reviewer");
    }

    private PermissionRoles permissionC() {
        return new PermissionRoles(PERMISSION_C, "Administrators", "Developers");
    }

    private PermissionRoles permissionB() {
        return new PermissionRoles(PERMISSION_B, "Developers");
    }

    private PermissionRoles permissionA() {
        return new PermissionRoles(PERMISSION_A, "Administrators");
    }

    private void assertThatProjectHasPermissions(List<ProjectPermissionsData> projectsPermission, String projectKey, String... permissions) {
        ProjectPermissionsData projectOnePermissions = projectsPermission.stream().filter(p -> p.projectKey.equals(projectKey)).findFirst().get();
        assertThat(projectOnePermissions.permissions).containsExactlyInAnyOrder(permissions);
    }

}
