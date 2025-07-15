package ru.iopump.qa.allure.properties;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;


@Setter
@Getter
@ConfigurationProperties(prefix = "basic")
@Component
@Slf4j
public class BasicProperties {

    private LdapProperties ldap;

    private List<User> users = new ArrayList<>();

    @Setter
    @Getter
    public static class User {
        private String username;
        private String password;
        private String roles;
        private boolean enable;

    }

    @Data
    public static class LdapProperties {
        private String host;
        private int port;
        private boolean useSSL;
        private boolean startTLS;
        private boolean sslSkipVerify;
        private String searchBaseDNS;
        private String memberOf;
        private String username;
        private String email;
    }

    @PostConstruct
    void init() {
        log.info("[ALLURE SERVER CONFIGURATION] Loaded LDAP properties: {}", ldap);
    }


}
