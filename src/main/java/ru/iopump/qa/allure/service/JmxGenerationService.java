package ru.iopump.qa.allure.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.iopump.qa.allure.model.TestParameters;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Сервис для генерации одного JMX-файла
 */
public class JmxGenerationService {

    public void generateJMX(List<JsonNode> requests, TestParameters params, OutputStream outputStream) throws Exception {
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

        Element threadGroup = createThreadGroup(doc, params);
        testPlanHashTree.appendChild(threadGroup);

        Element threadGroupHashTree = doc.createElement("hashTree");
        testPlanHashTree.appendChild(threadGroupHashTree);

        boolean includeAuth = params.isIncludeAuth();

        // Для каждого запроса создаём HTTP-сэмплер
        for (int i = 0; i < requests.size(); i++) {
            JsonNode req = requests.get(i);
            Element sampler = createHttpSampler(doc, req);
            threadGroupHashTree.appendChild(sampler);

            Element samplerHashTree = doc.createElement("hashTree");
            threadGroupHashTree.appendChild(samplerHashTree);

            // Если авторизация включена, первый запрос экстрактит токен
            if (includeAuth && i == 0) {
                Element extractor = createTokenExtractor(doc);
                samplerHashTree.appendChild(extractor);
                Element extractorHashTree = doc.createElement("hashTree");
                samplerHashTree.appendChild(extractorHashTree);
            }

            // HeaderManager
            boolean addAuthorizationHeader = includeAuth && i > 0;
            Element headerManager = createHeaderManager(doc, req, addAuthorizationHeader);
            samplerHashTree.appendChild(headerManager);
            Element headerManagerHashTree = doc.createElement("hashTree");
            samplerHashTree.appendChild(headerManagerHashTree);
        }

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

    private Element createThreadGroup(Document doc, TestParameters params) {
        Element threadGroup = doc.createElement("ThreadGroup");
        threadGroup.setAttribute("guiclass", "ThreadGroupGui");
        threadGroup.setAttribute("testclass", "ThreadGroup");
        threadGroup.setAttribute("testname", "Thread Group");
        threadGroup.setAttribute("enabled", "true");

        String testType = params.getTestType();
        String numThreads;
        String rampTime;
        String loops;
        String scheduler = "false";
        String duration = "";
        String delay = "";

        switch (testType) {
            case "Нагрузочное тестирование (Load Testing)":
                numThreads = "100";
                rampTime = "60";
                loops = "1";
                break;
            case "Тестирование производительности (Performance Testing)":
                numThreads = "200";
                rampTime = "120";
                loops = "1";
                break;
            case "Тестирование стабильности (Stability Testing)":
                numThreads = "50";
                rampTime = "30";
                loops = "-1";
                scheduler = "true";
                duration = "3600";
                break;
            case "Стрессовое тестирование (Stress Testing)":
                numThreads = "1000";
                rampTime = "300";
                loops = "1";
                break;
            case "Тестирование отказоустойчивости (Failover Testing)":
                numThreads = "100";
                rampTime = "60";
                loops = "1";
                break;
            case "Кастомное тестирование (Custom Testing)":
                // Берём поля из params
                numThreads = String.valueOf(params.getNumThreads());
                rampTime = String.valueOf(params.getRampTime());
                loops = String.valueOf(params.getLoops());
                duration = String.valueOf(params.getDuration());
                break;
            default:
                // Значения по умолчанию
                numThreads = "1";
                rampTime = "1";
                loops = "1";
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

    private Element createHttpSampler(Document doc, JsonNode request) throws UnsupportedEncodingException, MalformedURLException {
        Element sampler = doc.createElement("HTTPSamplerProxy");
        sampler.setAttribute("guiclass", "HttpTestSampleGui");
        sampler.setAttribute("testclass", "HTTPSamplerProxy");
        sampler.setAttribute("testname", request.get("name").asText());
        sampler.setAttribute("enabled", "true");

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
                PostmanCollectionParser parser = new PostmanCollectionParser();
                host = String.join(".", parser.getStringListFromJsonNode(urlNode.get("host")));
            }
            port = urlNode.has("port") ? urlNode.get("port").asText() : "";
            if (urlNode.has("path")) {
                PostmanCollectionParser parser = new PostmanCollectionParser();
                path = "/" + String.join("/", parser.getStringListFromJsonNode(urlNode.get("path")));
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
                for (String p : params) {
                    String[] keyValue = p.split("=", 2);
                    Map<String, String> paramMap = new HashMap<>();
                    paramMap.put("key", URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name()));
                    paramMap.put("value", keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()) : "");
                    queryParams.add(paramMap);
                }
            }
        }

        // Добавляем query-параметры
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

        // Заполняем sampler
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

        // Тело запроса (если оно "raw")
        JsonNode body = request.get("request").get("body");
        if (body != null && !body.isNull()) {
            String mode = body.has("mode") ? body.get("mode").asText() : "";
            if ("raw".equals(mode)) {
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
        }

        return sampler;
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

        // Собираем заголовки
        Map<String, String> currentHeaders = new HashMap<>();
        if (request.has("current_headers")) {
            JsonNode headersNode = request.get("current_headers");
            Iterator<Map.Entry<String, JsonNode>> fields = headersNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                currentHeaders.put(entry.getKey(), entry.getValue().asText());
            }
        }

        // Автогенерируем заголовки
        Map<String, String> autoGeneratedHeaders = getAutoGeneratedHeaders(addAuthorizationHeader);
        for (Map.Entry<String, String> entry : autoGeneratedHeaders.entrySet()) {
            currentHeaders.putIfAbsent(entry.getKey(), entry.getValue());
        }

        // Добавляем все заголовки
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

    private static Map<String, String> getAutoGeneratedHeaders(boolean addAuthorizationHeader) {
        Map<String, String> autoGeneratedHeaders = new HashMap<>();
        autoGeneratedHeaders.put("User-Agent", "PostmanRuntime/7.42.0");
        autoGeneratedHeaders.put("Accept", "*/*");
        autoGeneratedHeaders.put("Accept-Encoding", "gzip, deflate, br");
        autoGeneratedHeaders.put("Connection", "keep-alive");
        autoGeneratedHeaders.put("Content-Type", "application/json");

        if (addAuthorizationHeader) {
            autoGeneratedHeaders.put("Authorization", "${access_token}");
        }
        return autoGeneratedHeaders;
    }

    private void writeDocumentToOutputStream(Document doc, OutputStream outputStream) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(outputStream);
        transformer.transform(source, result);
    }
}