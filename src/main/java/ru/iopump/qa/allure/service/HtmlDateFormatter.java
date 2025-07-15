package ru.iopump.qa.allure.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlDateFormatter {


    public static void reformatDateInHtml(Path htmlFile) throws IOException {
        String content = Files.readString(htmlFile, StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile("Allure Report(.*?\\s)(\\d{1,2})/(\\d{1,2})/(\\d{4})");
        Matcher matcher = pattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String extraText = matcher.group(1); // Дополнительный текст между "Allure Report" и датой
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            int year = Integer.parseInt(matcher.group(4));
            String formattedDate = String.format("%02d/%02d/%04d", day, month, year);
            matcher.appendReplacement(sb, "Allure Report" + extraText + formattedDate);
        }
        matcher.appendTail(sb);
        Files.writeString(htmlFile, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
