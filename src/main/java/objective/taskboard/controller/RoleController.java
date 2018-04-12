package objective.taskboard.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ws/roles")
public class RoleController {

    @GetMapping
    public List<RoleData> get() {
        return Arrays.asList(new RoleData("Administrators"),
                new RoleData("Customer"),
                new RoleData("Developers"),
                new RoleData("Guest Developer"),
                new RoleData("Issue Handler"),
                new RoleData("KPI"),
                new RoleData("Partner"),
                new RoleData("Product Manager"),
                new RoleData("Project Manager"),
                new RoleData("Project Watcher"),
                new RoleData("Quality Assurance"),
                new RoleData("Reporter"),
                new RoleData("Reviewer"),
                new RoleData("Staff Assignee"),
                new RoleData("Users"));
    }

}
