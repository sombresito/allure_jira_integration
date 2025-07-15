package ru.iopump.qa.allure.gui.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Route(value = "load")
@PageTitle("Создать JMX сценарий")
@CssImport("./styles/load-view.css")
public class LoadView extends VerticalLayout {

    private MemoryBuffer buffer;
    private boolean includeAuth = false;
    private String testType = "";
    private JsonNode postmanCollection;

    public LoadView() {
        // Настройка основного стиля макета
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("main-layout");

        // Создаем заголовок
        H1 header = new H1("Создать JMX сценарий");
        header.addClassName("header-title");
        add(header);
        setHorizontalComponentAlignment(Alignment.CENTER, header);

        // Компонент загрузки файла
        buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".json");
        upload.setMaxFiles(1);
        Label uploadLabel = new Label("Перетащите файл Postman коллекции сюда или нажмите для выбора файла");
        uploadLabel.addClassName("upload-label");
        upload.setDropLabel(uploadLabel);
        upload.addClassName("upload-component");
        upload.addSucceededListener(event -> {
            Notification notification = Notification.show("Файл " + event.getFileName() + " успешно загружен", 3000, Position.TOP_CENTER);
            notification.addThemeName("success");
            try {
                InputStream is = buffer.getInputStream();
                ObjectMapper mapper = new ObjectMapper();
                postmanCollection = mapper.readTree(is);
            } catch (IOException e) {
                Notification errorNotification = Notification.show("Ошибка при чтении файла: " + e.getMessage(), 5000, Position.TOP_CENTER);
                errorNotification.addThemeName("error");
            }
        });
        upload.getStyle().set("margin-top", "20px");
        add(upload);
        setHorizontalComponentAlignment(Alignment.CENTER, upload);

        // Чекбокс для includeAuth
        Checkbox authCheckbox = new Checkbox("Присутствует ли в вашей коллекции метод авторизации?");
        authCheckbox.addClassName("checkbox");
        authCheckbox.addValueChangeListener(event -> includeAuth = event.getValue());
        add(authCheckbox);
        setHorizontalComponentAlignment(Alignment.CENTER, authCheckbox);

        // ComboBox для выбора типа тестирования
        Label testTypeLabel = new Label("Выберите тип тестирования:");
        testTypeLabel.addClassName("label");
        add(testTypeLabel);
        setHorizontalComponentAlignment(Alignment.CENTER, testTypeLabel);

        ComboBox<String> testTypeComboBox = new ComboBox<>();
        testTypeComboBox.setItems(
                "Нагрузочное тестирование (Load Testing)",
                "Тестирование производительности (Performance Testing)",
                "Тестирование стабильности (Stability Testing)",
                "Стрессовое тестирование (Stress Testing)",
                "Тестирование отказоустойчивости (Failover Testing)"
        );
        testTypeComboBox.addValueChangeListener(event -> testType = event.getValue());
        testTypeComboBox.addClassName("combobox");
        testTypeComboBox.setWidth("400px"); // Устанавливаем ширину для отображения полного текста
        add(testTypeComboBox);
        setHorizontalComponentAlignment(Alignment.CENTER, testTypeComboBox);

        // Кнопка для генерации JMX файла
        Button generateButton = new Button("Создать JMX файл", e -> {
            if (postmanCollection == null) {
                Notification notification = Notification.show("Пожалуйста, загрузите файл Postman коллекции", 3000, Position.TOP_CENTER);
                notification.addThemeName("warning");
                return;
            }
            if (testType == null || testType.isEmpty()) {
                Notification notification = Notification.show("Пожалуйста, выберите тип тестирования", 3000, Position.TOP_CENTER);
                notification.addThemeName("warning");
                return;
            }
            try {
                // Генерация JMX файла
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                generateJMX(postmanCollection, outputStream, includeAuth, testType);

                // Создаем ресурс для скачивания
                byte[] jmxData = outputStream.toByteArray();
                StreamResource resource = new StreamResource("generated.jmx", () -> new ByteArrayInputStream(jmxData));
                resource.setContentType("application/octet-stream");

                // Ссылка для скачивания файла
                Anchor downloadLink = new Anchor(resource, "Скачать JMX файл");
                downloadLink.getElement().setAttribute("download", true);
                downloadLink.addClassName("download-link");

                // Уведомление об успешной генерации
                Notification notification = Notification.show("JMX сценарий успешно создан.", 3000, Position.TOP_CENTER);
                notification.addThemeName("success");

                // Добавляем ссылку на страницу
                add(downloadLink);
                setHorizontalComponentAlignment(Alignment.CENTER, downloadLink);

            } catch (Exception ex) {
                Notification notification = Notification.show("Ошибка при генерации JMX файла: " + ex.getMessage(), 5000, Position.TOP_CENTER);
                notification.addThemeName("error");
                ex.printStackTrace();
            }
        });
        generateButton.addClassName("generate-button");
        add(generateButton);
        setHorizontalComponentAlignment(Alignment.CENTER, generateButton);

