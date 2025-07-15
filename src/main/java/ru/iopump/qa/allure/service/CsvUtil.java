package ru.iopump.qa.allure.service;

import java.io.StringWriter;
import java.util.Map;

public class CsvUtil {
    /**
     * Генерирует CSV-строку по данным из карты.
     *
     * @param formData карта с данными формы
     * @return CSV-строка
     */
    public static String generateCsvContent(Map<String, String> formData) {
        StringWriter csvContent = new StringWriter();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            csvContent.write("\"" + entry.getKey().replace("\"", "\"\"") + "\";\"" +
                    entry.getValue().replace("\"", "\"\"") + "\"\n");
        }
        return csvContent.toString();
    }
}