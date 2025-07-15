package ru.iopump.qa.allure.gui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletionException;

@Route("AI")
@PageTitle("Помогайка ИИ")
@CssImport("./styles/auto-view.css")
public class ArtificialIntelligence extends VerticalLayout {
    public ArtificialIntelligence() {
        this.addClassName("auto-view");
        this.setMargin(false);
        this.setPadding(false);
        this.setSpacing(false);
        this.setSizeFull();
        HorizontalLayout header = this.createHeader();
        TextArea responseField = new TextArea();
        responseField.setReadOnly(true);
        responseField.addClassName("response-field-custom");
        responseField.addValueChangeListener((event) -> {
            responseField.getElement().executeJs("this.scrollTop = this.scrollHeight;", new Serializable[0]);
        });
        Button sendButton = new Button("Отправить запрос");
        sendButton.addClassName("send-button");
        TextArea inputField = new TextArea();
        inputField.setPlaceholder("Введите данные для запроса:");
        inputField.addClassName("input-custom");
        inputField.getElement().addAttachListener((event) -> {
            inputField.getElement().executeJs("this.addEventListener('keydown', function(event) {    if (event.key === 'Enter' && !event.shiftKey) {        event.preventDefault();        this.dispatchEvent(new CustomEvent('custom-enter', { bubbles: true, detail: { value: this.value } }));    }});", new Serializable[0]);
        });
        inputField.getElement().addEventListener("custom-enter", (e) -> {
            String input = e.getEventData().getString("event.detail.value");
            if (input != null && !input.trim().isEmpty()) {
                System.out.println("Value on Enter press: " + input);
                sendButton.click();
                inputField.clear();
                inputField.focus();
            } else {
                System.out.println("Input is empty");
            }

        }).addEventData("event.detail.value");
        sendButton.addClickListener((event) -> {
            String input = inputField.getValue();
            if (input.isEmpty()) {
                responseField.setValue("Пожалуйста, введите данные для запроса.");
            } else {
                responseField.setValue("");
                String prefixToRemove = input + " Ответ:";
                this.sendStreamingRequestToServer(input, prefixToRemove, responseField);
                inputField.clear();
            }
        });
        VerticalLayout content = new VerticalLayout(new Component[]{responseField, inputField, sendButton});
        content.setSpacing(true);
        content.addClassName("auto-view-content");
        content.setAlignItems(Alignment.CENTER);
        this.add(new Component[]{header, content});
    }

    private HorizontalLayout createHeader() {
        H1 title = new H1("Помогайка ИИ");
        title.addClassName("header-title");
        Button backButton = new Button("Вернуться на главную страницу", (e) -> {
            this.getUI().ifPresent((ui) -> {
                ui.navigate("");
            });
        });
        backButton.addClassName("header-button");
        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("header");
        header.setWidthFull();
        header.add(new Component[]{title, backButton});
        return header;
    }

    private void sendStreamingRequestToServer(String input, String prefixToRemove, TextArea responseField) {
        try {
            String jsonRequest = String.format("{\"question\": \"%s\"}", input);
            System.out.println("Отправляемый JSON запрос: " + jsonRequest);
            HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
            HttpRequest request = HttpRequest.newBuilder().uri(new URI("http://192.168.1.71:5000/ask_stream")).header("Content-Type", "application/json").POST(BodyPublishers.ofString(jsonRequest)).build();
            UI ui = UI.getCurrent();
            client.sendAsync(request, BodyHandlers.ofInputStream()).thenAccept((response) -> {
                (new Thread(() -> {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream)response.body()));

                        try {
                            boolean firstChunk = true;

                            String line;
                            while((line = reader.readLine()) != null) {
                                String finalLine = line.trim();
                                if (firstChunk && finalLine.contains(prefixToRemove)) {
                                    finalLine = finalLine.replace(prefixToRemove, "").trim();
                                    firstChunk = false;
                                }

                                finalLine = finalLine.replace("Ответ:", "").trim();
                                finalLine = finalLine.replace(input, "").trim();
                                if (!finalLine.isEmpty()) {
                                    String cleanedLine = finalLine + " ";
                                    ui.access(() -> {
                                        String currentText = responseField.getValue();
                                        responseField.setValue(currentText + cleanedLine);
                                        responseField.getElement().executeJs("this.scrollTop = this.scrollHeight;", new Serializable[0]);
                                        ui.push();
                                    });
                                }
                            }
                        } catch (Throwable var11) {
                            try {
                                reader.close();
                            } catch (Throwable var10) {
                                var11.addSuppressed(var10);
                            }

                            throw var11;
                        }

                        reader.close();
                    } catch (IOException var12) {
                        IOException e = var12;
                        ui.access(() -> {
                            responseField.setValue("Ошибка при чтении ответа: " + e.getMessage());
                            ui.push();
                        });
                    }

                })).start();
            }).exceptionally((e) -> {
                if (e instanceof CompletionException) {
                    e.getCause();
                }

                ui.access(() -> {
                    responseField.setValue("На данный момент ведутся работы, попробуйте, пожалуйста, позже");
                    ui.push();
                });
                return null;
            });
        } catch (Exception var8) {
            responseField.setValue("На данный момент ведутся работы, попробуйте, пожалуйста, позже");
        }

    }
}
