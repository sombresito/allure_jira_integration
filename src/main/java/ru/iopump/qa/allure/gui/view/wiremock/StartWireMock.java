package ru.iopump.qa.allure.gui.view.wiremock;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Ports;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.vaadin.flow.component.textfield.TextArea;
import java.util.logging.Logger;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.api.command.KillContainerCmd;
import com.github.dockerjava.core.DockerClientBuilder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.TopContainerCmd;
import com.github.dockerjava.core.DockerClientBuilder;
import java.util.List;
import com.github.dockerjava.api.command.TopContainerResponse;

import static ru.iopump.qa.allure.gui.view.wiremock.CreateTeam.GetListTeams;
import static ru.iopump.qa.allure.gui.view.wiremock.Notifications.NotificationSuccess;
import static ru.iopump.qa.allure.gui.view.wiremock.Notifications.NotificationInform;
import static ru.iopump.qa.allure.gui.view.wiremock.Notifications.NotificationError;
import ru.iopump.qa.allure.gui.view.wiremock.Env;
import  ru.iopump.qa.allure.gui.view.wiremock.CreateTeam.*;

@Route("start-mock-form")
@PageTitle("Настройка и запуск WireMock")
@CssImport("./styles/wiremock.css")
public class StartWireMock extends VerticalLayout {
    // infoIconObjectivesField
    Icon infoPort = new Icon("vaadin", "info-circle");
    // infoDataRequiredField
    Icon infoJrn = new Icon("vaadin", "info-circle");
    Icon infoLimRecords = new Icon("vaadin", "info-circle");
    Icon infoCntThreads = new Icon("vaadin", "info-circle");
    Icon infoAsyncThreads = new Icon("vaadin", "info-circle");
    // Общие опции для ComboBox
    private static final List<String> YES_NO_OPTIONS = List.of("Да", "Нет");
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
    private final Checkbox jrn = new Checkbox("Отключить журнал запросов");
    private final Checkbox asyncRequests = new Checkbox("Включить асинхронную обработку запросов");
    private final Checkbox activeTemplate = new Checkbox("Активировать шаблоны Handlebars");
    private final IntegerField limRecords = new IntegerField("Ограничение записей в журнал запросов");
    private final IntegerField port = new IntegerField("Порт");
    private final IntegerField cntThreads = new IntegerField("Количество потоков контейнера");
    private final IntegerField asyncThreads = new IntegerField("Количество фоновых потоков ответа");
    //private final TextField jrn = new TextField("Отключить журнал запросов?");

    private final Div textFieldWrapper = new Div();
    private final Label displayLabel = new Label();
    private static final Logger logger = Logger.getLogger(StartWireMock.class.getName());
    private TextArea logArea;
    private final ComboBox<String> comboBoxTeam = new ComboBox<>("Выберите команду");
    public static String mockPort = null;
    public static String nameTeamMock = null;


