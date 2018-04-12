package objective.taskboard.auth;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.auth.LoggedUserDetails.Role;
import objective.taskboard.repository.PermissionRepository;

@Service
public class Authorizer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Authorizer.class);

    private LoggedUserDetails userDetails;

    private PermissionRepository permissionRepository;

    @Autowired
    public Authorizer(LoggedUserDetails userDetails, PermissionRepository permissionRepository) {
        this.userDetails = userDetails;
        this.permissionRepository = permissionRepository;
    }

    public boolean hasPermissionInAnyProject(String permission) {
        log.debug("Authorize permission {}", permission);
        return isPermissionAllowed(permissionRepository.findByName(permission), userDetails.getUserRoles());
    }

    public boolean hasPermissionInProject(String permission, String projectKey) {
        log.debug("Authorize permission {} for project {}", permission, projectKey);
        List<Role> userRoles = filterUserRolesWithProject(projectKey, userDetails.getUserRoles());
        return isPermissionAllowed(permissionRepository.findByName(permission), userRoles);
    }

    public List<ProjectPermissionsData> getProjectsPermission() {
        List<Role> userRoles = userDetails.getUserRoles();
        Set<String> projectKeys = userRoles.stream().map(role -> role.projectKey).collect(toSet());

        List<ProjectPermissionsData> projectsPermissionData = new ArrayList<>();
        for (String projectKey: projectKeys) {
            List<String> projectPermissions = permissionRepository.findAll().stream()
                .filter(p -> isPermissionAllowed(p, filterUserRolesWithProject(projectKey, userRoles)))
                .map(PermissionRoles::getName)
                .collect(toList());

            projectsPermissionData.add(new ProjectPermissionsData(projectKey, projectPermissions));
        }
        return projectsPermissionData;
    }

    public List<String> getAllowedProjectsForPermissions(String... permissions) {
        return getProjectsPermission().stream()
                .filter(projectPermissions -> projectPermissions.containsAny(Arrays.asList(permissions)))
                .map(projectPermissions -> projectPermissions.projectKey)
                .collect(toList());
    }

    public boolean hasAnyRoleInProjects(List<String> roles, List<String> projectKeys) {
        return userDetails.getUserRoles().stream().anyMatch(role -> roles.contains(role.name) && projectKeys.contains(role.projectKey));
    }

    public List<String> getRolesForProject(String projectKey) {
        return userDetails.getUserRoles().stream()
                .filter(role -> role.projectKey.equals(projectKey))
                .map(role -> role.name)
                .collect(toList());
    }

    private boolean isPermissionAllowed(PermissionRoles permission, List<Role> userRoles) {
        return userRoles.stream().anyMatch(role -> permission.accepts(role.name));
    }

    private List<Role> filterUserRolesWithProject(String projectKey, List<Role> userRoles) {
        List<Role> userRolesOfProject = userRoles.stream().filter(role -> role.projectKey.equals(projectKey)).collect(toList());
        return userRolesOfProject;
    }

    public static class ProjectPermissionsData {
        public final String projectKey;
        public final List<String> permissions;

        public ProjectPermissionsData(String projectKey, List<String> permissions) {
            this.projectKey = projectKey;
            this.permissions = Collections.unmodifiableList(permissions);
        }

        public boolean containsAny(List<String> permissions) {
            return this.permissions.stream().anyMatch(permissions::contains);
        }
    }
}
