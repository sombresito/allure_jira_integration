package ru.iopump.qa.allure.gui.view.wiremock;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static ru.iopump.qa.allure.gui.view.wiremock.DockerApi.CheckPortOpen;
import static ru.iopump.qa.allure.gui.view.wiremock.DockerApi.getUsedPorts;
import static ru.iopump.qa.allure.gui.view.wiremock.Notifications.*;


@Route("add-team-form")
@PageTitle("Создание команды")
@CssImport("./styles/wiremock.css")
public class CreateTeam extends VerticalLayout {
    Icon infoNameTeam = new Icon("vaadin", "info-circle");
    Icon infoPortCont = new Icon("vaadin", "info-circle");
    Icon infoPortMock = new Icon("vaadin", "info-circle");

    private static final TextField nameTeam = new TextField("Введите наименование команды");
    private static final TextField portCont = new TextField("Введите порт команды");
    private static final TextField portMock = new TextField("Введите порт WireMock");
    ComboBox<String> usedPortsComboBox = new ComboBox<>("Занятые порты");
    private static List<String> usedPorts = null;


    public CreateTeam() {
        this.createHeader();
        addClassName("wire-form");
        setPadding(false);

        getElement().getStyle()
                .set("background-image", "linear-gradient(135deg, #707070, #e0e0e0)")
                .set("min-height", "100vh");

        nameTeam.setRequiredIndicatorVisible(true); // Показываем звёздочку
        nameTeam.setRequired(true);                 // Помечаем как обязательное
        portCont.setRequiredIndicatorVisible(true); // Показываем звёздочку
        portCont.setRequired(true);                 // Помечаем как обязательное
        portMock.setRequiredIndicatorVisible(true); // Показываем звёздочку
        portMock.setRequired(true);                 // Помечаем как обязательное
        // Создаем биндер без модели
        Binder<Void> binder = new Binder<>();

        // Привязываем поле с валидацией, но без TeamConfig
        binder.forField(nameTeam)
                .asRequired("Имя команды обязательно")
                .bind(v -> null, (v, val) -> {}); // пустая привязка

        binder.forField(portCont)
                .asRequired("Порт контейнера обязателен для заполнения")
                .bind(v -> null, (v, val) -> {});

        binder.forField(portMock)
                .asRequired("Порт сервера WireMock обязателен для заполнения")
                .bind(v -> null, (v, val) -> {});



        nameTeam.getElement().setAttribute("title", "Наименование команды для которой создается отдельный контейнер с WireMock");
        portCont.getElement().setAttribute("title", "Порт по которому будет вызываться контейнер");
        portMock.getElement().setAttribute("title", "Порт по которому будет вызываться WireMock в контейнере");

        nameTeam.addClassName("custom-text-field-mock");
        portCont.addClassName("custom-text-field-mock");
        portMock.addClassName("custom-text-field-mock");

        nameTeam.setHelperComponent(infoNameTeam);
        portCont.setHelperComponent(infoPortCont);
        portMock.setHelperComponent(infoPortMock);

        usedPortsComboBox.setItems("Загрузка...");

        usedPortsComboBox.addFocusListener(event -> {
            List<String> ports = getUsedPorts();
            if (ports == null || ports.isEmpty()) {
                usedPortsComboBox.setItems("Нет доступных портов");
            } else {
                usedPortsComboBox.setItems(ports);
            }
        });

        Button createButton = new Button("Создать", e -> CreateDirTeam());
        createButton.addClassName("start-button");
        HorizontalLayout buttonLayout = new HorizontalLayout(createButton);

        FormLayout formLayout = new FormLayout();
        formLayout.addClassName("main-form");
        formLayout.add(
                nameTeam, portCont, portMock
        );
        Div divLayout= new Div(formLayout);
        formLayout.setWidth("fit-content"); // ширина по контенту
        divLayout.setWidth("fit-content"); // ширина по контенту
        Div divForm = new Div(divLayout);
        divForm.setWidth("fit-content");
        divForm.addClassName("div-form");
        formLayout.addClassName("set-form");
        Div mainDiv = new Div(divForm, createButton);
        mainDiv.addClassName("main-div");
        Div comboBoxDiv = new Div(usedPortsComboBox);
        HorizontalLayout horLayout = new HorizontalLayout(mainDiv, comboBoxDiv);
        //horLayout.setSpacing(true);
        horLayout.addClassName("hor-layout");
        //
        //mainDiv.getStyle().set("border", "1px solid red");
        //comboBoxDiv.getStyle().set("border", "1px solid blue");
        //horLayout.getStyle().set("border", "1px solid green");
        //
        add(horLayout);
        binder.validate(); // запускает валидацию сразу
    }

