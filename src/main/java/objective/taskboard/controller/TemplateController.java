package objective.taskboard.controller;

import static objective.taskboard.repository.PermissionRepository.ADMINISTRATIVE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import objective.taskboard.auth.Authorizer;
import objective.taskboard.followup.FollowUpFacade;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private FollowUpFacade followUpFacade;

    @Autowired
    private Authorizer authorizer;

    @RequestMapping
    public List<TemplateData> get() {
        if (!authorizer.hasPermissionInAnyProject(ADMINISTRATIVE))
            return followUpFacade.getTemplatesForCurrentUser();
        return followUpFacade.getTemplates();
    }

    @RequestMapping(method = RequestMethod.POST, consumes="multipart/form-data")
    public void upload(@RequestParam("file") MultipartFile file
            , @RequestParam("name") String templateName
            , @RequestParam("roles") List<String> roles) throws IOException {

        if (!authorizer.hasPermissionInAnyProject(ADMINISTRATIVE))
            throw new ResourceNotFoundException();

        followUpFacade.createTemplate(templateName, roles, file);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.PUT, consumes="multipart/form-data")
    public void update(@PathVariable("id") Long id
            , @RequestParam("file") Optional<MultipartFile > file
            , @RequestParam("name") String templateName
            , @RequestParam("roles") List<String> roles) throws IOException {

        if (!authorizer.hasPermissionInAnyProject(ADMINISTRATIVE))
            throw new ResourceNotFoundException();

        followUpFacade.updateTemplate(id, templateName, roles, file);
    }

    @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
    public void delete(@PathVariable("id") Long id) throws IOException {
        if (!authorizer.hasPermissionInAnyProject(ADMINISTRATIVE))
            throw new ResourceNotFoundException();

        followUpFacade.deleteTemplate(id);
    }

    @RequestMapping("{id}")
    public ResponseEntity<Object> downloadSavedTemplate(@PathVariable("id") Long id) {
        if (!authorizer.hasPermissionInAnyProject(ADMINISTRATIVE))
            return new ResponseEntity<>("Template not found.", NOT_FOUND);

        try {
            Optional<TemplateData> template = followUpFacade.getTemplate(id);
            if (!template.isPresent())
                return new ResponseEntity<>("Template not found.", NOT_FOUND);

            String templateName = template.get().name;
            Resource resource = followUpFacade.getTemplateResource(templateName);
            return ResponseEntity.ok()
                      .contentLength(resource.contentLength())
                      .header("Content-Disposition","attachment; filename=" + templateName + "-followup-template.xlsm")
                      .body(resource);
        } catch (Exception e) {
            log.warn("Error while serving Template", e);
            return new ResponseEntity<>(e.getMessage() == null ? e.toString() : e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

}
