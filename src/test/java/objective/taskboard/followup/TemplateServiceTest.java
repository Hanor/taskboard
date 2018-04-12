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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import objective.taskboard.followup.data.Template;
import objective.taskboard.followup.impl.DefaultTemplateService;
import objective.taskboard.repository.TemplateRepository;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = TemplateServiceTest.Configuration.class)
public class TemplateServiceTest {

    @EntityScan(
            basePackageClasses = {Template.class})
    public static class Configuration {
        @Bean
        public JpaRepositoryFactoryBean<TemplateRepository, Template, Long> templateRepository() {
            return new JpaRepositoryFactoryBean<>(TemplateRepository.class);
        }
        @Bean
        public TemplateService templateService() {
            return new DefaultTemplateService();
        }
    }

    @Autowired
    private TemplateService templateService;

    @Test
    public void givenTemplate_whenGetTemplates_thenReturnOneResult() {
        // given
        repositoryHasTemplate();

        // when
        List<Template> result = templateService.getTemplates();

        // then
        assertThat(result, hasSize(1));
    }

    @Test
    public void givenNoTemplate_whenGetTemplates_thenReturnNoResults() {
        // when
        List<Template> result = templateService.getTemplates();

        // then
        assertThat(result, hasSize(0));
    }

    private void repositoryHasTemplate() {
        String templateName = "Test Template";
        String templatePath = "test-path";
        try {
            templateService.saveTemplate(templateName, asList("Role"), templatePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}