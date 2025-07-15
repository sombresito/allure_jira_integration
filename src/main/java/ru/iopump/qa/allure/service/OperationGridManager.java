package ru.iopump.qa.allure.service;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;
import ru.iopump.qa.allure.helper.NotificationHelper;

import java.util.*;

public class OperationGridManager {

    private final Grid<Map<String, String>> operationsGrid;
    @Getter
    private final List<Map<String, String>> operationsList;
    private final TextField operationField;
    private final TextField intensityField;
    private final ComboBox<String> unitField;
    private final Button addButton;
    private final Button clearButton;
    private final VerticalLayout container;

    public OperationGridManager() {
        operationsList = new ArrayList<>();
        operationsGrid = new Grid<>();
        operationField = new TextField("Операция");
        intensityField = new TextField("Интенсивность");
        intensityField.setPattern("\\d+(\\.\\d{1,2})?");
        intensityField.setPreventInvalidInput(true);
        unitField = new ComboBox<>("Единица времени", List.of("Секунды", "Минуты", "Часы", "Дни"));
        addButton = new Button("Добавить");
        clearButton = new Button("Очистить всё");
        container = new VerticalLayout();
        configureGrid();
        configureControls();
        assembleContainer();
    }

    private void configureGrid() {
        operationsGrid.setItems(operationsList);
        operationsGrid.addColumn(item -> item.get("Операция"))
                .setHeader("Операция")
                .setEditorComponent(operationField);
        operationsGrid.addColumn(item -> item.get("Интенсивность"))
                .setHeader("Интенсивность")
                .setEditorComponent(intensityField);
        operationsGrid.addColumn(item -> item.get("Единица времени"))
                .setHeader("Единица времени")
                .setEditorComponent(unitField);
        operationsGrid.addComponentColumn(item -> {
            Button deleteButton = new Button(VaadinIcon.CLOSE.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(event -> {
                operationsList.remove(item);
                operationsGrid.getDataProvider().refreshAll();
            });
            return deleteButton;
        });

        operationsGrid.getEditor().setBuffered(true);
    }

    private void configureControls() {
        clearButton.addClickListener(e -> {
            operationsList.clear();
            operationsGrid.getDataProvider().refreshAll();
        });
        addButton.addClickListener(e -> addOperation());
    }

    private void assembleContainer() {
        HorizontalLayout controlsLayout = new HorizontalLayout(operationField, intensityField, unitField, addButton, clearButton);
        controlsLayout.setSpacing(true);
        controlsLayout.setPadding(true);
        controlsLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        container.add(operationsGrid, controlsLayout);
        container.setAlignItems(FlexComponent.Alignment.CENTER);
        container.getElement().getStyle()
                .set("border-top", "1px solid var(--lumo-contrast-20pct)")
                .set("background-color", "#f0f0f0")
                .set("padding", "var(--lumo-space-m)");
    }

    public void addOperation() {
        String op = operationField.getValue();
        String intensity = intensityField.getValue();
        String unit = unitField.getValue();
        if (op.isEmpty() || intensity.isEmpty() || unit == null) {
            NotificationHelper.showDebouncedNotification("Пожалуйста, заполните все поля", NotificationVariant.LUMO_ERROR);
            return;
        }


        boolean duplicate = operationsList.stream().anyMatch(map -> op.equals(map.get("Операция")));
        if (duplicate) {
            return;
        }
        Map<String, String> newOp = new HashMap<>();
        newOp.put("Операция", op);
        newOp.put("Интенсивность", intensity);
        newOp.put("Единица времени", unit);
        operationsList.add(newOp);
        operationsGrid.getDataProvider().refreshAll();
        operationField.clear();
        intensityField.clear();
        unitField.clear();
    }

    public Component getContainer() {
        return container;
    }

}