        // Кнопка "Назад"
        Button backButton = new Button("Вернуться на страницу отчётов", e ->
                getUI().ifPresent(ui -> ui.navigate("reports-preprod"))
        );
        backButton.addClassName("back-button");
        add(backButton);
        setHorizontalComponentAlignment(Alignment.CENTER, backButton);
    }

    private void generateJMX(JsonNode postmanCollection, OutputStream outputStream, boolean includeAuth, String testType) throws Exception {
        // Create XML document for JMeter Test Plan
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        Document doc = docFactory.newDocumentBuilder().newDocument();

        Element rootElement = doc.createElement("jmeterTestPlan");
        rootElement.setAttribute("version", "1.2");
        rootElement.setAttribute("properties", "5.0");
        rootElement.setAttribute("jmeter", "5.4.1");
        doc.appendChild(rootElement);

        Element hashTree = doc.createElement("hashTree");
        rootElement.appendChild(hashTree);

        Element testPlan = createTestPlan(doc);
        hashTree.appendChild(testPlan);

        Element testPlanHashTree = doc.createElement("hashTree");
        hashTree.appendChild(testPlanHashTree);

        Element threadGroup = createThreadGroup(doc, testType);
        testPlanHashTree.appendChild(threadGroup);

        Element threadGroupHashTree = doc.createElement("hashTree");
        testPlanHashTree.appendChild(threadGroupHashTree);

        // Get headers from the collection level
        Map<String, String> parentHeaders = getHeadersFromCollection(postmanCollection);

        // Extract requests from the collection
        List<JsonNode> requests = extractRequests(postmanCollection.get("item"), parentHeaders, null);

        for (int i = 0; i < requests.size(); i++) {
            JsonNode req = requests.get(i);
            Element sampler = createHttpSampler(doc, req);
            threadGroupHashTree.appendChild(sampler);

            Element samplerHashTree = doc.createElement("hashTree");
            threadGroupHashTree.appendChild(samplerHashTree);

            // Если требуется добавить JSON Extractor для токена
            if (includeAuth && i == 0) {
                Element extractor = createTokenExtractor(doc);
                samplerHashTree.appendChild(extractor);
                Element extractorHashTree = doc.createElement("hashTree");
                samplerHashTree.appendChild(extractorHashTree);
            }

            // Добавляем Header Manager
            // Для последующих запросов добавляем заголовок Authorization
            boolean addAuthorizationHeader = includeAuth && i > 0;
            Element headerManager = createHeaderManager(doc, req, addAuthorizationHeader);
            samplerHashTree.appendChild(headerManager);
            Element headerManagerHashTree = doc.createElement("hashTree");
            samplerHashTree.appendChild(headerManagerHashTree);
        }

        // Save XML document to outputStream
        writeDocumentToOutputStream(doc, outputStream);
    }

    private Element createTestPlan(Document doc) {
        Element testPlan = doc.createElement("TestPlan");
        testPlan.setAttribute("guiclass", "TestPlanGui");
        testPlan.setAttribute("testclass", "TestPlan");
        testPlan.setAttribute("testname", "Test Plan");
        testPlan.setAttribute("enabled", "true");

        appendChildWithText(doc, testPlan, "stringProp", "TestPlan.comments", "");
        appendChildWithText(doc, testPlan, "boolProp", "TestPlan.functional_mode", "false");
        appendChildWithText(doc, testPlan, "boolProp", "TestPlan.serialize_threadgroups", "false");

        Element elementProp = doc.createElement("elementProp");
        elementProp.setAttribute("name", "TestPlan.user_defined_variables");
        elementProp.setAttribute("elementType", "Arguments");
        elementProp.setAttribute("guiclass", "ArgumentsPanel");
        elementProp.setAttribute("testclass", "Arguments");
        elementProp.setAttribute("testname", "User Defined Variables");
        elementProp.setAttribute("enabled", "true");

        Element collectionProp = doc.createElement("collectionProp");
        collectionProp.setAttribute("name", "Arguments.arguments");
        elementProp.appendChild(collectionProp);

        testPlan.appendChild(elementProp);
        appendChildWithText(doc, testPlan, "stringProp", "TestPlan.user_define_classpath", "");

        return testPlan;
    }

    private Element createThreadGroup(Document doc, String testType) {
        Element threadGroup = doc.createElement("ThreadGroup");
        threadGroup.setAttribute("guiclass", "ThreadGroupGui");
        threadGroup.setAttribute("testclass", "ThreadGroup");
        threadGroup.setAttribute("testname", "Thread Group");
        threadGroup.setAttribute("enabled", "true");

        String numThreads = "1";
        String rampTime = "1";
        String loops = "1";
        String scheduler = "false";
        String duration = "";
        String delay = "";

        switch (testType) {
            case "Нагрузочное тестирование (Load Testing)":
                numThreads = "100";
                rampTime = "60";
                loops = "1";
                scheduler = "false";
                break;
            case "Тестирование производительности (Performance Testing)":
                numThreads = "200";
                rampTime = "120";
                loops = "1";
                scheduler = "false";
                break;
            case "Тестирование стабильности (Stability Testing)":
                numThreads = "50";
                rampTime = "30";
                loops = "-1"; // бесконечные циклы
                scheduler = "true";
                duration = "3600"; // 1 час
                break;
            case "Стрессовое тестирование (Stress Testing)":
                numThreads = "1000";
                rampTime = "300";
                loops = "1";
                scheduler = "false";
                break;
            case "Тестирование отказоустойчивости (Failover Testing)":
                numThreads = "100";
                rampTime = "60";
                loops = "1";
                scheduler = "false";
                break;
            default:
                break;
        }

        appendChildWithText(doc, threadGroup, "stringProp", "ThreadGroup.on_sample_error", "continue");

        Element elementProp = doc.createElement("elementProp");
        elementProp.setAttribute("name", "ThreadGroup.main_controller");
        elementProp.setAttribute("elementType", "LoopController");
        elementProp.setAttribute("guiclass", "LoopControlPanel");
        elementProp.setAttribute("testclass", "LoopController");
        elementProp.setAttribute("testname", "Loop Controller");
        elementProp.setAttribute("enabled", "true");

        appendChildWithText(doc, elementProp, "boolProp", "LoopController.continue_forever", loops.equals("-1") ? "true" : "false");
        appendChildWithText(doc, elementProp, "stringProp", "LoopController.loops", loops);

        threadGroup.appendChild(elementProp);
        appendChildWithText(doc, threadGroup, "stringProp", "ThreadGroup.num_threads", numThreads);
        appendChildWithText(doc, threadGroup, "stringProp", "ThreadGroup.ramp_time", rampTime);
        appendChildWithText(doc, threadGroup, "boolProp", "ThreadGroup.scheduler", scheduler);
        appendChildWithText(doc, threadGroup, "stringProp", "ThreadGroup.duration", duration);
        appendChildWithText(doc, threadGroup, "stringProp", "ThreadGroup.delay", delay);

        return threadGroup;
    }

    private void appendChildWithText(Document doc, Element parent, String tagName, String nameAttribute, String textContent) {
        Element element = doc.createElement(tagName);
        element.setAttribute("name", nameAttribute);
        element.setTextContent(textContent);
        parent.appendChild(element);
    }

    private Map<String, String> getHeadersFromCollection(JsonNode collection) {
        Map<String, String> headers = new HashMap<>();
        if (collection.has("header")) {
            JsonNode collectionHeaders = collection.get("header");
            if (collectionHeaders.isArray()) {
                for (JsonNode header : collectionHeaders) {
                    String key = header.has("key") ? header.get("key").asText() : null;
                    String value = header.has("value") ? header.get("value").asText() : null;
                    if (key != null) {
                        headers.put(key, value);
                    }
                }
            } else if (collectionHeaders.isTextual()) {
                String[] headerLines = collectionHeaders.asText().trim().split("\n");
                for (String line : headerLines) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        headers.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }
        return headers;
    }

    private List<JsonNode> extractRequests(JsonNode items, Map<String, String> parentHeaders, JsonNode parentAuth) {
        List<JsonNode> requests = new ArrayList<>();
        if (parentHeaders == null) {
            parentHeaders = new HashMap<>();
        }
        if (parentAuth == null) {
            parentAuth = null;
        }
        if (items.isArray()) {
            for (JsonNode item : items) {
                Map<String, String> currentHeaders = new HashMap<>(parentHeaders);
                JsonNode currentAuth = parentAuth;

                Map<String, String> itemHeaders = getHeadersFromItem(item);
                currentHeaders.putAll(itemHeaders);

                if (item.has("auth")) {
                    currentAuth = item.get("auth");
                }
                if (item.has("request")) {
                    Map<String, String> requestHeaders = getHeadersFromItem(item.get("request"));
                    currentHeaders.putAll(requestHeaders);

                    ((ObjectNode) item).set("current_headers", new ObjectMapper().valueToTree(currentHeaders));
                    ((ObjectNode) item).set("current_auth", currentAuth);
                    requests.add(item);
                }
                if (item.has("item")) {
                    requests.addAll(extractRequests(item.get("item"), currentHeaders, currentAuth));
                }
            }
        }
        return requests;
    }

    private Map<String, String> getHeadersFromItem(JsonNode item) {
        Map<String, String> headers = new HashMap<>();
        if (item.has("header")) {
            JsonNode itemHeaders = item.get("header");
            if (itemHeaders.isArray()) {
                for (JsonNode header : itemHeaders) {
                    String key = header.has("key") ? header.get("key").asText() : null;
                    String value = header.has("value") ? header.get("value").asText() : null;
                    if (key != null) {
                        headers.put(key, value);
                    }
                }
            } else if (itemHeaders.isTextual()) {
                String[] headerLines = itemHeaders.asText().trim().split("\n");
                for (String line : headerLines) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        headers.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }
        return headers;
    }

    private Element createHttpSampler(Document doc, JsonNode request) throws UnsupportedEncodingException, MalformedURLException {
        Element sampler = doc.createElement("HTTPSamplerProxy");
        sampler.setAttribute("guiclass", "HttpTestSampleGui");
        sampler.setAttribute("testclass", "HTTPSamplerProxy");
        sampler.setAttribute("testname", request.get("name").asText());
        sampler.setAttribute("enabled", "true");

        // Create elementProp for arguments
        Element elementProp = doc.createElement("elementProp");
        elementProp.setAttribute("name", "HTTPsampler.Arguments");
        elementProp.setAttribute("elementType", "Arguments");
        elementProp.setAttribute("guiclass", "HTTPArgumentsPanel");
        elementProp.setAttribute("testclass", "Arguments");
        elementProp.setAttribute("testname", "User Defined Variables");
        elementProp.setAttribute("enabled", "true");

        Element collectionProp = doc.createElement("collectionProp");
        collectionProp.setAttribute("name", "Arguments.arguments");
        elementProp.appendChild(collectionProp);
        sampler.appendChild(elementProp);

        // Extract method, URL, headers, body from the request
        String method = request.get("request").get("method").asText();
        JsonNode urlNode = request.get("request").get("url");

        String protocol = "";
        String host = "";
        String port = "";
        String path = "";
        List<Map<String, String>> queryParams = new ArrayList<>();

        if (urlNode.isObject()) {
            protocol = urlNode.has("protocol") ? urlNode.get("protocol").asText() : "";
            if (urlNode.has("host")) {
                host = String.join(".", getStringListFromJsonNode(urlNode.get("host")));
            }
            port = urlNode.has("port") ? urlNode.get("port").asText() : "";
            if (urlNode.has("path")) {
                path = "/" + String.join("/", getStringListFromJsonNode(urlNode.get("path")));
            }
            if (urlNode.has("query")) {
                for (JsonNode param : urlNode.get("query")) {
                    Map<String, String> paramMap = new HashMap<>();
                    paramMap.put("key", param.get("key").asText());
                    paramMap.put("value", param.get("value").asText());
                    queryParams.add(paramMap);
                }
            }
        } else if (urlNode.isTextual()) {
            String url = urlNode.asText();
            java.net.URL parsedUrl = new java.net.URL(url);
            protocol = parsedUrl.getProtocol();
            host = parsedUrl.getHost();
            port = parsedUrl.getPort() == -1 ? "" : String.valueOf(parsedUrl.getPort());
            path = parsedUrl.getPath();
            String query = parsedUrl.getQuery();
            if (query != null && !query.isEmpty()) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=", 2);
                    Map<String, String> paramMap = new HashMap<>();
                    paramMap.put("key", URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name()));
                    paramMap.put("value", keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()) : "");
                    queryParams.add(paramMap);
                }
            }
        }

        // Add query parameters
        for (Map<String, String> param : queryParams) {
            Element paramElement = doc.createElement("elementProp");
            paramElement.setAttribute("name", param.get("key"));
            paramElement.setAttribute("elementType", "HTTPArgument");
            collectionProp.appendChild(paramElement);

            appendChildWithText(doc, paramElement, "boolProp", "HTTPArgument.always_encode", "false");
            appendChildWithText(doc, paramElement, "stringProp", "Argument.name", param.get("key"));
            appendChildWithText(doc, paramElement, "stringProp", "Argument.value", param.get("value"));
            appendChildWithText(doc, paramElement, "stringProp", "Argument.metadata", "=");
            appendChildWithText(doc, paramElement, "boolProp", "HTTPArgument.use_equals", "true");
            appendChildWithText(doc, paramElement, "stringProp", "Argument.contentType", "");
        }

        // Set other properties
        appendChildWithText(doc, sampler, "stringProp", "HTTPSampler.domain", host);
        appendChildWithText(doc, sampler, "stringProp", "HTTPSampler.port", port);
        appendChildWithText(doc, sampler, "stringProp", "HTTPSampler.protocol", protocol);
        appendChildWithText(doc, sampler, "stringProp", "HTTPSampler.path", path);
        appendChildWithText(doc, sampler, "stringProp", "HTTPSampler.method", method);
        appendChildWithText(doc, sampler, "boolProp", "HTTPSampler.follow_redirects", "true");
        appendChildWithText(doc, sampler, "boolProp", "HTTPSampler.auto_redirects", "false");
        appendChildWithText(doc, sampler, "boolProp", "HTTPSampler.use_keepalive", "true");
        appendChildWithText(doc, sampler, "boolProp", "HTTPSampler.DO_MULTIPART_POST", "false");
        appendChildWithText(doc, sampler, "stringProp", "HTTPSampler.embedded_url_re", "");

        // Process request body
        JsonNode body = request.get("request").get("body");
        if (body != null && !body.isNull()) {
            String mode = body.has("mode") ? body.get("mode").asText() : "";
            if (mode.equals("raw")) {
                String rawBody = body.has("raw") ? body.get("raw").asText() : "";
                appendChildWithText(doc, sampler, "boolProp", "HTTPSampler.postBodyRaw", "true");

                Element paramElement = doc.createElement("elementProp");
                paramElement.setAttribute("name", "");
                paramElement.setAttribute("elementType", "HTTPArgument");
                collectionProp.appendChild(paramElement);

                appendChildWithText(doc, paramElement, "boolProp", "HTTPArgument.always_encode", "false");
                appendChildWithText(doc, paramElement, "stringProp", "Argument.name", "");
                appendChildWithText(doc, paramElement, "stringProp", "Argument.value", rawBody);
                appendChildWithText(doc, paramElement, "stringProp", "Argument.metadata", "=");
                appendChildWithText(doc, paramElement, "boolProp", "HTTPArgument.use_equals", "false");
                appendChildWithText(doc, paramElement, "stringProp", "Argument.contentType", "");
            }
            // Additional processing for other body types (urlencoded, formdata) if necessary
        }

        return sampler;
    }

    private List<String> getStringListFromJsonNode(JsonNode jsonNode) {
        List<String> list = new ArrayList<>();
        if (jsonNode.isArray()) {
            for (JsonNode node : jsonNode) {
                list.add(node.asText());
            }
        }
        return list;
    }

    private Element createTokenExtractor(Document doc) {
        Element extractor = doc.createElement("JSONPostProcessor");
        extractor.setAttribute("guiclass", "JSONPostProcessorGui");
        extractor.setAttribute("testclass", "JSONPostProcessor");
        extractor.setAttribute("testname", "Extract Access Token");
        extractor.setAttribute("enabled", "true");

        appendChildWithText(doc, extractor, "stringProp", "JSONPostProcessor.referenceNames", "access_token");
        appendChildWithText(doc, extractor, "stringProp", "JSONPostProcessor.jsonPathExprs", "$.access_token");
        appendChildWithText(doc, extractor, "stringProp", "JSONPostProcessor.match_numbers", "1");
        appendChildWithText(doc, extractor, "stringProp", "JSONPostProcessor.defaultValues", "");

        return extractor;
    }

    private Element createHeaderManager(Document doc, JsonNode request, boolean addAuthorizationHeader) {
        Element headerManager = doc.createElement("HeaderManager");
        headerManager.setAttribute("guiclass", "HeaderPanel");
        headerManager.setAttribute("testclass", "HeaderManager");
        headerManager.setAttribute("testname", "HTTP Header Manager");
        headerManager.setAttribute("enabled", "true");

        Element collectionProp = doc.createElement("collectionProp");
        collectionProp.setAttribute("name", "HeaderManager.headers");
        headerManager.appendChild(collectionProp);

        Map<String, String> currentHeaders = new HashMap<>();
        if (request.has("current_headers")) {
            JsonNode headersNode = request.get("current_headers");
            Iterator<Map.Entry<String, JsonNode>> fields = headersNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                currentHeaders.put(entry.getKey(), entry.getValue().asText());
            }
        }

        Map<String, String> autoGeneratedHeaders = new HashMap<>();
        autoGeneratedHeaders.put("User-Agent", "PostmanRuntime/7.42.0");
        autoGeneratedHeaders.put("Accept", "*/*");
        autoGeneratedHeaders.put("Accept-Encoding", "gzip, deflate, br");
        autoGeneratedHeaders.put("Connection", "keep-alive");
        autoGeneratedHeaders.put("Content-Type", "application/json");

        // Добавляем заголовок Authorization, если необходимо
        if (addAuthorizationHeader) {
            autoGeneratedHeaders.put("Authorization", "${access_token}");
        }

        for (Map.Entry<String, String> entry : autoGeneratedHeaders.entrySet()) {
            currentHeaders.putIfAbsent(entry.getKey(), entry.getValue());
        }

        // Add headers to HeaderManager
        for (Map.Entry<String, String> entry : currentHeaders.entrySet()) {
            Element headerElement = doc.createElement("elementProp");
            headerElement.setAttribute("name", entry.getKey());
            headerElement.setAttribute("elementType", "Header");
            collectionProp.appendChild(headerElement);

            appendChildWithText(doc, headerElement, "stringProp", "Header.name", entry.getKey());
            appendChildWithText(doc, headerElement, "stringProp", "Header.value", entry.getValue());
        }

        return headerManager;
    }

    private void writeDocumentToOutputStream(Document doc, OutputStream outputStream) throws Exception {
        // Write XML document to OutputStream
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        // Set indentation for readability
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(outputStream);
        transformer.transform(source, result);
    }
}