    public StartWireMock() throws IOException {
        this.createHeader();
        addClassName("wire-form");
        setPadding(false);

        getElement().getStyle()
                .set("background-image", "linear-gradient(135deg, #707070, #e0e0e0)")
                .set("min-height", "100vh");
        List<String> listTeam = GetListTeams();
        limRecords.setVisible(true);
        asyncThreads.setVisible(false);
        // Основная форма
        //FormLayout formLayout = new FormLayout();
        //formLayout.add(
        //        port, jrn, textFieldWrapper
        //);
        infoJrn.getElement().setAttribute("title", "Отключить журнал запросов, в данном журнале идет запись " +
                "всех связок запрос-ответ, которые приходят на порт WireMock. " +
                "Отключать журналирование нужно в том случае если вы собираетесь " +
                "использовать WireMock в нагрузочном тестировании, и вам не важен " +
                "мониторинг трафика, который прилетает на заглушку");
        infoLimRecords.getElement().setAttribute("title", "Параметр для ограничения записей в журналах запросов, " +
                "работает если не указан параметр --no-request-journal, " +
                "нужен для того чтобы не обслуживать WireMock в течении долгого времени, " +
                "Обычно в 1000 хватает, но при желании можно " +
                "изменить, но если его не указать журналирование не будет ограничено, " +
                "что приведет к жору места на диске и росту потребления ОЗУ");
        infoCntThreads.getElement().setAttribute("title", "Количество потоков, для обработки входящих запросов, " +
                "по умолчанию 10, есть смысл увеличивать если будете использовать " +
                "в нагрузочном тестировании");
        infoAsyncThreads.getElement().setAttribute("title", "Количество фоновых потоков ответа, по умолчанию 10, " +
                "работает только если указан параметр --async-response-enabled. " +
                "Повышать нужно только если на нагрузочном не хватает " +
                "производительности заглушек");
        infoPort.getElement().setAttribute("title", "Параметр для переопределения номера порта, по умолчанию 8080");
        asyncRequests.addClassName("checkbox-mock");
        activeTemplate.addClassName("checkbox-mock");
        limRecords.addClassName("custom-text-field-mock");
        port.addClassName("custom-text-field-mock");
        cntThreads.addClassName("custom-text-field-mock");
        asyncThreads.addClassName("custom-text-field-mock");
        jrn.addClassName("checkbox-mock");

        jrn.getElement().setProperty("title", "Отключить журнал запросов, в данном журнале идет " +
                "запись всех связок запрос-ответ, которые приходят на порт WireMock. " +
                "Отключать журналирование нужно в том случае если вы собираетесь использовать " +
                "WireMock в нагрузочном тестировании, и вам не важен мониторинг трафика, который прилетает на заглушку");
        asyncRequests.getElement().setProperty("title", "Эта настройка позволяет моделировать асинхронные поведения в тестах, когда ответы приходят не сразу, а с задержкой или через некоторое время после выполнения запроса");
        activeTemplate.getElement().setProperty("title", "Позволяет включить глобальную поддержку шаблонов ответов для всех запросов, которые проходят через сервер. Это означает, что вы можете использовать шаблоны (например, Handlebars) для динамического генерирования содержимого ответов на основе данных, полученных от клиента (например, параметры запроса, тело запроса, заголовки и т.д.)");
        port.setHelperComponent(infoPort);
        limRecords.setHelperComponent(infoLimRecords);
        cntThreads.setHelperComponent(infoCntThreads);
        asyncThreads.setHelperComponent(infoAsyncThreads);

        FormLayout formLayout = new FormLayout();
        formLayout.addClassName("main-form");
        formLayout.add(
                jrn, asyncRequests, activeTemplate,
                limRecords, cntThreads,
                asyncThreads
        );
        // Добавляем слушатель на изменение состояния чекбокса
        jrn.addValueChangeListener(event -> {
            // Если чекбокс отмечен, скрываем поле, иначе показываем
            limRecords.setVisible(!event.getValue());
        });
        asyncRequests.addValueChangeListener(event -> {
            // Если чекбокс отмечен, скрываем поле, иначе показываем
            asyncThreads.setVisible(event.getValue());
        });

        Div divForm = new Div(formLayout);
        divForm.addClassName("div-form");
        formLayout.addClassName("set-form");
        Button submitButton = new Button("Запуск", e -> RunWireMock());
        Button clearButton = new Button("Очистить", e -> clearForm());
        Button stopButton = new Button("Остановить", e -> StopWireMock());

        submitButton.addClassName("start-button");
        clearButton.addClassName("clear-wire-button");
        stopButton.addClassName("stop-button");

        HorizontalLayout buttonLayout = new HorizontalLayout(submitButton, stopButton, clearButton);

        buttonLayout.setAlignItems(Alignment.BASELINE); // Выравнивание по базовой линии
        buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER); // Центрирование по горизонтали
        buttonLayout.setSpacing(true); // Добавляем отступ между кнопками
        buttonLayout.setWidth("100%"); // Устанавливаем ширину на 100%

