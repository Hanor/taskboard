package objective.taskboard.followup;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public interface FollowUpDataRepository {

    FollowUpData get(LocalDate date, ZoneId timezone, String projectKey);
    List<LocalDate> getHistoryByProject(String projectKey);
    void save(String projectKey, LocalDate date, FollowUpData data);
    Optional<LocalDate> getFirstDate(String projectKey);

}