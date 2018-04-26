package objective.taskboard.controller;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.followup.FollowUpScopeByTypeDataSet;
import objective.taskboard.followup.cluster.ClusterNotConfiguredException;
import objective.taskboard.followup.impl.FollowUpScopeByTypeDataProvider;
import objective.taskboard.repository.PermissionRepository;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;

@RestController
public class FollowUpScopeByTypeController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FollowUpScopeByTypeController.class);

    @Autowired
    private FollowUpScopeByTypeDataProvider scopeByTypeDataProvider;

    @Autowired
    private ProjectFilterConfigurationCachedRepository projectRepository;

    @Autowired
    private Authorizer authorizer;

    @RequestMapping(value = "/api/projects/{projectKey}/followup/scope-by-type", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> scopeByTypeData(
            @PathVariable("projectKey") String projectKey, 
            @RequestParam("date") Optional<LocalDate> date, 
            @RequestParam("timezone") String timezone) {

        if (!authorizer.hasPermissionInProject(PermissionRepository.DASHBOARD_TACTICAL, projectKey))
            return new ResponseEntity<>("Resource not found.", HttpStatus.NOT_FOUND);

        if (isEmpty(projectKey))
            return new ResponseEntity<>("Project is required.", HttpStatus.BAD_REQUEST);

        if (!projectRepository.exists(projectKey))
            return new ResponseEntity<>("Project not found: " + projectKey + ".", HttpStatus.NOT_FOUND);

        if (!isValidZoneId(timezone))
            return new ResponseEntity<>("Invalid timezone: " + timezone + ".", HttpStatus.BAD_REQUEST);

        ZoneId zoneId = isEmpty(timezone) ? ZoneId.systemDefault() : ZoneId.of(timezone);
        try {
            FollowUpScopeByTypeDataSet scopeByTypeData = scopeByTypeDataProvider.getScopeByTypeData(projectKey, date, zoneId);
            return ResponseEntity.ok(scopeByTypeData);
        }catch(ClusterNotConfiguredException e) {//NOSONAR
            return new ResponseEntity<>("No cluster configuration found for project " + projectKey + ".", INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isValidZoneId(String zoneId) {
        if (!isEmpty(zoneId)) {
            try {
                ZoneId.of(zoneId);
            } catch(ZoneRulesException e) {
                log.warn("Error on getScopeByTypeData: Invalid timezone parameter: " + zoneId, e);
                return false;
            }
        }
        return true;
    }

}
