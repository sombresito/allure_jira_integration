package ru.iopump.qa.allure.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.jasypt.util.text.AES256TextEncryptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import ru.iopump.qa.allure.security.SecurityMail;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "mail")
@Slf4j
public class MailConfig {
    private String smtpHost;
    private int smtpPort;
    private String username;
    private String password;
    private String senderEmail;
    private String recipient;
    private String domain;
    private String ntlm;
    private String encr;

    // Получение списка почтовых адресов
    public List<String> getRecipients() {
        return Arrays.asList(recipient.split(","));
    }

    @PostConstruct
    public void decryptPassword() {
        if (password != null) {
            password = SecurityMail.decryptPassword(password, encr);
        }
    }



}
