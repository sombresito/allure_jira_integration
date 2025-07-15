package ru.iopump.qa.allure.gui.loadView;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.iopump.qa.allure.gui.component.CustomFileUpload;
import ru.iopump.qa.allure.helper.NotificationHelper;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.mail.MessagingException;
import ru.iopump.qa.allure.gui.component.EmailValidatorTextField;
import ru.iopump.qa.allure.properties.MailConfig;
import ru.iopump.qa.allure.service.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Route("load-form")
@PageTitle("Анкета нагрузочного тестирования")
@CssImport("./styles/load-view-form.css")
public class ClientFormView extends VerticalLayout {
    @Autowired
    private final JiraService jiraService;
    private final MailConfig mailConfig;
    private final TextField projectNameField = new TextField("Наименование проекта (Наименование ИС и подсистемы в Jira (пункт влияние на ИС))");
    private final EmailValidatorTextField responsiblePersons = new EmailValidatorTextField("Ответственные лица (через запятую)");
    private final TextField objectivesTextField = new TextField("Цели и задачи");
    private final TextField customObjectiveField = new TextField("Укажите свою цель");
    private final ComboBox<String> testDataRequired = new ComboBox<>("Тестовые данные нужны?");
    private final TextField testDataLink = new TextField("Ссылка на тестовые данные");
    private final TextArea commentsField = new TextArea("Комментарии");
    private final ComboBox<String> emulatorsRequired = new ComboBox<>("Эмуляторы/Заглушки нужны?");
    private final TextField emulatorText = new TextField("Подробно опишите Эмуляторы/Заглушки");
    private final ComboBox<String> monitoringAvailable = new ComboBox<>("Настроен ли мониторинг технических параметров нагружаемого сервера (CPU, RAM, диск, сеть)?");
    private final TextField monitoringLinkField = new TextField("Мониторинг (ссылка)");
    private final ComboBox<String> architectureAvailable = new ComboBox<>("Имеется ли архитектурная схема?");
    private final TextField architectureLinkField = new TextField("Архитектурная схема (ссылка)");
    private final ComboBox<String> environmentAvailable = new ComboBox<>("Имеется ли конфигурация стендов?");
    private final TextField environmentConfigField = new TextField("Конфигурация стендов");
    private final ComboBox<String> logsAvailable = new ComboBox<>("Имеется ли доступ к логам?");
    private final TextField logsLinkField = new TextField("Ссылка на логи");
    private final ComboBox<String> intensityDataRequired = new ComboBox<>("Есть ли данные о интенсивности запросов?");
    private final AtomicBoolean isSubmitting = new AtomicBoolean(false);
    private final ProgressBar loadingIndicator = new ProgressBar();
    private final CustomFileUpload postmanUpload = new CustomFileUpload();
    private final CheckboxGroup<String> dialogCheckboxGroup = new CheckboxGroup<>();
    private final OperationGridManager operationGridManager = new OperationGridManager();
    private final MicrosoftTeamsService teamsService;

    // Опции для целей и задач
    private static final List<String> OBJECTIVES_OPTIONS = List.of(
            "Тестирование производительности",
            "Тестирование отказоустойчивости",
            "Стресс-тестирование",
            "Тестирование стабильности",
            "Тестирование масштабируемости",
            "Тестирование на соответствие SLA",
            "Тестирование пиковых нагрузок",
            "Тестирование с постепенным увеличением нагрузки",
            "Другие"
    );

