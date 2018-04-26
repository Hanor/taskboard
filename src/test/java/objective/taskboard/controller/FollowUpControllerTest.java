package objective.taskboard.controller;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.followup.FollowUpFacade;
import objective.taskboard.followup.FollowUpHistoryKeeper;
import objective.taskboard.followup.TemplateService;
import objective.taskboard.followup.data.Template;
import objective.taskboard.utils.IOUtilities;

@RunWith(MockitoJUnitRunner.class)
public class FollowUpControllerTest {

    private static final String ZONE_ID = "America/Sao_Paulo";
    private static final String TEMPLATE_NAME = "TEMPLATE_TEST";
    private static final String TEMPLATE_NAME_NONEXISTENT = "TEMPLATE_NONEXISTENT";
    private static final List<String> TEMPLATE_ROLES = asList("ROLE1", "ROLE2");
    private static final String ALLOWED_PROJECT_KEY = "PROJ_ALLOWED1";
    private static final String DISALLOWED_PROJECT_KEY = "PROJ_DISALLOWED1";
    private static final Resource RESOURCE = IOUtilities.asResource(FollowUpControllerTest.class.getClassLoader().getResource("followup/Followup-template.xlsm"));
    private static final String EXPECTED_DOWNLOAD_STRING_BEFORE = "URL [file:";
    private static final String EXPECTED_DOWNLOAD_STRING_AFTER = "/test-classes/followup/Followup-template.xlsm]";

    @Mock
    private FollowUpFacade followUpFacade;

    @Mock
    private FollowUpHistoryKeeper historyKeeper;

    @Mock
    private Authorizer authorizer;

    @Mock
    private TemplateService templateService;

    @Mock
    private Template template;

    @InjectMocks
    private FollowUpController subject;

    @Before
    public void setup() throws IOException {
        when(followUpFacade.getGenericTemplate()).thenReturn(RESOURCE);
        when(followUpFacade.generateReport(eq(TEMPLATE_NAME), any(), any(), eq(ALLOWED_PROJECT_KEY))).thenReturn(RESOURCE);

        when(authorizer.hasAnyRoleInProjects(TEMPLATE_ROLES, asList(ALLOWED_PROJECT_KEY))).thenReturn(true);
        when(template.getRoles()).thenReturn(TEMPLATE_ROLES);
        when(templateService.getTemplate(TEMPLATE_NAME)).thenReturn(template);
    }

    @Test
    public void download_returnFileToDownloadIfAllParamsAreValid() throws IOException {
        ResponseEntity<Object> response = subject.download(ALLOWED_PROJECT_KEY, TEMPLATE_NAME, Optional.empty(), ZONE_ID);
        assertResponse(OK, EXPECTED_DOWNLOAD_STRING_BEFORE, EXPECTED_DOWNLOAD_STRING_AFTER, response);
    }

    @Test
    public void download_projectProjectParamMustExists() throws IOException {
        String projectNotExistsMessage = "You must provide the project";

        ResponseEntity<Object> responseEmpty = subject.download("", TEMPLATE_NAME, Optional.empty(), ZONE_ID);
        assertResponse(HttpStatus.BAD_REQUEST, projectNotExistsMessage, responseEmpty);

        ResponseEntity<Object> responseNull = subject.download(null, TEMPLATE_NAME, Optional.empty(), ZONE_ID);
        assertResponse(BAD_REQUEST, projectNotExistsMessage, responseNull);
    }

    @Test
    public void download_projectTemplateParamMustExists() throws IOException {
        String templateNotExistsMessage = "Template not selected";

        ResponseEntity<Object> responseEmpty = subject.download(ALLOWED_PROJECT_KEY, "", Optional.empty(), ZONE_ID);
        assertResponse(BAD_REQUEST, templateNotExistsMessage, responseEmpty);

        ResponseEntity<Object> responseNull = subject.download(ALLOWED_PROJECT_KEY, null, Optional.empty(), ZONE_ID);
        assertResponse(BAD_REQUEST, templateNotExistsMessage, responseNull);
    }

    @Test
    public void download_shouldCheckTemplateRolesWithAllProjects() throws IOException {
        String matchTemplateRolesWithProjectsMessage = "Template or project doesn't exist";

        ResponseEntity<Object> responseTemplateNonExistent = subject.download(ALLOWED_PROJECT_KEY, TEMPLATE_NAME_NONEXISTENT, Optional.empty(), ZONE_ID);
        assertResponse(HttpStatus.NOT_FOUND, matchTemplateRolesWithProjectsMessage, responseTemplateNonExistent);

        ResponseEntity<Object> responseDisallowed = subject.download(DISALLOWED_PROJECT_KEY, TEMPLATE_NAME, Optional.empty(), ZONE_ID);
        assertResponse(HttpStatus.NOT_FOUND, matchTemplateRolesWithProjectsMessage, responseDisallowed);
    }

    @Test
    public void genericTemplate_returnFileToDownloadIfNoErrorOccur() throws IOException {
        ResponseEntity<Object> response = subject.genericTemplate();
        assertResponse(OK, EXPECTED_DOWNLOAD_STRING_BEFORE, EXPECTED_DOWNLOAD_STRING_AFTER, response);
    }

    @Test
    public void genericTemplate_returnBooleanIfNoErrorOccur() {
        ResponseEntity<Boolean> response = subject.hasGenericTemplate();
        assertResponse(OK, "true", response);
    }

    private void assertResponse(HttpStatus expectedStatus, String expectedBodyString, ResponseEntity<?> response) {
        assertEquals(expectedStatus.value(), response.getStatusCodeValue());
        assertEquals(expectedBodyString, response.getBody().toString());
    }

    private void assertResponse(HttpStatus expectedStatus, String expectedBodyBeforeString, String expectedBodyAfterString, ResponseEntity<?> response) {
        assertEquals(expectedStatus.value(), response.getStatusCodeValue());
        assertTrue(response.getBody().toString().contains(expectedBodyAfterString));
        assertTrue(response.getBody().toString().contains(expectedBodyAfterString));
    }

}
