package objective.taskboard.followup;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tomcat.util.buf.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import objective.taskboard.Constants;
import objective.taskboard.followup.cluster.EmptyFollowupCluster;
import objective.taskboard.repository.ProjectFilterConfigurationCachedRepository;
import objective.taskboard.utils.DateTimeUtils;

@RunWith(MockitoJUnitRunner.class)
public class CumulativeFlowDiagramDataProviderTest {

    private static final LocalDate TODAY_DATE = LocalDate.now();

    @Mock
    private ProjectFilterConfigurationCachedRepository projectRepository;

    @Mock
    private FollowUpSnapshotService snapshotService;

    @InjectMocks
    private CumulativeFlowDiagramDataProvider subject;

    private FollowUpData followupData;

    @Before
    public void setup() {
        followupData = FollowUpHelper.getBiggerFollowupData();
        FollowUpTimeline timeline = new FollowUpTimeline(TODAY_DATE);
        FollowUpSnapshot snapshot = new FollowUpSnapshot(timeline, followupData, new EmptyFollowupCluster(), emptyList());
        doReturn(snapshot).when(snapshotService).getFromCurrentState(any(), eq("TASKB"));
        doReturn(true).when(projectRepository).exists(eq("TASKB"));

        FollowUpData emptyFollowupData = new FollowUpData(new FromJiraDataSet(Constants.FROMJIRA_HEADERS, emptyList()), emptyList(), emptySynthetics());
        FollowUpSnapshot emptySnapshot = new FollowUpSnapshot(timeline, emptyFollowupData, new EmptyFollowupCluster(), emptyList());
        doReturn(emptySnapshot).when(snapshotService).getFromCurrentState(any(), eq("EMPTY"));
        doReturn(true).when(projectRepository).exists(eq("EMPTY"));
    }

    private List<SyntheticTransitionsDataSet> emptySynthetics() {
        List<SyntheticTransitionsDataSet> synthetics = FollowUpHelper.getBiggerSyntheticTransitionsDataSet();
        return synthetics
                .stream()
                .map(ds -> new SyntheticTransitionsDataSet(ds.issueType, ds.headers, Collections.emptyList()))
                .collect(toList());
    }

    @Test
    public void whenGenerateCfdData_dataShouldBeOrdered() {
        CumulativeFlowDiagramDataSet cfdSubTask = subject.getCumulativeFlowDiagramDataSet("TASKB", "SUBTASK");
        String[] actualStatusSubTask = cfdSubTask.dataByStatus.keySet().toArray(new String[0]);

        assertEquals("To Do,Doing,Reviewing,UAT,Done", StringUtils.join(actualStatusSubTask));

        AnalyticsTransitionsDataSet subTaskAnalytics = followupData.analyticsTransitionsDsList.get(2);

        assertDates(subTaskAnalytics, cfdSubTask);
        assertTypes(subTaskAnalytics, cfdSubTask);

        CumulativeFlowDiagramDataSet cfdFeature = subject.getCumulativeFlowDiagramDataSet("TASKB", "Feature");
        String[] actualStatusFeature = cfdFeature.dataByStatus.keySet().toArray(new String[0]);

        assertEquals("To Do,Doing,QA,Done", StringUtils.join(actualStatusFeature));

        AnalyticsTransitionsDataSet featureAnalytics = followupData.analyticsTransitionsDsList.get(1);

        assertDates(featureAnalytics, cfdFeature);
        assertTypes(featureAnalytics, cfdFeature);
    }

    private void assertDates(AnalyticsTransitionsDataSet analytics, CumulativeFlowDiagramDataSet cfd) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String expectedDates = analytics.rows.stream()
                .map(row -> row.transitionsDates)
                .flatMap(r->r.stream())
                .filter(it -> it != null)
                .map(d->d.toLocalDate())
                .distinct()
                .sorted()
                .map(d -> format.format(d))
                .collect(Collectors.toList())
                .toString();

        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String actualDates = cfd.dataByStatus.get("To Do").stream()
            .map(each -> each.date)
            .map(d -> df.format(d))
            .distinct()
            .sorted()
            .collect(Collectors.toList())
            .toString();

