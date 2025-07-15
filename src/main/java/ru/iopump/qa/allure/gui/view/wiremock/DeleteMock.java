package ru.iopump.qa.allure.gui.view.wiremock;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.io.File;
import java.io.IOException;

import static ru.iopump.qa.allure.gui.view.wiremock.Mocks.mockDataList;
import static ru.iopump.qa.allure.gui.view.wiremock.MocksData.GetMocksData;
import static ru.iopump.qa.allure.gui.view.wiremock.Mocks.*;

public class DeleteMock {
    static HorizontalLayout DeleteButton(GridData item) {
        Icon trashIcon = new Icon(VaadinIcon.TRASH);
        trashIcon.getElement().getStyle().set("color", "red");
        Button deleteButton = new Button(trashIcon);
        HorizontalLayout layout = new HorizontalLayout(deleteButton);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER); // Центрируем по горизонтали
        layout.setAlignItems(FlexComponent.Alignment.CENTER); // Центрируем по вертикали
        deleteButton.addClickListener(click -> deleteItem(item));
        return layout;
    }

    // Метод для удаления элемента
    private static void deleteItem(GridData item) {
        // Получаем id удаляемой заглушки
        String idToDelete = item.getId();
        // Выодим диалоговое окно с подтверждением удаления
        Dialog dialog = new Dialog();

        // Создаем сообщение для диалога
        dialog.add("Вы уверены, что хотите удалить мок с ID: " + idToDelete + "?");

        // Кнопка "Да" - подтверждает удаление
        Button yesButton = new Button("Да", event -> {
            // Вызываем метод для удаления мока
            // Удаляем элемент из списка
            mockDataList.remove(item);
            DeleteFileMock(idToDelete);
            // Обновляем грид
            //Mocks.grid.setItems(mockDataList);
            //-----------------------------------//
            try {
                //Mocks rf = new Mocks();
                Mocks.refreshGrid();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            dialog.close(); // Закрываем диалог после удаления
        });
        yesButton.addClassName("del-mock-yes");
        // Кнопка "Нет" - отменяет удаление
        Button noButton = new Button("Нет", event -> {
            dialog.close(); // Закрываем диалог без удаления
        });
        noButton.addClassName("del-mock-no");
        dialog.addClassName("dlg-conf-del");
        // Размещаем кнопки в горизонтальном layout
        HorizontalLayout layout = new HorizontalLayout();
        layout.addClassName("hor-lay-conf-del");
        layout.add(yesButton, noButton);
        layout.setSpacing(true);
        dialog.add(layout);
        dialog.open(); // Открываем диалог
    }
    public static void DeleteFileMock(String idMock) {

        File file = new File(String.format("/allure-app/src/main/resources/WireMock/%s/mappings/%s.json", Mocks.nameTeam, idMock)); // Путь к файлу
        if (file.delete()) {
            Notifications.NotificationSuccess("Мок " + idMock + "удален!");
        } else {
            Notifications.NotificationError("Не удалось удалить мок " + idMock);
        }
    }
}


