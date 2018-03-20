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

    var searchData = {
        query: undefined
        , release: undefined
        , updatedIssues: undefined
        , hierarchy: undefined
        , dependencies: undefined
    };
    var rootHierarchicalFilter;

    var hasAnyFilter = function() {
        if(searchData.query || searchData.release)
            return true;
        if(searchData.updatedIssues && searchData.updatedIssues.length)
            return true;
        if(searchData.hierarchy && searchData.hierarchy.length)
            return true;
        if(searchData.dependencies && searchData.dependencies.length)
            return true;
    }

    function matchByString(issue, searchString) {//NOSONAR
        if (!searchString)
            return true;

        var searchUpperCase = searchString.toUpperCase();
        if (searchString.indexOf("has:") === 0) {
            var term = searchString.split(":")[1];

            switch(term) {
                case SearchFilter.INVALID_ASSIGNEE_TAG:
                    return issue.mismatchingUsers.length > 0;
            }
            return false;
        }
        if (matchByAttribute(issue.issueKey, searchUpperCase))
            return true;
        else if (matchByAttribute(issue.assignee.name, searchUpperCase))
            return true;
        else if (matchByAttribute(issue.coAssignees.map(function(s){return s.name}), searchUpperCase))
            return true;
        else if (matchByAttribute(issue.summary, searchUpperCase))
            return true;
        else if (issue.release && matchByAttribute(issue.release.name, searchUpperCase))
            return true;
        else if (matchByAttribute(issue.teamNames, searchUpperCase))
            return true;
        else if (matchByAttribute(issue.labels, searchUpperCase))
            return true;
        else if (matchByAttribute(issue.components, searchUpperCase))
            return true;

        return false;
    }

    function matchByAttribute(attribute, search) {
        if (!Array.isArray(attribute))
            attribute = [ attribute ];

        for (var index in attribute) {
            var value = attribute[index];
            
            if (value && value.toUpperCase().indexOf(search) !== -1)
                return true;
        }
        return false;
    }

    function matchByRelease(issue, releaseId) {
        if (!releaseId)
            return true;

        return issue.releaseId && issue.releaseId == releaseId;
    }

    function matchByKey(issue) {
        var allEmpty = true;
        // may receive an arbitrary number of arguments
        for (var i = 1; i < arguments.length; ++i) {
            var keys = arguments[i];
            if(!keys || !keys.length)
                continue;

            allEmpty = false;
            if(_.contains(keys, issue.issueKey))
                return true;
        }
        // if all keys are empty, return as matched
        return allEmpty;
    }

    this.searchByQuery = function(source, query) {
        searchData.query = query;
        source.fire('iron-signal', {name:'search-filter-changed', data:searchData});
    }

    this.updateFilter = function(source, change) {
        jQuery.extend(searchData, change)
        source.fire('iron-signal', {name:'search-filter-changed', data:searchData});
    }

    this.match = function(issue) {
        if (!issue || !hasAnyFilter())
            return true;

        if(searchData.updatedIssues && searchData.updatedIssues.length)
            return matchByKey(issue, searchData.updatedIssues);

        return matchByString(issue, searchData.query)
            && matchByRelease(issue, searchData.release)
            && matchByKey(issue, searchData.hierarchy, searchData.dependencies);
    };

    this.isHierarchyRoot = function(issueKey) {
        return rootHierarchicalFilter === issueKey;
    }

    this.isDependency = function(issueKey) {
        if(!searchData.dependencies || !searchData.dependencies.length)
            return false;

        return _.contains(searchData.dependencies, issueKey);
    };

    this.getRootHierarchicalFilter = function() {
        return rootHierarchicalFilter;
    };

    this.toggleRootHierarchicalFilter = function(source, issueKey) {
        rootHierarchicalFilter = rootHierarchicalFilter == issueKey ? null : issueKey;
        var hierarchy = taskboard.getHierarchyMatch(rootHierarchicalFilter);
        var dependencies = taskboard.getDependenciesMatch(hierarchy);
        this.updateFilter(source, {
            hierarchy: hierarchy
            , dependencies: dependencies
        });
        source.fire('iron-signal', {name: 'hierarchical-filter-changed'});

        if (rootHierarchicalFilter) {
            source.fire('iron-signal', {name: 'search-filter-reset'});
        } else {
            source.fire('iron-signal', {name: 'search-filter-restore'});
        }
    };
}
SearchFilter.INVALID_ASSIGNEE_TAG="invalid-assignee";
var searchFilter = new SearchFilter();