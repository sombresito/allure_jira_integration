package ru.iopump.qa.allure.gui.view.wiremock;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.grid.Grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ru.iopump.qa.allure.gui.view.wiremock.CreateTeam.GetListTeams;
import static ru.iopump.qa.allure.gui.view.wiremock.MocksData.GetMocksData;
import static ru.iopump.qa.allure.gui.view.wiremock.Notifications.NotificationInform;
import ru.iopump.qa.allure.gui.view.wiremock.ViewMock.*;
import static ru.iopump.qa.allure.gui.view.wiremock.DeleteMock.*;

@Route("create-mock-form")
@PageTitle("Создание заглушек WireMock")
@CssImport("./styles/wiremock.css")
public class Mocks extends VerticalLayout {
    private final TextArea textArea = new TextArea("Создание заглушки");
    private final ComboBox<String> comboBoxTeam = new ComboBox<>("Выберите команду");
    //private final Button showGridButton = new Button("Показать данные", event -> toggleGridVisibility());
    private String name;
    private String email;
    //private Grid<List<String>> grid;
    private Button toggleButton;
    private static List<List<String>> listMocks = new ArrayList<>();
    public static String nameTeam;
    public final Label l = new Label();
    public final Label l2 = new Label();
    public final Label l3 = new Label();
    static Grid<GridData> grid = new Grid<>(GridData.class);
    static List<GridData> mockDataList = new ArrayList<>();
    //private Icon downArrow = new Icon(VaadinIcon.CARET_DOWN );
    private String toggleTextBtn;

    public Mocks() throws IOException {
        this.createHeader();
        addClassName("mock-form");
        grid = new Grid<>();
        setPadding(false);
        getElement().getStyle()
                .set("background-image", "linear-gradient(135deg, #707070, #e0e0e0)")
                .set("min-height", "100vh");
        textArea.setPlaceholder("Введите заглушку в формате JSON");
        textArea.addClassName("create-mock-text-area");
        Button createMock = new Button("Создать", e -> CreateMock());
        createMock.addClassName("start-button");
        VerticalLayout layout = new VerticalLayout();
        layout.setVisible(false);
        layout.addClassName("vertical-layuot");
        textArea.setRequired(true);
        layout.setAlignItems(Alignment.CENTER); // Центрирование по горизонтали
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER); // Центрирование по вертикали
        layout.setAlignSelf(Alignment.START, createMock);
        layout.setAlignItems(Alignment.END);
        // грид с моками
        //grid = new Grid<>(GetDataMock.class, false);

        grid.addClassName("grid-mocks");
        toggleButton = new Button("Показать заглушки", event -> toggleGridVisibility());
        toggleButton.addClassName("trapezoid-button");
        grid.setVisible(false);
        toggleButton.setIcon(null);
        Div gridContainer = new Div(grid);
        // Обработчик для кнопки
        //Grid<List<String>> finalGrid = grid;
        //showGridButton.addClickListener(event -> {
        //    finalGrid.setVisible(true); // Показываем grid
        //});
        layout.add(textArea, createMock, toggleButton, grid);
        add(layout);

