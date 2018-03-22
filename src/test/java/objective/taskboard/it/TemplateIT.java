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
package objective.taskboard.it;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import objective.taskboard.utils.ZipUtils;

public class TemplateIT extends AbstractIntegrationTest {

    private HttpClient client;
    private Header[] session;

    @Before
    public void login() throws IOException, URISyntaxException {
        client = HttpClientBuilder.create().build();
        session = doLogin(client);
    }

    @AfterClass
    public static void cleanupTemplates() {
        File tempDir = FileUtils.getTempDirectory();
        final Pattern tempFilePattern = Pattern.compile("(sheet-template.*\\.xml|shared-strings.*\\.xml|Followup.*\\.xlsm)");
        String[] tempFiles = tempDir.list((dir, name) -> tempFilePattern.matcher(name).matches());
        for (String tempFile : tempFiles) {
            FileUtils.deleteQuietly(new File(tempDir, tempFile));
        }
    }

    @Test
    public void uploadOkTemplate() throws IOException, URISyntaxException {
        HttpResponse response = uploadTemplate(okTemplate());
        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }

    @Test
    public void uploadNotOkTemplate() throws URISyntaxException, IOException, JSONException {
        HttpResponse response = uploadTemplate(notOkTemplate());
        assertThat(response.getStatusLine().getStatusCode(), not(200));
        assertEquals("Invalid template, Worksheet \"From Jira\" should be empty", extractMessage(response));
    }

    @Test
    public void uploadOkTemplateWithoutSharedStrings() throws IOException, URISyntaxException, JSONException {
        HttpResponse response = uploadTemplate(okTemplateWithoutSharedStrings());
        assertThat(response.getStatusLine().getStatusCode(), not(200));
        assertEquals("Invalid file, could not find path \"xl/sharedStrings.xml\" within template", extractMessage(response));
    }

    @Test
    public void uploadCorruptedFile() throws IOException, URISyntaxException, JSONException {
        HttpResponse response = uploadTemplate(corruptedFile());
        assertThat(response.getStatusLine().getStatusCode(), not(200));
        assertEquals("Invalid file, seems to be corrupted", extractMessage(response));
    }

    @Test
    public void uploadImage() throws IOException, URISyntaxException, JSONException {
        HttpResponse response = uploadTemplate(favicon());
        assertThat(response.getStatusLine().getStatusCode(), not(200));
        assertEquals("Invalid file, cannot be used as template", extractMessage(response));
    }

    private static Header[] doLogin(HttpClient client) throws URISyntaxException, IOException {
        HttpPost post = new HttpPost();
        post.setURI(new URI("http://localhost:8900/login"));

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("username", "bla"));
        params.add(new BasicNameValuePair("password", "bla"));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);

        post.setEntity(entity);

        return client.execute(post).getHeaders("Set-Cookie");
    }

    private HttpResponse uploadTemplate(File file) throws URISyntaxException, IOException {
        FileBody fileBody = new FileBody(file, ContentType.DEFAULT_BINARY);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        StringBody templateName = new StringBody(file.getName(), ContentType.MULTIPART_FORM_DATA);
        StringBody projects = new StringBody("PROJ1", ContentType.MULTIPART_FORM_DATA);
        builder.addPart("file", fileBody);
        builder.addPart("name", templateName);
        builder.addPart("projects", projects);
        HttpEntity entity = builder.build();

        HttpPost post = new HttpPost();
        post.setURI(new URI("http://localhost:8900/api/templates"));
        post.setHeaders(session);
        post.setEntity(entity);

        return client.execute(post);
    }

    private static File okTemplate() throws URISyntaxException {
        return new File(TemplateIT.class.getResource("/objective/taskboard/followup/OkFollowupTemplate.xlsm").toURI());
    }

    private static File notOkTemplate() throws URISyntaxException {
        return new File(TemplateIT.class.getResource("/objective/taskboard/followup/NotOkFollowupTemplate.xlsm").toURI());
    }

    private static File okTemplateWithoutSharedStrings() throws IOException, URISyntaxException {
        Path temp = Files.createTempFile("Followup", ".xlsm");
        ZipUtils.zip(
                ZipUtils.stream(okTemplate())
                        .filter(ze -> !StringUtils.endsWith(ze.getName(), "sharedStrings.xml"))
                , temp);
        return temp.toFile();
    }

    private static File corruptedFile() throws IOException, URISyntaxException {
        Path temp = Files.createTempFile("Followup", ".xlsm");
        ZipUtils.zip(
                ZipUtils.stream(okTemplate())
                        .map(ze -> {
                            if (StringUtils.endsWith(ze.getName(), "workbook.xml.rels"))
                                ze.setInputStream(
                                        TemplateIT.class.getResourceAsStream("/objective/taskboard/utils/file.xml")
                                );
                            return ze;
                        })
                , temp);
        return temp.toFile();
    }

    private static File favicon() throws URISyntaxException {
        return new File(TemplateIT.class.getResource("/static/favicon.ico").toURI());
    }

    private String extractMessage(HttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity());
    }
}
