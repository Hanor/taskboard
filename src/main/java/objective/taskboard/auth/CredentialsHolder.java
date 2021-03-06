package objective.taskboard.auth;

/*-
 * [LICENSE]
 * Taskboard
 * - - -
 * Copyright (C) 2015 - 2016 Objective Solutions
 * - - -
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * [/LICENSE]
 */

import org.apache.commons.lang.Validate;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class CredentialsHolder {

    public static String username() {
        validateAuthentication();
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public static String password() {
        validateAuthentication();
        return (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
    }

    private static void validateAuthentication() {
        Validate.isTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated(), "Not authenticated");
    }
}
