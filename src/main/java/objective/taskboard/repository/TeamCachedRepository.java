package objective.taskboard.repository;

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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.data.Team;

@Service
public class TeamCachedRepository {

    private static final Logger log = LoggerFactory.getLogger(TeamCachedRepository.class);

    @Autowired
    private TeamRepository teamRepository;

    private List<Team> cache;

    @PostConstruct
    private void load() {
        loadCache();
    }

    public List<Team> getCache() {
        return cache;
    }

    public Team findByName(String teamName) {
        for (Team team : cache) {
            if (team.getName().equals(teamName))
                return team;
        }
        return null;
    }

    public Optional<Team> findById(Long teamId) {
        for (Team team : cache) {
            if (team.getId().equals(teamId))
                return Optional.of(team);
        }
        return Optional.empty();
    }

    public Boolean exists(String teamName) {
        if (findByName(teamName) != null)
            return true;
        return false;
    }

    public void loadCache() {
        log.info("------------------------------ > TeamCachedRepository.loadCache()");
        this.cache = Collections.unmodifiableList(teamRepository.findAll());
    }

    public Team save(Team team) {
        Team saved = teamRepository.save(team);
        loadCache();
        return saved;
    }
}
