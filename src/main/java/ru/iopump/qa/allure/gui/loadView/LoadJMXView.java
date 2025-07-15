package ru.iopump.qa.allure.gui.loadView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import ru.iopump.qa.allure.gui.component.CustomFileUpload;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ru.iopump.qa.allure.model.TestParameters;
import ru.iopump.qa.allure.service.JmxGenerationService;
import ru.iopump.qa.allure.service.PostmanCollectionParser;

@Route("load-jmx")
@PageTitle("Создать JMX сценарий")
@CssImport("./styles/load-view-jmx.css")
public class LoadJMXView extends VerticalLayout {

    // Используем кастомный компонент для загрузки файла
    private final CustomFileUpload postmanUpload = new CustomFileUpload();

    private final TextField scenarioNameField = new TextField("Название JMX сценария");
    private final Checkbox authCheckbox = new Checkbox("Присутствует ли в вашей коллекции метод авторизации?");
    private final Checkbox loadCheckBox = new Checkbox("Нагрузочное тестирование (Load Testing)");
    private final Checkbox performanceCheckBox = new Checkbox("Тестирование производительности (Performance Testing)");
    private final Checkbox stabilityCheckBox = new Checkbox("Тестирование стабильности (Stability Testing)");
    private final Checkbox stressCheckBox = new Checkbox("Стрессовое тестирование (Stress Testing)");
    private final Checkbox failoverCheckBox = new Checkbox("Тестирование отказоустойчивости (Failover Testing)");
    private final Checkbox customCheckBox = new Checkbox("Кастомное тестирование (Custom Testing)");
    private final NumberField numThreadsField = new NumberField("Threads");
    private final NumberField rampTimeField = new NumberField("Ramp-up (seconds)");
    private final NumberField durationField = new NumberField("Duration (seconds)");
    private final HorizontalLayout buttonsLayout = new HorizontalLayout();
    private final Anchor downloadLink = new Anchor();
    private Notification currentNotification;
    private final PostmanCollectionParser postmanCollectionParser = new PostmanCollectionParser();
    private final JmxGenerationService jmxGenerationService = new JmxGenerationService();

