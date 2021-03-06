package objective.taskboard.controller;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.followup.FollowUpDataSnapshot;
import objective.taskboard.followup.PlannedVsBallparkDataAccumulator;
import objective.taskboard.followup.impl.FollowUpDataProviderFromCurrentState;
import objective.taskboard.repository.PermissionRepository;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;


@RestController
public class FollowUpPlannedBallparkController {
    
    @Autowired
    private FollowUpDataProviderFromCurrentState followUpDataProviderFromCurrentState;
    
    @Autowired
    private ProjectFilterConfigurationCachedRepository projects;

    @Autowired
    private Authorizer authorizer;

    @RequestMapping(value = "/api/projects/{projectKey}/followup/planned-ballpark", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> data(@PathVariable("projectKey") String projectKey) {
        if (!authorizer.hasPermissionInProject(PermissionRepository.DASHBOARD_OPERATIONAL, projectKey))
            return new ResponseEntity<>("Resource not found", HttpStatus.NOT_FOUND);

        if (!projects.exists(projectKey))
            return new ResponseEntity<>("Project not found: " + projectKey + ".", HttpStatus.NOT_FOUND);
        
        final PlannedVsBallparkDataAccumulator accumulator = new PlannedVsBallparkDataAccumulator();
        FollowUpDataSnapshot snapshot = followUpDataProviderFromCurrentState.getJiraData(projectKey);
        if (!snapshot.hasClusterConfiguration())
            return new ResponseEntity<>("No cluster configuration found for project " + projectKey + ".", INTERNAL_SERVER_ERROR);
        
        snapshot.forEachRow(accumulator::accumulate);

        return ResponseEntity.ok(accumulator.getData());
    }
}
