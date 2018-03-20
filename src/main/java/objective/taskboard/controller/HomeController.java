package objective.taskboard.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
import objective.taskboard.data.Team;
import objective.taskboard.data.User;
import objective.taskboard.google.GoogleApiConfig;
import objective.taskboard.jira.FieldMetadataService;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.StatusPriorityOrder;
import objective.taskboard.jira.JiraService;
import objective.taskboard.jira.client.JiraFieldDataDto;
import objective.taskboard.repository.TeamCachedRepository;

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
    private TeamCachedRepository teamRepo;

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
        model.addAttribute("googleClientId", googleApiConfig.getClientId());
        model.addAttribute("permissions", serialize(authorizer.getProjectsPermission()));
        model.addAttribute("fieldNames", getFieldNames());
        
        Map<String, List<String>> statusOrderByIssueType = new LinkedHashMap<>();
        
        StatusPriorityOrder statusPriorityOrder = jiraPropeties.getStatusPriorityOrder();
        ArrayList<String> demands = new ArrayList<>(Arrays.asList(statusPriorityOrder.getDemandsInOrder()));
        Collections.reverse(demands);
        statusOrderByIssueType.put(""+jiraPropeties.getIssuetype().getDemand().getId(), demands);
        
        ArrayList<String> tasks = new ArrayList<>(Arrays.asList(statusPriorityOrder.getTasksInOrder()));
        Collections.reverse(tasks);
        jiraPropeties.getIssuetype().getFeatures().stream().forEach(feature -> {
            statusOrderByIssueType.put(""+feature.getId(), tasks);
        });
        
        ArrayList<String> subtasks = new ArrayList<>(Arrays.asList(statusPriorityOrder.getSubtasksInOrder()));
        Collections.reverse(subtasks);
        statusOrderByIssueType.put("subtasks", subtasks);
        model.addAttribute("statusOrderByIssueType", serialize(statusOrderByIssueType));

        List<Team> teams= teamRepo.getCache();
        model.addAttribute("teams", serialize(teams.stream().map(t->new TeamControllerData(t)).collect(Collectors.toList())));

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