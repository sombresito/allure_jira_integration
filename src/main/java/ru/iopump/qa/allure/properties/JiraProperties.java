package ru.iopump.qa.allure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jira")
public class JiraProperties {
    /**
     * URL API Jira
     */
    private String apiUrl = "http://10.15.132.23:8080/";

    /**
     * Токен авторизации для API Jira
     */
    private String apiToken = "OTk0MjI4MjcyMTA0Oin4cyGKvyXpVJ9VWTB8P9nLTLZg";

    /**
     * Ключ проекта в Jira
     */
    private String projectKey = "QA";

    /**
     * ID типа задачи в Jira
     */
    private String issueTypeId = "10002";

    /**
     * Имя пользователя, который будет назначен исполнителем по умолчанию
     */
    private String defaultAssignee = "KRASIKOS";

    /**
     * Имя пользователя, который будет указан как автор задачи по умолчанию
     */
    private String defaultReporter = "HUBSHARAPIEL";

    /**
     * Ключ системы влияния (customfield_11203)
     */
    private String influenceSystemKey = "IAS-50909";

    /**
     * Код компании (customfield_10100)
     */
    private String epicCode = "IC-3906";
}