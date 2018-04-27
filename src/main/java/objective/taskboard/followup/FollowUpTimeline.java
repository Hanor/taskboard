/*-
 * [LICENSE]
 * Taskboard
 * ---
 * Copyright (C) 2015 - 2018 Objective Solutions
 * ---
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

package objective.taskboard.followup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import objective.taskboard.domain.ProjectFilterConfiguration;
import objective.taskboard.utils.Optionals;

public class FollowUpTimeline {
    private final LocalDate reference;
    private final BigDecimal riskPercentage;
    private final Optional<LocalDate> start;
    private final Optional<LocalDate> end;
    private final Optional<LocalDate> baselineDate;

    public FollowUpTimeline(LocalDate reference, BigDecimal riskPercentage, Optional<LocalDate> start, Optional<LocalDate> end, Optional<LocalDate> baselineDate) {
        this.reference = reference;
        this.riskPercentage = riskPercentage;
        this.start = start;
        this.end = end;
        this.baselineDate = baselineDate;
    }

    public FollowUpTimeline(LocalDate reference) {
        this(reference, BigDecimal.ZERO, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public LocalDate getReference() {
        return reference;
    }
    
    public BigDecimal getRiskPercentage() {
        return riskPercentage;
    }

    public Optional<LocalDate> getStart() {
        return start;
    }

    public Optional<LocalDate> getEnd() {
        return end;
    }
    
    public Optional<LocalDate> getBaselineDate() {
        return baselineDate;
    }

    public static FollowUpTimeline build(LocalDate reference, ProjectFilterConfiguration project, FollowUpDataRepository dataRepository) {
        Optional<LocalDate> baselineDate = Optionals.or(
                () -> project.getBaselineDate(),
                () -> dataRepository.getFirstDate(project.getProjectKey()));
        
        return new FollowUpTimeline(
                reference, 
                project.getRiskPercentage(), 
                project.getStartDate(), 
                project.getDeliveryDate(),
                baselineDate);
    }
}