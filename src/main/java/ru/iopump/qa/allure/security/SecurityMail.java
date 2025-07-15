package ru.iopump.qa.allure.security;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;


@Slf4j
public class SecurityMail {

    public static String decryptPassword(String encryptedPassword, String encryptionKey) {
        if (encryptedPassword == null || encryptionKey == null || encryptionKey.isEmpty()) {
            log.error("Ошибка");
            return null;
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "openssl", "enc", "-aes-256-cbc", "-d", "-a", "-salt", "-pbkdf2",
                    "-pass", "pass:" + encryptionKey
            );
            Process process = processBuilder.start();
            process.getOutputStream().write((encryptedPassword + "\n").getBytes());
            process.getOutputStream().flush();
            process.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder decryptedPassword = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                decryptedPassword.append(line);
            }
            return decryptedPassword.toString();
        } catch (Exception e) {
            log.error("Ошибка");
            return null;
        }
    }
}

