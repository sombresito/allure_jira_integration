package ru.iopump.qa.allure.service;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomBasicAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // Сообщаем браузеру, что нужна Basic-аутентификация
        response.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealmName() + "\"");
        // Отправляем код 401 "Unauthorized"
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Неверный логин или пароль");
    }

    @Override
    public void afterPropertiesSet() {
        setRealmName("CustomRealm");
        super.afterPropertiesSet();
    }
}

