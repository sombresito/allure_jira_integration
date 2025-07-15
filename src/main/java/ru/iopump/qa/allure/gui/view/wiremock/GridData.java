package ru.iopump.qa.allure.gui.view.wiremock;

public class GridData {
    private String number;
    private String id;
    private String body;

    // Конструктор
    public GridData(String number, String id, String body) {
        this.number = number;
        this.id = id;
        this.body = body;
    }

    // Геттеры
    public String getNumber() {
        return number;
    }

    public String getId() {
        return id;
    }
    public String getBody() {
        return body;
    }
}
