package objective.taskboard.controller;

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

import static objective.taskboard.domain.converter.JiraIssueToIssueConverter.INVALID_TEAM;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.cycletime.CycleTimeProperties;
import objective.taskboard.cycletime.HolidayService;
import objective.taskboard.data.User;
import objective.taskboard.followup.FollowUpFacade;
import objective.taskboard.google.GoogleApiConfig;
import objective.taskboard.jira.FieldMetadataService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraService;
import objective.taskboard.jira.client.JiraFieldDataDto;

@Controller
public class HomeController {

    @Autowired
    private JiraService jiraService;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private JiraProperties jiraPropeties;

    @Autowired
    private CycleTimeProperties cycleTimePropeties;
    
    @Autowired
    private GoogleApiConfig googleApiConfig;

    @Autowired
    private Authorizer authorizer;

    @Autowired
    private FieldMetadataService fieldMetadataService;

    @Autowired
    private FollowUpFacade followupFacade;

    @RequestMapping("/")
    public String home(Model model) {
        User user = jiraService.getLoggedUser();
        model.addAttribute("user", serialize(user));
        model.addAttribute("jiraCustomfields", jiraPropeties.getCustomfield());
        model.addAttribute("jiraIssuetypes", jiraPropeties.getIssuetype());
        model.addAttribute("jiraStatusesCompletedIds", serialize(jiraPropeties.getStatusesCompletedIds()));
        model.addAttribute("jiraStatusesCanceledIds", serialize(jiraPropeties.getStatusesCanceledIds()));
        model.addAttribute("jiraTransitionsWithRequiredCommentNames", serialize(jiraPropeties.getTransitionsWithRequiredCommentNames()));
        model.addAttribute("cycleTimeStartBusinessHours", serialize(cycleTimePropeties.getStartBusinessHours()));
        model.addAttribute("cycleTimeEndBusinessHours", serialize(cycleTimePropeties.getEndBusinessHours()));
        model.addAttribute("holidays", serialize(holidayService.getHolidays()));
        model.addAttribute("invalidTeam", INVALID_TEAM);
        model.addAttribute("googleClientId", googleApiConfig.getClientId());
        model.addAttribute("permissions", serialize(authorizer.getProjectsPermission()));
        model.addAttribute("fieldNames", getFieldNames());
        model.addAttribute("hasFollowupTemplateAvailable", followupFacade.getTemplatesForCurrentUser().size() > 0);
        return "index";
    }

    private String serialize(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getFieldNames() {
        return fieldMetadataService.getFieldsMetadataAsUser()
                .stream()
                .collect(Collectors.toMap(JiraFieldDataDto::getId, JiraFieldDataDto::getName));
    }
}