        submitButton.setHeight("40px");
        clearButton.setHeight("40px");
        stopButton.setHeight("40px");
        setHorizontalComponentAlignment(Alignment.CENTER, divForm, buttonLayout);
        // Добавляем все элементы на страницу
        // Скрываем основную форму до выбора
        divForm.setVisible(false);
        buttonLayout.setVisible(false);
        add(divForm, buttonLayout, displayLabel);
        // Создаем диалог для выбора
        Dialog dialog = new Dialog();
        dialog.addClassName("dialog-form");
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(true);
        comboBoxTeam.setItems(listTeam);
        //comboBoxTeam.setItems("GreenFront", "BCCBusiness", "MSB");
        comboBoxTeam.setRequired(true);  // Устанавливаем поле обязательным для заполнения
        comboBoxTeam.setRequiredIndicatorVisible(true);  // Отображаем индикатор обязательного поля (красная звездочка)
        Button selectButton = new Button("Выбрать", e -> {
            // Когда пользователь выбирает вариант, закрываем диалог и показываем основную форму
            // Когда пользователь выбирает вариант, закрываем диалог и показываем основную форму
            if (comboBoxTeam.getValue() == null) {
                comboBoxTeam.setErrorMessage("Это поле обязательно для заполнения");
                comboBoxTeam.setInvalid(true);
            } else {
                comboBoxTeam.setInvalid(false); // Убираем ошибку, если значение выбрано
                dialog.close();
                String selectedOption = comboBoxTeam.getValue();
                Notification inform = NotificationInform("Вы выбрали: " + selectedOption);
                inform.open();
                // Отображаем основную форму после выбора
                divForm.setVisible(true);
                buttonLayout.setVisible(true);
            }
            //dialog.close();
            //String selectedOption = comboBoxTeam.getValue();
            //Notification inform = NotificationInform("Вы выбрали: " + selectedOption);
            //inform.open();
            // Отображаем основную форму после выбора
            //divForm.setVisible(true);
            //buttonLayout.setVisible(true);
        });
        Icon closeIconDlgTeam = new Icon(VaadinIcon.CLOSE);
        Button closeDlgTeam = new Button(closeIconDlgTeam, e -> {
            dialog.close();
        });
        closeDlgTeam.getStyle().set("position", "absolute");
        closeDlgTeam.getStyle().set("top", "10px");
        closeDlgTeam.getStyle().set("right", "10px");
        closeDlgTeam.getStyle().set("border", "none");
        closeDlgTeam.getStyle().set("background", "transparent");
        closeDlgTeam.getStyle().set("cursor", "pointer");
        closeDlgTeam.getStyle().set("color", "red");
        // Добавляем ComboBox и кнопку в диалог
        dialog.add(comboBoxTeam, selectButton, closeDlgTeam);
        // Показываем диалог
        dialog.open();
    }

    private void createHeader() {
        H1 logo = new H1("Настройка и запуск MockServer");
        logo.addClassName("logo-text");
        Div spacer = new Div();
        logo.getElement().getStyle().set("color", "#FDFEFE");
        logo.getStyle().set("margin-top", "1px");
        Button backButton = new Button("Вернуться на главную MockServer", (e) -> this.getUI().ifPresent((ui) -> ui.navigate("mock-main")));
        backButton.addClassName("header-button");
        HorizontalLayout header = new HorizontalLayout(logo, spacer, backButton);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.expand(spacer);
        header.setWidth("100%");
        header.addClassName("header");
        this.add(header);
    }

    private void clearForm() {
        // Clear all text fields
        port.clear();
        jrn.clear();
        asyncRequests.clear();
        activeTemplate.clear();
        limRecords.clear();
        cntThreads.clear();
        asyncThreads.clear();

    }
    private String GetNameContainer(String nameTeam) {
        if (nameTeam.equals("GreenFront")) {
            return "greenfront";
        } else if (nameTeam.equals("BCCBusiness")) {
            return "bccbusiness";
        } else if (nameTeam.equals("MSB")) {
            return "msb";
        } else {
            return null;
        }
    }
    private String GetPortTeam(String nameTeam) {
        if (nameTeam.equals("GreenFront")) {
            return "WIREMOCK_PORT_GF";
        } else if (nameTeam.equals("BCCBusiness")) {
            return "WIREMOCK_PORT_BS";
        } else if (nameTeam.equals("MSB")) {
            return "WIREMOCK_PORT_MSB";
        } else {
            return null;
        }
    }
    public void WriteMockPort(String port) {
        // Записываем новый порт в файл .env
        try {
            String envFilePath = "./allure/.env";
            String newEnvVariable = "WIREMOCK_PORT=" + port; // Пример: новый порт

            // Читаем все строки из файла .env
            List<String> lines = Files.readAllLines(Paths.get(envFilePath));

            // Флаг, указывающий на то, была ли заменена строка
            boolean updated = false;

            // Ищем строку с WIREMOCK_PORT и заменяем её
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("WIREMOCK_PORT=")) {
                    lines.set(i, newEnvVariable); // Заменяем строку на новую
                    updated = true;
                    break;
                }
            }

            // Если строка не была найдена, добавляем её в конец файла
            if (!updated) {
                lines.add(newEnvVariable);
            }

            // Записываем все строки обратно в файл
            Files.write(Paths.get(envFilePath), lines, StandardOpenOption.WRITE);

            System.out.println("Файл обновлен!");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void saveSetWireMock() {
        // Получаем значения всех полей с проверкой на null
        //String projectName = (port.getValue() != null) ? port.getValue().trim() : "";
        //String persons = (jrn.getValue() != null) ? jrn.getValue().trim() : "";

        // Для хранения данных файла
        byte[] postmanFileData = null;
        String postmanFileName = null; // Изначально имя файла пустое
    }
    private void showInputText() {
        Label l1 = new Label();
        Label l2 = new Label();
        Label l3 = new Label();
        Label l4 = new Label();
        Label l5 = new Label();
        Label l6 = new Label();
        Label l7 = new Label();
        Label l8 = new Label();
        //String jrnValue = String.valueOf((jrn.getValue() != null));
        //String asyncRequestsValue = String.valueOf((asyncRequests.getValue() != null));
        //String activeTemplateValue = String.valueOf((activeTemplate.getValue() != null));
        String portValue = (String.valueOf(port.getValue()) != null ? String.valueOf(port.getValue()).trim() : "");
        String limRecordsValue = (String.valueOf(limRecords.getValue()) != null ? String.valueOf(limRecords.getValue()).trim() : "");
        String cntThreadsValue = (String.valueOf(cntThreads.getValue()) != null ? String.valueOf(cntThreads.getValue()).trim() : "");
        String asyncThreadsValue = (String.valueOf(asyncThreads.getValue()) != null ? String.valueOf(asyncThreads.getValue()).trim() : "");
        String jrnValue = String.valueOf(jrn.getValue());
        String asyncRequestsValue = String.valueOf(asyncRequests.getValue());
        String activeTemplateValue = String.valueOf(activeTemplate.getValue());
        String nameTeam = (comboBoxTeam.getValue().toLowerCase() != null ? comboBoxTeam.getValue().toLowerCase().trim() : "");
        //String portValue = String.valueOf(port.getValue().intValue());
        //String limRecordsValue = String.valueOf(limRecords.getValue().intValue());
        //String cntThreadsValue = String.valueOf(cntThreads.getValue().intValue());
        //String asyncThreadsValue = String.valueOf(asyncThreads.getValue().intValue());
        l1.setText(jrnValue);
        l2.setText(asyncRequestsValue);
        l3.setText(activeTemplateValue);
        l4.setText(portValue);
        l5.setText(limRecordsValue);
        l6.setText(cntThreadsValue);
        l7.setText(asyncThreadsValue);

        // Формируем команду CMD для dockerfile
        //ReplaceCmdDockerfile(portValue, activeTemplateValue, jrnValue,
        //                                limRecordsValue, cntThreadsValue,
        //                                asyncRequestsValue, asyncThreadsValue);

        // Вызываем метод для запуска WireMock
        RunWireMock();
        //l8.setText(statusCode);
        add(l1, l2, l3, l4, l5, l6, l7, l8);
        //String cmdCommand = String.format("docker run -d %s wiremock %s %s %s %s %s %s",
        //                                 GetSetPort(portValue), GetSetHandlebars(activeTemplateValue), GetSetJrnValue(jrnValue),
        //                                 GetSetCntJrnValue(limRecordsValue), GetSetCntThreads(cntThreadsValue),
        //                                 GetSetAsyncResponse(asyncRequestsValue), GetSetAsyncResponseThreads(asyncThreadsValue));
        //RunWireMock(cmdCommand);
    }
    private String GetSetPort(String portCode) {
        if (!Objects.equals(portCode, "null")) {
            return String.format("--port %s", portCode);
        } else {
            return "--port 8080";
        }
    }
    private String GetSetJrnValue(String jrnValue) {
        if (Objects.equals(jrnValue, "true")) {
            return "--no-request-journal";
        } else {
            return "";
        }
    }
    private String GetSetCntJrnValue(String cntJrnValue) {
        if (!Objects.equals(cntJrnValue, "null")) {
            return String.format("--max-request-journal-entries %s", cntJrnValue);
        } else {
            return "";
        }
    }
    private String GetSetCntThreads(String cntThreads) {
        if (!Objects.equals(cntThreads, "null")) {
            return String.format("--container-threads %s", cntThreads);
        } else {
            return "";
        }
    }
    private String GetSetAsyncResponse(String asyncResponse) {
        if (Objects.equals(asyncResponse, "true")) {
            return String.format("--async-response-enabled %s", asyncResponse);
        } else {
            return "";
        }
    }
    private String GetSetAsyncResponseThreads(String asyncResponseThreads) {
        if (!Objects.equals(asyncResponseThreads, "null")) {
            return String.format("--async-response-threads %s", asyncResponseThreads);
        } else {
            return "";
        }
    }
    private String GetSetHandlebars(String handlebars) {
        if (Objects.equals(handlebars, "true")) {
            return "--global-response-templating";
        } else {
            return "";
        }
    }
    private void RunWireMock() {
        // запускаем WireMock
        try {
            String nameTeam = (comboBoxTeam.getValue().toLowerCase() != null ? comboBoxTeam.getValue().toLowerCase().trim() : "");
            //String portValue = (String.valueOf(port.getValue()) != null ? String.valueOf(port.getValue()).trim() : "");
            String portValue = "8081";
            String limRecordsValue = (String.valueOf(limRecords.getValue()) != null ? String.valueOf(limRecords.getValue()).trim() : "");
            String cntThreadsValue = (String.valueOf(cntThreads.getValue()) != null ? String.valueOf(cntThreads.getValue()).trim() : "");
            String asyncThreadsValue = (String.valueOf(asyncThreads.getValue()) != null ? String.valueOf(asyncThreads.getValue()).trim() : "");
            String jrnValue = String.valueOf(jrn.getValue());
            String asyncRequestsValue = String.valueOf(asyncRequests.getValue());
            String activeTemplateValue = String.valueOf(activeTemplate.getValue());

            mockPort = portValue;
            nameTeamMock = nameTeam;
            DockerClient dockerClient = DockerClientBuilder.getInstance().build();
            // Получаем имя контейнера
            //String nameContainer = GetNameContainer(nameTeam);

            // Создаем команду для выполнения внутри контейнера
            String commandMock = ConfigureCmdCommand(portValue, activeTemplateValue,
                    jrnValue, limRecordsValue, cntThreadsValue, asyncRequestsValue,
                    asyncThreadsValue); // получаем строку с командой
            ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(nameTeam) // nameContainer
                    .withAttachStdout(true)  // Подключаем стандартный вывод
                    .withAttachStderr(true)  // Подключаем стандартную ошибку
                    .withCmd(commandMock.split(" "));
            // Выполнение команды и получение ExecStartCmd
            ExecStartCmd execStartCmd = dockerClient.execStartCmd(execCreateCmd.exec().getId())
                    .withDetach(true); // Без отсоединения, будем ожидать завершения
            // Выполняем команду и выводим результат в консоль
            try {
                execStartCmd.exec(new ExecStartResultCallback(System.out, System.err)).awaitCompletion();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // проверяем, что WireMock запустился
            //boolean chkId = false;
            //chkId = CheckWireProc("wiremock-container");
            System.out.println("WireMock процесс успешно запущен");
            Notification notif = NotificationSuccess("WireMock успешно запущен");
            notif.open();
        } catch (Exception a) {
            Notification error = NotificationError("Возникла ошибка. Попробуйте снова");
            Notification error2 = NotificationError(a.getMessage());
            error.open();
            error2.open();
            a.printStackTrace();
        }
    }
    private void StopWireMock() {
        // Получаем PID процесса WireMock внутри контейнера

        try {
            String nameTeam = (comboBoxTeam.getValue().toLowerCase() != null ? comboBoxTeam.getValue().toLowerCase().trim() : "");
            //String nameContainer = GetNameContainer(nameTeam);
            DockerClient dockerClient = DockerClientBuilder.getInstance().build();
            // Создаем команду для выполнения внутри контейнера
            ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(nameTeam) // nameContainer
                    .withAttachStdout(true)  // Подключаем стандартный вывод
                    .withAttachStderr(true)  // Подключаем стандартную ошибку
                    .withCmd("pkill", "-f", "wiremock-standalone");
            // Выполнение команды и получение ExecStartCmd
            ExecStartCmd execStartCmd = dockerClient.execStartCmd(execCreateCmd.exec().getId())
                    .withDetach(true); // Без отсоединения, будем ожидать завершения
            // Выполняем команду и выводим результат в консоль
            try {
                execStartCmd.exec(new ExecStartResultCallback(System.out, System.err)).awaitCompletion();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // проверяем, что WireMock запустился
            //boolean chkId = false;
            //chkId = CheckWireProc("wiremock-container");
            Notification notif = NotificationSuccess("WireMock успешно остановлен");
            notif.open();
        } catch (Exception a) {
            Notification error = NotificationError("Возникла ошибка. Попробуйте снова");
            error.open();
            a.printStackTrace();
        }
    }
    private boolean CheckWireProc(String nameContainer)  {
        // метод для проверки запущенного процесса wiremock
        // Создание экземпляра Docker клиента
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // Получаем информацию о процессах внутри контейнера (по ID или имени)
        TopContainerResponse topContainerResponse = dockerClient.topContainerCmd("container_id_or_name").exec();

        return false;
    }

    private String GetIdWireProc(String nameContainer) throws IOException {
        // метод для проверки запущенного процесса wiremock
        // Создание экземпляра Docker клиента
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // Получаем информацию о процессах внутри контейнера (по ID или имени)
        TopContainerCmd topContainerCmd = dockerClient.topContainerCmd(nameContainer);

        // Выполнение команды и получение списка строк, представляющих процессы
        List<String> processes = (List<String>) topContainerCmd.exec();

        // Выводим информацию о процессах
        for (String process : processes) {
            if (process.contains("wiremock")) {
                System.out.println(process);
            }

        }
        return null;
    }
    private void AddPortToEnv(String port) throws IOException {
        String envFilePath = "/allure/.env"; // Specify the path to your .env file
        String nameTeam = (comboBoxTeam.getValue().toLowerCase() != null ? comboBoxTeam.getValue().toLowerCase().trim() : "");
        // Read the current content of the .env file
        File envFile = new File(envFilePath);
        if (!envFile.exists()) {
            envFile.createNewFile();
        }
        String namePort = GetPortTeam(nameTeam);
        // Read all lines from the .env file
        List<String> lines = Files.readAllLines(Paths.get(envFilePath));

        // Flag to track if the port was updated
        boolean portUpdated = false;

        // Create a new list to hold the updated lines
        List<String> updatedLines = new ArrayList<>();

        // Loop through all lines and modify the line containing WIREMOCK_PORT
        for (String line : lines) {
            if (line.startsWith(namePort+"=")) {
                // If the line with WIREMOCK_PORT exists, replace the value
                updatedLines.add(namePort + "=" + port);
                portUpdated = true;
            } else {
                updatedLines.add(line);
            }
        }

        // If the port was not found, add it at the end of the file
        if (!portUpdated) {
            updatedLines.add(namePort + "=" + port);
        }

        // Write the updated lines back to the .env file
        Files.write(Paths.get(envFilePath), updatedLines);
    }

    private void ReplaceCmdDockerfile(String portCode, String handlebars, String jrnValue,
                                      String cntJrnValue, String cntThreads, String asyncResponse,
                                      String asyncResponseThreads) {
        logArea = new TextArea("Логи");
        logArea.setReadOnly(true);
        logArea.setHeight("200px");
        logArea.setWidth("100%");
        Label l = new Label();
        Label la = new Label();
        String currentDirectory = System.getProperty("user.dir");
        Path filePath = Paths.get("/allure-app/src/main/resources/WireMock/Dockerfile");
        String newCmd = ConfigureCmdCommand(portCode, handlebars, jrnValue,
                cntJrnValue, cntThreads, asyncResponse,
                asyncResponseThreads);
        displayLabel.setText("Вы ввели: " + newCmd); // Отображаем введённый текст
        try {
            // Чтение всех строк из Dockerfile
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

            // Флаг для проверки, была ли найдена строка с CMD
            boolean cmdFound = false;

            // Перебор всех строк и замена CMD
            for (int i = 0; i < lines.size(); i++) {
                l.setText("Зашли в цикл " + lines.get(i));
                String line = lines.get(i).trim();
                if (line.startsWith("CMD")) {
                    lines.set(i, newCmd);  // Заменяем строку CMD
                    cmdFound = true;
                    break;  // Прерываем цикл после замены
                }
            }

            // Если строка CMD не была найдена, можно добавить новую
            if (!cmdFound) {
                lines.add(newCmd);  // Добавляем CMD в конец файла, если не нашли
            }

            // Запись изменённого содержимого обратно в Dockerfile
            Files.write(filePath, lines, StandardCharsets.UTF_8);
            logger.info("Dockerfile обновлен.");
            logArea.setValue(logArea.getValue() + "Dockerfile обновлен.");
            System.out.println("Dockerfile обновлен.");
            add(l);

        } catch (IOException e) {
            logger.info(e.getMessage());
            logArea.setValue(logArea.getValue() + e.getMessage());
            l.setText(String.format("Ошибка1 - %s", e));
            la.setText("Рабочая директория -" + currentDirectory);
            add(l, la, logArea);
            e.printStackTrace();
        }

    }
    private String ConfigureCmdCommand(String portCode, String handlebars, String jrnValue,
                                       String cntJrnValue, String cntThreads, String asyncResponse,
                                       String asyncResponseThreads) {
        try {
            String port =  GetSetPort(portCode);
            String jrnConf = GetSetJrnValue(jrnValue);
            String cntJrnConf = GetSetCntJrnValue(cntJrnValue);
            String threadsConf = GetSetCntThreads(cntThreads);
            String asyncRespConf = GetSetAsyncResponse(asyncResponse);
            String asyncRespThreadsConf = GetSetAsyncResponseThreads(asyncResponseThreads);
            String handlConf = GetSetHandlebars(handlebars);
            String defaultCmd = String.format("java -jar wiremock-standalone-3.10.0.jar %s %s %s %s %s %s %s --root-dir /wiremock --verbose", port,
                    handlConf, jrnConf, cntJrnConf, threadsConf, asyncRespConf, asyncRespThreadsConf);
            // перенос строки
            // --bind-address 0.0.0.0
            return defaultCmd;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

