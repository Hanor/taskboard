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
package objective.taskboard.issue;

import java.io.Serializable;

import objective.taskboard.data.Issue;

public class IssueUpdate implements Serializable {
    private static final long serialVersionUID = -4585168887857725892L;
    
    public final Long issueId;
    public final int stateHash;
    public final IssueUpdateType updateType;
    public IssueUpdate(Issue target, IssueUpdateType updateType) {
        if (target != null) {
            this.issueId = target.getId();
            this.stateHash = target.getStateHash();
        }
        else {
            this.issueId = -1L;
            this.stateHash = -1;
        }
        this.updateType = updateType;
    }
}
