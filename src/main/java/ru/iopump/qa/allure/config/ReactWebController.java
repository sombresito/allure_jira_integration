package ru.iopump.qa.allure.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Возвращаем index.html лишь тогда, когда запрошенный путь
 * НЕ содержит точки (.) – значит это не файл, а маршрут SPA.
 */
@Controller
public class ReactWebController {

    @GetMapping({"/ui/react",                 // корень
            "/ui/react/",                // с закрывающим слэшем
            "/ui/react/{path:[^\\.]*}"})
    public Resource index() {
        return new ClassPathResource("/static/ui/react/index.html");
    }
}