    public ClientFormView(JiraService jiraService, MailConfig mailConfig, MicrosoftTeamsService teamsService) {
        this.jiraService = jiraService;
        this.teamsService = teamsService;
        this.mailConfig = mailConfig;
        createHeader();

        getUpload(); // инициализация upload-компонента
        intensityDataRequired.setItems("Да", "Нет");
        operationGridManager.getContainer().setVisible(false);
        setPadding(false);
        getElement().getStyle()
                .set("background-image", "linear-gradient(135deg, #707070, #e0e0e0)")
                .set("min-height", "100vh");

        // Конфигурация динамических полей через FieldConfigurator
        Map<ComboBox<String>, TextField> dynamicMapping = Map.of(
                testDataRequired, testDataLink,
                emulatorsRequired, emulatorText,
                architectureAvailable, architectureLinkField,
                environmentAvailable, environmentConfigField,
                logsAvailable, logsLinkField,
                monitoringAvailable, monitoringLinkField
        );
        FieldConfigurator.configureDynamicFields(dynamicMapping);
        FieldConfigurator.configureCommentsField(commentsField);
        commentsField.addClassName("custom-textarea");
        // Настройка диалога выбора целей через FieldConfigurator
        Dialog objectivesDialog = FieldConfigurator.configureObjectivesField(
                objectivesTextField, customObjectiveField, dialogCheckboxGroup, OBJECTIVES_OPTIONS
        );
        // Обертка для поля выбора целей
        dialogCheckboxGroup.getElement().getStyle().set("--lumo-primary-color", "#007ACC");
        dialogCheckboxGroup.getElement().getStyle().set("--lumo-primary-contrast-color", "#FFFFFF");

        Div textFieldWrapper = new Div();
        textFieldWrapper.setWidthFull();
        textFieldWrapper.add(objectivesTextField);
        objectivesTextField.setPlaceholder("Нажмите для выбора...");
        objectivesTextField.setReadOnly(true);
        textFieldWrapper.addClickListener(event -> {
            if (!objectivesDialog.isOpened()) {
                objectivesDialog.open();
            }
        });

        // Формирование основной формы
        FormLayout formLayout = new FormLayout();
        formLayout.add(
                projectNameField, responsiblePersons, textFieldWrapper, customObjectiveField,
                testDataRequired, testDataLink,
                emulatorsRequired, emulatorText,
                architectureAvailable, architectureLinkField,
                environmentAvailable, environmentConfigField,
                logsAvailable, logsLinkField,
                monitoringAvailable, monitoringLinkField,
                intensityDataRequired, commentsField, postmanUpload
        );
        formLayout.getElement().getStyle().set("padding", "var(--lumo-space-m)");
        // Установка подсказок (tooltip) для иконок
        Icon infoDataRequiredField = new Icon("vaadin", "info-circle");
        Icon infoIconObjectivesField = new Icon("vaadin", "info-circle");
        // Другие подсказки можно добавить аналогичным образом.
        Icon infoIntensityField = new Icon("vaadin", "info-circle");
        intensityDataRequired.setHelperComponent(infoIntensityField);
        Icon infoMonitoringField = new Icon("vaadin", "info-circle");
        monitoringAvailable.setHelperComponent(infoMonitoringField);
        Icon infoLogsField = new Icon("vaadin", "info-circle");
        logsAvailable.setHelperComponent(infoLogsField);
        Icon infoEnvironmentConfigField = new Icon("vaadin", "info-circle");
        environmentAvailable.setHelperComponent(infoEnvironmentConfigField);
        Icon infoArchitectureSchemeField = new Icon("vaadin", "info-circle");
        architectureAvailable.setHelperComponent(infoArchitectureSchemeField);
        Icon infoEmulatorsField = new Icon("vaadin", "info-circle");
        infoDataRequiredField.getElement().setAttribute("title", "Тест-дата — входные данные для проверки работы системы.");
        infoIconObjectivesField.getElement().setAttribute("title", "Тестирование производительности – Проверка скорости и эффективности работы системы при нормальной нагрузке.\n" +
                "Тестирование отказоустойчивости – Оценка поведения системы при сбоях и восстановлении после них.\n" +
                "Стресс-тестирование – Проверка системы на предельных нагрузках, чтобы выявить её пределы.\n" +
                "Тестирование стабильности – Оценка работы системы на протяжении длительного времени без сбоев.\n" +
                "Тестирование масштабируемости – Оценка способности системы справляться с увеличением нагрузки.\n" +
                "Тестирование на соответствие SLA – Проверка соответствия системы обещанным уровням сервиса.\n" +
                "Тестирование пиковых нагрузок – Проверка работы системы при краткосрочных высоких нагрузках.\n" +
                "Тестирование с постепенным увеличением нагрузки – Оценка работы системы при постепенном росте нагрузки.");
        infoEmulatorsField.getElement().setAttribute("title", "Эмулятор/заглушка — имитация работы системы или её части.");
        infoArchitectureSchemeField.getElement().setAttribute("title", "Архитектурная схема — структура системы и связи между её частями.");
        infoEnvironmentConfigField.getElement().setAttribute("title", "Конфигурация стенда — окружение для тестирования системы (её характеристики).");
        infoLogsField.getElement().setAttribute("title", "Логи — записи о событиях и ошибках системы (логи базы данных или иные).");
        infoMonitoringField.getElement().setAttribute("title", "Мониторинг — это процесс наблюдения за состоянием системы, её производительностью и доступностью. Сюда входят сбор и анализ данных о работе приложений, серверов, сети и других компонентов инфраструктуры с целью своевременного обнаружения отклонений, предотвращения сбоев и обеспечения стабильной работы системы.");
        infoIntensityField.getElement().setAttribute("title", "Интенсивность — это характеристика нагрузки на систему, показывающая, как часто и с какой частотой выполняются запросы. Например, сколько операций в секунду обрабатывает система или какой объём данных передаётся за единицу времени.");

        emulatorsRequired.setHelperComponent(infoEmulatorsField);
        objectivesTextField.setHelperComponent(infoIconObjectivesField);
        testDataRequired.setHelperComponent(infoDataRequiredField);

        // Управление видимостью секции интенсивности через intensityDataRequired
        intensityDataRequired.addValueChangeListener(event -> {
            boolean enabled = "Да".equals(event.getValue());
            Component grid = operationGridManager.getContainer();
            grid.setVisible(enabled);
        });

        objectivesTextField.addValueChangeListener(event -> {
            boolean enabled = "Другие".equals(event.getValue());
            customObjectiveField.setVisible(enabled);
        });

        // Кнопки отправки и очистки формы
        Button submitButton = new Button("Отправить на почту", event -> saveFormAsCsvAndSendEmail());
        Button clearButton = new Button("Очистить", e -> clearForm());
        submitButton.addClassName("generate-button");
        clearButton.addClassName("clear-button");
        HorizontalLayout buttonLayout = new HorizontalLayout(submitButton, clearButton);
        buttonLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidth("100%");
        submitButton.setHeight("40px");
        clearButton.setHeight("40px");
        setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);
        // Добавление всех компонентов на страницу
        loadingIndicator.setIndeterminate(true); // Бесконечная анимация загрузки
        loadingIndicator.setVisible(false); // Изначально скрыт
        add(loadingIndicator); // Добавляем индикатор в UI
        add(formLayout, operationGridManager.getContainer(), buttonLayout);
    }

    private void createHeader() {
        H1 logo = new H1("Анкета нагрузочного тестирования");
        logo.addClassName("header-title");
        Div spacer = new Div();
        logo.getElement().getStyle().set("color", "#FDFEFE");
        logo.getStyle().set("margin-top", "1px");
        Button backButton = new Button("Вернуться на главную НТ", e -> this.getUI().ifPresent(ui -> ui.navigate("load-main")));
        backButton.addClassName("back-button");
        HorizontalLayout header = new HorizontalLayout(logo, spacer, backButton);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(spacer);
        header.setWidth("100%");
        header.addClassName("top-bar");
        this.add(header);
    }

    private void clearForm() {
        projectNameField.clear();
        responsiblePersons.clear();
        objectivesTextField.setValue("Нажмите для выбора...");
        customObjectiveField.clear();
        testDataLink.clear();
        emulatorText.clear();
        architectureLinkField.clear();
        environmentConfigField.clear();
        logsLinkField.clear();
        monitoringLinkField.clear();
        testDataRequired.clear();
        emulatorsRequired.clear();
        architectureAvailable.clear();
        environmentAvailable.clear();
        logsAvailable.clear();
        monitoringAvailable.clear();
        intensityDataRequired.clear();
        dialogCheckboxGroup.clear();
        commentsField.clear();
        resetUpload();
        // Можно добавить вызов метода очистки операций, если он реализован в OperationGridManager.
        NotificationHelper.showDebouncedNotification("Форма успешно очищена", NotificationVariant.LUMO_SUCCESS);
    }

    private void resetUpload() {
        postmanUpload.clear(); // Очистка загруженного файла
    }

    private void getUpload() {
        postmanUpload.setAcceptedFileTypes(".json");
        postmanUpload.setWidth("100%");
        postmanUpload.addClassName("upload-component");

        // Обработчик события выбора файлов
        postmanUpload.addFilesSelectedListener(event -> {
            if (event.hasFiles()) {
                List<String> fileNames = event.getFileNames();
                StringBuilder message = new StringBuilder();

                // Проверяем размер списка файлов
                if (fileNames.size() == 1) {
                    message.append("Файл ").append(fileNames.get(0)).append(" успешно загружен");
                } else {
                    message.append("Загружено файлов: ").append(fileNames.size())
                            .append(" (").append(String.join(", ", fileNames)).append(")");
                }

                NotificationHelper.showDebouncedNotification(
                        message.toString(),
                        NotificationVariant.LUMO_SUCCESS
                );
            }
        });
    }

    private void saveFormAsCsvAndSendEmail() {

        if (!isSubmitting.compareAndSet(false, true)) {
            NotificationHelper.showDebouncedNotification(
                    "Форма уже отправляется. Пожалуйста, подождите.",
                    NotificationVariant.LUMO_ERROR
            );
            return;
        }

        UI ui = UI.getCurrent(); // Сохраняем UI перед запуском асинхронного кода

        ui.access(() -> loadingIndicator.setVisible(true));

        try {
            Map<String, String> formData = collectFormData();

            if (!validateFormData(formData)) {
                resetSubmissionState(ui);
                return;
            }

            // Проверка необязательных полей
            if (customObjectiveField.isEnabled() && (customObjectiveField.getValue() == null || customObjectiveField.getValue().trim().isEmpty())) {
                NotificationHelper.showDebouncedNotification("Заполните необязательное поле: Другие цели", NotificationVariant.LUMO_CONTRAST);
                resetSubmissionState(ui);
                return;
            }

            if (testDataLink.isEnabled() && (testDataLink.getValue() == null || testDataLink.getValue().trim().isEmpty())) {
                NotificationHelper.showDebouncedNotification("Заполните необязательное поле: Ссылка на тестовые данные", NotificationVariant.LUMO_CONTRAST);
                resetSubmissionState(ui);
                return;
            }

            if (emulatorText.isEnabled() && (emulatorText.getValue() == null || emulatorText.getValue().trim().isEmpty())) {
                NotificationHelper.showDebouncedNotification(
                        "Заполните необязательное поле: Эмуляторы/Заглушки (описание)",
                        NotificationVariant.LUMO_CONTRAST
                );
                resetSubmissionState(ui);
                return;
            }
            if (architectureLinkField.isEnabled() && (architectureLinkField.getValue() == null || architectureLinkField.getValue().trim().isEmpty())) {
                NotificationHelper.showDebouncedNotification(
                        "Заполните необязательное поле: Архитектурная схема (ссылка)",
                        NotificationVariant.LUMO_CONTRAST
                );
                resetSubmissionState(ui);
                return;
            }
            if (environmentConfigField.isEnabled() && (environmentConfigField.getValue() == null || environmentConfigField.getValue().trim().isEmpty())) {
                NotificationHelper.showDebouncedNotification(
                        "Заполните необязательное поле: Конфигурация стендов",
                        NotificationVariant.LUMO_CONTRAST
                );
                resetSubmissionState(ui);
                return;
            }
            if (logsLinkField.isEnabled() && (logsLinkField.getValue() == null || logsLinkField.getValue().trim().isEmpty())) {
                NotificationHelper.showDebouncedNotification(
                        "Заполните необязательное поле: Ссылка на логи",
                        NotificationVariant.LUMO_CONTRAST
                );
                resetSubmissionState(ui);
                return;
            }
            if (monitoringLinkField.isEnabled() && (monitoringLinkField.getValue() == null || monitoringLinkField.getValue().trim().isEmpty())) {
                NotificationHelper.showDebouncedNotification(
                        "Заполните необязательное поле: Ссылка на мониторинг",
                        NotificationVariant.LUMO_CONTRAST
                );
                resetSubmissionState(ui);
                return;
            }


            boolean hasIntensity = operationGridManager.getOperationsList().stream()
                    .anyMatch(op -> op.get("Интенсивность") != null && !op.get("Интенсивность").trim().isEmpty());

            if (intensityDataRequired.getValue().equals("Да") && !hasIntensity) {
                NotificationHelper.showDebouncedNotification("Необходимо заполнить хотя бы одну интенсивность", NotificationVariant.LUMO_CONTRAST);
                resetSubmissionState(ui);
                return;
            }

            List<Map<String, byte[]>> postmanFiles = new ArrayList<>();

            if (postmanUpload.hasFiles()) {
                for (String fileName : postmanUpload.getFileNames()) {
                    try {
                        InputStream inputStream = postmanUpload.getInputStream(fileName);
                        if (inputStream != null) {
                            byte[] fileData = inputStream.readAllBytes();
                            if (fileData.length > 0) {
                                Map<String, byte[]> fileMap = new HashMap<>();
                                fileMap.put(fileName, fileData);
                                postmanFiles.add(fileMap);
                            } else {
                                NotificationHelper.showDebouncedNotification("Файл " + fileName + " пуст или содержит некорректные данные", NotificationVariant.LUMO_ERROR);
                                resetSubmissionState(ui);
                                return;
                            }
                        } else {
                            System.err.println("InputStream для файла " + fileName + " равен null");
                            NotificationHelper.showDebouncedNotification("Не удалось прочитать файл " + fileName, NotificationVariant.LUMO_ERROR);
                            resetSubmissionState(ui);
                            return;
                        }
                    } catch (IOException e) {
                        System.err.println("Ошибка при чтении файла " + fileName + ": " + e.getMessage());
                        NotificationHelper.showDebouncedNotification("Ошибка при чтении файла " + fileName + ": " + e.getMessage(), NotificationVariant.LUMO_ERROR);
                        resetSubmissionState(ui);
                        return;
                    }
                }
            }

            // Отправка письма выполняется асинхронно
            String csvData = CsvUtil.generateCsvContent(formData);
            List<String> recipients = mailConfig.getRecipients();
            String subject = "Форма проекта: " + formData.get("Наименование проекта");
            String fileName = projectNameField.getValue().replaceAll("[^a-zA-Zа-яА-Я0-9.-]", "_") + ".csv";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            SecurityContextHolder.getContext().setAuthentication(authentication); // Устанавливаем контекст
            String username = authentication.getName();

            CompletableFuture<String> jiraTask = CompletableFuture.supplyAsync(() -> {
                try {
                    return jiraService.createJiraIssue(formData, username);
                } catch (Exception e) {
                    System.err.println("Ошибка при создании задачи в Jira: " + e.getMessage());
                    return null; // Возвращаем null, чтобы не прерывать цепочку
                } finally {
                    SecurityContextHolder.clearContext(); // Чистим контекст после использования
                }
            });

            CompletableFuture.supplyAsync(() -> {
                try {
                    return teamsService.createLoadTestingChat(formData);
                } catch (Exception e) {
                    System.err.println("Ошибка при создании чата в Microsoft Teams: " + e.getMessage());
                    return null;
                }
            });

            // Запускаем emailTask в любом случае, даже если Jira упала
            jiraTask.handle((jiraIssueKey, ex) -> {
                        if (ex != null) {
                            System.err.println("Ошибка при создании задачи в Jira: " + ex.getMessage());
                        }
                        if (jiraIssueKey != null) {
                            String jiraLink = "https://jira.bcc.kz/browse/" + jiraIssueKey;
                            formData.put("JiraTaskLink", jiraLink);
                        } else {
                            formData.put("JiraTaskLink", "Задача в Jira не создана");
                        }


                        // Отправка email
                        return CompletableFuture.runAsync(() -> {
                            try {
                                String jiraInfo = formData.get("JiraTaskLink") != null ? "\nJira Task link: " + formData.get("JiraTaskLink") : "";
                                if (!postmanFiles.isEmpty()) {
                                    EmailService.sendEmailWithCsvAndMultiplePostmanCollections(recipients, subject, csvData, fileName, postmanFiles, formData.get("Комментарии") + jiraInfo, mailConfig);
                                } else {
                                    EmailService.sendEmailWithCsvOnly(recipients, subject, csvData, fileName, formData.get("Комментарии") + jiraInfo, mailConfig);
                                }
                            } catch (MessagingException | InterruptedException e) {
                                throw new RuntimeException("Ошибка отправки email: " + e.getMessage(), e);
                            }
                        });
                    }).thenCompose(Function.identity()) // Дожидаемся завершения emailTask
                    .thenRun(() -> ui.access(() -> {
                        NotificationHelper.showDebouncedNotification(
                                "Данные успешно отправлены на почту" + (formData.get("JiraTaskLink").contains("https") ? " и создана задача в Jira" : ", но задача в Jira не была создана"),
                                NotificationVariant.LUMO_SUCCESS
                        );
                        resetSubmissionState(ui);
                    })).exceptionally(ex -> {
                        System.err.println("Ошибка при обработке формы: " + ex.getMessage());
                        ui.access(() -> {
                            NotificationHelper.showDebouncedNotification("Ошибка при обработке формы: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
                            resetSubmissionState(ui);
                        });
                        return null;
                    });


        } catch (Exception e) {
            resetSubmissionState(ui);
            throw e;
        }
    }

    private void resetSubmissionState(UI ui) {
        isSubmitting.set(false);
        ui.access(() -> loadingIndicator.setVisible(false));
    }

    private Map<String, String> collectFormData() {
        Map<String, String> formData = new LinkedHashMap<>();

        formData.put("Наименование проекта", getTrimmedValue(projectNameField));
        formData.put("Ответственные лица", getTrimmedValue(responsiblePersons));
        formData.put("Цели и задачи", String.join(", ", dialogCheckboxGroup.getValue()));
        formData.put("Комментарии", getTrimmedValue(commentsField));
        formData.put("Тестовые данные нужны?", getTrimmedValue(testDataRequired));
        formData.put("Эмуляторы/Заглушки нужны?", getTrimmedValue(emulatorsRequired));
        formData.put("Имеется ли архитектурная схема?", getTrimmedValue(architectureAvailable));
        formData.put("Имеется ли конфигурация стендов?", getTrimmedValue(environmentAvailable));
        formData.put("Имеется ли доступ к логам?", getTrimmedValue(logsAvailable));
        formData.put("Настроен ли мониторинг технических параметров нагружаемого сервера (CPU, RAM, диск, сеть)?", getTrimmedValue(monitoringAvailable));
        formData.put("Есть ли данные о интенсивности запросов?", getTrimmedValue(intensityDataRequired));
        formData.put("Операции", convertOperationsListToString(operationGridManager.getOperationsList()));
        addIfNotEmpty(formData, "Другие цели", customObjectiveField);
        addIfNotEmpty(formData, "Ссылка на тестовые данные", testDataLink);
        addIfNotEmpty(formData, "Эмуляторы/Заглушки (описание)", emulatorText);
        addIfNotEmpty(formData, "Архитектурная схема (ссылка)", architectureLinkField);
        addIfNotEmpty(formData, "Конфигурация стендов", environmentConfigField);
        addIfNotEmpty(formData, "Ссылка на логи", logsLinkField);
        addIfNotEmpty(formData, "Ссылка на мониторинг", monitoringLinkField);


        return formData;
    }

    private String convertOperationsListToString(List<Map<String, String>> operationsList) {
        if (operationsList.isEmpty()) {
            return "Нет операций";
        }
        return operationsList.stream()
                .map(op -> String.join(" ", op.values())) // Берем значения без ключей
                .collect(Collectors.joining(", ")); // Разделяем операции точкой с запятой
    }

    private boolean validateFormData(Map<String, String> formData) {
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            if (!entry.getKey().equals("Комментарии") && entry.getValue().isEmpty()) {
                NotificationHelper.showDebouncedNotification("Заполните поле: " + entry.getKey(), NotificationVariant.LUMO_ERROR);
                return false;
            }
        }

        if (!responsiblePersons.isAllEmailsValid()) {
            NotificationHelper.showDebouncedNotification("Пожалуйста, проверьте корректность введенных email адресов", NotificationVariant.LUMO_ERROR);
            return false;
        }

        String comments = formData.get("Комментарии");
        if (!comments.isEmpty() && (comments.length() < 10 || comments.length() > 500)) {
            NotificationHelper.showDebouncedNotification("Длина комментария должна быть от 10 до 500 символов", NotificationVariant.LUMO_ERROR);
            return false;
        }

        return true;
    }

    private String getTrimmedValue(HasValue<?, String> field) {
        String value = field.getValue();
        return (value != null) ? value.trim() : "";
    }

    private void addIfNotEmpty(Map<String, String> formData, String key, HasValue<?, String> field) {
        String value = getTrimmedValue(field);
        if (!value.isEmpty()) {
            formData.put(key, value);
        }
    }

}