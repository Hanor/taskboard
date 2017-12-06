/*-
 * [LICENSE]
 * Taskboard
 * ---
 * Copyright (C) 2015 - 2016 Objective Solutions
 * ---
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

function SearchFilter() {
    this.match = function(issue, search) {
        if (!issue || !search)
            return true;
        
        var matches = false;
        if (typeof search.query === 'object') {
            for(var index=0; index < search.query.length; index++) {
                matches = matchByString(issue, search.query[index]);
                if (matches) break;
            }
        }
        else
            matches = matchByString(issue, search.query);

        return matches && filterByRelease(issue, search.release);
    };

    var matchByString = function(issue, searchString) {
        if (!searchString)
            return true;

        var searchUpperCase = searchString.toUpperCase();
        if (matchByAttribute(issue.issueKey, searchUpperCase))
            return true;
        else if (matchByAttribute(issue.assignee, searchUpperCase))
            return true;
        else if (matchByAttribute(issue.subResponsaveis, searchUpperCase))
            return true;
        else if (matchByAttribute(issue.summary, searchUpperCase))
            return true;
        else if (taskboard.getRelease(issue) && matchByAttribute(taskboard.getRelease(issue).value, searchUpperCase))
            return true;
        else if (matchByAttribute(issue.usersTeam, searchUpperCase))
            return true;
        else if (matchByAttribute(issue.labels, searchUpperCase))
            return true;
        else if (matchByAttribute(issue.components, searchUpperCase))
            return true;

        return false;
    };

    var matchByAttribute = function(attribute, search) {
        if (!Array.isArray(attribute))
            attribute = [ attribute ];

        for (var index in attribute) {
            var value = attribute[index];
            if (value && value.toUpperCase().indexOf(search) !== -1)
                return true;
        }
        return false;
    };

    var filterByRelease = function(issue, releaseId) {
        if (!releaseId)
            return true;

        return issue.releaseId && issue.releaseId == releaseId;
    };
}

var searchFilter = new SearchFilter();