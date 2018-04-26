package objective.taskboard.controller;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import objective.taskboard.jira.JiraService;
import objective.taskboard.jira.data.JiraUser.JiraUserAutocomplete;

@RestController
@RequestMapping("/ws/users")
public class UserController {
    @Autowired
    private JiraService jiraBean;
    
    @Autowired
    private UserTeamService userTeamService;

    @RequestMapping(path = "search", method = RequestMethod.GET)
    public List<JiraUserAutocomplete> usersWhoseNameContains(
            @RequestParam("query") String userQuery, 
            @RequestParam("teamFilter") String teamFilter) {
        List<JiraUserAutocomplete> findUsers = jiraBean.findUsers(userQuery);
        return findUsers.stream().filter(user->userTeamService.isUserVisible(user.name)).collect(Collectors.toList());
    }

    @RequestMapping("logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}
