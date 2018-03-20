package objective.taskboard.controller;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;

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

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import objective.taskboard.jira.JiraService;
import objective.taskboard.jira.data.JiraUser.JiraUserAutocomple;

@RestController
@RequestMapping("/ws/users")
public class UserController {
    @Autowired
    private JiraService jiraBean;

    @RequestMapping(path = "search", method = RequestMethod.GET)
    public List<JiraUserAutocomple> usersThatNameStartsWith(
            @RequestParam("query") String userQuery, 
            @RequestParam("teamFilter") String teamFilter) {
        return jiraBean.findUsers(userQuery);
    }    

    @RequestMapping("logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}
