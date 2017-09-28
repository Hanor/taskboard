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
package objective.taskboard.jira.endpoint;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.util.concurrent.Promise;

import objective.taskboard.jira.JiraClientFactory;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraServiceException;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

@Component
public class JiraEndpoint {

    @Autowired 
    private JiraProperties jiraProperties;

    /**
     * @throws JiraServiceException
     * @throws RuntimeException
     */
    public <T> T executeRequest(String username, String password, Request<T> request) {
        JiraRestClient client = getClient(username, password);

        try {
            return request.execute(client).claim();

        } catch (RestClientException e) {
            throw new JiraServiceException(e);

        } finally {
            closeClient(client);
        }
    }

    private JiraRestClient getClient(String username, String password) {
        JiraClientFactory jiraClient = new JiraClientFactory();
        return jiraClient.getInstance(jiraProperties.getUrl(), username, password);
    }

    private void closeClient(JiraRestClient client) {
        try {
            client.close();
        } catch (Exception e) {}
    }

    public <S> S request(Class<S> service, String username, String password) {
        OkHttpClient client = new OkHttpClient();
        client.setReadTimeout(60, TimeUnit.SECONDS);
        client.setConnectTimeout(60, TimeUnit.SECONDS);
        client.interceptors().add(new AuthenticationInterceptor(username, password));
        RestAdapter retrofit = new RestAdapter.Builder()
                .setEndpoint(jiraProperties.getUrl())
                .setConverter(new GsonConverter(new Gson()))
                .setClient(new OkClient(client))
                .build();

        return retrofit.create(service);
    }

    public class AuthenticationInterceptor implements Interceptor {

        private String authToken;

        public AuthenticationInterceptor(String token) {
            this.authToken = token;
        }

        public AuthenticationInterceptor(String username, String password) {
            this(Credentials.basic(username, password));
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            com.squareup.okhttp.Request original = chain.request();

            com.squareup.okhttp.Request.Builder builder = original.newBuilder()
                    .header("Authorization", authToken);

            com.squareup.okhttp.Request request = builder.build();
            return chain.proceed(request);
        }
    }

    public String postWithRestTemplate(String username, String password, String path, MediaType mediaType, JSONObject jsonRequest) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> request = new HttpEntity<>(jsonRequest.toString(), getAuthorizationRequestHeader(username, password, mediaType));
        String url = jiraProperties.getUrl() + path;
        setConnectionTimeout(restTemplate);
        return restTemplate.postForObject(url, request, String.class);
    }

    public void setConnectionTimeout(RestTemplate restTemplate) {
        ClientHttpRequestFactory rf = restTemplate.getRequestFactory();
        if (rf instanceof SimpleClientHttpRequestFactory) {
            SimpleClientHttpRequestFactory httprf = (SimpleClientHttpRequestFactory)rf;
            httprf.setReadTimeout((int) TimeUnit.SECONDS.toMillis(60));
            httprf.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(60));
            
            return;
        }
        throw new IllegalStateException("SimpleClientHttpRequestFactory of unsupported type " + rf.getClass());
    }

    private HttpHeaders getAuthorizationRequestHeader(String username, String password, MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        String userAndPass = username + ":" + password;
        headers.add("Authorization", "Basic " + new String(Base64.encodeBase64((userAndPass).getBytes())));
        headers.setContentType(mediaType);
        return headers;
    }

    public interface Request<T> {
        Promise<T> execute(JiraRestClient client);
    }
}
