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
    private String apiUrl;

    /**
     * Токен авторизации для API Jira
     */
    private String apiToken;

    /**
     * Ключ проекта в Jira
     */
    private String projectKey;

    /**
     * ID типа задачи в Jira
     */
    private String issueTypeId;

    /**
     * Имя пользователя, который будет назначен исполнителем по умолчанию
     */
    private String defaultAssignee;

    /**
     * Имя пользователя, который будет указан как автор задачи по умолчанию
     */
    private String defaultReporter;

    /**
     * Ключ системы влияния (customfield_11203)
     */
    private String influenceSystemKey;

    /**
     * Код компании (customfield_10100)
     */
    private String epicCode;
}