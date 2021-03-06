package objective.taskboard.controller;

import static objective.taskboard.utils.DateTimeUtils.determineTimeZoneId;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Objects;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.config.CacheConfiguration;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.followup.FollowUpDataSnapshot;
import objective.taskboard.followup.data.FollowupProgressCalculator;
import objective.taskboard.followup.data.ProgressData;
import objective.taskboard.followup.impl.FollowUpDataProviderFromCurrentState;
import objective.taskboard.repository.PermissionRepository;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;

@RestController
public class FollowUpProgressController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FollowUpProgressController.class);

    @Autowired
    private FollowUpDataProviderFromCurrentState providerFromCurrentState;

    @Autowired
    private ProjectFilterConfigurationCachedRepository projects;

    @Autowired
    private CacheManager cacheManager;

    private Cache cache;

    @Autowired
    private Authorizer authorizer;

    @PostConstruct
    public void initCache() {
        cache = cacheManager.getCache(CacheConfiguration.DASHBOARD_PROGRESS_DATA);
    }

    @RequestMapping(value = "/api/projects/{project}/followup/progress", method = RequestMethod.GET)
    public ResponseEntity<Object> progress(
            @PathVariable("project") String projectKey,
            @RequestParam("timezone") String zoneId,
            @RequestParam("projection") Optional<Integer> projectionTimespan) {
        if (!authorizer.hasPermissionInProject(PermissionRepository.DASHBOARD_TACTICAL, projectKey))
            return new ResponseEntity<>("Resource not found.", HttpStatus.NOT_FOUND);

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Key cacheKey = new Key(projectKey, zoneId, today, projectionTimespan);

        try {
            return cache.get(cacheKey, () -> load(cacheKey));
        } catch(Exception e) {
            log.error("Error on load " + projectKey, e);
            return new ResponseEntity<>("Unexpected behavior. Please, report this error to the administrator.", INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<Object> load(Key key) throws Exception {
        Optional<ProjectFilterConfiguration> projectOpt = projects.getProjectByKey(key.projectKey);

        if (!projectOpt.isPresent())
            return new ResponseEntity<>("Project not found: " + key.projectKey + ".", NOT_FOUND);

        ProjectFilterConfiguration project = projectOpt.get();

        final Integer projectionTimespan;

        projectionTimespan = key.projectionTimespan.orElseGet(project::getProjectionTimespan);

        if (projectionTimespan <= 0)
            return new ResponseEntity<>("The projection timespan should be a positive number.", BAD_REQUEST);

        if (project.getDeliveryDate() == null)
            return new ResponseEntity<>("The project " + key.projectKey + " has no delivery date.", INTERNAL_SERVER_ERROR);

        if (project.getStartDate() == null)
            return new ResponseEntity<>("The project " + key.projectKey + " has no start date.", INTERNAL_SERVER_ERROR);

        FollowUpDataSnapshot snapshot = providerFromCurrentState.getJiraData(new String[]{key.projectKey}, determineTimeZoneId(key.zoneId));
        if (!snapshot.hasClusterConfiguration())
            return new ResponseEntity<>("No cluster configuration found for project " + key.projectKey + ".", INTERNAL_SERVER_ERROR);

        if (!snapshot.getHistory().isPresent())
            return new ResponseEntity<>("No progress history found for project " + key.projectKey + ".", INTERNAL_SERVER_ERROR);

        FollowupProgressCalculator calculator = new FollowupProgressCalculator();

        ProgressData progressData = calculator.calculate(snapshot, project.getStartDate(), project.getDeliveryDate(), projectionTimespan);
        return ResponseEntity.ok().body(progressData);

    }

    private static class Key {
        public final String projectKey;
        public final String zoneId;
        public final String today;
        public final Optional<Integer> projectionTimespan;

        public Key(String projectKey, String zoneId, String today, Optional<Integer> projection) {
            this.projectKey = projectKey;
            this.zoneId = zoneId;
            this.today = today;
            this.projectionTimespan = projection;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return Objects.equal(projectKey, key.projectKey) &&
                    Objects.equal(zoneId, key.zoneId) &&
                    Objects.equal(today, key.today) &&
                    Objects.equal(projectionTimespan, key.projectionTimespan);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(projectKey, zoneId, today, projectionTimespan);
        }
    }
}
