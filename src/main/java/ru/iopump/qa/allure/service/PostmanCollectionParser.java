package ru.iopump.qa.allure.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * Сервис-парсер Postman Collection
 */
public class PostmanCollectionParser {

    public Map<String, String> getHeadersFromCollection(JsonNode collection) {
        Map<String, String> headers = new HashMap<>();
        if (collection != null && collection.has("header")) {
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

    public List<JsonNode> extractRequests(JsonNode items, Map<String, String> parentHeaders, JsonNode parentAuth) {
        List<JsonNode> requests = new ArrayList<>();
        if (parentHeaders == null) {
            parentHeaders = new HashMap<>();
        }
        if (parentAuth == null) {
            parentAuth = null;
        }
        if (items != null && items.isArray()) {
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

    public Map<String, String> getHeadersFromItem(JsonNode item) {
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

    public List<String> getStringListFromJsonNode(JsonNode jsonNode) {
        List<String> list = new ArrayList<>();
        if (jsonNode.isArray()) {
            for (JsonNode node : jsonNode) {
                list.add(node.asText());
            }
        }
        return list;
    }
}