    public LoadJMXView() {
        setSizeFull();
        addClassName("main-layout");

        setPadding(false);
        setSpacing(true);

        // Верхняя панель
        HorizontalLayout topBar = new HorizontalLayout();
        topBar.addClassName("top-bar");
        topBar.setWidthFull();
        topBar.setAlignItems(Alignment.CENTER);
        topBar.setJustifyContentMode(JustifyContentMode.BETWEEN);

        H1 header = new H1("Создать JMX сценарий автоматически");
        header.getElement().getStyle().set("color", "#FDFEFE");
        header.addClassName("header-title");

        Button backButton = new Button("Вернуться на главную НТ", e ->
                getUI().ifPresent(ui -> ui.navigate("load-main"))
        );
        backButton.addClassName("back-button");

        topBar.add(header, backButton);
        add(topBar);

        // Инициализация кастомного файла для загрузки Postman коллекции
        initializeUpload();

        scenarioNameField.setPlaceholder("Введите название сценария");
        scenarioNameField.setWidth("50%");
        scenarioNameField.addClassName("scenario-name-field");
        add(scenarioNameField);
        setHorizontalComponentAlignment(Alignment.CENTER, scenarioNameField);

        authCheckbox.addClassName("checkbox");
        add(authCheckbox);
        setHorizontalComponentAlignment(Alignment.CENTER, authCheckbox);

        Label hintLabel = new Label("Выберите один или несколько типов тестирования:");
        hintLabel.addClassName("label");

        VerticalLayout testsLayout = new VerticalLayout(
                loadCheckBox,
                performanceCheckBox,
                stabilityCheckBox,
                stressCheckBox,
                failoverCheckBox,
                customCheckBox
        );
        testsLayout.setPadding(false);
        testsLayout.setSpacing(false);
        testsLayout.setAlignItems(Alignment.START);

        loadCheckBox.addClassName("checkbox");
        performanceCheckBox.addClassName("checkbox");
        stabilityCheckBox.addClassName("checkbox");
        stressCheckBox.addClassName("checkbox");
        failoverCheckBox.addClassName("checkbox");
        customCheckBox.addClassName("checkbox");

        customCheckBox.addValueChangeListener(e -> {
            boolean isCustomSelected = e.getValue();
            numThreadsField.setVisible(isCustomSelected);
            rampTimeField.setVisible(isCustomSelected);
            durationField.setVisible(isCustomSelected);
        });

        numThreadsField.setWidth("200px");
        rampTimeField.setWidth("200px");
        durationField.setWidth("200px");
        numThreadsField.setVisible(false);
        rampTimeField.setVisible(false);
        durationField.setVisible(false);

        VerticalLayout allTestsLayout = new VerticalLayout(testsLayout, numThreadsField, rampTimeField, durationField);
        allTestsLayout.setPadding(false);
        allTestsLayout.setSpacing(true);
        allTestsLayout.setClassName("tests-container");

        Details testTypesDetails = new Details("Нажмите, чтобы выбрать тип(ы) тестирования", allTestsLayout);
        testTypesDetails.setOpened(false);
        testTypesDetails.addClassName("combobox");

        VerticalLayout detailsLayout = new VerticalLayout(hintLabel, testTypesDetails);
        detailsLayout.setAlignItems(Alignment.CENTER);
        detailsLayout.setSpacing(true);

        add(detailsLayout);
        setHorizontalComponentAlignment(Alignment.CENTER, detailsLayout);

        // Кнопки внизу
        buttonsLayout.setSpacing(true);
        buttonsLayout.setPadding(false);
        buttonsLayout.setAlignItems(Alignment.CENTER);

        Button generateButton = new Button("Создать JMX файл", e -> handleGenerateJMX());
        generateButton.addClassName("generate-button");

        Button clearButton = new Button("Очистить", e -> handleClear());
        clearButton.addClassName("clear-button");

        buttonsLayout.add(generateButton, clearButton);
        add(buttonsLayout);
        setHorizontalComponentAlignment(Alignment.CENTER, buttonsLayout);

        downloadLink.setVisible(false);
        downloadLink.addClassName("download-button");
        add(downloadLink);
        setHorizontalComponentAlignment(Alignment.CENTER, downloadLink);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Глобально предотвращаем дефолтное поведение dragover и drop на уровне окна
        attachEvent.getUI().getPage().executeJs(
                "window.addEventListener('dragover', function(e){ e.preventDefault(); e.stopPropagation(); }, false);"
        );
        attachEvent.getUI().getPage().executeJs(
                "window.addEventListener('drop', function(e){ e.preventDefault(); e.stopPropagation(); }, false);"
        );
    }

