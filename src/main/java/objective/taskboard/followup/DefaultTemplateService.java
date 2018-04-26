package objective.taskboard.followup;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import objective.taskboard.followup.data.Template;
import objective.taskboard.repository.TemplateRepository;

@Service
public class DefaultTemplateService implements TemplateService {

    @Autowired
    private TemplateRepository templateRepository;

    @Override
    public void saveTemplate(String templateName, List<String> roles, String path) {
        Template template = new Template();
        template.setName(templateName);
        template.setPath(path);
        template.setRoles(roles);

        templateRepository.save(template);
    }

    @Override
    public void updateTemplate(Long id, String templateName, List<String> roles, String path) {
        Template template = templateRepository.findById(id);
        template.setName(templateName);
        template.setRoles(roles);

        if (path != null)
            template.setPath(path);

        Template templateWithSameName = getTemplate(template.getName());
        if (templateWithSameName != null && !templateWithSameName.getId().equals(template.getId()))
            throw new RuntimeException("This template name is already in use");

        templateRepository.save(template);
    }

    @Override
    public void deleteTemplate(Long id) {
        Template template = new Template();
        template.setId(id);
        templateRepository.delete(template);
    }

    @Override
    public List<Template> getTemplates() {
        return templateRepository.findAll();
    }

    @Override
    public Template getTemplate(String templateName) {
        return templateRepository.findByName(templateName);
    }

    @Override
    public Template getTemplate(Long id) {
        return templateRepository.findById(id);
    }

}
