package objective.taskboard.followup;

import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

public class FollowUpDataRepositoryMock implements FollowUpDataRepository {
    private final Map<String, Map<LocalDate, FollowUpData>> values = new TreeMap<>();

    @Override
    public FollowUpData get(LocalDate date, ZoneId timezone, String projectKey) {
        if (!values.containsKey(projectKey) || !values.get(projectKey).containsKey(date))
            throw new IllegalArgumentException();
        
        return values.get(projectKey).get(date);
    }

    @Override
    public List<LocalDate> getHistoryByProject(String projectKey) {
        if (!values.containsKey(projectKey))
            return Collections.emptyList();

        return new ArrayList<>(values.get(projectKey).keySet());
    }

    @Override
    public void save(String projectKey, LocalDate date, FollowUpData data) {
        if (!values.containsKey(projectKey))
            values.put(projectKey, new TreeMap<>());
        
        values.get(projectKey).put(date, data);
    }

    public void assertValues(String... expected) {
        String actual = values.keySet().stream()
                .map(p -> p + "\n" + values.get(p).entrySet().stream()
                        .map(e -> "  " + e.getKey() + " (rows: " + e.getValue().fromJiraDs.rows.size() + ")")
                        .collect(joining("\n")))
                .collect(joining("\n"));
        
        assertEquals(StringUtils.join(expected, "\n"), actual);
    }

    @Override
    public Optional<LocalDate> getFirstDate(String projectKey) {
        return getHistoryByProject(projectKey).stream().findFirst();
    }
}
