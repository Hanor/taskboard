package objective.taskboard.it;

import org.apache.commons.io.FileUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Created by herbert on 03/07/17.
 */
public class UploadTemplateIT {

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
        for(String tempFile : tempFiles) {
            FileUtils.deleteQuietly(new File(tempDir, tempFile));
        }
    }

    @Test
    public void uploadOkTemplate() throws IOException, URISyntaxException {
        HttpResponse response = uploadTemplate(okTemplate());
        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }

    @Test
    public void uploadNotOkTemplate() throws URISyntaxException, IOException {
        HttpResponse response = uploadTemplate(notOkTemplate());
        assertThat(response.getStatusLine().getStatusCode(), not(200));
    }

    // ---

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
        HttpPost post = new HttpPost();
        post.setURI(new URI("http://localhost:8900/ws/followup"));
        FileBody fileBody = new FileBody(file, ContentType.DEFAULT_BINARY);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("file", fileBody);
        HttpEntity entity = builder.build();

        post.setHeaders(session);
        post.setEntity(entity);

        return client.execute(post);
    }

    private static File okTemplate() throws URISyntaxException {
        return new File(UploadTemplateIT.class.getResource("/objective/taskboard/followup/OkFollowupTemplate.xlsm").toURI());
    }

    private static File notOkTemplate() throws URISyntaxException {
        return new File(UploadTemplateIT.class.getResource("/objective/taskboard/followup/NotOkFollowupTemplate.xlsm").toURI());
    }

}
