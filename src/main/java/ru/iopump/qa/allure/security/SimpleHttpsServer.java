package ru.iopump.qa.allure.security;

import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

public class SimpleHttpsServer {

    public static void startHttpsServer() throws Exception {
        // Порт HTTPS
        int port = 9443;

        // Создание HTTPS-сервера
        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);

        // Настройка SSL
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        // Загрузка самоподписанного сертификата
        try (FileInputStream keyFile = new FileInputStream("keystore.p12")) {
            keyStore.load(keyFile, "changeit".toCharArray());
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, "changeit".toCharArray());
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        // Применение SSL
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));

        // Пример обработки запросов
        httpsServer.createContext("/", exchange -> {
            String response = "HTTPS сервер работает!";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        System.out.println("HTTPS сервер запущен на порту " + port);
        httpsServer.start();
    }
}
