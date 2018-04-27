package objective.taskboard.controller;

import static objective.taskboard.config.CacheConfiguration.DASHBOARD_PROGRESS_DATA;
import static objective.taskboard.repository.PermissionRepository.DASHBOARD_TACTICAL;
import static objective.taskboard.utils.DateTimeUtils.determineTimeZoneId;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.guava.GuavaCacheManager;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.followup.cluster.ClusterNotConfiguredException;
import objective.taskboard.followup.data.FollowupProgressCalculator;
import objective.taskboard.followup.data.FollowupProgressCalculator.ProjectDatesNotConfiguredException;
import objective.taskboard.followup.data.ProgressData;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.testUtils.ControllerTestUtils.AssertResponse;

@RunWith(MockitoJUnitRunner.class)
public class FollowUpProgressControllerTest {

    private static final String PROJECT_KEY = "TEST";
    private static final LocalDate START_DATE = LocalDate.of(2017, 12, 30);
    private static final LocalDate END_DATE = LocalDate.of(2017, 12, 31);
    private static final Integer PROJECT_PROJECTION_TIMESPAN = 12;
    private static final String ZONE_ID = "America/Sao_Paulo";

    @Mock
    private ProjectFilterConfigurationCachedRepository projects;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Authorizer authorizer;

    @InjectMocks
    private FollowUpProgressController subject;

    @Mock
    private FollowupProgressCalculator calculator;

    private ProjectFilterConfiguration project;

    @Before
    public void setup() {
        setupValidProject();

        MockitoAnnotations.initMocks(this);
        when(authorizer.hasPermissionInProject(DASHBOARD_TACTICAL, PROJECT_KEY)).thenReturn(true);
        when(cacheManager.getCache(DASHBOARD_PROGRESS_DATA)).thenReturn(new GuavaCacheManager(DASHBOARD_PROGRESS_DATA).getCache(DASHBOARD_PROGRESS_DATA));
        when(projects.getProjectByKey(PROJECT_KEY)).thenReturn(Optional.of(project));
        when(calculator.calculate(eq(determineTimeZoneId(ZONE_ID)), eq(PROJECT_KEY), anyInt())).thenReturn(new ProgressData());

        subject.initCache();
    }

    private void setupValidProject() {
        project = new ProjectFilterConfiguration(PROJECT_KEY);
        project.setStartDate(START_DATE);
        project.setDeliveryDate(END_DATE);
        project.setProjectionTimespan(PROJECT_PROJECTION_TIMESPAN);
    }

    @Test
    public void ifAllValidationsWereSuccessful_returnOkAndTheCorrectValue() {
        final Integer projectionTimespan = 20;

        AssertResponse.of(subject.progress(PROJECT_KEY, ZONE_ID, Optional.of(projectionTimespan)))
                .httpStatus(OK)
                .bodyClass(ProgressData.class);

        verify(calculator).calculate(determineTimeZoneId(ZONE_ID), PROJECT_KEY, projectionTimespan);
    }

    @Test
    public void ifProjectionParameterWasNotPassed_returnOkAndTheCorrectValueWithDefaultProjectProjection() {
        AssertResponse.of(subject.progress(PROJECT_KEY, ZONE_ID, Optional.empty()))
                .httpStatus(OK)
                .bodyClass(ProgressData.class);
        
        verify(calculator).calculate(determineTimeZoneId(ZONE_ID), PROJECT_KEY, PROJECT_PROJECTION_TIMESPAN);
    }

    @Test
    public void ifUserHasNoPermissionInTactical_returnResourceNotFound() throws Exception {
        when(authorizer.hasPermissionInProject(DASHBOARD_TACTICAL, PROJECT_KEY)).thenReturn(false);

        AssertResponse.of(subject.progress(PROJECT_KEY, ZONE_ID, Optional.empty()))
                .httpStatus(NOT_FOUND)
                .bodyAsString("Resource not found.");
    }

    @Test
    public void ifSomeErrorOccur_returnInternalServerError() throws Exception {
        when(projects.getProjectByKey(PROJECT_KEY)).thenReturn(null);

        AssertResponse.of(subject.progress(PROJECT_KEY, ZONE_ID, Optional.empty()))
                .httpStatus(INTERNAL_SERVER_ERROR)
                .bodyAsString("Unexpected behavior. Please, report this error to the administrator.");
    }

    @Test
    public void ifProjectNotFound_returnNotFound() throws Exception {
        when(projects.getProjectByKey(PROJECT_KEY)).thenReturn(Optional.empty());

        AssertResponse.of(subject.progress(PROJECT_KEY, ZONE_ID, Optional.empty()))
                .httpStatus(NOT_FOUND)
                .bodyAsString("Project not found: "+ PROJECT_KEY +".");
    }

    @Test
    public void ifProjectionTimespanIsNegativeOrZero_returnNotFound() throws Exception {
        AssertResponse.of(subject.progress(PROJECT_KEY, ZONE_ID, Optional.of(-1)))
                .httpStatus(BAD_REQUEST)
                .bodyAsString("The projection timespan should be a positive number.");

        AssertResponse.of(subject.progress(PROJECT_KEY, ZONE_ID, Optional.of(0)))
                .httpStatus(BAD_REQUEST)
                .bodyAsString("The projection timespan should be a positive number.");
    }

    @Test
    public void ifProjectDateIsNull_returnBadRequestError() throws Exception {
        when(calculator.calculate(any(), any(), anyInt())).thenThrow(new ProjectDatesNotConfiguredException());

        AssertResponse.of(subject.progress(PROJECT_KEY, ZONE_ID, Optional.empty()))
                .httpStatus(BAD_REQUEST)
                .bodyAsString("The project "+ PROJECT_KEY +" has no start or delivery date.");
    }

    @Test
    public void ifSnapshotHasNoClusterConfiguration_returnInternalServerError() throws Exception {
        when(calculator.calculate(any(), any(), anyInt())).thenThrow(new ClusterNotConfiguredException());
        
        AssertResponse.of(subject.progress(PROJECT_KEY, ZONE_ID, Optional.empty()))
                .httpStatus(INTERNAL_SERVER_ERROR)
                .bodyAsString("No cluster configuration found for project "+ PROJECT_KEY +".");
    }

}
