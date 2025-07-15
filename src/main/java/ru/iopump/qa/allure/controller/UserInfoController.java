package ru.iopump.qa.allure.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UserInfoController {

    @GetMapping(value = "/api/user/info",  produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> userInfo(Authentication auth) {
        // auth никогда не null, если у вас включён Spring-Security
        return Map.of(
                "name",  auth.getName(),
                "roles", auth.getAuthorities()
                        .stream()
                        .map(a -> a.getAuthority()) // ROLE_ADMIN, ROLE_USER …
                        .collect(Collectors.toList())
        );
    }
}