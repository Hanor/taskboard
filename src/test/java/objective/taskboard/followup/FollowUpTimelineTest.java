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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import objective.taskboard.domain.ProjectFilterConfiguration;

public class FollowUpTimelineTest {

    @Test
    public void shouldBuild() {
        ProjectFilterConfiguration project = new ProjectFilterConfiguration("PX");
        project.setStartDate(LocalDate.parse("2018-03-01"));
        project.setDeliveryDate(LocalDate.parse("2018-04-01"));
        project.setBaselineDate(LocalDate.parse("2018-03-05"));

        FollowUpDataRepository dataRepository = mock(FollowUpDataRepository.class);
        FollowUpTimeline timeline = FollowUpTimeline.build(LocalDate.parse("2018-03-15"), project, dataRepository);

        assertEquals("2018-03-15", timeline.getReference().toString());
        assertEquals("2018-03-01", timeline.getStart().map(LocalDate::toString).orElse(null));
        assertEquals("2018-04-01", timeline.getEnd().map(LocalDate::toString).orElse(null));
        assertEquals("2018-03-05", timeline.getBaselineDate().map(LocalDate::toString).orElse(null));
    }

    @Test
    public void whenProjectBaselineIsEmpy_timelineBaselineShouldBeTheFirstFollowUpDate() {
        ProjectFilterConfiguration project = new ProjectFilterConfiguration("PX");
        
        FollowUpDataRepository dataRepository = mock(FollowUpDataRepository.class);
        when(dataRepository.getFirstDate("PX")).thenReturn(Optional.of(LocalDate.parse("2018-03-02")));
        
        FollowUpTimeline timeline = FollowUpTimeline.build(LocalDate.parse("2018-03-15"), project, dataRepository);
        assertEquals("2018-03-02", timeline.getBaselineDate().map(LocalDate::toString).orElse(null));
    }
}
