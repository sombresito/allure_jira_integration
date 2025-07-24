package ru.iopump.qa.allure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.iopump.qa.allure.properties.AllureProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JpaReportServiceTest {
    @Test
    public void extractIssueKeysFindsLinks() throws Exception {
        Path dir = Files.createTempDirectory("res");
        String json = "{\"links\":[{\"url\":\"https://jira.example.com/browse/PROJ-123\"}]}";
        Files.writeString(dir.resolve("a-result.json"), json);
        AllureProperties props = new AllureProperties(new AllureProperties.Reports(), "", false, "", null);
        JpaReportService service = new JpaReportService(props, new ObjectMapper(), null, null, null, null, null);
        List<String> keys = service.extractIssueKeys(List.of(dir));
        Assertions.assertThat(keys).containsExactly("PROJ-123");
    }
}
