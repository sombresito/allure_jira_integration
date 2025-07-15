package ru.iopump.qa.allure.gui.view.wiremock;

public class Env {
    public static void SetEnv(String name, String value) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        // Устанавливаем переменную окружения для нового процесса
        processBuilder.environment().put(name, value);
    }
    public static String GetEnv(String name) {
        // Получаем значение переменной окружения
        String myVariable = System.getenv(name);

        // Выводим значение переменной
        return myVariable;
    }
}