    private void initializeUpload() {
        postmanUpload.setAcceptedFileTypes(".json");
        postmanUpload.setWidth("60%");
        postmanUpload.addClassName("upload-component");
        postmanUpload.addFilesSelectedListener(event -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                InputStream is = postmanUpload.getInputStream();
                JsonNode collection = mapper.readTree(is);
                // Сохраняем коллекцию в сессии для сохранения состояния
                VaadinSession.getCurrent().setAttribute("postmanCollection", collection);
                showSingleNotification("Файл " + event.getFileNames() + " успешно загружен", "success");
            } catch (IOException e) {
                showSingleNotification("Ошибка при чтении файла: " + e.getMessage(), "error");
            }
        });

        add(postmanUpload);
        setHorizontalComponentAlignment(Alignment.CENTER, postmanUpload);
    }

    private void handleGenerateJMX() {
        // Получаем коллекцию из сессии
        JsonNode postmanCollection = (JsonNode) VaadinSession.getCurrent().getAttribute("postmanCollection");
        if (postmanCollection == null) {
            showSingleNotification("Пожалуйста, загрузите файл Postman коллекции", "warning");
            return;
        }

        List<String> selectedTypes = getSelectedTestTypes();
        if (selectedTypes.isEmpty()) {
            showSingleNotification("Пожалуйста, выберите хотя бы один тип тестирования", "warning");
            return;
        }

        String baseName = scenarioNameField.getValue().trim();
        if (baseName.isEmpty()) {
            if (postmanCollection.has("info") && postmanCollection.get("info").has("name")) {
                baseName = postmanCollection.get("info").get("name").asText();
            } else {
                baseName = "collection";
            }
        }

        boolean includeAuth = Boolean.TRUE.equals(authCheckbox.getValue());
        Map<String, String> parentHeaders = postmanCollectionParser.getHeadersFromCollection(postmanCollection);
        List<JsonNode> requests = postmanCollectionParser.extractRequests(
                postmanCollection.get("item"), parentHeaders, null
        );


        try {
            ByteArrayOutputStream zipByteOut = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(zipByteOut);

            for (String testType : selectedTypes) {
                TestParameters params = new TestParameters();
                params.setTestType(testType);
                params.setIncludeAuth(includeAuth);

                if ("Кастомное тестирование (Custom Testing)".equals(testType)) {
                    if (numThreadsField.isEmpty() || rampTimeField.isEmpty() || durationField.isEmpty()) {
                        showSingleNotification("Укажите Threads, Ramp-up, Duration для кастомного тестирования", "warning");
                        zipOut.close();
                        return;
                    }
                    params.setNumThreads(numThreadsField.getValue().intValue());
                    params.setRampTime(rampTimeField.getValue().intValue());
                    params.setDuration(durationField.getValue().intValue());
                    params.setLoops(1);
                }

                ByteArrayOutputStream singleJmxStream = new ByteArrayOutputStream();
                jmxGenerationService.generateJMX(requests, params, singleJmxStream);

                String safeTypeName = testType.replaceAll("\\s+", "_").replaceAll("[()]", "");
                String jmxFileName = baseName + "-" + safeTypeName + ".jmx";

                zipOut.putNextEntry(new ZipEntry(jmxFileName));
                zipOut.write(singleJmxStream.toByteArray());
                zipOut.closeEntry();
            }

            zipOut.close();

            String zipName = baseName + "-all-tests.zip";
            StreamResource resource = new StreamResource(zipName,
                    () -> new ByteArrayInputStream(zipByteOut.toByteArray()));
            resource.setContentType("application/octet-stream");

            downloadLink.setHref(resource);
            downloadLink.setText("Скачать все JMX (ZIP)");
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.setVisible(true);

            showSingleNotification("Успешно сгенерировано " + selectedTypes.size() + " JMX-файлов и упаковано в ZIP.", "success");

        } catch (Exception ex) {
            showSingleNotification("Ошибка при генерации JMX: " + ex.getMessage(), "error");
            ex.printStackTrace();
        }
    }

    private List<String> getSelectedTestTypes() {
        List<String> selected = new ArrayList<>();
        if (Boolean.TRUE.equals(loadCheckBox.getValue())) {
            selected.add("Нагрузочное тестирование (Load Testing)");
        }
        if (Boolean.TRUE.equals(performanceCheckBox.getValue())) {
            selected.add("Тестирование производительности (Performance Testing)");
        }
        if (Boolean.TRUE.equals(stabilityCheckBox.getValue())) {
            selected.add("Тестирование стабильности (Stability Testing)");
        }
        if (Boolean.TRUE.equals(stressCheckBox.getValue())) {
            selected.add("Стрессовое тестирование (Stress Testing)");
        }
        if (Boolean.TRUE.equals(failoverCheckBox.getValue())) {
            selected.add("Тестирование отказоустойчивости (Failover Testing)");
        }
        if (Boolean.TRUE.equals(customCheckBox.getValue())) {
            selected.add("Кастомное тестирование (Custom Testing)");
        }
        return selected;
    }

    private void handleClear() {
        // Удаляем коллекцию из сессии
        VaadinSession.getCurrent().setAttribute("postmanCollection", null);
        scenarioNameField.clear();
        authCheckbox.setValue(false);

        loadCheckBox.setValue(false);
        performanceCheckBox.setValue(false);
        stabilityCheckBox.setValue(false);
        stressCheckBox.setValue(false);
        failoverCheckBox.setValue(false);
        customCheckBox.setValue(false);

        numThreadsField.clear();
        rampTimeField.clear();
        durationField.clear();
        numThreadsField.setVisible(false);
        rampTimeField.setVisible(false);
        durationField.setVisible(false);

        resetUpload();

        downloadLink.setVisible(false);
        downloadLink.setHref("");
        downloadLink.setText("");

        showSingleNotification("Все параметры были очищены.", "success");
    }

    private void resetUpload() {
        postmanUpload.clear();
    }

    private void showSingleNotification(String message, String themeName) {
        if (currentNotification != null) {
            currentNotification.close();
        }
        currentNotification = Notification.show(message, 3000, Position.TOP_CENTER);
        currentNotification.addClassName("custom-notification");
        if (themeName != null && !themeName.isEmpty()) {
            currentNotification.addThemeName(themeName);
        }
    }
}