package ru.iopump.qa.allure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.iopump.qa.allure.model.jira.JiraModels.JiraIssueRequest;
import ru.iopump.qa.allure.model.jira.JiraModels.Fields;
import ru.iopump.qa.allure.model.jira.JiraModels.Project;
import ru.iopump.qa.allure.model.jira.JiraModels.IssueType;
import ru.iopump.qa.allure.model.jira.JiraModels.User;
import ru.iopump.qa.allure.model.jira.JiraModels.Priority;
import ru.iopump.qa.allure.model.jira.JiraModels.ProjectReference;
import ru.iopump.qa.allure.model.jira.JiraModels.JiraResponse;
import ru.iopump.qa.allure.properties.JiraProperties;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraService {

    private final RestTemplate restTemplate;
    private final JiraProperties jiraProperties;

    public String createJiraIssue(Map<String, String> formData, String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + jiraProperties.getApiToken());

        JiraIssueRequest request = buildJiraRequest(formData, username);

        HttpEntity<JiraIssueRequest> entity = new HttpEntity<>(request, headers);

        String url = jiraProperties.getApiUrl() + "/rest/api/2/issue/";

        log.info("Отправка запроса в Jira для создания задачи: {}", formData.get("Наименование проекта"));
        ResponseEntity<JiraResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                JiraResponse.class
        );

        log.info("Задача успешно создана в Jira: {}", response.getBody());
        return Objects.requireNonNull(response.getBody()).getKey();
    }

    /**
     * Добавить комментарий к задаче Jira.
     *
     * @param issueKey Ключ задачи
     * @param body     Текст комментария
     */
    public void addComment(String issueKey, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + jiraProperties.getApiToken());

        var url = jiraProperties.getApiUrl() + "/rest/api/2/issue/" + issueKey + "/comment";
        var request = new HttpEntity<>(Map.of("body", body), headers);

        var browseUrl = jiraProperties.getApiUrl() + "/browse/" + issueKey;
        log.info("Отправка комментария в Jira для задачи {} ({}). Тело: {}", issueKey, browseUrl, body);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("Ответ Jira: status='{}' body='{}'", response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            log.error("Ошибка при отправке комментария в Jira", e);
            throw e;
        }
    }

    /**
     * Сформировать комментарий с информацией о репорте и добавить его в задачу.
     *
     * @param issueKey  Ключ задачи
     * @param reportDir Директория отчета Allure
     * @param reportUrl URL отчета
     */
    public void addReportComment(String issueKey, Path reportDir, String reportUrl) {
        Path summaryPath = reportDir.resolve("widgets/summary.json");
        try {
            if (Files.notExists(summaryPath)) {
                log.warn("Summary file '{}' not found", summaryPath);
                return;
            }
            String summary = Files.readString(summaryPath);
            log.debug("Готовим комментарий для {} с отчётом {}", issueKey, reportUrl);
            String comment = String.format("Allure report: %s\n\n{code:json}\n%s\n{code}", reportUrl, summary);
            addComment(issueKey, comment);
        } catch (IOException e) {
            log.error("Не удалось добавить комментарий в Jira", e);
        }
    }


    private JiraIssueRequest buildJiraRequest(Map<String, String> formData, String username) {
        String projectName = formData.get("Наименование проекта");
        String responsiblePersons = formData.get("Ответственные лица");
        String objectives = formData.get("Цели и задачи");
        String comments = formData.getOrDefault("Комментарии", "");

        // Формируем описание задачи
        StringBuilder description = new StringBuilder();
        description.append("h2. Информация о проекте нагрузочного тестирования\n\n");
        description.append("* *Наименование проекта:* ").append(projectName).append("\n");
        description.append("* *Ответственные лица:* ").append(responsiblePersons).append("\n");
        description.append("* *Цели и задачи:* ").append(objectives).append("\n\n");

        // Добавляем информацию о тестовых данных
        if (formData.containsKey("Тестовые данные нужны?")) {
            description.append("h3. Тестовые данные\n");
            description.append("* *Тестовые данные нужны:* ").append(formData.get("Тестовые данные нужны?")).append("\n");
            if (formData.containsKey("Ссылка на тестовые данные")) {
                description.append("* *Ссылка на тестовые данные:* ").append(formData.get("Ссылка на тестовые данные")).append("\n");
            }
            description.append("\n");
        }

        // Добавляем информацию об эмуляторах
        if (formData.containsKey("Эмуляторы/Заглушки нужны?")) {
            description.append("h3. Эмуляторы/Заглушки\n");
            description.append("* *Эмуляторы/Заглушки нужны:* ").append(formData.get("Эмуляторы/Заглушки нужны?")).append("\n");
            if (formData.containsKey("Эмуляторы/Заглушки (описание)")) {
                description.append("* *Описание:* ").append(formData.get("Эмуляторы/Заглушки (описание)")).append("\n");
            }
            description.append("\n");
        }

        // Добавляем информацию о технической инфраструктуре
        description.append("h3. Техническая инфраструктура\n");
        if (formData.containsKey("Имеется ли архитектурная схема?")) {
            description.append("* *Архитектурная схема:* ").append(formData.get("Имеется ли архитектурная схема?")).append("\n");
            if (formData.containsKey("Архитектурная схема (ссылка)")) {
                description.append("* *Ссылка:* ").append(formData.get("Архитектурная схема (ссылка)")).append("\n");
            }
        }

        if (formData.containsKey("Имеется ли конфигурация стендов?")) {
            description.append("* *Конфигурация стендов:* ").append(formData.get("Имеется ли конфигурация стендов?")).append("\n");
            if (formData.containsKey("Конфигурация стендов")) {
                description.append("* *Детали:* ").append(formData.get("Конфигурация стендов")).append("\n");
            }
        }

        if (formData.containsKey("Имеется ли доступ к логам?")) {
            description.append("* *Доступ к логам:* ").append(formData.get("Имеется ли доступ к логам?")).append("\n");
            if (formData.containsKey("Ссылка на логи")) {
                description.append("* *Ссылка:* ").append(formData.get("Ссылка на логи")).append("\n");
            }
        }

        if (formData.containsKey("Настроен ли мониторинг технических параметров нагружаемого сервера (CPU, RAM, диск, сеть)?")) {
            description.append("* *Мониторинг:* ").append(formData.get("Настроен ли мониторинг технических параметров нагружаемого сервера (CPU, RAM, диск, сеть)?")).append("\n");
            if (formData.containsKey("Ссылка на мониторинг")) {
                description.append("* *Ссылка:* ").append(formData.get("Ссылка на мониторинг")).append("\n");
            }
        }
        description.append("\n");

        // Добавляем информацию об интенсивности запросов
        if (formData.containsKey("Есть ли данные о интенсивности запросов?")) {
            description.append("h3. Интенсивность запросов\n");
            description.append("* *Данные доступны:* ").append(formData.get("Есть ли данные о интенсивности запросов?")).append("\n");
            if (formData.containsKey("Операции") && !formData.get("Операции").equals("Нет операций")) {
                description.append("* *Операции:* ").append(formData.get("Операции")).append("\n");
            }
            description.append("\n");
        }

        // Добавляем комментарии
        if (!comments.isEmpty()) {
            description.append("h3. Комментарии\n");
            description.append(comments);
        }

        // Формируем заголовок задачи
        String summary = "Запрос на нагрузочное тестирование: " + projectName;

        // Устанавливаем срок выполнения на 7 дней вперед
        LocalDate dueDate = LocalDate.now().plusDays(7);
        String dueDateStr = dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        return JiraIssueRequest.builder()
                .fields(Fields.builder()
                        .project(Project.builder().key(jiraProperties.getProjectKey()).build())
                        .summary(summary)
                        .description(description.toString())
                        .issuetype(IssueType.builder().id(jiraProperties.getIssueTypeId()).build())
                        .assignee(User.builder().name(jiraProperties.getDefaultAssignee()).build())
                        .reporter(User.builder().name(username).build())
                        .priority(Priority.builder().id("3").build()) // Medium priority
                        .duedate(dueDateStr)
                        .customfield_11203(Collections.singletonList(
                                ProjectReference.builder().key(jiraProperties.getInfluenceSystemKey()).build()
                        ))
                        .build())
                .build();
    }
}