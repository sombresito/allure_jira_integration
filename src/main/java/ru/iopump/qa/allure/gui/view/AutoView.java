package ru.iopump.qa.allure.gui.view;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.concurrent.CompletionException;

@Route(value = "auto")
@PageTitle("Помогайка ИИ")
@CssImport("./styles/auto-view.css")
public class AutoView extends VerticalLayout {

    public AutoView() {
        addClassName("auto-view");
        setMargin(false);
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        HorizontalLayout header = createHeader();
        TextArea responseField = new TextArea();
        responseField.setReadOnly(true);
        responseField.addClassName("response-field-custom");
        responseField.addValueChangeListener(event -> {
            responseField.getElement().executeJs("this.scrollTop = this.scrollHeight;");
        });


        Button sendButton = new Button("Отправить запрос");

        TextArea inputField = new TextArea();
        inputField.setPlaceholder("Введите данные для запроса:"); // Устанавливаем placeholder
        inputField.addClassName("input-custom");

        // Гарантируем инициализацию JavaScript
        inputField.getElement().addAttachListener(event -> {
            inputField.getElement().executeJs(
                    "this.addEventListener('keydown', function(event) {" +
                            "    if (event.key === 'Enter' && !event.shiftKey) {" +
                            "        event.preventDefault();" + // Отключаем добавление новой строки
                            "        this.dispatchEvent(new CustomEvent('custom-enter', { bubbles: true, detail: { value: this.value } }));" +
                            "    }" +
                            "});"
            );
        });

        // Слушаем пользовательское событие 'custom-enter'
        inputField.getElement().addEventListener("custom-enter", e -> {
            // Указываем Vaadin, что нам нужен detail.value
            String input = e.getEventData().getString("event.detail.value");
            if (input != null && !input.trim().isEmpty()) {
                System.out.println("Value on Enter press: " + input);
                sendButton.click(); // Эмулируем нажатие кнопки
                inputField.clear(); // Очищаем поле после отправки
                inputField.focus(); // Возвращаем фокус на поле
            } else {
                System.out.println("Input is empty");
            }
        }).addEventData("event.detail.value");







        sendButton.addClickListener(event -> {
            String input = inputField.getValue();
            if (input.isEmpty()) {
                responseField.setValue("Пожалуйста, введите данные для запроса.");
                return;
            }
            responseField.setValue("");

            // Формируем префикс, который нужно убрать из ответа
            final String prefixToRemove = input + " Ответ:";

            sendStreamingRequestToServer(input, prefixToRemove, responseField);
            inputField.clear(); // очищаем поле ввода
        });

        VerticalLayout content = new VerticalLayout(responseField, inputField, sendButton);
        content.setSpacing(true);
        content.addClassName("auto-view-content");
        content.setAlignItems(Alignment.CENTER);
        add(header, content);
    }


    private HorizontalLayout createHeader() {
        H1 title = new H1("Помогайка ИИ");
        title.addClassName("header-title");

        Button backButton = new Button("Вернуться на главную страницу", e ->
                getUI().ifPresent(ui -> ui.navigate("reports-preprod"))
        );
        backButton.addClassName("header-button");

        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("header");
        header.setWidthFull();
        header.add(title, backButton);
        return header;
    }

    private void sendStreamingRequestToServer(String input, String prefixToRemove, TextArea responseField) {
        try {
            String jsonRequest = String.format("{\"question\": \"%s\"}", input);
            System.out.println("Отправляемый JSON запрос: " + jsonRequest);

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://192.168.1.71:5000/ask_stream"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            UI ui = UI.getCurrent();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenAccept(response -> {
                        new Thread(() -> {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                                String line;
                                boolean firstChunk = true;
                                while ((line = reader.readLine()) != null) {
                                    String finalLine = line.trim();

                                    // На первом чанке удалим префикс вопроса и "Ответ:"
                                    if (firstChunk && finalLine.contains(prefixToRemove)) {
                                        finalLine = finalLine.replace(prefixToRemove, "").trim();
                                        firstChunk = false;
                                    }

                                    // Если нужно убрать просто все вхождения слова "Ответ:" на всякий случай
                                    finalLine = finalLine.replace("Ответ:", "").trim();
                                    // Также, если хотите удалить сам вопрос везде, можно так:
                                    finalLine = finalLine.replace(input, "").trim();

                                    if (!finalLine.isEmpty()) {
                                        String cleanedLine = finalLine + " "; // Добавим пробел для разделения слов
                                        ui.access(() -> {
                                            String currentText = responseField.getValue();
                                            responseField.setValue(currentText + cleanedLine);
                                            // Прокрутка вниз
                                            responseField.getElement().executeJs("this.scrollTop = this.scrollHeight;");
                                            ui.push();
                                        });
                                    }
                                }
                            } catch (IOException e) {
                                ui.access(() -> {
                                    responseField.setValue("Ошибка при чтении ответа: " + e.getMessage());
                                    ui.push();
                                });
                            }
                        }).start();
                    })
                    .exceptionally(e -> {
                        Throwable cause = (e instanceof CompletionException) ? e.getCause() : e;
                        ui.access(() -> {
                            responseField.setValue("На данный момент ведутся работы, попробуйте, пожалуйста, позже");
                            ui.push();
                        });
                        return null;
                    });

        } catch (Exception e) {
            responseField.setValue("На данный момент ведутся работы, попробуйте, пожалуйста, позже");
        }
    }
}