        assertEquals(expectedDates, actualDates);
    }

    private void assertTypes(AnalyticsTransitionsDataSet analytics, CumulativeFlowDiagramDataSet cfd) {
        String expectedTypes = analytics.rows.stream()
                .map(row -> row.issueType)
                .distinct()
                .sorted()
                .collect(Collectors.toList())
                .toString();

        String actualTypes = cfd.dataByStatus.get("To Do").stream()
                .map(each -> each.type)
                .distinct()
                .sorted()
                .collect(Collectors.toList())
                .toString();

        assertEquals(expectedTypes, actualTypes);
    }

    @Test
    public void invalidProject() {
        try {
            subject.getCumulativeFlowDiagramDataSet("INVALID", "Subtask");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("Unknown project <INVALID>", e.getMessage());
        }
    }

    @Test
    public void whenProjectHasNoData_CfdShouldGenerateEmpty() {
        CumulativeFlowDiagramDataSet cfd = subject.getCumulativeFlowDiagramDataSet("EMPTY", "SUBTASK");
        assertTrue(cfd.dataByStatus.isEmpty());
    }

    @Test
    public void whenProjectHasStartAndDeliveryDates_cfdShouldRespectDateRange() {
        // given
        LocalDate projectStart = LocalDate.of(1999, 12, 1);
        LocalDate projectDelivery = LocalDate.of(2000, 1, 31);
        AssertionContext ctx = setupProject("STARTDELIVERY", projectStart, projectDelivery);

        // when
        CumulativeFlowDiagramDataSet cfd = subject.getCumulativeFlowDiagramDataSet("STARTDELIVERY", "SUBTASK");

        // then
        assertThat(cfd)
                .satisfies(ctx::containsAllDaysBetweenProjectStartAndDelivery)
                .satisfies(ctx::allDataPointsBeforeAnyIssueWereCreatedShouldHaveCountEqualsZero)
                .satisfies(ctx::allDataPointsAfterLastIssueTransitionShouldRepeatLastRow)
                .satisfies(ctx::statusInOrder);
    }

    @Test
    public void whenProjectHasDeliveryDateOnly_cfdShouldRespectDateRange() {
        // given
        LocalDate projectStart = null;
        LocalDate projectDelivery = LocalDate.of(2000, 1, 31);
        AssertionContext ctx = setupProject("DELIVERY", projectStart, projectDelivery);

        // when
        CumulativeFlowDiagramDataSet cfd = subject.getCumulativeFlowDiagramDataSet("DELIVERY", "SUBTASK");

        // then
        assertThat(cfd)
                .satisfies(ctx::containsAllDaysBetweenProjectStartAndDelivery)
                .satisfies(ctx::allDataPointsBeforeAnyIssueWereCreatedShouldHaveCountEqualsZero)
                .satisfies(ctx::allDataPointsAfterLastIssueTransitionShouldRepeatLastRow)
                .satisfies(ctx::statusInOrder);
    }

    @Test
    public void whenProjectHasNarrowerStartAndDeliveryDate_cfdShouldRespectDateRange() {
        // given
        LocalDate projectStart = LocalDate.of(2000, 1, 3);
        LocalDate projectDelivery = LocalDate.of(2000, 1, 5);
        AssertionContext ctx = setupProject("NARROW", projectStart, projectDelivery);

        // when
        CumulativeFlowDiagramDataSet cfd = subject.getCumulativeFlowDiagramDataSet("NARROW", "SUBTASK");

        // then
        assertThat(cfd)
                .satisfies(ctx::containsAllDaysBetweenProjectStartAndDelivery)
                .satisfies(ctx::allDataPointsBeforeAnyIssueWereCreatedShouldHaveCountEqualsZero)
                .satisfies(ctx::allDataPointsAfterLastIssueTransitionShouldRepeatLastRow)
                .satisfies(ctx::statusInOrder);
    }

    private AssertionContext setupProject(String projectKey, LocalDate projectStart, LocalDate projectDelivery) {
        FollowUpTimeline timeline = new FollowUpTimeline(TODAY_DATE, Optional.ofNullable(projectStart), Optional.ofNullable(projectDelivery));
        FollowUpSnapshot snapshot = new FollowUpSnapshot(timeline, followupData, new EmptyFollowupCluster(), emptyList());
        doReturn(snapshot).when(snapshotService).getFromCurrentState(any(), eq(projectKey));
        doReturn(true).when(projectRepository).exists(eq(projectKey));
        return new AssertionContext(snapshot);
    }

    private static class AssertionContext {

        private final ZoneId zone = ZoneId.systemDefault();
        private final FollowUpTimeline timeline;
        private final SyntheticTransitionsDataSet ds;
        private final ZonedDateTime followupStart;
        private final ZonedDateTime followupEnd;
        private final List<String> statuses;
        private final List<String> issueTypes;

        private AssertionContext(FollowUpSnapshot snapshot) {
            // only sub-task data is checked
            ds = snapshot.getData().syntheticsTransitionsDsList.get(2);
            timeline = snapshot.getTimeline();
            followupStart = ds.rows.stream().map(row -> row.date).min(Comparator.comparing(date -> date)).orElseThrow(AssertionError::new);
            followupEnd = ds.rows.stream().map(row -> row.date).max(Comparator.comparing(date -> date)).orElseThrow(AssertionError::new);
            statuses = ds.headers.subList(ds.getInitialIndexStatusHeaders(), ds.headers.size());
            issueTypes = ds.rows.stream()
                    .map(row -> row.issueType)
                    .distinct()
                    .collect(toList());
        }

        private Stream<CumulativeFlowDiagramDataPoint> dataPointsFor(CumulativeFlowDiagramDataSet cfd, String status, String issueType) {
            return cfd.dataByStatus
                    .get(status)
                    .stream()
                    .filter(point -> issueType.equals(point.type));
        }

        private int lastRowIssueCountFor(String status, String issueType) {
            return ds.rows.stream()
                    .filter(row -> issueType.equals(row.issueType))
                    .max(Comparator.comparing(row -> row.date))
                    .map(row -> row.amountOfIssueInStatus.get(statuses.indexOf(status)))
                    .orElseThrow(AssertionError::new);
        }

        public void containsAllDaysBetweenProjectStartAndDelivery(CumulativeFlowDiagramDataSet cfd) {
            final LocalDate projectStart = timeline.getStart().orElseGet(followupStart::toLocalDate);
            final LocalDate projectDelivery = timeline.getEnd().orElseGet(followupEnd::toLocalDate);
            for (String status : statuses) {
                for (String issueType : issueTypes) {
                    assertThat(dataPointsFor(cfd, status, issueType))
                            .extracting(dataPoint -> DateTimeUtils.toLocalDate(dataPoint.date, zone))
                            .containsExactlyElementsOf(DateTimeUtils.dayStream(projectStart, projectDelivery).collect(toList()));
                }
            }
        }

        public void allDataPointsBeforeAnyIssueWereCreatedShouldHaveCountEqualsZero(CumulativeFlowDiagramDataSet cfd) {
            for (String status : statuses) {
                for (String issueType : issueTypes) {
                    assertThat(dataPointsFor(cfd, status, issueType))
                            .filteredOn(dataPoint -> DateTimeUtils.get(dataPoint.date, zone).isBefore(followupStart))
                            .allSatisfy(dataPoint ->
                                    assertThat(dataPoint.count).isEqualTo(0)
                            );
                }
            }
        }

        public void allDataPointsAfterLastIssueTransitionShouldRepeatLastRow(CumulativeFlowDiagramDataSet cfd) {
            for (String status : statuses) {
                for (String issueType : issueTypes) {
                    assertThat(dataPointsFor(cfd, status, issueType))
                            .filteredOn(dataPoint -> DateTimeUtils.get(dataPoint.date, zone).isAfter(followupEnd))
                            .allSatisfy(dataPoint ->
                                    assertThat(dataPoint.count).isEqualTo(lastRowIssueCountFor(status, issueType))
                            );
                }
            }
        }

        public void statusInOrder(CumulativeFlowDiagramDataSet cfd) {
            assertThat(cfd.dataByStatus.keySet())
                    .containsExactly(
                            "To Do"
                            , "Doing"
                            , "Reviewing"
                            , "UAT"
                            , "Done"
                    );
        }
    }
}
