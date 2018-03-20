package objective.taskboard.domain;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "project_filter_configuration")
public class ProjectFilterConfiguration implements Serializable {
    private static final long serialVersionUID = 765599368694090438L;

    private static final int DEFAULT_PROJECTION_TIMESPAN = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column
    private String projectKey;

    @Column
    private LocalDate startDate;//NOSONAR

    @Column
    private LocalDate deliveryDate;//NOSONAR
    
    @Column
    private Long defaultTeam;

    @Column(nullable = false)
    private boolean isArchived;

    @Column(precision = 5, scale = 4)
    private BigDecimal riskPercentage = BigDecimal.ZERO;

    @Column
    private Integer projectionTimespan;

    protected ProjectFilterConfiguration() {} //NOSONAR

    public ProjectFilterConfiguration(String projectKey) {
        this.setProjectKey(projectKey);
    }

    public Integer getId() {
        return id;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        if (isBlank(projectKey))
            throw new IllegalArgumentException("\"projectKey\" required");

        this.projectKey = projectKey.trim();
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean isArchived) {
        this.isArchived = isArchived;
    }

    public Integer getProjectionTimespan() {
        return projectionTimespan != null ? projectionTimespan : DEFAULT_PROJECTION_TIMESPAN;
    }

    public void setProjectionTimespan(Integer projectionTimespan) {
        if (projectionTimespan == null || projectionTimespan <= 0)
            throw new IllegalArgumentException("\"projectionTimespan\" must be a positive number");

        this.projectionTimespan = projectionTimespan;
    }

    public BigDecimal getRiskPercentage() {
        return riskPercentage;
    }

    public void setRiskPercentage(BigDecimal riskPercentage) {
        BigDecimal newValue = requireNonNull(riskPercentage).setScale(4, RoundingMode.HALF_EVEN);

        if (newValue.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Risk percentage should be between 0 and 1 (inclusive). Actual: " + newValue);

        this.riskPercentage = newValue;
    }

    public Long getDefaultTeam() {
        return defaultTeam;
    }

    public void setDefaultTeam(Long defaultTeamId) {
        this.defaultTeam = defaultTeamId;
    }
}
