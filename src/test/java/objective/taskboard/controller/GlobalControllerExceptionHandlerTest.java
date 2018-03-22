package objective.taskboard.controller;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import objective.taskboard.controller.GlobalControllerExceptionHandlerTest.TestController;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GlobalControllerExceptionHandler.class, TestController.class})
@WebAppConfiguration
public class GlobalControllerExceptionHandlerTest {

    private MockMvc mockMvc;

    @Autowired
    private TestController testController;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(testController)
                .setControllerAdvice(new GlobalControllerExceptionHandler())
                .build();
    }

    @Test
    public void whenExceptionOccurs_returnUnexpectedBehavior() throws Exception {
        Matcher<String> matcher = endsWith("Unexpected behavior. Please, report this code to the administrator.");

        mockMvc.perform(get("/error-generator/internal-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value(matcher));
    }

    @Test
    public void whenTheControllerReturnAResponseEntity_dontExecuteControllerAdvice() throws Exception {
        Matcher<String> matcher = endsWith("This error doesn't touch the controller advice.");

        mockMvc.perform(get("/error-generator/not-execute-controller-advice"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value(matcher));
    }

    @RestController
    @RequestMapping("/error-generator")
    public static class TestController {

        @RequestMapping("/internal-error")
        public ResponseEntity<Object> generateError() {
            throw new NullPointerException();
        }

        @RequestMapping("/not-execute-controller-advice")
        public ResponseEntity<Object> generateErrorThatNotExecuteControllerAdvice() {
            try {
                throw new NullPointerException();
            } catch (Exception e) {
                return new ResponseEntity<>("This error doesn't touch the controller advice.", INTERNAL_SERVER_ERROR);
            }
        }

    }

}