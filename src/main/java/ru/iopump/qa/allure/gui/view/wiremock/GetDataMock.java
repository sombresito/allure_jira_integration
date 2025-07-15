package ru.iopump.qa.allure.gui.view.wiremock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetDataMock {
    private String id;
    private String name;
    private String body;

    // Геттеры и сеттеры
    @JsonProperty("id")
    public String getId() {
        return id;
    }
    @JsonProperty("name")
    public String getNameMock() {
        return name;
    }
    @JsonProperty("body")
    private void getBodyMock(String body) {
        this.body = body;
    }
}
