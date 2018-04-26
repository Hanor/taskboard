package objective.taskboard.controller;

import java.util.List;
import java.util.Set;
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

    public List<Long> getIdsOfTeamsVisibleToUser() {
        return getTeamsVisibleToUser(loggedUser.getUsername()).stream().map(ut->ut.getId()).collect(Collectors.toList());
    }
    
    public Set<Team> getTeamsVisibleToLoggedInUser() {
        return getTeamsVisibleToUser(loggedUser.getUsername());
    }
    
    public boolean isUserVisible(String username) {
        Set<Team> teams = getTeamsVisibleToLoggedInUser();
        for (Team team : teams) {
            List<UserTeam> members = team.getMembers();
            for (UserTeam userTeam : members) {
                if (userTeam.getUserName().equals(username))
                    return true;
            }
        }
        return false;
    }
    
    private Set<Team> getTeamsVisibleToUser(String username) {
        List<UserTeam> userTeam = userTeamRepo.findByUserName(username);
        Set<Team> userTeams = userTeam.stream().map(ut->teamRepo.findByName(ut.getTeam())).filter(t->t!=null).distinct().collect(Collectors.toSet());

        List<Team> teamsInProjectsVisibleToUser = teamFilterConfigurationService.getDefaultTeamsInProjectsVisibleToUser();
        teamsInProjectsVisibleToUser.stream().forEach(t->userTeams.add(t));

        return userTeams;
    }
}