    private void createHeader() {
        H1 logo = new H1("Создание команды");
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
    private void CreateDirTeam() {
        // метод для создания директории внутри контейнера Allure-Server
        // которая будет отвечать за общение с контейнером создаваемой команды
        String team = (nameTeam.getValue().toLowerCase() != null ? nameTeam.getValue().toLowerCase().trim() : "");
        Path path = Paths.get(String.format("./allure-app/src/main/resources/WireMock/%s", team)); // путь до новой папки
        Path dataContayner = Paths.get("./allure-app/src/main/resources/WireMock/ConteinerData"); // путь до новой папки
        try {
            // проверка, что порт свободен
            if (CheckPortOpen(Integer.parseInt(portCont.getValue())) && CheckPortOpen(Integer.parseInt(portMock.getValue()))) {
                // проверяем, что создаваемая команда не существует
                // если существует, то выводим информационное сообщение
                // и прерываем выполнение
                List<String> listTeams = GetListTeams(); // получаем список созданных команд
                if (CheckExistTeam(listTeams, nameTeam.getValue().toLowerCase())) {
                    NotificationError(String.format("Команда %s уже существует!", nameTeam.getValue().toLowerCase()));
                } else {
                    Files.createDirectories(path); // создаёт все промежуточные папки, если их нет
                    CopyDataContayner(dataContayner, path);
                    // Создаем json-файл с данными команды
                    TeamConfig config = new TeamConfig();
                    config.setImage("wiremock:latest");
                    config.setTeam(nameTeam.getValue().toLowerCase());
                    //config.setHostPort(Integer.parseInt(portMock.getValue()));
                    //config.setContainerPort(Integer.parseInt(portCont.getValue()));
                    config.setPorts(List.of(String.format("%s:8080", portMock.getValue()), String.format("%s:8081", portCont.getValue())));
                    config.setCommand("sleep infinity");
                    config.setVolumes(List.of(String.format("/home/allure/allure-server/src/main/resources/WireMock/%s:/wiremock", nameTeam.getValue())));
                    config.setNetwork("bridge");

                    saveTeamConfig(config, "./allure-app/src/main/resources/Teams");
                    // "/home/allure/allure-server/src/main/resources/Teams"
                    // Отправляем запрос на создание и запуск контейнера
                    // В спящем режиме
                    SendRequestCreateCont();
                }
            } else {
                NotificationError(String.format("Порт контейнера %s занят", portCont.getValue()));
                NotificationError(String.format("Порт сервера WireMock %s занят", portMock.getValue()));
            }
        } catch (IOException e) {
            System.err.println("Ошибка при создании папки: " + e.getMessage());
        }
    }
    public static void CopyDataContayner(Path sourceDir, Path targetDir) throws IOException {
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relativePath = sourceDir.relativize(dir);
                Path targetPath = targetDir.resolve(relativePath);
                if (!Files.exists(targetPath)) {
                    Files.createDirectories(targetPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relativePath = sourceDir.relativize(file);
                Path targetPath = targetDir.resolve(relativePath);
                Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    public static void saveTeamConfig(TeamConfig config, String outputDir) {
        // метод сериализует данные для создания команды
        // в json формат, чтобы при последующих перезапусках allure-server
        // команды автоматически создавались заново из json-файлов
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Путь, например: configs/myteam.json
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs(); // создаём директорию, если её нет
            }
            File file = new File(dir, config.getTeam() + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, config);
            System.out.println("Конфигурация сохранена: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void SendRequestCreateCont() {
        TeamConfig dto = new TeamConfig();
        dto.setTeam(nameTeam.getValue().toLowerCase());
        dto.setImage("wiremock:latest");
        dto.setPorts(List.of(String.format("%s:8080", portMock.getValue()), String.format("%s:8081", portCont.getValue())));
        dto.setCommand("sleep infinity");
        dto.setVolumes(List.of(String.format("/home/allure/allure-server/src/main/resources/WireMock/%s:/wiremock", nameTeam.getValue().toLowerCase())));
        dto.setNetwork("bridge");

        String result = DockerApi.createContainer(dto);
        NotificationSuccess(result);
    }
    public static List<String> GetListTeams() throws IOException {
        // метод получает список созданных команд
        // необходим для проверки существования создаваемой команды
        Path dirPath = Paths.get("./allure-app/src/main/resources/Teams");
        List<String> filenamesWithoutExtension = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    String filename = path.getFileName().toString();
                    String nameWithoutExtension = filename.contains(".")
                            ? filename.substring(0, filename.lastIndexOf('.'))
                            : filename;
                    filenamesWithoutExtension.add(nameWithoutExtension);
                }
            }
        }
        return filenamesWithoutExtension;
    }
    public static boolean CheckExistTeam(List<String> listTeams, String nameTeam) {
        return listTeams.contains(nameTeam);
    }
}

