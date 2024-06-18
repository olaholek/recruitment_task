package io.getint.recruitment_task;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Base64;

public class JiraIssueClient {

    private final String jiraUrl;
    private final String username;
    private final String apiToken;
    private final CloseableHttpClient httpClient;

    public JiraIssueClient(String jiraUrl, String username, String apiToken, CloseableHttpClient httpClient) {
        this.jiraUrl = jiraUrl;
        this.username = username;
        this.apiToken = apiToken;
        this.httpClient = httpClient;
    }

    public void createIssueInTargetProject(String targetProjectKey, JSONObject issue) throws IOException {
        String summary = issue.getJSONObject("fields").getString("summary");
        String description = issue.getJSONObject("fields").getString("description");
        String priority = issue.getJSONObject("fields").getJSONObject("priority").getString("id");

        JSONObject newIssue = buildNewIssue(targetProjectKey, summary, description, priority);

        String createUrl = jiraUrl + "/rest/api/2/issue";
        HttpPost createRequest = new HttpPost(createUrl);
        setHeaders(createRequest);
        createRequest.setEntity(new StringEntity(newIssue.toString(), "UTF-8"));

        try (CloseableHttpResponse response = httpClient.execute(createRequest)) {
            String responseString = EntityUtils.toString(response.getEntity());
            JSONObject createdIssue = new JSONObject(responseString);
            String newIssueKey = createdIssue.getString("key");

            JSONArray comments = fetchComments(issue.getString("key"));
            addComments(newIssueKey, comments);
        }
    }

    public JSONArray fetchIssues(String projectKey) throws IOException {
        String searchUrl = jiraUrl + "/rest/api/2/search?jql=project=" + projectKey + "&maxResults=5";
        HttpGet searchRequest = new HttpGet(searchUrl);
        setHeaders(searchRequest);

        try (CloseableHttpResponse response = httpClient.execute(searchRequest)) {
            String responseString = EntityUtils.toString(response.getEntity());
            JSONObject searchJson = new JSONObject(responseString);
            return searchJson.getJSONArray("issues");
        }
    }

    public JSONArray fetchComments(String issueKey) throws IOException {
        String commentsUrl = jiraUrl + "/rest/api/2/issue/" + issueKey + "/comment";
        HttpGet getRequest = new HttpGet(commentsUrl);
        setHeaders(getRequest);

        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            String responseString = EntityUtils.toString(response.getEntity());
            JSONObject commentsJson = new JSONObject(responseString);
            return commentsJson.getJSONArray("comments");
        }
    }

    private JSONObject buildNewIssue(String targetProjectKey, String summary, String description, String priority) {
        return new JSONObject()
                .put("fields", new JSONObject()
                        .put("project", new JSONObject().put("key", targetProjectKey))
                        .put("summary", summary)
                        .put("description", description)
                        .put("priority", new JSONObject().put("id", priority))
                        .put("issuetype", new JSONObject().put("name", "Task")));
    }

    private void addComments(String issueKey, JSONArray comments) throws IOException {
        for (int i = 0; i < comments.length(); i++) {
            JSONObject comment = comments.getJSONObject(i);
            String body = comment.getString("body");

            String addCommentUrl = jiraUrl + "/rest/api/2/issue/" + issueKey + "/comment";
            HttpPost addCommentRequest = new HttpPost(addCommentUrl);
            setHeaders(addCommentRequest);
            JSONObject commentData = new JSONObject().put("body", body);
            addCommentRequest.setEntity(new StringEntity(commentData.toString(), "UTF-8"));

            try (CloseableHttpResponse response = httpClient.execute(addCommentRequest)) {
                EntityUtils.consume(response.getEntity());
            }
        }
    }

    private void setHeaders(HttpGet request) {
        request.setHeader("Authorization", "Basic " + encodeCredentials());
        request.setHeader("Content-Type", "application/json");
    }

    private void setHeaders(HttpPost request) {
        request.setHeader("Authorization", "Basic " + encodeCredentials());
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");
    }

    private String encodeCredentials() {
        String auth = username + ":" + apiToken;
        return Base64.getEncoder().encodeToString(auth.getBytes());
    }
}
