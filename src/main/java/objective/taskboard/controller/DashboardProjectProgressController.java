package objective.taskboard.controller;

import static objective.taskboard.utils.DateTimeUtils.determineTimeZoneId;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Objects;
import com.google.common.cache.Cache;

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
public class DashboardProjectProgressController {
    @Autowired
    private FollowUpDataProviderFromCurrentState providerFromCurrentState;
    
    @Autowired
    private ProjectFilterConfigurationCachedRepository projects;

    @Autowired
    private CacheManager cacheManager;

    private Cache<Key, ResponseEntity<Object>> cache;

    @Autowired
    private Authorizer authorizer;

    @SuppressWarnings("unchecked")
    public void initCache() {
        cache = (Cache<Key, ResponseEntity<Object>>) cacheManager.getCache(CacheConfiguration.DASHBOARD_PROGRESS_DATA).getNativeCache();
    }
    
    @RequestMapping(value = "/api/projects/{project}/followup/progress", method = RequestMethod.GET)
    public ResponseEntity<Object> progress(
            @PathVariable("project") String projectKey,
            @RequestParam("timezone") String zoneId,
            @RequestParam(value = "projection", defaultValue = "20") Integer projectionTimespan) {
        if (!authorizer.hasPermissionInProject(PermissionRepository.DASHBOARD_TACTICAL, projectKey))
            return new ResponseEntity<>("Resource not found", HttpStatus.NOT_FOUND);

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Key cacheKey = new Key(projectKey, zoneId, today, projectionTimespan);

        try {
            return cache.get(cacheKey, () -> load(cacheKey));
        } catch(Exception ex) {
            return new ResponseEntity<>(ex, INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<Object> load(Key key) throws Exception {
        String projectKey = key.projectKey;
        String zoneId = key.zoneId;
        Integer projectionSampleSize = key.projection;

        if (projectionSampleSize < 0)
            return new ResponseEntity<>("The projection timespan should be a positive number.", INTERNAL_SERVER_ERROR);

        Optional<ProjectFilterConfiguration> project = projects.getProjectByKey(projectKey);

        if (!project.isPresent())
            return new ResponseEntity<>("Project not found: " + projectKey + ".", HttpStatus.NOT_FOUND);

        ZoneId timezone = determineTimeZoneId(zoneId);

        if (project.get().getDeliveryDate() == null)
            return new ResponseEntity<>("The project " + projectKey + " has no delivery date.", INTERNAL_SERVER_ERROR);

        LocalDate deliveryDate = project.get().getDeliveryDate();

        if (project.get().getStartDate() == null)
            return new ResponseEntity<>("The project " + projectKey + " has no start date.", INTERNAL_SERVER_ERROR);

        LocalDate projectStartDate = project.get().getStartDate();

        FollowUpDataSnapshot snapshot = providerFromCurrentState.getJiraData(new String[]{projectKey}, timezone);
        if (!snapshot.hasClusterConfiguration())
            return new ResponseEntity<>("No cluster configuration found for project " + projectKey + ".", INTERNAL_SERVER_ERROR);

        if (!snapshot.getHistory().isPresent())
            return new ResponseEntity<>("No progress history found for project " + projectKey, INTERNAL_SERVER_ERROR);

        FollowupProgressCalculator calculator = new FollowupProgressCalculator();
        ProgressData progressData = calculator.calculate(snapshot, projectStartDate, deliveryDate, projectionSampleSize);

        return ResponseEntity.ok().body(progressData);
    }

    private static class Key {
        public final String projectKey;
        public final String zoneId;
        public final String today;
        public final Integer projection;

        public Key(String projectKey, String zoneId, String today, Integer projection) {
            this.projectKey = projectKey;
            this.zoneId = zoneId;
            this.today = today;
            this.projection = projection;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return Objects.equal(projectKey, key.projectKey) &&
                    Objects.equal(zoneId, key.zoneId) &&
                    Objects.equal(today, key.today) &&
                    Objects.equal(projection, key.projection);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(projectKey, zoneId, today, projection);
        }
    }
}
