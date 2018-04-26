package objective.taskboard.followup;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import objective.taskboard.domain.FollowupDailySynthesis;
import objective.taskboard.repository.FollowupDailySynthesisRepository;

public class FollowupDailySynthesisRepositoryMock implements FollowupDailySynthesisRepository {
    private final List<FollowupDailySynthesis> values = new ArrayList<>();

    @Override
    public boolean exists(Integer projectId, LocalDate date) {
        return values.stream().anyMatch(findBy(projectId, date));
    }

    @Override
    public List<FollowupDailySynthesis> listAllBefore(Integer projectId, LocalDate maxDateExclusive) {
        return values.stream()
                .filter(s -> s.getProjectId().equals(projectId) && s.getFollowupDate().isBefore(maxDateExclusive))
                .sorted(comparing(FollowupDailySynthesis::getFollowupDate))
                .collect(toList());
    }

    @Override
    public void add(FollowupDailySynthesis followupDailySynthesis) {
        values.add(followupDailySynthesis);
    }

    @Override
    public void remove(Integer projectId, LocalDate date) {
        values.stream().filter(findBy(projectId, date)).findFirst().ifPresent(values::remove);
    }

    private static Predicate<FollowupDailySynthesis> findBy(Integer projectId, LocalDate date) {
        return s -> s.getProjectId().equals(projectId) && s.getFollowupDate().equals(date);
    }

    public void assertValues(String... expected) {
        String actual = values.stream()
                .sorted(comparing(FollowupDailySynthesis::getFollowupDate).thenComparing(FollowupDailySynthesis::getProjectId))
                .map(s -> s.getFollowupDate() + " | " + s.getProjectId() + " | " + s.getSumEffortDone() + " | " + s.getSumEffortBacklog())
                .collect(joining("\n"));
        
        assertEquals(StringUtils.join(expected, "\n"), actual);
    }

}
