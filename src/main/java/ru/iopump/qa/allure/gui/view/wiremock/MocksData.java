package ru.iopump.qa.allure.gui.view.wiremock;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.notification.Notification;

import java.util.stream.Stream;

import java.io.IOException;

import static ru.iopump.qa.allure.gui.view.wiremock.Notifications.NotificationInform;

public class MocksData {
    private static String nameFolder;
    private static int countMocks = 0;
    private static List<List<String>> listMocks = new ArrayList<>();

    public static List<List<String>> GetMocksData() throws IOException {
        // Путь к папке, где хранятся JSON файлы
        //if (Mocks.nameTeam.equals("GreenFront")) {
        //    nameFolder = "GreenFront";
        //} else if (Mocks.nameTeam.equals("BCCBusiness")) {
        //    nameFolder = "BCCBusiness";
        //} else if (Mocks.nameTeam.equals("MSB")) {
        //    nameFolder = "MSB";
        //} else {
        //    nameFolder = null;
        //}
        String directoryPath = String.format("/allure-app/src/main/resources/WireMock/%s/mappings", Mocks.nameTeam);
        // Обрабатываем все JSON файлы в директории
        countMocks = 0;
        listMocks.clear();
        processJsonFiles(directoryPath);
        return listMocks;
    }
    public static void processJsonFiles(String directoryPath) throws IOException {
        // Получаем все файлы в папке (включая вложенные) с расширением .json
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json")) // фильтруем только json файлы
                    .forEach(path -> {
                        try {
                            // Обрабатываем каждый JSON файл
                            MocksData(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                            //Notifications.NotificationInform(e.getMessage());

                        }
                    });
        }
    }
    public static void MocksData(Path jsonFilePath) throws IOException {
        // Используем Jackson для работы с JSON
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> mocksData = new ArrayList<>();
        // Чтение JSON файла в объекты
        GetDataMock jsonObject = objectMapper.readValue(jsonFilePath.toFile(), GetDataMock.class);
        // Здесь можно добавить логику обработки данных, например:
        String getId = jsonObject.getId();
        String getName = String.valueOf(jsonObject.getNameMock());
        String getBody = Files.readString(jsonFilePath);

        // переименовываем файл в id для удобства использования при редактировании моков
        if (jsonFilePath.getFileName().equals(getId)) {
            System.out.println("Файл уже переименован - " + getId);
        } else {
            RenameFileMock(getId, String.valueOf(jsonFilePath.getFileName()));
        }

        countMocks = countMocks + 1;
        // Добавляем данные в список
        mocksData.add(String.valueOf(countMocks));
        mocksData.add(getId);
        //mocksData.add(getName);
        mocksData.add(getBody);

        // добавляем список с данными конкретного мока в другой список
        listMocks.add(mocksData);

    }
    private static void RenameFileMock(String newName, String oldName) {

        Path oldFilePath = Path.of(String.format("/allure-app/src/main/resources/WireMock/%s/mappings/%s", nameFolder, oldName));  // Путь к старому файлу
        Path newFilePath = Path.of(String.format("/allure-app/src/main/resources/WireMock/%s/mappings/%s.json", nameFolder, newName));  // Путь к новому файлу

        try {
            // Переименование файла
            Files.move(oldFilePath, newFilePath);
            System.out.println("Файл переименован успешно!");

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Ошибка при переименовании файла.");
        }
    }
}
