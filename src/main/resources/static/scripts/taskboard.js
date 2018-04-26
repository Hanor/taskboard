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

function Taskboard() {
    var self = this;

    var aspectFilters;
    var issuesBySteps;
    var laneConfiguration;

    this.getLoggedUser = function() {
        return window.user;
    }
    
    this.getAvailableTeams = function() {
        return TEAMS;
    }

    this.getTeamById = function(id) {
        return TEAMS_BY_ID[id];
    }

    this.getTaskboardLogoUrl = function() {
        var logoUrlExistis = window.logo !== null && window.logo !== undefined && window.logo !== '';
        var logoUrlDefault = '/static/images/touch/icon-128x128.png';
        return logoUrlExistis ? window.logo : logoUrlDefault;
    }

    this.setAspectFilters = function(source, filters) {
        aspectFilters = filters;
        this.applyFilterPreferences();
        if(source)
            source.fire('iron-signal', {name:'refresh-release-filter'});
    };

    this.getAspectFilters = function() {
        return aspectFilters;
    };

    this.getTeams = function() {
        var teamFilter = _.find(this.getAspectFilters(), function (filter) {
            return filter.description == "Team"
        });
        return teamFilter.aspectsSubitemFilter;
    };

    this.setIssues = function(issues) {
        this.issues = issues;
        for (var index in this.issues) {
            this.issues[index] = this.resolveIssueFields(issues[index]);
        }
        setIssuesBySteps(issues);
        this.refitSteps();
    };

    function setIssuesBySteps(issues) {
        issuesBySteps = new Object()

        var steps = self.getAllSteps()
        for (var s in steps) {
            var step = steps[s]
            var filters = step.issuesConfiguration
            var issuesByStep = []

            for (var f in filters) {
                var filter = filters[f]

                for (var i in issues) {
                    var issue = issues[i]
                    if (filter.issueType == issue.type && filter.status == issue.status)//NOSONAR
                        issuesByStep.push(issue)
                }
            }

            issuesBySteps[step.id] = issuesByStep;
        }
    }

    this.getIssueStep = function(issue) {
        var steps = self.getAllSteps()
        for (var s in steps) {
            var step = steps[s]
            var filters = step.issuesConfiguration

            for (var f in filters) {
                var filter = filters[f]

                if (filter.issueType == issue.type && filter.status == issue.status)
                    return step;
            }
        }
        return null;
    };

    this.getIssuesByStep = function(stepId) {
        if (issuesBySteps)
            return issuesBySteps[stepId];
    };

    this.getIssueByKey = function(issueKey) {
        return findInArray(this.issues, function(i) {
                return i.issueKey === issueKey
            });
    };

    this.setLaneConfiguration = function(laneConfiguration) {
        this.laneConfiguration = laneConfiguration;
    };

    this.getLaneConfiguration = function() {
        return this.laneConfiguration;
    };

    this.getAllSteps = function() {
        var steps = new Array()
        for(var laneIndex in this.laneConfiguration) {
            var lane = this.laneConfiguration[laneIndex]
            for(var stageIndex in lane.stages) {
                steps = steps.concat(lane.stages[stageIndex].steps)
            }
        }
        return steps;
    }

    this.getFilters = function(stepId) {
        for(var lane = 0; lane < this.laneConfiguration.length; lane++)
            for(var stage = 0; stage < this.laneConfiguration[lane].stages.length; stage++)
                for(var step = 0; step < this.laneConfiguration[lane].stages[stage].steps.length; step++) {
                    var cStep = this.laneConfiguration[lane].stages[stage].steps[step];
                    if(cStep.id == stepId)//NOSONAR
                        return cStep.issuesConfiguration;
                }
        return null;
    };

    this.getTotalLaneWeight = function() {
        var total = 0;

        if (userPreferences.getLevels().length == 0) {
            this.laneConfiguration.forEach(function(lane) {
                total += lane.weight;
            });
            return total;
        }

        userPreferences.getLevels().forEach(function(x) {
            if(x.showLevel)
                total += x.weightLevel;
        });

        return total;
    };

    this.getLaneContainerHeight = function() {
        var container = document.querySelector('#lane-container');
        return container ? container.offsetHeight : 0;
    };

    this.getLane = function(id) {
        return document.querySelector('#lane-'+id);
    };

    this.refitSteps = _.debounce(function() {
        var steps = document.querySelectorAll('board-step');
        for(var i = 0; i < steps.length; i++)
            steps[i].refit();
        self.refitTables();
    }, 100);

    this.refitTables = function() {
        $('table').floatThead('reflow');
    };

    this.getHierarchyMatch = function(rootIssueKey) {
        if(!rootIssueKey)
            return null;

        var queue = [];
        var issue = this.getIssueByKey(rootIssueKey);
        queue.push({issue:issue, parents:true, children:true});

        function enqueueParent(item) {
            var parents = item.parents;
            var issue = item.issue;
            if(!parents || !issue.parent)
                return;

            // enqueue parent
            queue.push({issue:self.getIssueByKey(issue.parent), parents:true});
        }

        function enqueueChildren(item) {
            var children = item.children;
            var issue = item.issue;
            if(!children)
                return;

            for(var i = 0; i < self.issues.length; ++i) {
                var child = self.issues[i];
                // enqueue all children
                if(child.parent === issue.issueKey)
                    queue.push({issue:child, children:true});
            }
        }

        var hierarchyIssueKeys = [];
        while(queue.length > 0) {
            var next = queue.shift();
            issue = next.issue;
            if(_.contains(hierarchyIssueKeys, issue.issueKey))
                continue;

            hierarchyIssueKeys.push(issue.issueKey);

            enqueueParent(next);
            enqueueChildren(next);
        }
        return hierarchyIssueKeys;
    };

    this.getDependenciesMatch = function(hierarchyIssueKeys) {
        if(!hierarchyIssueKeys)
            return null;

        var dependencies = [];
        var hierarchyIssues = hierarchyIssueKeys.map(function(key) {
            return self.getIssueByKey(key);
        });
        for(var i = 0; i < this.issues.length; ++i) {
            var issue = this.issues[i];
            if(_.contains(hierarchyIssueKeys, issue.issueKey))
                continue;

            for(var h = 0; h < hierarchyIssues.length; ++h) {
                var hierarchyIssue = hierarchyIssues[h];
                if(_.contains(hierarchyIssue.dependencies, issue.issueKey))
                    dependencies.push(issue.issueKey);
            }
        }
        return dependencies;
    }

    this.applyFilterPreferences = function() {
        var filterPreferences = userPreferences.getFilters();

        if (_.isEmpty(filterPreferences))
            return;

        var filterTeams = [];
        aspectFilters.forEach(function(item) {
            if (item.description !== 'Project')
                return;

            item.aspectsSubitemFilter.forEach(function(subitem) {
                if (filterPreferences[subitem.value] == true)
                    filterTeams = filterTeams.concat(subitem.teams);
            });
        });

        aspectFilters.forEach(function(item) {
            item.aspectsSubitemFilter.forEach(function(subitem) {
                if (this.description === 'Issue Type') {
                    if (filterPreferences[subitem.value.id] != null)
                        subitem.selected = filterPreferences[subitem.value.id];
                } else {
                    if (filterPreferences[subitem.value] != null)
                        subitem.selected = filterPreferences[subitem.value];
                    else if (this.description === 'Project')
                        subitem.selected = false;
                    subitem.visible = true;
                    if (this.description === 'Team' && filterTeams.indexOf(subitem.value) == -1)
                        subitem.visible = false;
                }
            }, item);
        });
    },

    this.getIssueTypeName = function(issueTypeId) {
        var types = JSON.parse(localStorage.getItem("issueTypes"));
        for (var i in types)
            if (types[i].id == issueTypeId)
                return types[i].name;
        return "";
    };

    this.getStatuses = function() {
        if (this.statuses === undefined)
            this.statuses = JSON.parse(localStorage.getItem("statuses"));
        return this.statuses;
    };

    this.getFieldName = function(fs) {
        return FIELDNAMES[fs.fieldId] || fs.fieldId;
    };

    this.getStatus = function(statusId) {
        var statusIdNumber = parseInt(statusId);
        return Object.values(this.getStatuses()).find( function(s) { return s.id === statusIdNumber } );
    };

    this.getStatusName = function(statusId) {
        return this.getStatus(statusId).name;
    };

    this.connectToWebsocket = function(taskboardHome) {
        var socket = new SockJS('/taskboard-websocket');
        var stompClient = Stomp.over(socket);
        stompClient.debug = function(message) {
            if(!message || message.indexOf('PING') > -1 || message.indexOf('PONG') > -1)
                return;
            console.log(message);
        };
        stompClient.connect({}, function () {
            stompClient.subscribe('/topic/issues/updates', function (response) {
                handleIssueUpdates(taskboardHome, response)
            });

            stompClient.subscribe('/user/topic/sizing-import/status', function(status) {
                handleSizingImportStatus(taskboardHome, status);
            });

            stompClient.subscribe('/topic/cache-state/updates', function (response) {
                taskboardHome.fire("iron-signal", {name:"issue-cache-state-updated", data:{
                    newstate: JSON.parse(response.body)
                }})
            });

            stompClient.subscribe('/topic/projects/updates', function (response) {
                taskboardHome.fire('iron-signal', {name:'projects-changed', data:{
                    projects: JSON.parse(response.body)
                }});
            });
        });
    }

    function handleIssueUpdates(taskboardHome, response) {
        var issueUpdateData = JSON.parse(response.body)
        var relevantEvents = issueUpdateData.filter(function(event) {
            if (event.updateType === 'DELETED')
                return true;

            var previousInstance = getPreviousIssueInstanceById(event.issueId);
            if (!previousInstance)
                return true;

            return previousInstance.issue.stateHash != event.stateHash
        })

        var ids = relevantEvents.map(function(i) { return i.issueId})

        $.post({
            url:"/ws/issues/byids", 
            contentType: 'application/json', 
            data: JSON.stringify(ids)
        })
        .done(function(issues) {
            var issueById = {};
            issues.forEach(function(i){
                issueById[i.id] = i;
            })
            var issueUpdateEvents = []
            relevantEvents.forEach(function(event){
                var issueData = issueById[event.issueId];
                if (!issueData)
                    event.updateType = 'DELETED'

                if (event.updateType === 'DELETED') {
                    var previous = getPreviousIssueInstanceById(event.issueId);
                    if (!previous)
                        return;
                    event.target = getPreviousIssueInstanceById(event.issueId).issue;
                }
                else {
                    event.target = issueData;
                }
                issueUpdateEvents.push(event);
            })

            updateIssuesByEvents(taskboardHome, issueUpdateEvents)
        })
    }

    function updateIssuesByEvents(taskboardHome, updateEvents) {
        var updatedIssueKeys = []
        var updateByStep = {};
        updateEvents.forEach(function(anEvent) {
            var previousInstance = getPreviousIssueInstanceById(anEvent.target.id);
            if (anEvent.updateType === 'DELETED' && previousInstance !== null) {
                self.issues.splice(previousInstance.index, 1);
                updatedIssueKeys.push(anEvent.target.issueKey);
                self.fireIssueUpdated('server', taskboardHome, anEvent.target, anEvent.updateType);
                return;
            }

            var converted = self.resolveIssueFields(anEvent.target);
            if (previousInstance === null)
                self.issues.push(converted);
            else
                self.issues[previousInstance.index] = converted;

            updatedIssueKeys.push(anEvent.target.issueKey);
            var issueStep = self.getIssueStep(converted);
            if (issueStep !== null) {
                var stepId = issueStep.id;
                if (Object.keys(updateByStep).indexOf(stepId) === -1)
                    updateByStep[stepId] = [];

                updateByStep[stepId].push(anEvent.target);
            }

            self.fireIssueUpdated('server', taskboardHome, anEvent.target, anEvent.updateType);
        });

        if (updatedIssueKeys.length === 0)
            return;

        Object.keys(updateByStep).forEach(function(step) {
            taskboardHome.fire("iron-signal", {name:"step-update", data:{
                issues: updateByStep[step],
                stepId: step
            }})
        })

        if (!hasSomeFilteredIssue(updatedIssueKeys))
            return;

        taskboardHome.fire("iron-signal", {name:"show-issue-updated-message", data:{
            message: "Issues updated",
            updatedIssueKeys: updatedIssueKeys
        }})
    }

    function handleSizingImportStatus(taskboardHome, response) {
        var status = JSON.parse(response.body);
        taskboardHome.fire("iron-signal", {name:"sizing-import-status", data:{
            status: status
        }});
    }

    function getPreviousIssueInstanceById(id) {
        var previousInstance = null;
        var piIndex = findIndexInArray(self.issues, function(i) { return i.id === id });
        if (piIndex > -1)
            previousInstance = {issue: self.issues[piIndex], index: piIndex};
        return previousInstance;
    }

    this.fireIssueUpdated = function(source, triggerSource, issue, updateType) {
        var converted = updateType === 'DELETED' ? issue : self.convertAndRegisterIssue(issue);
        converted.__eventInfo = {source: source, type: updateType}
        triggerSource.fire("iron-signal", {name:"issues-updated", data:{
            source: source,
            eventType: updateType,
            issue: converted
        }})
        return converted;
    }

    this.issueGivenKey = function(issueKey) {
        return $("paper-material.issue [data-issue-key='"+issueKey+"']").closest("paper-material.issue");
    }

    this.convertAndRegisterIssues = function(issues) {
        var converted = []
        issues.forEach(function(issue) {
            converted.push(self.convertAndRegisterIssue(issue));
        })
        return converted;
    }

    this.convertAndRegisterIssue = function(issue) {
        var converted = this.resolveIssueFields(issue);
        
        var previousInstance = getPreviousIssueInstanceById(issue.id);
        if (previousInstance === null)
            self.issues.push(converted)
        else
            self.issues[previousInstance.index] = converted;
        return converted;
    }
    
    this.resolveIssueFields = function(issue) {
        var converted = issue;
        if (issue.additionalEstimatedHoursField)
            issue.additionalEstimatedHoursField.name = this.getFieldName(issue.additionalEstimatedHoursField);

        issue.subtasksTshirtSizes.forEach(function(ts) {
            ts.name = this.getFieldName(ts);
        }.bind(this))
        
        return issue;
    }

    function hasSomeFilteredIssue(issues) {
        var filteredIssues = self.getFilteredIssues();
        var filteredIssue = _.find(filteredIssues, function(f) {
            return issues.indexOf(f.issueKey) >= 0;
        });
        return filteredIssue != null;
    }

    this.getFilteredIssues = function() {
        var filteredIssues = [];

        self.laneConfiguration.forEach(function(lane) {
            if (!lane.showLevel)
                return;

            var stepsOfLane = getStepsOfLane(lane);
            stepsOfLane.forEach(function(step) {
                var boardStep = document.querySelector('.step-' + step.id) == null ?
                                    document.querySelector('board-step') :
                                    document.querySelector('.step-' + step.id);

                if (boardStep == null)
                    return;

                var allIssuesStep = self.getIssuesByStep(step.id);
                var filteredIssuesStep = boardStep.filterIssuesDisabledTeam(allIssuesStep);
                filteredIssues = filteredIssues.concat(filteredIssuesStep);
            });
        });
        return filteredIssues;
    };

    this.getTimeZoneIdFromBrowser = function() {
        return Intl.DateTimeFormat().resolvedOptions().timeZone || jstz.determine().name();
    };

    this.getLocaleFromBrowser = function() {
        return window.navigator.userLanguage || window.navigator.language;
    };

    function getStepsOfLane(lane) {
        var stepsOfLane = [];
        lane.stages.forEach(function(stage) {
            stepsOfLane = stepsOfLane.concat(stage.steps);
        });
        return stepsOfLane;
    }

    this.getResolutionFieldName = function() {
        return "resolution";
    };

    this.getArchivedText = function(isArchived) {
        return isArchived ? 'archived' : 'active';
    };

    this.getVisibleProjectKeys = function() {
        return _.chain(this.getAspectFilters())
            .filter(function(itemFilter) {
                return itemFilter.description === 'Project';
            })
            .map(function(itemFilter) {
                return _.chain(itemFilter.aspectsSubitemFilter)
                    .filter(function(subitemFilter) {
                        return subitemFilter.visible && subitemFilter.selected;
                    })
                    .map(function(subitemFilter) {
                        return subitemFilter.value;
                    })
                    .value();
            })
            .flatten()
            .value();
    }

    this.showError = function(source, message, actions) {
        source.fire("iron-signal", {name:"show-error-message", data: { message: message, actions: actions}});
    }
    
    this.showIssueError = function(source, issueKey, message) {
        source.fire("iron-signal", {name:"show-issue-error-message", data: {
            issueKey: issueKey,
            errorMessage: message
        }});
    }
    
    this.statusOrder = function(issueType, status) {
        if (STATUS_ORDER_BY_ISSUETYPE[issueType]) 
            return STATUS_ORDER_BY_ISSUETYPE[issueType].indexOf(status);
        
        return STATUS_ORDER_BY_ISSUETYPE["subtasks"].indexOf(status);
    }
}

