package ru.iopump.qa.allure.gui.component;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.Arrays;
import java.util.regex.Pattern;

public class EmailValidatorTextField extends TextField {
    private static final String EMAIL_PATTERN =
            "^(?i)[A-Za-z0-9._%+-]+@(bcc\\.kz|bcchub\\.kz)$";
    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    public EmailValidatorTextField(String label) {
        super(label);
        setValueChangeMode(ValueChangeMode.EAGER);

        // Добавляем слушатель изменений
        addValueChangeListener(event -> {
            String value = event.getValue();
            if (value != null && !value.isEmpty()) {
                boolean isValid = validateEmails(value);
                if (!isValid) {
                    setInvalid(true);
                    setErrorMessage("Пожалуйста, введите корректные email адреса через запятую (только домены @bcc.kz и @bcchub.kz)");
                } else {
                    setInvalid(false);
                }
            } else {
                setInvalid(false);
            }
        });
        setPlaceholder("Введите email адреса через запятую (например: user@bcc.kz, user2@bcchub.kz)");
    }

    private boolean validateEmails(String input) {
        // Разделяем строку по запятым и проверяем каждый email
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .allMatch(email -> pattern.matcher(email).matches());
    }

    // Метод для проверки валидности всех введённых email'ов
    public boolean isAllEmailsValid() {
        String value = getValue();
        return value != null && !value.isEmpty() && validateEmails(value);
    }
}