package ru.iopump.qa.allure.service;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.util.List;
import ru.iopump.qa.allure.properties.MailConfig;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.util.ByteArrayDataSource;


public class EmailService {

    public static void sendEmailWithCsvOnly(List<String> recipients, String subject, String csvData, String fileName, String commentText, MailConfig mailConfig) throws MessagingException, InterruptedException {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", mailConfig.getSmtpHost());
        properties.put("mail.smtp.port", String.valueOf(mailConfig.getSmtpPort()));
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true"); // Включаем STARTTLS
        properties.put("mail.smtp.ssl.trust", "*"); // Доверяем всем сертификатам
        properties.put("mail.smtp.auth.mechanisms", mailConfig.getNtlm());
        properties.put("mail.debug", "true");
//        properties.put("mail.smtp.auth.ntlm.domain", mailConfig.getDomain());

        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailConfig.getUsername(), mailConfig.getPassword());
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(mailConfig.getSenderEmail()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", recipients)));
        message.setSubject(subject);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(commentText);

        MimeBodyPart csvPart = new MimeBodyPart();
        csvPart.setFileName(fileName);
        csvPart.setContent(csvData, "text/csv; charset=UTF-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(csvPart);

        message.setContent(multipart);
        Transport.send(message);

        TimeUnit.SECONDS.sleep(5);
    }

    public static void sendEmailWithCsvAndMultiplePostmanCollections(
            List<String> recipients,
            String subject,
            String csvData,
            String csvFileName,
            List<Map<String, byte[]>> postmanFiles,
            String commentText,
            MailConfig mailConfig) throws MessagingException, InterruptedException {

        // Настройка свойств и сессии
        Properties properties = new Properties();
        properties.put("mail.smtp.host", mailConfig.getSmtpHost());
        properties.put("mail.smtp.port", String.valueOf(mailConfig.getSmtpPort()));
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true"); // Включаем STARTTLS
        properties.put("mail.smtp.ssl.trust", "*"); // Доверяем всем сертификатам
        properties.put("mail.smtp.auth.mechanisms", mailConfig.getNtlm());
        properties.put("mail.debug", "false");
//        properties.put("mail.smtp.auth.ntlm.domain", mailConfig.getDomain());

        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailConfig.getUsername(), mailConfig.getPassword());
            }
        });

        // Создание сообщения
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(mailConfig.getSenderEmail()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", recipients)));
        message.setSubject(subject);

        // Создание многокомпонентного сообщения
        Multipart multipart = new MimeMultipart();

        // Текстовая часть сообщения
        MimeBodyPart textPart = new MimeBodyPart();
        StringBuilder messageText = new StringBuilder("Прикрепленные файлы:\n");
        messageText.append("1. ").append(csvFileName).append(" - данные формы\n");

        int fileCounter = 2;
        for (Map<String, byte[]> fileMap : postmanFiles) {
            for (String fileName : fileMap.keySet()) {
                messageText.append(fileCounter++).append(". ").append(fileName).append(" - Postman коллекция\n");
            }
        }

        if (commentText != null && !commentText.isEmpty()) {
            messageText.append("\nКомментарии:\n").append(commentText);
        }


        textPart.setText(messageText.toString());
        multipart.addBodyPart(textPart);

        // Вложение CSV файла
        MimeBodyPart csvAttachment = new MimeBodyPart();
        csvAttachment.setContent(csvData, "text/csv; charset=UTF-8");
        csvAttachment.setFileName(csvFileName);
        multipart.addBodyPart(csvAttachment);

        // Вложение всех Postman коллекций
        for (Map<String, byte[]> fileMap : postmanFiles) {
            for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
                String fileName = entry.getKey();
                byte[] fileData = entry.getValue();

                MimeBodyPart postmanAttachment = new MimeBodyPart();
                DataSource source = new ByteArrayDataSource(fileData, "application/json");
                postmanAttachment.setDataHandler(new DataHandler(source));
                postmanAttachment.setFileName(fileName);
                multipart.addBodyPart(postmanAttachment);
            }
        }

        // Установка содержимого сообщения
        message.setContent(multipart);

        // Отправка сообщения
        Transport.send(message);

        // Задержка для предотвращения слишком частых запросов
        TimeUnit.SECONDS.sleep(5);
    }

}