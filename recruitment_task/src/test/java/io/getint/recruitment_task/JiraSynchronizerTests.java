package io.getint.recruitment_task;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class JiraSynchronizerTests {

    private JiraSynchronizer jiraSynchronizer;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Before
    public void setup() {
        String jiraUrl = "http://localhost:8080";
        String username = "user";
        String apiToken = "token";
        jiraSynchronizer = new JiraSynchronizer(jiraUrl, username, apiToken);
    }

    @Test
    public void shouldSyncTasks() throws IOException {
        stubFor(get(urlPathEqualTo("/rest/api/2/search"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"issues\": [{\"key\": \"TEST-1\", \"fields\": {\"summary\": \"Test Summary\", \"description\": \"Test Description\", \"priority\": {\"id\": \"1\"}}}]}")));

        stubFor(post(urlPathEqualTo("/rest/api/2/issue"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"key\": \"NEW-1\"}")));

        stubFor(get(urlPathMatching("/rest/api/2/issue/TEST-1/comment"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"comments\": []}")));

        jiraSynchronizer.moveTasksToOtherProject("TEST", "NEW");

        verify(postRequestedFor(urlPathEqualTo("/rest/api/2/issue"))
                .withRequestBody(matchingJsonPath("$.fields.summary", equalTo("Test Summary")))
                .withRequestBody(matchingJsonPath("$.fields.description", equalTo("Test Description")))
                .withRequestBody(matchingJsonPath("$.fields.priority.id", equalTo("1"))));
    }
}
