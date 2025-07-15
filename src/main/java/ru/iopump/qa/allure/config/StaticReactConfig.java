package ru.iopump.qa.allure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Делает /ui/react/** статикой и минует ViewResolver */
@Configuration
public class StaticReactConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/ui/react/**")                 // URL-префикс
                .addResourceLocations("classpath:/static/ui/react/")// куда положили билд
                .resourceChain(false);
    }
}