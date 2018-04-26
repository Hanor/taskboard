package objective.taskboard.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.auth.LoggedUserDetails;
import objective.taskboard.data.Team;
import objective.taskboard.data.UserTeam;
import objective.taskboard.filterConfiguration.TeamFilterConfigurationService;
import objective.taskboard.repository.TeamCachedRepository;
import objective.taskboard.repository.UserTeamCachedRepository;

@Service
public class UserTeamService {
    @Autowired
    private LoggedUserDetails loggedUser;

    @Autowired
    private UserTeamCachedRepository userTeamRepo;

    @Autowired
    private TeamCachedRepository teamRepo;

    @Autowired
    private TeamFilterConfigurationService teamFilterConfigurationService;

    public List<Long> getTeamsVisibleToUser() {
        List<UserTeam> userTeam = userTeamRepo.findByUserName(loggedUser.getUsername());
        List<Long> idsOfVisibleTeams = userTeam.stream().map(ut->teamRepo.findByName(ut.getTeam()).getId()).collect(Collectors.toList());

        List<Team> teamsVisibleToUser = teamFilterConfigurationService.getDefaultTeamsInProjectsVisibleToUser();
        teamsVisibleToUser.stream().forEach(t->idsOfVisibleTeams.add(t.getId()));

        return idsOfVisibleTeams;
    }
}
