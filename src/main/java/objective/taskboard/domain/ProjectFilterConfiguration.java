/*-
 * [LICENSE]
 * Taskboard
 * - - -
 * Copyright (C) 2015 - 2016 Objective Solutions
 * - - -
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * [/LICENSE]
 */

package objective.taskboard.domain;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
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

    @Column(nullable = false)
    private boolean isArchived;

    @Column(precision = 5, scale = 4)
    private BigDecimal riskPercentage = BigDecimal.ZERO;

    @Column
    private Integer projectionTimespan;
    
    @Column
    private LocalDate baselineDate;

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
        validateProjectDates(startDate, this.deliveryDate);
        this.startDate = startDate;
    }

    public Optional<LocalDate> getStartDate() {
        return Optional.ofNullable(startDate);
    }

    public Optional<LocalDate> getDeliveryDate() {
        return Optional.ofNullable(deliveryDate);
    }

    public void setDeliveryDate(LocalDate deliveryDate) {
        validateProjectDates(this.startDate, deliveryDate);
        this.deliveryDate = deliveryDate;
    }
    
    private static void validateProjectDates(LocalDate startDate, LocalDate deliveryDate) {
        if (startDate != null && deliveryDate != null && startDate.isAfter(deliveryDate))
            throw new IllegalArgumentException("'startDate' should be before or equal to 'deliveryDate'");
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

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="projectKey", referencedColumnName="projectKey")
    private List<ProjectTeam> projectTeams;

    public List<Long> getTeamsIds() {
        return projectTeams.stream().map(el->el.getTeamId()).collect(Collectors.toList());
    }

    
    public void setBaselineDate(LocalDate baselineDate) {
        this.baselineDate = baselineDate;
    }
    
    public Optional<LocalDate> getBaselineDate() {
        return Optional.ofNullable(baselineDate);
    }
}
