package objective.taskboard.domain;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "followup_daily_synthesis")
@NamedQueries({
        @NamedQuery(
                name="FollowupDailySynthesis.count", 
                query="SELECT count(s) FROM FollowupDailySynthesis s WHERE s.projectId = :projectId AND s.followupDate = :date"),
        @NamedQuery(
                name="FollowupDailySynthesis.findByMaxDate", 
                query="SELECT s FROM FollowupDailySynthesis s WHERE s.projectId = :projectId AND s.followupDate < :maxDate ORDER BY s.followupDate"),
        @NamedQuery(
                name="FollowupDailySynthesis.delete", 
                query="DELETE FROM FollowupDailySynthesis s WHERE s.projectId = :projectId AND s.followupDate = :date")
})
public class FollowupDailySynthesis extends TaskboardEntity {
    @Column
    private Integer projectId;

    @Column
    private LocalDate followupDate;

    @Column
    private Double sumEffortDone;

    @Column
    private Double sumEffortBacklog;

    public FollowupDailySynthesis(){}

    public FollowupDailySynthesis(Integer projectId, LocalDate followupDate, Double effortDone, Double effortBacklog){
        this.projectId = projectId;
        this.followupDate = followupDate;
        this.sumEffortDone = effortDone;
        this.sumEffortBacklog = effortBacklog;
    }

    public LocalDate getFollowupDate() {
        return followupDate;
    }
    
    public Double getSumEffortBacklog() {
        return sumEffortBacklog;
    }
    
    public Double getSumEffortDone() {
        return sumEffortDone;
    }
    
    public Integer getProjectId() {
        return projectId;
    }

    @Override
    public String toString() {
        return "FollowupDailySynthesis [id=" + id + ", projectId=" + projectId + ", followupDate=" + followupDate
                + ", sumEffortDone=" + sumEffortDone + ", sumEffortBacklog=" + sumEffortBacklog + "]";
    }
    
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