var taskboard = new Taskboard();

function IssueLocalState(issueKey) {
    this.isUpdating = false;
    this.errorMessage = null;
    this.getIssueKey = function() {
        return issueKey;
    }
    this.hasError = function() {
        return !_.isEmpty(this.errorMessage);
    }
}

function IssueLocalStateBuilder(issueKey) {
    var issueLocalState = new IssueLocalState(issueKey);
    return {
        isUpdating: function(isUpdating) {
            issueLocalState.isUpdating = isUpdating;
            return this;
        },
        errorMessage: function(errorMessage) {
            issueLocalState.errorMessage = errorMessage;
            return this;
        },
        build: function() {
            return issueLocalState;
        }
    }
}

function StepLocalState(idOfStep) {
    var stepId = idOfStep;
    this.isUpdating = false;
    this.getStepId = function() {
        return stepId;
    }
}

function StepLocalStateBuilder(idOfStep) {
    var stepLocalState = new StepLocalState(idOfStep);
    return {
        isUpdating: function(isUpdating) {
            stepLocalState.isUpdating = isUpdating;
            return this;
        },
        build: function() {
            return stepLocalState;
        }
    }
}

var buttonTypes = {
    BUTTON: 'button',
    LINK: 'link'
};

function ButtonBuilder(text) {
    var button = {
        text: text,
        type: buttonTypes.BUTTON,
        disabled: false,
        hidden: false,
        onClick: function() {
            throw '"onClick" callback needs to be implemented.';
        }
    };
    return {
        id: function(id) {
            button.id = id;
            return this;
        },
        classes: function(classes) {
            button.classes = classes;
            return this;
        },
        type: function(type) {
            button.type = type;
            return this;
        },
        disabled: function(disabled) {
            button.disabled = disabled;
            return this;
        },
        hidden: function(hidden) {
            button.hidden = hidden;
            return this;
        },
        onClick: function(callback) {
            button.onClick = callback;
            return this;
        },
        build: function() {
            return button;
        }
    }
}

function flash(el, color) {
    var original = el.css('backgroundColor');
    el.animate({backgroundColor:color},300).animate({backgroundColor:original},800)
}
