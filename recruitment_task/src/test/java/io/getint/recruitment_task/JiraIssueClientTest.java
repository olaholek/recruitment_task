package io.getint.recruitment_task;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class JiraIssueClientTest {

    private JiraIssueClient jiraIssueClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    public void setup() {
        String jiraUrl = "http://localhost:8080";
        String username = "user";
        String apiToken = "token";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        jiraIssueClient = new JiraIssueClient(jiraUrl, username, apiToken, httpClient);
    }

    @Test
    public void testCreateIssueInTargetProject() throws IOException {
        setup();
        String targetProjectKey = "TARGET_PROJECT";
        JSONObject issue = new JSONObject()
                .put("key", "ISSUE-1")
                .put("fields", new JSONObject()
                        .put("summary", "Test issue")
                        .put("description", "Test description")
                        .put("priority", new JSONObject().put("id", "1"))
                );

        stubFor(post(urlEqualTo("/rest/api/2/issue"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody("{\"key\": \"NEW-123\"}")));

        stubFor(get(urlEqualTo("/rest/api/2/issue/ISSUE-1/comment"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"comments\": [{\"body\": \"Comment 1\"}]}")));

        stubFor(post(urlEqualTo("/rest/api/2/issue/NEW-123/comment"))
                .willReturn(aResponse()
                        .withStatus(201)));

        jiraIssueClient.createIssueInTargetProject(targetProjectKey, issue);

        verify(postRequestedFor(urlEqualTo("/rest/api/2/issue"))
                .withRequestBody(matchingJsonPath("$.fields.summary", equalTo("Test issue")))
                .withRequestBody(matchingJsonPath("$.fields.description", equalTo("Test description"))));

        verify(getRequestedFor(urlEqualTo("/rest/api/2/issue/ISSUE-1/comment")));

        verify(postRequestedFor(urlEqualTo("/rest/api/2/issue/NEW-123/comment"))
                .withRequestBody(matchingJsonPath("$.body", equalTo("Comment 1"))));
    }

    @Test
    public void testFetchIssues() throws IOException {
        setup();

        String projectKey = "TEST";

        stubFor(get(urlEqualTo("/rest/api/2/search?jql=project=TEST&maxResults=5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"issues\": [{\"key\": \"ISSUE-1\"}, {\"key\": \"ISSUE-2\"}]}")));

        JSONArray issues = jiraIssueClient.fetchIssues(projectKey);

        assertEquals(2, issues.length());
        assertEquals("ISSUE-1", issues.getJSONObject(0).getString("key"));
        assertEquals("ISSUE-2", issues.getJSONObject(1).getString("key"));

        verify(getRequestedFor(urlEqualTo("/rest/api/2/search?jql=project=TEST&maxResults=5")));
    }

    @Test
    public void testFetchComments() throws IOException {
        setup();

        String issueKey = "ISSUE-1";

        stubFor(get(urlEqualTo("/rest/api/2/issue/ISSUE-1/comment"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"comments\": [{\"body\": \"Comment 1\"}, {\"body\": \"Comment 2\"}]}")));

        JSONArray comments = jiraIssueClient.fetchComments(issueKey);

        assertEquals(2, comments.length());
        assertEquals("Comment 1", comments.getJSONObject(0).getString("body"));
        assertEquals("Comment 2", comments.getJSONObject(1).getString("body"));

        verify(getRequestedFor(urlEqualTo("/rest/api/2/issue/ISSUE-1/comment")));
    }
}