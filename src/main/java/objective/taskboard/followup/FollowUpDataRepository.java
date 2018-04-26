package objective.taskboard.followup;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public interface FollowUpDataRepository {

    FollowUpData get(LocalDate date, ZoneId timezone, String projectKey);
    List<LocalDate> getHistoryByProject(String projectKey);
    void save(String projectKey, LocalDate date, FollowUpData data);

}