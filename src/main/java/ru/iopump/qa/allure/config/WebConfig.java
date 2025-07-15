package ru.iopump.qa.allure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /* 1 ─ AntPathMatcher (нужно Vaadin-у) */
    @Override
    public void configurePathMatch(PathMatchConfigurer c) {
        c.setPatternParser(null);
    }

    /* 2 ─ раздаём React-статику */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry r) {
        r.addResourceHandler("/ui/**")
                .addResourceLocations("classpath:/static/ui/")
                .resourceChain(false);
    }

    /* 3 ─ history-fallback + редирект */
    @Override
    public void addViewControllers(ViewControllerRegistry r) {

        /* ---------- ❶ РЕДИРЕКТ /ui -------------- */
        r.addRedirectViewController("/ui", "/ui/")
                // если нужен 301 вместо 302:
                // .setStatusCode(HttpStatus.MOVED_PERMANENTLY)
                // сохранить query-параметры, если пригодятся
                .setKeepQueryParams(true);

        /* ---------- ❷ Главная страница SPA ------ */
        r.addViewController("/ui/").setViewName("forward:/ui/index.html");

        /* ---------- ❸ Fallback для внутренних роутов React
           (один сегмент)  ------------------------ */
        r.addViewController("/ui/{path:^(?!vn)[^\\.]+$}")
                .setViewName("forward:/ui/index.html");

        /* ---------- ❹ Fallback для вложенных роутов
           (несколько сегментов) ------------------- */
        r.addViewController("/ui/**/{path:^(?!vn)[^\\.]+$}")
                .setViewName("forward:/ui/index.html");
    }
}



//package ru.iopump.qa.allure.config;


//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.*;
//
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    /* 1 ? оставляем AntPathMatcher (Vaadin 23) */
//    @Override public void configurePathMatch(PathMatchConfigurer c) {
//        c.setPatternParser(null);
//    }
//
//    /* 2 ? раздаём React-статику */
//    @Override public void addResourceHandlers(ResourceHandlerRegistry r) {
//        r.addResourceHandler("/ui/**")
//                .addResourceLocations("classpath:/static/ui/")
//                .resourceChain(false);
//    }
//
//    @Override
//    public void addViewControllers(ViewControllerRegistry r) {   // SPA-fallback
//        r.addViewController("/ui").setViewName("forward:/ui/index.html");
//        r.addViewController("/ui/").setViewName("forward:/ui/index.html");
//
//        // одиночный сегмент /ui/foo
//        r.addViewController("/ui/{path:^(?!vn)[^\\.]+$}")
//                .setViewName("forward:/ui/index.html");
//
//        // вложенные сегменты /ui/foo/bar
//        r.addViewController("/ui/**/{path:^(?!vn)[^\\.]+$}")
//                .setViewName("forward:/ui/index.html");
//    }
//}
