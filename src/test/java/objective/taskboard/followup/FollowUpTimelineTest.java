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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import objective.taskboard.domain.ProjectFilterConfiguration;

public class FollowUpTimelineTest {

    @Test
    public void givenNoProjectConfiguration_whenGetTimelineWithReferenceDateNull_thenAllTimelineDatesShouldBeNull() {
        FollowUpTimeline timeline = FollowUpTimeline.getTimeline(null, Optional.empty());

        assertNull("Timeline reference date should be null", timeline.getReference());
        assertFalse("Timeline start date should be empty", timeline.getStart().isPresent());
        assertFalse("Timeline end date should be empty", timeline.getEnd().isPresent());
    }

    @Test
    public void givenProjectConfiguration_whenGetTimelineWithReferenceDateNotNull_thenAllTimelineDatesShouldBeNotNull() {
        ProjectFilterConfiguration projectConfiguration = mock(ProjectFilterConfiguration.class);
        when(projectConfiguration.getStartDate()).thenReturn(LocalDate.parse("2018-03-01"));
        when(projectConfiguration.getDeliveryDate()).thenReturn(LocalDate.parse("2018-04-01"));

        FollowUpTimeline timeline = FollowUpTimeline.getTimeline(LocalDate.parse("2018-03-15"), Optional.of(projectConfiguration));

        assertEquals("Timeline reference date", "2018-03-15", timeline.getReference().toString());
        assertTrue("Timeline start date should not be empty", timeline.getStart().isPresent());
        assertEquals("Timeline start date", "2018-03-01", timeline.getStart().get().toString());
        assertTrue("Timeline end date should not be empty", timeline.getEnd().isPresent());
        assertEquals("Timeline end date", "2018-04-01", timeline.getEnd().get().toString());
    }

}
