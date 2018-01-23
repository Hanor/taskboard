package objective.taskboard.jira.client;

import static objective.taskboard.utils.IOUtilities.resourceAsString;
import static org.apache.tomcat.util.buf.StringUtils.join;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

public class JiraSearchResultJsonParserTest {
    @Test
	public void happyDay() throws JSONException {
        String searchSample = resourceAsString(JiraSearchResultJsonParserTest.class.getResourceAsStream("search_parseSubject.json"));
	    JiraSearchResultJsonParser subject = new JiraSearchResultJsonParser();
	    
	    JiraIssueDtoSearch result = subject.parse(searchSample);
	    
	    assertEquals(100, result.getMaxResults());
	    assertEquals(0, result.getStartAt());
	    assertEquals(2, result.getTotal());
	    
	    JiraIssueDto resultIssue = result.getIssues().get(0);
	    JiraUserDto assignee = resultIssue.getAssignee();
	    assertEquals("jean.takano", assignee.getName());
	    assertEquals("http://www.gravatar.com/avatar/6a9dde9b22771e019aa288794f8013c1?d=mm&s=24", assignee.getAvatarUri("24x24").toASCIIString());
	    assertEquals("TASKB-685", resultIssue.getKey());
	    assertEquals("2017-06-02T14:14:10.000Z", resultIssue.getCreationDate().toString()); 
	    assertEquals("Some issue", resultIssue.getDescription());
	    assertEquals(new Long(226015), resultIssue.getId());
	    assertEquals("2017-06-02T14:20:49.000Z", resultIssue.getUpdateDate().toString());
	    assertEquals("teste", resultIssue.getSummary());
	    
	    JiraUserDto reporter = resultIssue.getReporter();
	    assertEquals("jean.takano", reporter.getName());
	    assertEquals("Jean Takano", reporter.getDisplayName());
	    assertEquals("http://54.68.128.117:8100/rest/api/2/user?username=jean.takano", reporter.getSelf());
	    
	    JiraTimeTrackingDto timeTracking = resultIssue.getTimeTracking();
	    assertEquals((Integer)1483, timeTracking.getOriginalEstimateMinutes());
	    assertEquals((Integer)1650, timeTracking.getTimeSpentMinutes());
	    
	    ChangelogGroupDto changelogGroupDto = resultIssue.getChangelog().get(0);
	    assertEquals("jean.takano", changelogGroupDto.getAuthor().getName());
	    assertNotNull(changelogGroupDto.getCreated());
	    ChangelogItemDto changelogItemDto = changelogGroupDto.getItems().get(0);
	    assertEquals("status", changelogItemDto.getField());
	    assertEquals("10052", changelogItemDto.getTo());
	    assertEquals("Open", changelogItemDto.getFromString());
	    assertEquals("To Do", changelogItemDto.getToString());
	    
	    JiraProjectDto project = resultIssue.getProject();
	    assertEquals("TASKB", project.getKey());
	    assertEquals("Taskboard", project.getName());
	    
	    JiraStatusDto status = resultIssue.getStatus();
	    assertEquals(10657, status.getId());
	    
	    JiraIssueTypeDto issueType = resultIssue.getIssueType();
	    assertEquals(new Long(11), issueType.getId());
	    assertEquals("Task", issueType.getName());
	    assertEquals("http://54.68.128.117:8100/secure/viewavatar?size=xsmall&avatarId=12262&avatarType=issuetype", issueType.getIconUri().toASCIIString());
	    
	    JiraPriorityDto priority = resultIssue.getPriority();
	    assertEquals(9, priority.getId());
	    
	    assertEquals("blue,black,rose",join(resultIssue.getLabels()));
	    
	    JiraLinkDto jiraLink1 = resultIssue.getIssueLinks().get(0);
	    assertEquals("TASKB-1019", jiraLink1.getTargetIssueKey());
	    JiraIssueLinkTypeDto issueLinkType = jiraLink1.getIssueLinkType();
	    assertEquals("is demanded by", issueLinkType.getDescription());
	    assertEquals(JiraIssueLinkTypeDto.Direction.INBOUND, issueLinkType.getDirection());
	    
	    JiraLinkDto jiraLink2 = resultIssue.getIssueLinks().get(1);
        assertEquals("TASKB-1020", jiraLink2.getTargetIssueKey());
        JiraIssueLinkTypeDto issueLinkType2 = jiraLink2.getIssueLinkType();
        assertEquals("demands", issueLinkType2.getDescription());
        assertEquals(JiraIssueLinkTypeDto.Direction.OUTBOUND, issueLinkType2.getDirection());
        
        JiraIssueFieldDto field = resultIssue.getField("customfield_11440");
        JSONObject fieldValue = (JSONObject) field.getValue();
        assertEquals("Standard", fieldValue.get("value"));
        
        assertEquals("Last Block Reason", resultIssue.getField("customfield_11452").getValue());
	    
	    /*
        public List<JiraCommentDto> getComments() {
        public List<JiraComponentDto> getComponents() {
        public List<JiraSubtaskDto> getSubtasks() {
	     */
	}
}