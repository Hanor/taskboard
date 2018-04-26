package objective.taskboard.auth;

import java.util.List;

public interface LoggedUserDetails {

    List<Role> getUserRoles();

    public String getUsername();

    public static class Role {
        public Long id;
        public String name;
        public String projectKey;

        public Role() {}

        public Role(Long id, String name, String projectKey) {
            this.id = id;
            this.name = name;
            this.projectKey = projectKey;
        }
    }
}
