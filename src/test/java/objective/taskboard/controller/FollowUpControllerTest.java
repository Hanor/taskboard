package objective.taskboard.controller;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
import objective.taskboard.followup.FollowUpDataHistoryGenerator;
import objective.taskboard.followup.FollowUpFacade;
import objective.taskboard.followup.FollowUpGenerator;
import objective.taskboard.followup.TemplateService;
import objective.taskboard.followup.data.Template;
import objective.taskboard.utils.IOUtilities;

@RunWith(MockitoJUnitRunner.class)
public class FollowUpControllerTest {

    private static final String ZONE_ID = "America/Sao_Paulo";
    private static final String TEMPLATE_NAME = "TEMPLATE_TEST";
    private static final String TEMPLATE_NAME_NONEXISTENT = "TEMPLATE_NONEXISTENT";
    private static final List<String> TEMPLATE_ROLES = asList("ROLE1", "ROLE2");

    private static final String ALLOWED_PROJECT_KEYS = "PROJ_ALLOWED1,PROJ_ALLOWED2";
    private static final String DISALLOWED_PROJECT_KEYS = "PROJ_DISALLOWED1,PROJ_DISALLOWED2";
    private static final List<String> ALLOWED_PROJECT_KEYS_AS_LIST = asList(ALLOWED_PROJECT_KEYS.split(","));
    private static final String ALLOWED_AND_DISALLOWED_PROJECT_KEYS = ALLOWED_PROJECT_KEYS + "," + DISALLOWED_PROJECT_KEYS;

    private static final Resource RESOURCE = IOUtilities.asResource(FollowUpControllerTest.class.getClassLoader().getResource("followup/Followup-template.xlsm"));
    private static final String EXPECTED_DOWNLOAD_STRING_BEFORE = "URL [file:";
    private static final String EXPECTED_DOWNLOAD_STRING_AFTER = "/test-classes/followup/Followup-template.xlsm]";

    private FollowUpGenerator followupGenerator = mock(FollowUpGenerator.class);

    @Mock
    private FollowUpFacade followUpFacade;

    @Mock
    private FollowUpDataHistoryGenerator followUpDataHistoryGenerator;

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
        when(followUpFacade.getGenerator(any(), any())).thenReturn(followupGenerator);
        when(followUpFacade.getGenericTemplate()).thenReturn(RESOURCE);
        when(followupGenerator.generate(any(), any())).thenReturn(RESOURCE);
        when(authorizer.hasAnyRoleInProjects(TEMPLATE_ROLES, ALLOWED_PROJECT_KEYS_AS_LIST)).thenReturn(true);
        when(template.getRoles()).thenReturn(TEMPLATE_ROLES);
        when(templateService.getTemplate(TEMPLATE_NAME)).thenReturn(template);
    }

    @Test
    public void download_returnFileToDownloadIfAllParamsAreValid() throws IOException {
        ResponseEntity<Object> response = subject.download(ALLOWED_PROJECT_KEYS, TEMPLATE_NAME, Optional.empty(), ZONE_ID);
        assertResponse(OK, EXPECTED_DOWNLOAD_STRING_BEFORE, EXPECTED_DOWNLOAD_STRING_AFTER, response);
    }

    @Test
    public void download_projectProjectParamMustExists() throws IOException {
        String projectNotExistsMessage = "You must provide a list of projects separated by comma";

        ResponseEntity<Object> responseEmpty = subject.download("", TEMPLATE_NAME, Optional.empty(), ZONE_ID);
        assertResponse(BAD_REQUEST, projectNotExistsMessage, responseEmpty);

        ResponseEntity<Object> responseNull = subject.download(null, TEMPLATE_NAME, Optional.empty(), ZONE_ID);
        assertResponse(BAD_REQUEST, projectNotExistsMessage, responseNull);
    }

    @Test
    public void download_projectTemplateParamMustExists() throws IOException {
        String templateNotExistsMessage = "Template not selected";

        ResponseEntity<Object> responseEmpty = subject.download(ALLOWED_PROJECT_KEYS, "", Optional.empty(), ZONE_ID);
        assertResponse(BAD_REQUEST, templateNotExistsMessage, responseEmpty);

        ResponseEntity<Object> responseNull = subject.download(ALLOWED_PROJECT_KEYS, null, Optional.empty(), ZONE_ID);
        assertResponse(BAD_REQUEST, templateNotExistsMessage, responseNull);
    }

    @Test
    public void download_shouldCheckTemplateRolesWithAllProjects() throws IOException {
        String matchTemplateRolesWithProjectsMessage = "Template or some project does not exist";

        ResponseEntity<Object> responseTemplateNonExistent = subject.download(ALLOWED_PROJECT_KEYS, TEMPLATE_NAME_NONEXISTENT, Optional.empty(), ZONE_ID);
        assertResponse(BAD_REQUEST, matchTemplateRolesWithProjectsMessage, responseTemplateNonExistent);

        ResponseEntity<Object> responseDisallowed = subject.download(DISALLOWED_PROJECT_KEYS, TEMPLATE_NAME, Optional.empty(), ZONE_ID);
        assertResponse(BAD_REQUEST, matchTemplateRolesWithProjectsMessage, responseDisallowed);

        ResponseEntity<Object> responseAllowedsAndDisalloweds = subject.download(ALLOWED_AND_DISALLOWED_PROJECT_KEYS, TEMPLATE_NAME, Optional.empty(), ZONE_ID);
        assertResponse(BAD_REQUEST, matchTemplateRolesWithProjectsMessage, responseAllowedsAndDisalloweds);
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
