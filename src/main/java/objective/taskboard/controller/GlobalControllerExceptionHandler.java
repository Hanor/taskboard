package objective.taskboard.controller;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import objective.taskboard.followup.FollowUpTemplateValidator.InvalidTemplateException;

@ControllerAdvice
public class GlobalControllerExceptionHandler extends ResponseEntityExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> exception(Exception ex, WebRequest request) {
        UUID uuid = UUID.randomUUID();

        logError(ex, request, uuid);

        String responseBody = errorMessageFormatter(uuid, "Unexpected behavior. Please, report this code to the administrator.");
        return handleExceptionInternal(ex, responseBody, new HttpHeaders(), INTERNAL_SERVER_ERROR, request);
    }

    private String errorMessageFormatter(UUID uuid, String message) {
        return "Error "+ uuid +": " + message;
    }

    private void logError(Exception ex, WebRequest request, UUID uuid) {
        log.warn("ERROR CODE: " + uuid + " | USER: " + getUserName(request), ex);
    }

    private String getUserName(WebRequest request) {
        return isNotEmpty(request.getRemoteUser()) ? request.getRemoteUser() : "NO USER FOUND";
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<Object> illegalArgumentException(InvalidTemplateException ex, WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = InvalidTemplateException.class)
    public ResponseEntity<Object> invalidTemplateException(InvalidTemplateException ex, WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

}