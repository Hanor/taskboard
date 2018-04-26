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

import static objective.taskboard.utils.ZipUtils.unzip;
import static objective.taskboard.utils.ZipUtils.zip;
import static org.apache.commons.io.FileUtils.deleteQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import objective.taskboard.database.directory.DataBaseDirectory;
import objective.taskboard.utils.IOUtilities;

@Service
public class FollowUpTemplateStorage implements FollowUpTemplateStorageInterface {

    @Autowired
    private DataBaseDirectory dataBaseDirectory;

    @Override
    public FollowUpTemplate getTemplate(String path) {
        Path templatePath = getTemplateRoot().resolve(path);
        return new FollowUpTemplate(resolve(templatePath, "Followup-template.xlsm"));
    }

    @Override
    public String storeTemplate(File template, FollowUpTemplateValidator validator) throws IOException {
        return storeTemplate(new FileInputStream(template), validator);
    }

    @Override
    public String storeTemplate(InputStream stream, FollowUpTemplateValidator validator) throws IOException {
        if(!getTemplateRoot().toFile().exists())
            Files.createDirectories(getTemplateRoot());

        Path pathFollowup = Files.createTempDirectory(getTemplateRoot(), "Followup");
        Path tempFolder = pathFollowup.resolve("temp");
        unzip(stream, tempFolder);
        try {
            validator.validate(tempFolder);
            zip(tempFolder, pathFollowup.resolve("Followup-template.xlsm"));
            deleteQuietly(tempFolder.toFile());
        } catch (Exception e) {
            deleteQuietly(pathFollowup.toFile());
            throw e;
        }
        return getTemplateRoot().relativize(pathFollowup).toString();
    }

    @Override
    public void deleteFile(String templatePath) {
        Path template = getTemplateRoot().resolve(templatePath);
        deleteQuietly(template.toFile());
    }

    private Path getTemplateRoot() {
        return dataBaseDirectory.path("followup-templates");
    }

    private Resource resolve(Path templatePath, String relativePath) {
        return IOUtilities.asResource(templatePath.resolve(relativePath));
    }

}
