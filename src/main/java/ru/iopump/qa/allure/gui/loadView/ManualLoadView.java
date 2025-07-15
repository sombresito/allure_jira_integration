package ru.iopump.qa.allure.gui.loadView;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import ru.iopump.qa.allure.service.JmxGenerationService;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Страница для ручного создания JMX-сценария (Manual).
 */
@Route("manual-load")
@PageTitle("Создание JMX сценария вручную")
@CssImport("./styles/load-view.css")
public class ManualLoadView extends VerticalLayout {

    private final HorizontalLayout topBar;
    private final H1 header;
    private final Button backButton;
    private final Label hintLabel;
    private final ManualRequestFormList requestList;
    private final HorizontalLayout plusMinusButtons;
    private final HorizontalLayout expandCollapseButtons;
    private final HorizontalLayout buttonsLayout;
    private final Button generateButton;
    private final Button clearButton;
    private final Anchor downloadLink;
    private Notification currentNotification;
    private final JmxGenerationService jmxGenerationService = new JmxGenerationService();

    public ManualLoadView() {
        setPadding(false);
        setSpacing(true);
        addClassName("main-layout");

        // Верхняя панель
        topBar = new HorizontalLayout();
        topBar.addClassName("top-bar");
        topBar.setWidthFull();
        topBar.setAlignItems(Alignment.CENTER);
        topBar.setJustifyContentMode(JustifyContentMode.BETWEEN);

        header = new H1("Создать JMX сценарий вручную");
        header.getElement().getStyle().set("color", "#FDFEFE");
        header.addClassName("header-title");

        backButton = new Button("Вернуться на главную НТ", e ->
                getUI().ifPresent(ui -> ui.navigate("load-main"))
        );
        backButton.addClassName("back-button");

        topBar.add(header, backButton);
        add(topBar);

        // Надпись с инструкцией
        hintLabel = new Label("Добавьте один или несколько запросов (полный URL + метод + тело):");
        hintLabel.addClassName("hint-title");
        add(hintLabel);
        setHorizontalComponentAlignment(Alignment.CENTER, hintLabel);

        // Список запросов и кнопки управления
        requestList = new ManualRequestFormList();
        plusMinusButtons = requestList.getManageButtons();

        // Кнопки "Развернуть все" / "Свернуть все"
        expandCollapseButtons = new HorizontalLayout();
        expandCollapseButtons.setSpacing(true);
        expandCollapseButtons.setPadding(false);
        expandCollapseButtons.setAlignItems(Alignment.CENTER);

        Button expandAllButton = new Button("Развернуть все", e -> handleExpandAll(true));
        expandAllButton.addClassName("generate-button");

        Button collapseAllButton = new Button("Свернуть все", e -> handleExpandAll(false));
        collapseAllButton.addClassName("clear-button");

        expandCollapseButtons.add(expandAllButton, collapseAllButton);
        setHorizontalComponentAlignment(Alignment.CENTER, expandCollapseButtons);

        add(requestList);
        add(plusMinusButtons);
        add(expandCollapseButtons);
        setHorizontalComponentAlignment(Alignment.CENTER, requestList);
        setHorizontalComponentAlignment(Alignment.CENTER, plusMinusButtons);

        // Кнопки "Создать JMX" и "Очистить"
        buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSpacing(true);
        buttonsLayout.setPadding(false);
        buttonsLayout.setAlignItems(Alignment.CENTER);

        generateButton = new Button("Создать JMX (Manual)", e -> handleGenerateManualJMX());
        generateButton.addClassName("generate-button");

        clearButton = new Button("Очистить", e -> handleClear());
        clearButton.addClassName("clear-button");

        buttonsLayout.add(generateButton, clearButton);
        add(buttonsLayout);
        setHorizontalComponentAlignment(Alignment.CENTER, buttonsLayout);

        // Ссылка для скачивания
        downloadLink = new Anchor();
        downloadLink.setText("Скачать JMX сценарий");
        downloadLink.addClassName("download-button");
        downloadLink.setVisible(false);
        add(downloadLink);
        setHorizontalComponentAlignment(Alignment.CENTER, downloadLink);
    }

