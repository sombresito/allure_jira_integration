package ru.iopump.qa.allure.service;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.iopump.qa.allure.properties.JiraProperties;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class JiraServiceTest {
    @Test
    public void addCommentBuildsRequest() {
        RestTemplate template = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(template);
        JiraProperties props = new JiraProperties();
        props.setApiUrl("http://jira");
        props.setApiToken("token");
        JiraService service = new JiraService(template, props);

        server.expect(requestTo("http://jira/rest/api/2/issue/KEY-1/comment"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(content().json("{\"body\":\"hello\"}"))
                .andRespond(withSuccess());

        service.addComment("KEY-1", "hello");
        server.verify();
    }
}
