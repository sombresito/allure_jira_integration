package ru.iopump.qa.allure.gui.view.wiremock;
import com.github.dockerjava.api.model.ContainerConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CreateTeamFromJson implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(CreateTeamFromJson.class);
    private final String configDir = "/allure-app/src/main/resources/Teams"; // путь к JSON-файлам в контейнере
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) {
        System.out.println("▶ Выполняем загрузку JSON через CommandLineRunner");
        List<TeamConfig> configs = loadConfigsFromDirectory(configDir);

        for (TeamConfig config : configs) {
            String result = DockerApi.createContainer(config);
            System.out.println("Создан контейнер: " + result);
        }
    }
    public static List<TeamConfig> loadConfigsFromDirectory(String directoryPath) {
        List<TeamConfig> configs = new ArrayList<>();
        File dir = new File(directoryPath);

        if (dir.exists() && dir.isDirectory()) {
            File[] jsonFiles = dir.listFiles((dir1, name) -> name.endsWith(".json"));
            if (jsonFiles != null) {
                for (File jsonFile : jsonFiles) {
                    try {
                        TeamConfig config = objectMapper.readValue(jsonFile, TeamConfig.class);
                        configs.add(config);
                    } catch (IOException e) {
                        //System.err.println("Ошибка при обработке файла: " + jsonFile.getName());
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.out.println("Не найдена директория с файлами команд");
            //Notifications.NotificationError("Не найдена директория с файлами команд");
        }

        return configs;
    }
}
