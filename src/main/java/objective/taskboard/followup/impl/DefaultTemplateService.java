package objective.taskboard.followup.impl;

/*-
 * [LICENSE]
 * Taskboard
 * - - -
 * Copyright (C) 2015 - 2016 Objective Solutions
 * - - -
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * [/LICENSE]
 */

import objective.taskboard.followup.TemplateService;
import objective.taskboard.followup.data.Template;
import objective.taskboard.repository.TemplateRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