        // Создаем диалог для выбора
        Dialog dialog = new Dialog();
        // Закрываем диалог при переходе назад в браузере
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(true);
        List<String> listTeam = GetListTeams();
        comboBoxTeam.setItems(listTeam);
        //comboBoxTeam.setItems("GreenFront", "BCCBusiness", "MSB");
        comboBoxTeam.setRequired(true);  // Устанавливаем поле обязательным для заполнения
        comboBoxTeam.setRequiredIndicatorVisible(true);  // Отображаем индикатор обязательного поля (красная звездочка)
        Button selectButton = new Button("Выбрать", e -> {
            // Когда пользователь выбирает вариант, закрываем диалог и показываем основную форму
            if (comboBoxTeam.getValue() == null) {
                comboBoxTeam.setErrorMessage("Это поле обязательно для заполнения");
                comboBoxTeam.setInvalid(true);
            } else {
                comboBoxTeam.setInvalid(false); // Убираем ошибку, если значение выбрано
                dialog.close();
                nameTeam = comboBoxTeam.getValue();
                Notification inform = NotificationInform("Вы выбрали: " + nameTeam);
                inform.open();
                // Отображаем основную форму после выбора
                layout.setVisible(true);
                //nameTeam = (String.valueOf(comboBoxTeam.getValue()) != null ? String.valueOf(comboBoxTeam.getValue()).trim() : "");
                // Обновляем grid

            }
        });
        Icon closeIconDlgTeam = new Icon(VaadinIcon.CLOSE);
        Button closeDlgTeam = new Button(closeIconDlgTeam, e -> {
            dialog.close();
        });
        closeDlgTeam.getStyle().set("position", "absolute");
        closeDlgTeam.getStyle().set("top", "10px");
        closeDlgTeam.getStyle().set("right", "10px");
        closeDlgTeam.getStyle().set("border", "none");
        closeDlgTeam.getStyle().set("background", "transparent");
        closeDlgTeam.getStyle().set("cursor", "pointer");
        closeDlgTeam.getStyle().set("color", "red");
        // Добавляем ComboBox и кнопку в диалог
        dialog.add(comboBoxTeam, selectButton, closeDlgTeam);
        // Показываем диалог
        dialog.open();
        ViewMock viewMock = new ViewMock();
        grid.addColumn(GridData::getNumber).setHeader("№").setResizable(true).setWidth("10%");
        grid.addColumn(GridData::getId).setHeader("ID").setResizable(true).setWidth("50%");
        grid.addComponentColumn(viewMock::ViewButton)
                .setHeader("Просмотр")
                .setResizable(true)
                .setWidth("20%").setTextAlign(ColumnTextAlign.CENTER);
        // Добавляем кнопку "Удалить"
        grid.addComponentColumn(DeleteMock::DeleteButton)
                .setHeader("Удалить")
                .setResizable(true)
                .setWidth("20%").setTextAlign(ColumnTextAlign.CENTER);
        // Добавляем колонку с кнопкой "Просмотр"


        // Добавляем JavaScript код для отслеживания истории браузера
        UI.getCurrent().getPage().executeJs(
                "window.history.pushState({}, '','/new-state');" +
                        "window.addEventListener('popstate', function(event) {" +
                        "  var dialog = document.querySelector('vaadin-dialog');" +
                        "  if (dialog) {" +
                        "    dialog.close();" + // Закрытие диалога
                        "  }" +
                        "});"
        );

