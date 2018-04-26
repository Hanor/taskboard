/*-
 * [LICENSE]
 * Taskboard
 * ---
 * Copyright (C) 2015 - 2017 Objective Solutions
 * ---
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

package objective.taskboard.utils;

import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.size;
import static objective.taskboard.followup.FollowUpDataRepositoryByFile.EXTENSION_JSON;
import static objective.taskboard.followup.FollowUpDataRepositoryByFile.EXTENSION_ZIP;
import static objective.taskboard.utils.IOUtilities.ENCODE_UTF_8;
import static objective.taskboard.utils.IOUtilities.asResource;
import static objective.taskboard.utils.ZipUtils.unzip;
import static objective.taskboard.utils.ZipUtils.zip;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ZipUtilsTest {

    @Test
    public void whenZipAndUnzipFile_BothShouldBeGenerated() throws IOException, URISyntaxException {
        Path source = Paths.get(getClass().getResource("zipUtilsTest.json").toURI());
        Path destiny = Paths.get("zipAndUnzipFile.zip");
        Path assertDir = Paths.get("zipAndUnzipFile");

        try {
            zip(source, destiny);

            assertTrue("Zip file should be generated", exists(destiny));
            assertThat(size(destiny), greaterThan(0L));

            unzip(destiny.toFile(), assertDir);

            File file = assertDir.resolve(source.getFileName()).toFile();
            assertTrue("JSON file should be generated", file.exists());

            String actual = IOUtils.toString(asResource(file).getInputStream(), ENCODE_UTF_8);
            String expected = IOUtils.toString(asResource(source).getInputStream(), ENCODE_UTF_8);

            assertEquals("JSON", expected, actual);
        } finally {
            deleteQuietly(destiny.toFile());
            deleteQuietly(assertDir.toFile());
        }
    }

    @Test
    public void whenZipAndUnzipFolder_BothShouldBeGenerated() throws IOException {
        Path source = createTempDirectory("zipAndUnzipFolder");
        Path sourceFile = createTempFile(source, "zipAndUnzipFolder", EXTENSION_JSON);
        Path destiny = createTempFile("zipAndUnzipFolder", EXTENSION_ZIP);
        Path assertDir = createTempDirectory("zipAndUnzipFolderAssert");

        try {
            zip(source, destiny);

            assertTrue("Zip file should be generated", exists(destiny));
            assertThat(size(destiny), greaterThan(0L));

            unzip(destiny.toFile(), assertDir);

            File file = assertDir.resolve(sourceFile.getFileName()).toFile();
            assertTrue("JSON file should be generated", file.exists());

            String actual = IOUtils.toString(asResource(file).getInputStream(), ENCODE_UTF_8);
            String expected = IOUtils.toString(asResource(sourceFile).getInputStream(), ENCODE_UTF_8);

            assertEquals("JSON", expected, actual);
        } finally {
            deleteQuietly(source.toFile());
            deleteQuietly(destiny.toFile());
            deleteQuietly(assertDir.toFile());
        }
    }

    @Test
    public void whenUnzipAndZipXLSM_BothShouldBeGenerated() throws IOException, URISyntaxException {
        Path source = Paths.get(getClass().getResource("zipUtilsTest.xlsm").toURI());
        Path destiny = createTempDirectory("unzipAndZipXLSM");
        Path assertXLSM = createTempFile("unzipAndZipXLSM", ".xlsm");

        try {
            unzip(source.toFile(), destiny);

            File file = destiny.resolve("[Content_Types].xml").toFile();
            assertTrue("XML file should be generated", file.exists());
            Path folderXl = destiny.resolve("xl");
            assertTrue("Folder Xl should be generated", exists(folderXl));

            zip(destiny, assertXLSM);

            assertTrue("XLSM file should be generated", exists(destiny));
            assertThat(size(assertXLSM), greaterThan(0L));
        } finally {
            deleteQuietly(destiny.toFile());
            deleteQuietly(assertXLSM.toFile());
        }
    }

    @Test
    public void whenZipFolderNonexistent_ShouldThrowAnUncheckedIOException() throws IOException {
        Path source = Paths.get("sourceNonexistent");
        Path destiny = Paths.get("destinyNonexistent");
        try {
            zip(source, destiny);
            fail("Should have thrown UncheckedIOException");
        } catch (UncheckedIOException e) {
            assertEquals("Exception message", "java.nio.file.NoSuchFileException: sourceNonexistent", e.getMessage());
        } finally {
            deleteQuietly(destiny.toFile());
        }
    }

    @Test
    public void whenUnzipFileNonexistent_ShouldThrowAFileNotFoundException() throws IOException {
        Path source = Paths.get("sourceNonexistent.zip");
        Path destiny = Paths.get("destinyNonexistent");
        try {
            unzip(source.toFile(), destiny);
            fail("Should have thrown FileNotFoundException");
        } catch (FileNotFoundException e) {
            assertNotNull("Exception shouldn't be null", e);
        } finally {
            deleteQuietly(destiny.toFile());
        }
    }

    @Test
    public void whenZipToAnExistentFolder_ShouldThrowAnUncheckedIOException() throws IOException, URISyntaxException {
        Path source = Paths.get(getClass().getResource("zipUtilsTest.json").toURI());
        Path destiny = createTempDirectory("folder");
        try {
            zip(source, destiny);
            fail("Should have thrown UncheckedIOException");
        } catch (UncheckedIOException e) {
            assertNotNull("Exception shouldn't be null", e);
        } finally {
            deleteQuietly(destiny.toFile());
        }
    }

    @Test
    public void whenUnzipToAnExistentFile_ShouldThrowAFileAlreadyExistsException() throws IOException, URISyntaxException {
        Path source = Paths.get(getClass().getResource("zipUtilsTest.xlsm").toURI());
        Path destiny = createTempFile("file", EXTENSION_ZIP);
        try {
            unzip(source.toFile(), destiny);
            fail("Should have thrown RunTimeException");
        } catch (RuntimeException e) {
            assertEquals("Exception message", "Output must be a directory", e.getMessage());
        } finally {
            deleteQuietly(destiny.toFile());
        }
    }

    @Test
    public void whenZipAndUnzipToADestinyWithNonexistentParentFolder_BothShouldBeGenerated() throws IOException, URISyntaxException {
        Path source = Paths.get(getClass().getResource("zipUtilsTest.json").toURI());
        Path destiny = Paths.get("parent", "zipAndUnzipFile.zip");
        Path assertDir = Paths.get("parentAssert", "zipAndUnzipFile");

        try {
            zip(source, destiny);

            assertTrue("Zip file should be generated", exists(destiny));
            assertThat(size(destiny), greaterThan(0L));

            unzip(destiny.toFile(), assertDir);

            File file = assertDir.resolve(source.getFileName()).toFile();
            assertTrue("JSON file should be generated", file.exists());

            String actual = IOUtils.toString(asResource(file).getInputStream(), ENCODE_UTF_8);
            String expected = IOUtils.toString(asResource(source).getInputStream(), ENCODE_UTF_8);

            assertEquals("JSON", expected, actual);
        } finally {
            deleteQuietly(destiny.getParent().toFile());
            deleteQuietly(assertDir.getParent().toFile());
        }
    }

}
