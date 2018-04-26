package objective.taskboard.repository;

import java.time.LocalDate;
import java.util.List;

import objective.taskboard.domain.FollowupDailySynthesis;

public interface FollowupDailySynthesisRepository {

    boolean exists(Integer projectId, LocalDate date);
    List<FollowupDailySynthesis> listAllBefore(Integer projectId, LocalDate maxDateExclusive);
    void add(FollowupDailySynthesis followupDailySynthesis);
    void remove(Integer projectId, LocalDate date);

}