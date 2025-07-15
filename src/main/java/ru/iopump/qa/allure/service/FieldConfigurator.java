package ru.iopump.qa.allure.service;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.Map;
import java.util.Set;
import java.util.List;

public class FieldConfigurator {

    public static void configureComboBox(ComboBox<String> comboBox, String... items) {
        comboBox.setItems(items);
    }

    public static void configureFieldVisibility(ComboBox<String> comboBox, TextField field) {
        field.setVisible(false);
        field.setEnabled(false);
        comboBox.addValueChangeListener(event -> {
            String value = event.getValue();
            if ("Да".equals(value) || "Другие".equals(value)) {
                field.setVisible(true);
                field.setEnabled(true);
            } else {
                field.setVisible(false);
                field.setEnabled(false);
            }
        });
    }

    public static void configureDynamicFields(Map<ComboBox<String>, TextField> mapping) {
        mapping.forEach((comboBox, field) -> {
            configureComboBox(comboBox, "Да", "Нет");
            configureFieldVisibility(comboBox, field);
        });
    }

    public static void configureCommentsField(TextArea commentsField) {
        commentsField.setPlaceholder("Здесь Вы можете оставить свои дополнения или пожелания");
        commentsField.setMaxLength(500);
        commentsField.setMinLength(10);
        commentsField.setClearButtonVisible(true);
        commentsField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (value.length() > 500) {
                commentsField.setErrorMessage("Превышено ограничение в 500 символов");
                commentsField.setInvalid(true);
            } else if (!value.isEmpty() && value.length() < 10) {
                commentsField.setErrorMessage("Минимальная длина комментария - 10 символов");
                commentsField.setInvalid(true);
            } else {
                commentsField.setErrorMessage(null);
                commentsField.setInvalid(false);
            }
        });
    }

    public static Dialog configureObjectivesField(
            TextField objectivesTextField,
            TextField customObjectiveField,
            CheckboxGroup<String> dialogCheckboxGroup,
            List<String> objectivesOptions) {
        Dialog objectivesDialog = new Dialog();
        objectivesDialog.setHeaderTitle("Выберите цели");
        objectivesTextField.getElement().executeJs("this.inputElement.style.setProperty('opacity', '1', 'important');");
        dialogCheckboxGroup.setLabel("Цели");
        dialogCheckboxGroup.setItems(objectivesOptions);
        dialogCheckboxGroup.addThemeName("spacing");
        objectivesDialog.setWidth("50%");
        Button confirmButton = new Button("Применить", event -> {
            Set<String> selectedValues = dialogCheckboxGroup.getValue();
            if (selectedValues.isEmpty()) {
                objectivesTextField.setValue("Нажмите для выбора...");
            } else {
                objectivesTextField.setValue(String.join(", ", selectedValues));
            }
            if (selectedValues.contains("Другие")) {
                customObjectiveField.setVisible(true);
                customObjectiveField.setEnabled(true);
            } else {
                customObjectiveField.setVisible(false);
                customObjectiveField.setEnabled(false);
                customObjectiveField.clear();
            }
            objectivesDialog.close();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        Button cancelButton = new Button("Отмена", event -> objectivesDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        HorizontalLayout buttonsLayout = new HorizontalLayout(confirmButton, cancelButton);
        buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        VerticalLayout dialogContent = new VerticalLayout(dialogCheckboxGroup, buttonsLayout);
        dialogContent.setPadding(false);
        dialogContent.setSpacing(true);
        objectivesDialog.add(dialogContent);
        objectivesTextField.setWidthFull();
        customObjectiveField.setVisible(false);
        customObjectiveField.setEnabled(false);
        return objectivesDialog;
    }
}