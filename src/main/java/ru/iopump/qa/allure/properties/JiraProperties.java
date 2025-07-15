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
    private String apiUrl = "https://jira.bcc.kz";

    /**
     * Токен авторизации для API Jira
     */
    private String apiToken = "NTY1MTUxMjg3NjM4Oh2s8v4+gO1eqiyLXhDOv9qhdzO3";

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