package objective.taskboard.data;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tomcat.util.buf.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;

import objective.taskboard.data.Issue.CardTeam;
import objective.taskboard.domain.converter.IssueTeamService;

public class IssueTest {
    @Test
    public void ensureAllScratchFieldsAreCopied() throws IllegalArgumentException, IllegalAccessException {
        IssueScratch issueScratch = makeIssueScratch();

        Issue subject = new Issue(issueScratch, null, null, null, null, null, null, null, null, null, null);

        Field[] declaredFields = IssueScratch.class.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            assertEquals(field.get(issueScratch), field.get(subject));
        }
    }
    
    private IssueScratch makeIssueScratch(Long ...teamId)  {
        LinkedList<Long> assignedTeamsIds = new LinkedList<>(Arrays.asList(teamId));
        
        IssueScratch issueScratch = new IssueScratch(
                66l,
                "K-66",
                "K",
                "A project",
                1l,
                "summary",
                2l,
                3000l,
                "K", 
                Arrays.asList("x","y"), 
                Arrays.asList(new User("subresponsaveis")), 
                new User("assignee"),
                13l,
                new Date(),
                6000l,
                new Date(),
                "description",
                "comments",
                Arrays.asList("labels"),
                Arrays.asList("components"),
                false,
                "lastBlockReason",
                new LinkedHashMap<>(),
                new CustomField("id", 1.0),
                new TaskboardTimeTracking(),
                "reporter",
                new CustomField("classOfService", null),
                "releaseId",
                new LinkedList<>(),
                new LinkedList<>(),
                assignedTeamsIds
                );

        Issue subject = new Issue(issueScratch, null, null, null, null, null, null, null, null, null, null);

        Field[] declaredFields = IssueScratch.class.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            try {
                assertEquals(field.get(issueScratch), field.get(subject));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return issueScratch;
    }
    
    @Test
    public void addTeam_IfAddingATeamThatIsNotTheDefault_MakeSureTheDefaultIsIncludedInTheNextResult() {
        IssueTeamService is = Mockito.mock(IssueTeamService.class);
        Mockito.when(is.getDefaultTeamId(Mockito.any())).thenReturn(1L);
        Issue subject = new Issue(makeIssueScratch(), null, null, is, null, null, null, null, null, null, null);
        Team teamToAdd = new Team();
        teamToAdd.setId(13L);
        subject.addTeam(teamToAdd );
        assertEquals("1,13", subject.getRawAssignedTeamsIds().stream().sorted().map(s->""+s).collect(Collectors.joining(",")));
    }
    
    @Test
    public void addTeam_IfAddingATeamThatIsNotTheDefault_MakeSureTheParentTeamIsIncludedInTheNextResult() {
        IssueTeamService is = Mockito.mock(IssueTeamService.class);
        Mockito.when(is.getDefaultTeamId(Mockito.any())).thenReturn(1L);
        Issue subject = new Issue(makeIssueScratch(), null, null, is, null, null, null, null, null, null, null);
        Issue parent = new Issue(makeIssueScratch(37L), null, null, is, null, null, null, null, null, null, null);
        subject.setParentCard(parent);
        Team teamToAdd = new Team();
        teamToAdd.setId(13L);
        subject.addTeam(teamToAdd);
        assertEquals("37,13", subject.getRawAssignedTeamsIds().stream().map(s->""+s).collect(Collectors.joining(",")));
    }
    
    @Test
    public void replaceTeam_ShouldReplaceAnExistingTeamByAnother() {
        IssueTeamService is = Mockito.mock(IssueTeamService.class);
        Mockito.when(is.getDefaultTeamId(Mockito.any())).thenReturn(1L);
        Issue subject = new Issue(makeIssueScratch(), null, null, is, null, null, null, null, null, null, null);
        Team replacementTeam = new Team();
        replacementTeam.setId(13L);
        Team teamToReplace = new Team();
        teamToReplace.setId(1L);
        subject.replaceTeam(Optional.of(teamToReplace), replacementTeam);
        
        assertEquals("13", subject.getRawAssignedTeamsIds().stream().sorted().map(s->""+s).collect(Collectors.joining(",")));
    }

    @Test
    public void addTeamThatIsNotTheDefault_ShouldNotReaddTheDefaul() {
        IssueTeamService is = Mockito.mock(IssueTeamService.class);
        Mockito.when(is.getDefaultTeamId(Mockito.any())).thenReturn(1L);
        Issue subject = new Issue(makeIssueScratch(), null, null, is, null, null, null, null, null, null, null);
        Team replacementTeam = new Team();
        replacementTeam.setId(13L);
        Team teamToReplace = new Team();
        teamToReplace.setId(1L);
        subject.replaceTeam(Optional.of(teamToReplace), replacementTeam);
        
        Team anotherTeam = new Team();
        anotherTeam.setId(3L);
        subject.addTeam(anotherTeam );
        
        assertEquals("3,13", subject.getRawAssignedTeamsIds().stream().sorted().map(s->""+s).collect(Collectors.joining(",")));
    }

    @Test
    public void whenGetTeams_ShouldReturnTeamNamesForIdsInTeamCustomField(){
        IssueTeamService is = Mockito.mock(IssueTeamService.class);
        when(is.getTeamsForIds(Arrays.asList(37L))).thenReturn(Sets.newSet(new CardTeam("bravo1337", 37L)));

        Issue subject = new Issue(makeIssueScratch(37L), null, null, is, null, null, null, null, null, null, null);

        List<String> issueTeams = toStringSet(subject.getTeams());
        assertEquals("bravo1337", StringUtils.join(issueTeams));
    }

    @Test
    public void whenIssueHasNoTeamAndNoParent_ShouldReturnDefaultProjectTeam(){
        IssueTeamService is = Mockito.mock(IssueTeamService.class);
        when(is.getDefaultTeam(Mockito.any())).thenReturn(new CardTeam("default project team", 1l));
        Issue subject = new Issue(makeIssueScratch(), null, null, is, null, null, null, null, null, null, null);

        List<String> issueTeams = toStringSet(subject.getTeams());
        assertEquals("default project team", StringUtils.join(issueTeams));
    }

    @Test
    public void whenIssueHasNoTeamAndHasParent_ShouldReturnParentTeam(){
        IssueTeamService is = Mockito.mock(IssueTeamService.class);
        when(is.getTeamsForIds(Arrays.asList(37L))).thenReturn(Sets.newSet(new CardTeam("bravo1337", 37L)));

        Issue subject = new Issue(makeIssueScratch(), null, null, is, null, null, null, null, null, null, null);
        Issue parent = new Issue(makeIssueScratch(37L), null, null, is, null, null, null, null, null, null, null);
        subject.setParentCard(parent);

        List<String> issueTeams = toStringSet(subject.getTeams());
        assertEquals("bravo1337", StringUtils.join(issueTeams));
    }

    @Test
    public void whenIssueHasNoTeamAndHasParentWithoutTeam_ShouldReturnDefaultTeam(){
        IssueTeamService is = Mockito.mock(IssueTeamService.class);
        when(is.getDefaultTeam(Mockito.any())).thenReturn(new CardTeam("default project team", 1l));

        Issue subject = new Issue(makeIssueScratch(), null, null, is, null, null, null, null, null, null, null);
        Issue parent = new Issue(makeIssueScratch(), null, null, is, null, null, null, null, null, null, null);
        subject.setParentCard(parent);

        List<String> issueTeams = toStringSet(subject.getTeams());
        assertEquals("default project team", StringUtils.join(issueTeams));
    }

    private List<String> toStringSet(Set<Issue.CardTeam> s) {
        return s.stream().map(c->c.name).sorted().collect(Collectors.toList());
    }
}