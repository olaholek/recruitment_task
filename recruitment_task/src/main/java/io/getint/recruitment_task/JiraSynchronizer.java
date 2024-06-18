package io.getint.recruitment_task;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class JiraSynchronizer {

    /**
     * Search for 5 tickets in one project, and move them
     * to the other project within same Jira instance.
     * When moving tickets, please move following fields:
     * - summary (title)
     * - description
     * - priority
     * Bonus points for syncing comments.
     */

    private final JiraIssueClient issueClient;

    public JiraSynchronizer(String jiraUrl, String username, String apiToken) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        this.issueClient = new JiraIssueClient(jiraUrl, username, apiToken, httpClient);
    }

    public void moveTasksToOtherProject(String sourceProjectKey, String targetProjectKey) throws IOException {
        JSONArray issues = issueClient.fetchIssues(sourceProjectKey);

        for (int i = 0; i < issues.length(); i++) {
            JSONObject issue = issues.getJSONObject(i);
            issueClient.createIssueInTargetProject(targetProjectKey, issue);
        }
    }
}