    /**
     * Обработка создания JMX-файла.
     */
    private void handleGenerateManualJMX() {
        if (requestList.getRequestForms().isEmpty()) {
            showSingleNotification("Укажите хотя бы один запрос", "warning");
            return;
        }
        try {
            byte[] jmxData = createValidJmxFromForms();
            StreamResource resource = new StreamResource("manual-generated.jmx",
                    () -> new ByteArrayInputStream(jmxData));
            resource.setContentType("application/octet-stream");

            downloadLink.setHref(resource);
            downloadLink.setText("Скачать JMX сценарий");
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.setVisible(true);

            showSingleNotification("Успешно сгенерирован JMX-файл.", "success");
            getUI().ifPresent(ui -> ui.getPage().executeJs("window.scrollTo(0,0);"));

        } catch (Exception ex) {
            showSingleNotification("Ошибка при генерации JMX: " + ex.getMessage(), "error");
            ex.printStackTrace();
        }
    }

    /**
     * Генерация JMX-файла с нужной структурой.
     */
    private byte[] createValidJmxFromForms() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<jmeterTestPlan version=\"1.2\" properties=\"5.0\" jmeter=\"5.4.1\">\n")
                .append("  <hashTree>\n")
                // TestPlan
                .append("    <TestPlan guiclass=\"TestPlanGui\" testclass=\"TestPlan\" testname=\"Test Plan\" enabled=\"true\">\n")
                .append("      <stringProp name=\"TestPlan.comments\"></stringProp>\n")
                .append("      <boolProp name=\"TestPlan.functional_mode\">false</boolProp>\n")
                .append("      <boolProp name=\"TestPlan.tearDown_on_shutdown\">true</boolProp>\n")
                .append("      <boolProp name=\"TestPlan.serialize_threadgroups\">false</boolProp>\n")
                .append("      <elementProp name=\"TestPlan.user_defined_variables\" elementType=\"Arguments\" guiclass=\"ArgumentsPanel\" testclass=\"Arguments\" testname=\"User Defined Variables\" enabled=\"true\">\n")
                .append("        <collectionProp name=\"Arguments.arguments\"/>\n")
                .append("      </elementProp>\n")
                .append("      <stringProp name=\"TestPlan.user_define_classpath\"></stringProp>\n")
                .append("    </TestPlan>\n")
                .append("    <hashTree>\n")
                // ThreadGroup
                .append("      <ThreadGroup guiclass=\"ThreadGroupGui\" testclass=\"ThreadGroup\" testname=\"Thread Group\" enabled=\"true\">\n")
                .append("        <stringProp name=\"ThreadGroup.on_sample_error\">continue</stringProp>\n")
                .append("        <elementProp name=\"ThreadGroup.main_controller\" elementType=\"LoopController\" guiclass=\"LoopControlPanel\" testclass=\"LoopController\" testname=\"Loop Controller\" enabled=\"true\">\n")
                .append("          <boolProp name=\"LoopController.continue_forever\">false</boolProp>\n")
                .append("          <stringProp name=\"LoopController.loops\">1</stringProp>\n")
                .append("        </elementProp>\n")
                .append("        <stringProp name=\"ThreadGroup.num_threads\">1</stringProp>\n")
                .append("        <stringProp name=\"ThreadGroup.ramp_time\">1</stringProp>\n")
                .append("        <boolProp name=\"ThreadGroup.same_user_on_next_iteration\">true</boolProp>\n")
                .append("        <stringProp name=\"ThreadGroup.delay_start\">0</stringProp>\n")
                .append("        <boolProp name=\"ThreadGroup.scheduler\">false</boolProp>\n")
                .append("        <stringProp name=\"ThreadGroup.duration\"></stringProp>\n")
                .append("        <stringProp name=\"ThreadGroup.delay\"></stringProp>\n")
                .append("      </ThreadGroup>\n")
                .append("      <hashTree>\n");

        // Генерация запросов
        for (RequestFromLoad form : requestList.getRequestForms()) {
            ParsedUrl pUrl = parseUrl(form.getUrl());
            String samplerName = escapeXml(form.getTitle());

            sb.append("        <!-- ").append(samplerName).append(" -->\n")
                    .append("        <HTTPSamplerProxy guiclass=\"HttpTestSampleGui\" testclass=\"HTTPSamplerProxy\" ")
                    .append("testname=\"").append(samplerName).append("\" enabled=\"true\">\n")
                    .append("          <boolProp name=\"HTTPSampler.postBodyRaw\">true</boolProp>\n")
                    .append("          <elementProp name=\"HTTPsampler.Arguments\" elementType=\"Arguments\" ")
                    .append("guiclass=\"HTTPArgumentsPanel\" testclass=\"Arguments\" testname=\"User Defined Variables\" enabled=\"true\">\n")
                    .append("            <collectionProp name=\"Arguments.arguments\">\n")
                    .append("              <elementProp name=\"\" elementType=\"HTTPArgument\">\n")
                    .append("                <boolProp name=\"HTTPArgument.always_encode\">false</boolProp>\n")
                    .append("                <stringProp name=\"Argument.value\">")
                    .append(form.getBody() == null ? "" : escapeXml(form.getBody()))
                    .append("</stringProp>\n")
                    .append("                <stringProp name=\"Argument.metadata\">=</stringProp>\n")
                    .append("              </elementProp>\n")
                    .append("            </collectionProp>\n")
                    .append("          </elementProp>\n")
                    .append("          <stringProp name=\"HTTPSampler.domain\">").append(escapeXml(pUrl.domain)).append("</stringProp>\n")
                    .append("          <stringProp name=\"HTTPSampler.port\">").append(escapeXml(pUrl.port)).append("</stringProp>\n")
                    .append("          <stringProp name=\"HTTPSampler.protocol\">").append(escapeXml(pUrl.protocol)).append("</stringProp>\n")
                    .append("          <stringProp name=\"HTTPSampler.path\">").append(escapeXml(pUrl.path)).append("</stringProp>\n")
                    .append("          <stringProp name=\"HTTPSampler.method\">").append(escapeXml(form.getMethod())).append("</stringProp>\n")
                    .append("          <stringProp name=\"HTTPSampler.contentEncoding\">UTF-8</stringProp>\n")
                    .append("          <stringProp name=\"HTTPSampler.connect_timeout\"></stringProp>\n")
                    .append("          <stringProp name=\"HTTPSampler.response_timeout\"></stringProp>\n")
                    .append("          <boolProp name=\"HTTPSampler.auto_redirects\">false</boolProp>\n")
                    .append("          <boolProp name=\"HTTPSampler.follow_redirects\">true</boolProp>\n")
                    .append("          <boolProp name=\"HTTPSampler.use_keepalive\">true</boolProp>\n")
                    .append("          <boolProp name=\"HTTPSampler.DO_MULTIPART_POST\">false</boolProp>\n")
                    .append("          <boolProp name=\"HTTPSampler.BROWSER_COMPATIBLE_MULTIPART\">false</boolProp>\n")
                    .append("        </HTTPSamplerProxy>\n")
                    .append("        <hashTree>\n");

            // Если есть заголовки, добавляем HeaderManager
            if (hasNonEmptyHeader(form)) {
                sb.append("          <HeaderManager guiclass=\"HeaderPanel\" testclass=\"HeaderManager\" testname=\"HTTP Header Manager\" enabled=\"true\">\n")
                        .append("            <collectionProp name=\"HeaderManager.headers\">\n");
                for (HeaderRow hr : form.getHeaders()) {
                    String hName = escapeXml(hr.getHeaderName());
                    String hValue = escapeXml(hr.getHeaderValue());
                    if ((hName != null && !hName.isEmpty()) || (hValue != null && !hValue.isEmpty())) {
                        sb.append("              <elementProp name=\"\" elementType=\"Header\">\n")
                                .append("                <stringProp name=\"Header.name\">").append(hName).append("</stringProp>\n")
                                .append("                <stringProp name=\"Header.value\">").append(hValue).append("</stringProp>\n")
                                .append("              </elementProp>\n");
                    }
                }
                sb.append("            </collectionProp>\n")
                        .append("          </HeaderManager>\n")
                        .append("          <hashTree/>\n");
            }
            sb.append("        </hashTree>\n");
        }
        sb.append("      </hashTree>\n")
                .append("    </hashTree>\n")
                .append("  </hashTree>\n")
                .append("</jmeterTestPlan>\n");

        return sb.toString().getBytes();
    }

    /**
     * Вспомогательный метод для разбора URL.
     */
    private ParsedUrl parseUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return new ParsedUrl("", "", "", "");
        }
        try {
            URL parsed = new URL(url.trim());
            String protocol = parsed.getProtocol();
            String domain = parsed.getHost();
            String port = parsed.getPort() == -1 ? "" : String.valueOf(parsed.getPort());
            String path = parsed.getPath();
            if (parsed.getQuery() != null) {
                path += "?" + parsed.getQuery();
            }
            return new ParsedUrl(protocol, domain, port, path);
        } catch (MalformedURLException e) {
            return new ParsedUrl("", "", "", "");
        }
    }

    /**
     * Проверка наличия хотя бы одного непустого заголовка.
     */
    private boolean hasNonEmptyHeader(RequestFromLoad form) {
        return form.getHeaders().stream().anyMatch(hr ->
                (hr.getHeaderName() != null && !hr.getHeaderName().isEmpty()) ||
                        (hr.getHeaderValue() != null && !hr.getHeaderValue().isEmpty()));
    }

    /**
     * Экранирование спецсимволов для корректного XML.
     */
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Очистка формы – удаление всех запросов и скрытие ссылки скачивания.
     */
    private void handleClear() {
        requestList.clearAll();
        downloadLink.setVisible(false);
        downloadLink.setHref("");
        downloadLink.setText("");
        showSingleNotification("Все параметры были очищены.", "success");
    }

    /**
     * Разворачивание или сворачивание всех запросов.
     */
    private void handleExpandAll(boolean expand) {
        requestList.getRequestForms().forEach(r -> r.getDetails().setOpened(expand));
    }

    /**
     * Отображение уведомления.
     */
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

    // Вспомогательный класс для хранения разобранного URL
    private static class ParsedUrl {
        final String protocol;
        final String domain;
        final String port;
        final String path;
        ParsedUrl(String protocol, String domain, String port, String path) {
            this.protocol = protocol;
            this.domain = domain;
            this.port = port;
            this.path = path;
        }
    }

    // ---------------------------------------------------------------------------------
    // Вспомогательный класс – список запросов
    // ---------------------------------------------------------------------------------
    private class ManualRequestFormList extends VerticalLayout {
        private final VerticalLayout formsContainer = new VerticalLayout();
        private final List<RequestFromLoad> requestForms = new ArrayList<>();
        private final HorizontalLayout manageButtons;

        public ManualRequestFormList() {
            setSpacing(true);
            setPadding(false);
            addClassName("manual-request-list");
            setDefaultHorizontalComponentAlignment(Alignment.CENTER);

            manageButtons = new HorizontalLayout();
            manageButtons.setSpacing(true);
            manageButtons.setPadding(false);
            manageButtons.addClassName("manual-request-list-buttons");

            Button addButton = new Button("+", e -> addRequestForm());
            addButton.addClassName("generate-button");
            Button removeButton = new Button("–", e -> removeRequestForm());
            removeButton.addClassName("clear-button");

            manageButtons.add(addButton, removeButton);
            formsContainer.setSpacing(true);
            formsContainer.setPadding(false);
            formsContainer.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

            addRequestForm();
            add(formsContainer);
        }

        private void addRequestForm() {
            int newIndex = requestForms.size() + 1;
            RequestFromLoad form = new RequestFromLoad(newIndex);
            requestForms.forEach(existing -> existing.getDetails().setOpened(false));
            form.getDetails().setOpened(true);
            requestForms.add(form);
            formsContainer.add(form);
        }

        private void removeRequestForm() {
            if (!requestForms.isEmpty()) {
                RequestFromLoad last = requestForms.remove(requestForms.size() - 1);
                formsContainer.remove(last);
            }
        }

        public List<RequestFromLoad> getRequestForms() {
            return requestForms;
        }

        public HorizontalLayout getManageButtons() {
            return manageButtons;
        }

        public void clearAll() {
            formsContainer.removeAll();
            requestForms.clear();
            addRequestForm();
        }
    }

    // ---------------------------------------------------------------------------------
    // Вспомогательный класс – форма запроса (Details + поля + заголовки)
    // ---------------------------------------------------------------------------------
    private static class RequestFromLoad extends VerticalLayout {
        private final String requestTitle;
        private final Details details;
        private final TextField urlField;
        private final ComboBox<String> methodBox;
        private final TextArea bodyArea;
        private final List<HeaderRow> headers = new ArrayList<>();
        private final Details headersDetails;

        public RequestFromLoad(int index) {
            addClassName("request-form");
            setSpacing(false);
            setPadding(false);
            setDefaultHorizontalComponentAlignment(Alignment.CENTER);

            requestTitle = "Запрос №" + index;
            urlField = new TextField("URL");
            urlField.setWidth("100%");

            methodBox = new ComboBox<>("Method");
            methodBox.setItems("GET", "POST", "PUT", "DELETE", "PATCH");
            methodBox.setWidth("100%");

            bodyArea = new TextArea("Body (JSON)");
            bodyArea.setWidth("100%");
            bodyArea.setHeight("100px");

            Div formContent = new Div(urlField, methodBox, bodyArea);
            formContent.addClassName("request-form-content");
            formContent.setWidth("100%");

            VerticalLayout headersContainer = new VerticalLayout();
            headersContainer.setSpacing(false);
            headersContainer.setPadding(false);
            headersContainer.setDefaultHorizontalComponentAlignment(Alignment.START);

            addHeaderRow(headersContainer);

            HorizontalLayout headerButtons = new HorizontalLayout();
            headerButtons.setSpacing(true);
            headerButtons.setPadding(false);

            Button addHeaderButton = new Button("+", e -> addHeaderRow(headersContainer));
            addHeaderButton.addClassName("generate-button");

            Button removeHeaderButton = new Button("–", e -> removeHeaderRow(headersContainer));
            removeHeaderButton.addClassName("clear-button");

            headerButtons.add(addHeaderButton, removeHeaderButton);
            headersContainer.add(headerButtons);

            headersDetails = new Details("Headers", headersContainer);
            headersDetails.addClassName("combobox");
            headersDetails.setOpened(false);

            formContent.add(headersDetails);
            details = new Details(requestTitle, formContent);
            details.addClassName("combobox");
            details.setOpened(false);

            add(details);
        }

        private void addHeaderRow(VerticalLayout container) {
            HeaderRow row = new HeaderRow();
            headers.add(row);
            container.add(row);
        }

        private void removeHeaderRow(VerticalLayout container) {
            if (!headers.isEmpty()) {
                HeaderRow last = headers.remove(headers.size() - 1);
                container.remove(last);
            }
        }

        public String getTitle() {
            return requestTitle;
        }

        public String getUrl() {
            return urlField.getValue();
        }

        public String getMethod() {
            String val = methodBox.getValue();
            return (val == null || val.isEmpty()) ? "GET" : val;
        }

        public String getBody() {
            return bodyArea.getValue();
        }

        public Details getDetails() {
            return details;
        }

        public List<HeaderRow> getHeaders() {
            return headers;
        }
    }

    // ----------------------------------------------------------------------------
    // Класс для одной строки заголовка: два текстовых поля
    // ----------------------------------------------------------------------------
    private static class HeaderRow extends HorizontalLayout {
        private final TextField nameField;
        private final TextField valueField;

        public HeaderRow() {
            setSpacing(true);
            setPadding(false);
            nameField = new TextField("Header Name");
            nameField.setWidth("200px");
            valueField = new TextField("Header Value");
            valueField.setWidth("200px");
            add(nameField, valueField);
        }

        public String getHeaderName() {
            return nameField.getValue();
        }

        public String getHeaderValue() {
            return valueField.getValue();
        }
    }
}
