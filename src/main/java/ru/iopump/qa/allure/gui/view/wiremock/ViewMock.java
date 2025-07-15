package ru.iopump.qa.allure.gui.view.wiremock;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;

public class ViewMock {
    public HorizontalLayout ViewButton(GridData item) {

        Button viewButton = new Button(new Icon(VaadinIcon.EYE));
        viewButton.addClickListener(e -> openPreviewDialog(item));
        HorizontalLayout layout = new HorizontalLayout(viewButton);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER); // Центрируем по горизонтали
        layout.setAlignItems(FlexComponent.Alignment.CENTER); // Центрируем по вертикали
        return layout;
    }

    // Метод для открытия окна с телом заглушки
    private void openPreviewDialog(GridData item) {
        // Создаем окно
        Dialog dialog = new Dialog();

        // Добавляем компонент с телом заглушки
        TextArea bodyTextArea = new TextArea("Содержимое заглушки");
        bodyTextArea.addClassName("view-mock-textarea");
        bodyTextArea.setValue(item.getBody());
        bodyTextArea.setReadOnly(true); // Делаем текстовое поле только для чтения
        bodyTextArea.setWidth("400px");

        // Добавляем компонент в окно


        // Добавляем кнопку закрытия окна

        Icon closeViewIcon = new Icon(VaadinIcon.CLOSE);
        closeViewIcon.getElement().getStyle().set("color", "red");
        Button closeButton = new Button(closeViewIcon, e -> dialog.close());

        HorizontalLayout headerLayout = new HorizontalLayout(closeButton);
        headerLayout.setWidthFull(); // Сделать горизонтальный лейаут на всю ширину
        headerLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END); // Выравнивание кнопки по правому краю
        headerLayout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.START); // Выравнивание по верхнему краю
        dialog.add(headerLayout);
        dialog.add(bodyTextArea);
        // Показываем окно
        dialog.open();
    }
}