        // Колонка с иконкой
        //grid.addComponentColumn(item -> createViewButton(item))
        //        .setHeader("Просмотр")
        //        .setWidth("150px")
        //        .setResizable(true);
    }
    static void updateGrid() {

        // Преобразуем listMocks в список объектов MockData

        for (List<String> row : listMocks) {
            // Предполагаем, что в каждой строке listMocks 3 элемента: номер и ID
            if (row.size() >= 3) {
                GridData gridData = new GridData(row.get(0), row.get(1), row.get(2));
                mockDataList.add(gridData);
            }
        }
    }
    static void refreshGrid() throws IOException {
        mockDataList.clear();
        listMocks.clear();
        listMocks = GetMocksData();

        // Обновление UI на правильном потоке
        // Если нужно, можно явно вызвать обновление провайдера данных
        //grid.setItems(mockDataList);
        ListDataProvider<GridData> dataProvider = new ListDataProvider<>(mockDataList);
        grid.setDataProvider(dataProvider);
        grid.getDataProvider().refreshAll();
        updateGrid();


    }
    private void toggleGridVisibility() {
        // Если Grid скрыт, показываем его и меняем текст кнопки
        if (grid.isVisible()) {
            grid.setVisible(false);
            toggleButton.setText("Показать заглушки");
            toggleButton.setIcon(null);
            toggleButton.removeClassName("rectangular-button");
            toggleButton.addClassName("trapezoid-button");
        } else {
            grid.setVisible(true);
            //toggleButton.setText("Скрыть заглушки");
            toggleButton.setText(null);
            Icon iconDown = new Icon(VaadinIcon.ARROW_DOWN);
            iconDown.addClassName("v-icon");
            toggleButton.setIcon(iconDown);
            toggleButton.removeClassName("trapezoid-button");
            toggleButton.addClassName("rectangular-button");
            try {
                mockDataList.clear();
                listMocks.clear();
                listMocks = GetMocksData();
                grid.setItems(mockDataList);

                updateGrid();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }
    }
    // Метод для создания кнопки с иконкой
    private Component createViewButton(GridData item) {
        Button viewButton = new Button();
        Icon viewIcon = new Icon(VaadinIcon.EYE);  // Иконка "глаз" для просмотра
        viewButton.setIcon(viewIcon);

        // Добавление действия при клике
        viewButton.addClickListener(e -> {
            // Логика для обработки просмотра (например, отображение деталей)
            //String id = item.get(1);  // Получаем id из текущей строки
            System.out.println("Просмотр элемента с ID: ");
            // Можно открыть диалог, страницу или выполнить другую операцию
        });

        return viewButton;  // Возвращаем компонент, который будет отображаться в ячейке
    }
    public void CreateMock() {
        try {
            String mockJson = (String.valueOf(textArea.getValue()) != null ? String.valueOf(textArea.getValue()).trim() : "");
            String nameTeam = (String.valueOf(comboBoxTeam.getValue()) != null ? String.valueOf(comboBoxTeam.getValue()).trim() : "");
            DockerClient dockerClient = DockerClientBuilder.getInstance().build();
            // Получаем имя контейнера
            //String nameContainer = GetNameContainer(nameTeam);
            // Создаем команду для выполнения внутри контейнера
            String[] commandMock = ConfigureCmdCommand(mockJson); // получаем строку с командой
            ExecCreateCmd execCreateCmd = dockerClient.execCreateCmd(nameTeam)
                    .withAttachStdout(true)  // Подключаем стандартный вывод
                    .withAttachStderr(true)  // Подключаем стандартную ошибку
                    .withCmd(commandMock);
            // Выполнение команды и получение ExecStartCmd
            ExecStartCmd execStartCmd = dockerClient.execStartCmd(execCreateCmd.exec().getId())
                    .withDetach(true); // Без отсоединения, будем ожидать завершения
            execStartCmd.exec(new ExecStartResultCallback(System.out, System.err)).awaitCompletion();
            textArea.clear();
            try {
                // Приостановить выполнение на 3 секунды (3000 миллисекунд)
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //------------------------------------//
            // Обновление UI на правильном потоке
            UI.getCurrent().access(() -> {
                try {
                    refreshGrid();  // Обновляем грид в UI потоке
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            //-----------------------------------//
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String GetNameContainer(String nameTeam) {
        if (nameTeam.equals("GreenFront")) {
            return "greenfront";
        } else if (nameTeam.equals("BCCBusiness")) {
            return "bccbusiness";
        } else if (nameTeam.equals("MSB")) {
            return "msb";
        } else {
            return null;
        }
    }
    private String[] ConfigureCmdCommand(String mockJson) {
        try {
            // Получаем системную переменную с портом
            // на котором запущен wiremock
            String[] command = {
                    "curl",
                    "-X", "POST",
                    String.format("http://localhost:%s/__admin/mappings", StartWireMock.mockPort),
                    "-H", "Content-Type: application/json",
                    "-d", mockJson
            };
            //String command = String.format("curl -X POST http://localhost:8081/__admin/mappings -H \"Content-Type:application/json\" -d '%s'", mockJson);
            // перенос строки
            return command;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createHeader() {
        H1 logo = new H1("Создание заглушек MockServer");
        logo.addClassName("logo-text");
        Div spacer = new Div();
        logo.getElement().getStyle().set("color", "#FDFEFE");
        logo.getStyle().set("margin-top", "1px");
        Button backButton = new Button("Вернуться на главную MockServer", (e) -> this.getUI().ifPresent((ui) -> ui.navigate("mock-main")));
        backButton.addClassName("header-button");
        HorizontalLayout header = new HorizontalLayout(logo, spacer, backButton);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.expand(spacer);
        header.setWidth("100%");
        header.addClassName("header");
        this.add(header);
    }
}
