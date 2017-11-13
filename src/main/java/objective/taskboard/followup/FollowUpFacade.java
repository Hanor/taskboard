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
package objective.taskboard.followup;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import objective.taskboard.controller.TemplateData;
import objective.taskboard.database.directory.DataBaseDirectory;
import objective.taskboard.domain.Project;
import objective.taskboard.followup.data.Template;
import objective.taskboard.followup.impl.FollowUpDataProviderFromCurrentState;
import objective.taskboard.followup.impl.FollowUpDataProviderFromHistory;
import objective.taskboard.issueBuffer.IssueBufferState;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.ProjectService;
import objective.taskboard.spreadsheet.SimpleSpreadsheetEditor;
import objective.taskboard.utils.IOUtilities;

@Service
public class FollowUpFacade implements FollowUpFacadeInterface {

    private final static String SAMPLE_FOLLOWUP_TEMPLATE_PATH = "followup-generic/generic-followup-template.xlsm";

    @Autowired
    private FollowUpTemplateStorageInterface followUpTemplateStorage;

    @Autowired
    private FollowUpDataProviderFromCurrentState providerFromCurrentState;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private Converter<Template, TemplateData> templateConverter;

    @Autowired
    private DataBaseDirectory dataBaseDirectory;

    @Autowired
    private JiraProperties jiraProperties;

    @Override
    public FollowUpGenerator getGenerator(String templateName, Optional<String> date) {
        Template template = templateService.getTemplate(templateName);
        return new FollowUpGenerator(getProvider(date), new SimpleSpreadsheetEditor(followUpTemplateStorage.getTemplate(template.getPath())), jiraProperties);
    }

    @Override
    public FollowupDataProvider getProvider(Optional<String> date) {
        if (!date.isPresent() || date.get().isEmpty())
            return providerFromCurrentState;
        return new FollowUpDataProviderFromHistory(date.get(), dataBaseDirectory);
    }

    @Override
    public IssueBufferState getFollowUpState(Optional<String> date) {
        return getProvider(date).getFollowupState();
    }

    @Override
    public List<TemplateData> getTemplatesForCurrentUser() {
        List<String> projectKeys = projectService.getVisibleProjects()
                .stream()
                .map(Project::getKey)
                .collect(Collectors.toList());

        List<Template> templates = templateService.findTemplatesForProjectKeys(projectKeys);

        return templates
                .stream()
                .map(t -> templateConverter.convert(t))
                .collect(Collectors.toList());
    }

    @Override
    public void createTemplate(String templateName, String projects,
                               MultipartFile file) throws IOException {
        List<String> projectKeys = Arrays.asList(projects.split(","));
        
        if (templateService.getTemplate(templateName) != null)
            throw new RuntimeException("This template name is already in use");
        
        if (templateService.findATemplateOnlyMatchedWithThisProjectKey(projectKeys) != null)
            throw new RuntimeException("This match of projects is already used by other template");
        
        String path = followUpTemplateStorage
                .storeTemplate(file.getInputStream(), new FollowUpTemplateValidator());
        
        templateService.saveTemplate(templateName, projectKeys, path);
    }
    
    public void deleteTemplate(Long id) throws IOException {
        Template template = templateService.getTemplate(id);
        followUpTemplateStorage.deleteFile(template.getPath());
        templateService.deleteTemplate(id);
    }
    
    public void updateTemplate(Long id, String templateName, String projects,
                               Optional<MultipartFile> file) throws IOException {
        String path = null;
        String oldPath = null;
        if (file.isPresent()) {
            oldPath = templateService.getTemplate(id).getPath();
            path = followUpTemplateStorage
                    .storeTemplate(file.get().getInputStream(),
                            new FollowUpTemplateValidator());
        }

        try {
            templateService.updateTemplate(id, templateName, projects, path);
        } catch(Exception t) {
            if(path != null) {
                followUpTemplateStorage.deleteFile(path);
            }
            throw t;
        }
        if(oldPath != null) {
            followUpTemplateStorage.deleteFile(oldPath);
        }
    }

    @Override
    public Resource getGenericTemplate() {
        return IOUtilities.asResource(dataBaseDirectory.path(SAMPLE_FOLLOWUP_TEMPLATE_PATH));
    }
}
