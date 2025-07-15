package ru.iopump.qa.allure.gui.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.grid.Grid;
import ru.iopump.qa.allure.properties.GenerateConfig;
import ru.iopump.qa.allure.service.generate.ConnectionData;
import ru.iopump.qa.allure.service.generate.CreateAccClient;
import ru.iopump.qa.allure.service.generate.GenerateIINUI;
import ru.iopump.qa.allure.service.generate.IINRecord;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Route("Gen-Syn")
@PageTitle("Генерация Синтетики")
@CssImport("./styles/gen-view.css")
@CssImport(value = "./styles/gen-view.css", themeFor = "vaadin-notification")
public class SyntheticGenerationView extends VerticalLayout {

    private Notification currentNotification;

    private Dialog environmentDialog;
    private Dialog thirdSelectionDialog;
    private Dialog multiSelectionDialog;
    private Dialog singleSelectionDialog;

    // Основные кнопки
    private final Button selectedValuesButton;
    private final Button additionalSelectionButton;
    private final Button thirdSelectionButton;
    private final Button clearButton;
    private final Button createButton;
    private final Button environmentButton;

    // Кнопка «+» для добавления валют
    private final Button addAccountButton;

    // Чекбоксы
    private final Checkbox residentCheckbox;
    private final Checkbox createCardCheckbox;
    private final Checkbox createAccountCheckbox;  // «Создание счета»

    // Текстовые поля
    private final TextField quantityField;
    private final TextField loginField;
    private final PasswordField passwordField;

    // Динамические кнопки «Выбрать валюту»
    private final List<Button> currencyButtons = new ArrayList<>();

    // Layout для кнопок «Выбрать валюту»
    private final VerticalLayout currencyLayout;

    // Прочие поля
    private String selectedDbUrl;
    private String selectedTypeIIN;
    private final GenerateConfig config;
    private Grid<IINRecord> iinGrid;

    // Основные Layout’ы
    private final HorizontalLayout topRow;
    private final HorizontalLayout environmentRow;
    private final HorizontalLayout loginRow;
    private HorizontalLayout middleRow;
    private final VerticalLayout cardLayout;
    private final VerticalLayout accountLayout; // Тут чекбокс, кнопка + и currencyLayout

    public SyntheticGenerationView(GenerateConfig config) {
        this.config = config;
        this.addClassName("gen-view");
        this.setMargin(false);
        this.setPadding(false);
        this.setSpacing(false);
        this.setWidthFull();
        this.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        // ---------- Заголовок ----------
        HorizontalLayout header = createHeader();

        // ---------- Поле "Количество штук" ----------
        quantityField = new TextField();
        quantityField.setPlaceholder("Количество штук");
        quantityField.setPattern("\\d+");
        quantityField.setPreventInvalidInput(true);
        quantityField.addClassName("custom-text-field");
        quantityField.setWidth("600px");
        // Валидация
        quantityField.addValueChangeListener(event -> {
            String value = event.getValue();
            if (!value.matches("^[1-9]\\d*$")) {
                quantityField.setInvalid(true);
                quantityField.setErrorMessage("Введите число без ведущих нулей и больше нуля");
            } else {
                try {
                    int numericValue = Integer.parseInt(value);
                    if (numericValue > 20) {
                        quantityField.setInvalid(true);
                        quantityField.setErrorMessage("Введите число не больше 20");
                    } else {
                        quantityField.setInvalid(false);
                    }
                } catch (NumberFormatException e) {
                    quantityField.setInvalid(true);
                    quantityField.setErrorMessage("Введите корректное число");
                }
            }
        });

        // ---------- Кнопка "Выберите тип клиента" ----------
        thirdSelectionButton = new Button("Выберите тип клиента", e -> openThirdSelectionDialog());
        thirdSelectionButton.addClassName("custom-button");
        thirdSelectionButton.setWidth("600px");

        topRow = new HorizontalLayout(quantityField, thirdSelectionButton);
        topRow.setWidth("1200px");
        topRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        topRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        // ---------- Кнопка "Выбрать стенд" ----------
        environmentButton = new Button("Выбрать стенд", e -> openEnvironmentDialog());
        environmentButton.addClassName("custom-button");
        environmentButton.setWidth("600px");

        environmentRow = new HorizontalLayout(environmentButton);
        environmentRow.setWidth("1200px");
        environmentRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        environmentRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        // ---------- Поля "Логин" и "Пароль" ----------
        loginField = new TextField();
        loginField.setPlaceholder("Логин");
        loginField.addClassName("custom-text-field");
        loginField.setRequiredIndicatorVisible(true);
        loginField.setMaxLength(40);

        passwordField = new PasswordField();
        passwordField.setPlaceholder("Пароль");
        passwordField.addClassName("custom-text-field");
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setMaxLength(40);

        loginRow = new HorizontalLayout(loginField, passwordField);
        loginRow.setWidth("1200px");
        loginRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        loginRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        // ---------- Чекбокс "Резидент" ----------
        residentCheckbox = new Checkbox("Проставьте чекбокс для создания Резидента");
        residentCheckbox.addClassName("custom-checkbox");
        // Собираем accountLayout
        accountLayout = new VerticalLayout();
        accountLayout.setAlignItems(Alignment.CENTER);
        accountLayout.setWidth("1200px");
        accountLayout.setVisible(false);

        // ---------- Чекбокс "Создание карты" ----------
        createCardCheckbox = new Checkbox("Создание карточки клиента");
        createCardCheckbox.addValueChangeListener(event -> {
            boolean checked = event.getValue();
            if (checked) {
                // Если включено создание карты, ставим "1" и блокируем поле
                quantityField.setValue("1");
                quantityField.setEnabled(false);

                // Показать уведомление (пример текста — отредактируйте под себя)
                showSingleNotification("Убедитесь, что ИИН уже сгенерированы, перед генерацией карточки клиента!", "warning");
            } else {
                // Если чекбокс сняли — возвращаем возможность редактировать поле
                quantityField.setEnabled(true);
            }
            middleRow.setVisible(checked);
            accountLayout.setVisible(checked);
        });
        createCardCheckbox.setValue(false);


        // Кнопки для параметров карты
        selectedValuesButton = new Button("Выберите параметры", e -> openMultiSelectionDialog());
        selectedValuesButton.addClassName("custom-button");
        selectedValuesButton.setWidth("600px");

        additionalSelectionButton = new Button("Выберите параметры", e -> openSingleSelectionDialog());
        additionalSelectionButton.addClassName("custom-button");
        additionalSelectionButton.setWidth("600px");

        middleRow = new HorizontalLayout(selectedValuesButton, additionalSelectionButton);
        middleRow.setWidth("1200px");
        middleRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        middleRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        middleRow.setVisible(false);

        cardLayout = new VerticalLayout();
        cardLayout.setAlignItems(Alignment.CENTER);
        cardLayout.add(createCardCheckbox, middleRow);

        // ---------- Чекбокс "Создание счета" ----------
        createAccountCheckbox = new Checkbox("Создание счета");
        createAccountCheckbox.addClassName("custom-checkbox");
        createAccountCheckbox.setValue(false);

        // ---------- Layout для динамических кнопок «Выбрать валюту» ----------
        currencyLayout = new VerticalLayout();
        currencyLayout.setVisible(false);
        // Выравниваем по центру, чтобы кнопки «Выбрать валюту» были по середине
        currencyLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        // Кнопка «+» для добавления валют
        addAccountButton = new Button("+");
        addAccountButton.addClassName("custom-button");
        // Разместим «+» по центру
        // Можно обернуть в HorizontalLayout, если нужно ещё более гибко:
        //  HorizontalLayout plusLayout = new HorizontalLayout(addAccountButton);
        //  plusLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        //  plusLayout.setWidthFull();
        //  и добавить этот plusLayout в accountLayout
        addAccountButton.addClickListener(e -> {
            if (currencyButtons.size() >= 5) {
                showSingleNotification("Максимум 5 счетов", "warning");
                return;
            }
            addCurrencyButton();
        });


        // Добавляем чекбокс "Создание счета"
        accountLayout.add(createAccountCheckbox);
        // Добавляем кнопку «+»
        accountLayout.add(addAccountButton);
        // Добавляем layout с валютами
        accountLayout.add(currencyLayout);

        // По умолчанию и «+», и currencyLayout скрыты, пока чекбокс не включён
        addAccountButton.setVisible(false);
        currencyLayout.setVisible(false);

        // Слушатель на чекбокс "Создание счета"
        createAccountCheckbox.addValueChangeListener(event -> {
            boolean checked = event.getValue();
            addAccountButton.setVisible(checked);
            currencyLayout.setVisible(checked);
        });

        // ---------- Собираем блок опций ----------
        VerticalLayout optionsLayout = new VerticalLayout();
        optionsLayout.setWidth("1200px");
        optionsLayout.setAlignItems(Alignment.CENTER);
        // Добавляем: резидент, блок карты, блок счёта
        optionsLayout.add(residentCheckbox, cardLayout, accountLayout);

        // ---------- Кнопки "Создать", "Очистить", "Удалить" ----------
        clearButton = new Button("Очистить", e -> clearSelection());
        clearButton.addClassName("clear-button");

        createButton = new Button("Создать", e -> createAction());
        createButton.addClassName("create-button");

        Button deleteButton = new Button("Удалить", e -> deleteAction());
        deleteButton.addClassName("delete-button");

        HorizontalLayout buttonLayout = new HorizontalLayout(createButton, clearButton, deleteButton);
        buttonLayout.setWidthFull();
        buttonLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        // ---------- Собираем основной Layout ----------
        this.add(header, topRow, environmentRow, loginRow, optionsLayout);

        Div spacer = new Div();
        spacer.getStyle().set("height", "60px");
        this.add(spacer);

        // ---------- Таблица (Grid) ----------
        Div tableContainer = new Div();
        tableContainer.setWidth("80%");
        tableContainer.setMaxWidth("1850px");
        tableContainer.getStyle().set("margin", "0 auto");

        iinGrid = new Grid<>(IINRecord.class);
        iinGrid.removeAllColumns();
        iinGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        iinGrid.setWidth("100%");
        iinGrid.setHeight("300px");
        tableContainer.add(iinGrid);
        this.add(tableContainer, buttonLayout);
    }

    /**
     * Метод для динамического добавления кнопки «Выбрать валюту».
     */
    private void addCurrencyButton() {
        final Button currencyBtn = new Button("Выбрать валюту");
        currencyBtn.addClassName("custom-button");
        currencyBtn.setWidth("600px");
        currencyBtn.addClickListener(e -> openCurrencyDialog(currencyBtn));

        currencyButtons.add(currencyBtn);
        currencyLayout.add(currencyBtn);
    }

    /**
     * Диалог выбора валюты для конкретной кнопки.
     */
    private void openCurrencyDialog(Button currencyBtn) {
        Dialog dialog = new Dialog();
        dialog.setModal(false);

        RadioButtonGroup<String> currencyGroup = new RadioButtonGroup<>();
        currencyGroup.setLabel("Валюта");
        currencyGroup.setItems("KZT", "USD");

        Button confirmButton = new Button("Применить", e -> {
            String selected = currencyGroup.getValue();
            if (selected == null || selected.isEmpty()) {
                showSingleNotification("Выберите валюту!", "warning");
            } else {
                currencyBtn.setText("Валюта: " + selected);
            }
            dialog.close();
        });
        confirmButton.addClassName("custom-button");

        Button cancelButton = new Button("Отмена", e -> dialog.close());
        cancelButton.addClassName("cancel-button");

        HorizontalLayout buttonsLayout = new HorizontalLayout(confirmButton, cancelButton);
        VerticalLayout layout = new VerticalLayout(currencyGroup, buttonsLayout);
        dialog.add(layout);
        dialog.open();
    }

    /**
     * Создаёт заголовок (header) с кнопкой "Вернуться на главную страницу".
     */
    private HorizontalLayout createHeader() {
        H1 title = new H1("Генерация синтетических данных");
        title.addClassName("header-title");

        Button backButton = new Button("Вернуться на главную страницу", e ->
                UI.getCurrent()                 // текущее UI
                        .getPage()                   // страница-браузер
                        .setLocation("/ui/")         // <-- абсолютный путь к React-SPA
        );

        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("header");
        header.setWidthFull();
        header.setHeight("60px");
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.add(title, backButton);
        return header;
    }

    /**
     * Метод "Создать" (логика генерации, создания карточек, счетов и т. д.)
     */
    private void createAction() {
        boolean valid = true;
        String quantityValue = quantityField.getValue();
        if (quantityValue == null || quantityValue.trim().isEmpty()) {
            quantityField.setInvalid(true);
            quantityField.setErrorMessage("Количество штук обязательно");
            valid = false;
        } else {
            quantityField.setInvalid(false);
        }

        String dbUser = loginField.getValue();
        if (dbUser == null || dbUser.trim().isEmpty()) {
            loginField.setInvalid(true);
            loginField.setErrorMessage("Логин обязателен");
            valid = false;
        } else {
            loginField.setInvalid(false);
        }

        String dbPass = passwordField.getValue();
        if (dbPass == null || dbPass.trim().isEmpty()) {
            passwordField.setInvalid(true);
            passwordField.setErrorMessage("Пароль обязателен");
            valid = false;
        } else {
            passwordField.setInvalid(false);
        }

        if (selectedDbUrl == null || selectedDbUrl.isEmpty()) {
            System.err.println("Стенд не выбран!");
            valid = false;
        }

        if (!valid) {
            showSingleNotification("Проверьте правильность заполнения полей", "error");
            return;
        }

        int cntIIN;
        try {
            cntIIN = Integer.parseInt(quantityValue);
        } catch (NumberFormatException e) {
            quantityField.setInvalid(true);
            quantityField.setErrorMessage("Введите корректное число");
            return;
        }

        // Подключаемся к БД, отключаем триггер, загружаем процедуру (при необходимости)
        try {
            ConnectionData.connectToDatabase(selectedDbUrl, dbUser, dbPass);
            ConnectionData.executeQueryDML("ALTER trigger SCMCHECK_BS disable", selectedDbUrl, dbUser, dbPass);

            // Проверка наличия таблицы MONEY_ACC
            if (!tableExists(selectedDbUrl, dbUser, dbPass, "MONEY_ACC")) {
                String createTableSQL = "CREATE TABLE MONEY_ACC(ACC varchar2(255), VAL varchar2(255), SUMM varchar2(255), TYPE_CLI varchar2(255), CLI_CODE varchar2(255))";
                ConnectionData.executeQueryDML(createTableSQL, selectedDbUrl, dbUser, dbPass);
                showSingleNotification("Таблица MONEY_ACC создана", "success");
            }

            // Проверка наличия пакета Z_PKG_AUTO_TEST_GENERATOR
            if (!packageExists(selectedDbUrl, dbUser, dbPass, "Z_PKG_AUTO_TEST_GENERATOR")) {
                String pkgSpec = loadSQLFromFile("Z_PKG_AUTO_TEST_GENERATOR.spc");
                String pkgBody = loadSQLFromFile("Z_PKG_AUTO_TEST_GENERATOR.bdy");
                ConnectionData.executeQueryDML(pkgSpec, selectedDbUrl, dbUser, dbPass);
                ConnectionData.executeQueryDML(pkgBody, selectedDbUrl, dbUser, dbPass);
                showSingleNotification("Пакет GENERATOR загружен в базу данных", "success");
            }

            // Проверка наличия пакета Z_PKG_AUTO_TEST_GENERATOR
            if (!packageExists(selectedDbUrl, dbUser, dbPass, "Z_PKG_AUTO_TEST")) {
                String pkgSpec = loadSQLFromFile("Z_PKG_AUTO_TEST.spc");
                String pkgBody = loadSQLFromFile("Z_PKG_AUTO_TEST.bdy");
                ConnectionData.executeQueryDML(pkgSpec, selectedDbUrl, dbUser, dbPass);
                ConnectionData.executeQueryDML(pkgBody, selectedDbUrl, dbUser, dbPass);
                showSingleNotification("Пакет AUTO_TEST загружен в базу данных", "success");
            }


            // Проверка наличия процедуры CreateClientCardJava
            if (!procedureExists(selectedDbUrl, dbUser, dbPass, "CreateClientCardJava")) {
                String sql = loadSQLFromFile("CreateClientCardJava.prc");
                ConnectionData.executeQueryDML(sql, selectedDbUrl, dbUser, dbPass);
                showSingleNotification("Процедура CreateClient загружена в базу данных", "success");
            }
        } catch (SQLException ex) {
            if (ex.getMessage().toLowerCase().contains("invalid")
                    || ex.getMessage().toLowerCase().contains("password")) {
                showSingleNotification("Неверный пароль или логин", "warning");
                return;
            }
            showSingleNotification("Ошибка подключения к БД: " + ex.getMessage(), "error");
            return;
        } catch (Exception e) {
            showSingleNotification("Ошибка загрузки процедуры: " + e.getMessage(), "error");
            return;
        }

        try {
            GenerateIINUI generator = new GenerateIINUI();
            boolean isResident = residentCheckbox.getValue();
            String desiredResident = isResident ? "1" : "0";

            boolean createClientCard = createCardCheckbox.getValue();
            int originalCount = cntIIN;
            if (createClientCard) {
                // Если выбрано "Создание карты", удваиваем кол-во ИИН
                cntIIN = cntIIN * 2;
            }
            // Определяем тип для генерации: если выбран ИП или ИПС, генерируем как для Юр лица
            String generationType = selectedTypeIIN;
            if ("PBOYULS".equals(selectedTypeIIN)) {
                generationType = "JUR";
            } else if ("PBOYUL".equals(selectedTypeIIN) ) {
                generationType = "FL";
            }


            // Генерируем ИИН
            ArrayList<IINRecord> records = generator.GenerateIINUI(
                    "TESTINGIINMANUAL" + generationType,
                    generationType,
                    cntIIN,
                    selectedDbUrl,
                    dbUser,
                    dbPass,
                    desiredResident
            );

            List<IINRecord> finalRecords;
            if (createClientCard) {
                if (records.size() < originalCount) {
                    showSingleNotification("Сгенерировалось меньше записей, чем нужно!", "warning");
                    finalRecords = records;
                } else {
                    finalRecords = new ArrayList<>(records.subList(0, originalCount));
                }
            } else {
                finalRecords = records;
            }

            // Настраиваем Grid
            iinGrid.removeAllColumns();
            iinGrid.setSelectionMode(Grid.SelectionMode.MULTI);

            //if (!createClientCard) {
            iinGrid.addColumn(IINRecord::getIin).setHeader("ИИН");
            //}

            if ("FL".equals(selectedTypeIIN) || "PBOYUL".equals(selectedTypeIIN)) {
                iinGrid.addColumn(IINRecord::getSex).setHeader("Пол");
                iinGrid.addColumn(IINRecord::getBirthday).setHeader("Дата рождения");
                iinGrid.addColumn(IINRecord::getResident).setHeader("Резидент");
                iinGrid.addColumn(IINRecord::getName).setHeader("Имя");
                iinGrid.addColumn(IINRecord::getSurname).setHeader("Фамилия");
                iinGrid.addColumn(IINRecord::getMiddleName).setHeader("Отчество");
            }  else if ("JUR".equals(selectedTypeIIN) || "PBOYULS".equals(selectedTypeIIN)) {
                iinGrid.addColumn(IINRecord::getTypeEntity).setHeader("Тип юрлица");
                iinGrid.addColumn(IINRecord::getTypeOrg).setHeader("Тип организации");
                iinGrid.addColumn(IINRecord::getDateReg).setHeader("Дата регистрации");
                iinGrid.addColumn(IINRecord::getResident).setHeader("Резидент");
                iinGrid.addColumn(IINRecord::getNameCom).setHeader("Наименование компании");
            }

            if (createClientCard) {
                iinGrid.addColumn(IINRecord::getClientCardResult).setHeader("Карточка Клиента");
            }

            // Динамическое число колонок для счетов = кол-ву кнопок «Выбрать валюту»
            if (createAccountCheckbox.getValue()) {
                int accountQty = currencyButtons.size();
                for (int i = 0; i < accountQty; i++) {
                    final int index = i;
                    iinGrid.addColumn(record -> {
                        String result = record.getClientAccountResult();
                        if (result == null || result.isEmpty()) {
                            return "";
                        }
                        String[] accounts = result.split(",\\s*");
                        return (index < accounts.length) ? accounts[index] : "";
                    }).setHeader(index == 0 ? "Счет клиента" : "Счет клиента " + (index + 1));
                }
            }
            iinGrid.setItems(finalRecords);

            // Если нужно — создаём карточки клиента
            if (createClientCard && !finalRecords.isEmpty()) {
                String cli_role = "CLI";
                String dep_code = "CNT";
                String type_doc = desiredResident.equals("1") ? "УЛ" : "IP";
                for (IINRecord record : finalRecords) {
                    String code_inn = record.getIin();
                    if ("JUR".equals(selectedTypeIIN)) {
                        ru.iopump.qa.allure.service.generate.CreateClientCardJurFL cardCreator =
                                new ru.iopump.qa.allure.service.generate.CreateClientCardJurFL();
                        String cardResult = cardCreator.CreateClientCardJur(
                                code_inn,
                                desiredResident,
                                type_doc,
                                cli_role,
                                record.getNameCom(),
                                dep_code,
                                record.getDateReg(),
                                selectedDbUrl,
                                dbUser,
                                dbPass
                        );
                        record.setClientCardResult(cardResult);
                    } else if ("FL".equals(selectedTypeIIN)) {
                        ru.iopump.qa.allure.service.generate.CreateClientCard cardCreator =
                                new ru.iopump.qa.allure.service.generate.CreateClientCard();
                        String cardResult = cardCreator.CreateClientCard(
                                code_inn,
                                type_doc,
                                cli_role,
                                "FL",
                                desiredResident,
                                "",
                                "",
                                record.getSex(),
                                record.getName(),
                                record.getSurname(),
                                record.getMiddleName(),
                                dep_code,
                                record.getBirthday(),
                                "",
                                selectedDbUrl,
                                dbUser,
                                dbPass
                        );
                        record.setClientCardResult(cardResult);
                    } else if ("PBOYUL".equals(selectedTypeIIN)) {
                        ru.iopump.qa.allure.service.generate.CreateClientCard cardCreator =
                                new ru.iopump.qa.allure.service.generate.CreateClientCard();
                        String cardResultIP = cardCreator.CreateClientCard(
                                code_inn,
                                type_doc,
                                cli_role,
                                "PBOYUL",
                                desiredResident,
                                "",
                                "ИП " + record.getName() + " " + record.getSurname(),
                                record.getSex(),
                                record.getName(),
                                record.getSurname(),
                                record.getMiddleName(),
                                dep_code,
                                record.getBirthday(),
                                record.getDateReg(),
                                selectedDbUrl,
                                dbUser,
                                dbPass
                        );
                        record.setClientCardResult(cardResultIP);
                    } else if ("PBOYULS".equals(selectedTypeIIN)) {
                        // Создание карточки для ИПС: передаём упрощённый набор параметров
                        ru.iopump.qa.allure.service.generate.CreateClientCardJurFL cardCreator =
                                new ru.iopump.qa.allure.service.generate.CreateClientCardJurFL();
                        String cardResult = cardCreator.CreateClientCardIpS(
                                code_inn,
                                desiredResident,
                                type_doc,
                                cli_role,
                                record.getNameCom(),
                                dep_code,
                                record.getDateReg(),
                                selectedDbUrl,
                                dbUser,
                                dbPass
                        );
                        record.setClientCardResult(cardResult);
                    }
                }
                iinGrid.getDataProvider().refreshAll();
                showSingleNotification("Карточки клиентов созданы для " + finalRecords.size() + " записей.", "success");
            }


            // Если нужно — создаём счета (по числу валют)
            if (createAccountCheckbox.getValue() && !finalRecords.isEmpty()) {
                CreateAccClient accCreator = new CreateAccClient();
                // Список валют, выбранных в кнопках
                List<String> currencyList = new ArrayList<>();
                for (Button btn : currencyButtons) {
                    String text = btn.getText();
                    if (text.startsWith("Валюта: ")) {
                        currencyList.add(text.substring(8).trim());
                    } else {
                        // Если не выбрано — ставим "KZT" по умолчанию
                        currencyList.add("KZT");
                    }
                }

                for (IINRecord record : finalRecords) {
                    String cli_code = record.getClientCardResult();
                    if (cli_code == null || cli_code.isEmpty()) {
                        // Если карточка не создана, то счёт не создаём
                        continue;
                    }
                    List<String> accCodes = new ArrayList<>();
                    for (String val_code : currencyList) {
                        String product_code = "FL".equals(selectedTypeIIN) ? "0.101.2.1" : "0.101.4.1";
                        String dep_code = "CNT";
                        String accCode = accCreator.CreateCurrentAcc(
                                dep_code, val_code, cli_code, product_code,
                                selectedDbUrl, dbUser, dbPass
                        );
                        accCodes.add(accCode);
                    }
                    record.setClientAccountResult(String.join(", ", accCodes));
                }
                iinGrid.getDataProvider().refreshAll();
                showSingleNotification("Счета созданы для " + finalRecords.size() + " клиентов.", "success");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод «Удалить» — удаляет записи из БД, если для них не созданы карточка/счёт
     */
    private void deleteAction() {
        Set<IINRecord> selectedRecords = iinGrid.getSelectedItems();
        if (selectedRecords.isEmpty()){
            showSingleNotification("Выберите строки для удаления", "warning");
            return;
        }
        String tableName = "TESTINGIINMANUAL" + selectedTypeIIN;
        String dbUser = loginField.getValue();
        String dbPass = passwordField.getValue();

        for (IINRecord record : selectedRecords) {
            // Если для записи созданы карточка или счёт — не удаляем
            if ((record.getClientCardResult() != null && !record.getClientCardResult().isEmpty()) ||
                    (record.getClientAccountResult() != null && !record.getClientAccountResult().isEmpty())) {
                continue;
            }
            String deleteQuery = String.format("DELETE FROM %s WHERE IIN = '%s'", tableName, record.getIin());
            try {
                ConnectionData.executeQueryDML(deleteQuery, selectedDbUrl, dbUser, dbPass);
            } catch (Exception ex) {
                ex.printStackTrace();
                showSingleNotification("Ошибка при удалении ИИН: " + record.getIin(), "error");
            }
        }

        List<IINRecord> newRecords = new ArrayList<>();
        for (IINRecord rec : iinGrid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList())) {
            if (!selectedRecords.contains(rec)) {
                newRecords.add(rec);
            }
        }
        iinGrid.setItems(newRecords);
        iinGrid.deselectAll();

    }

    /**
     * Метод очистки всех полей и динамических кнопок
     */
    private void clearSelection() {
        selectedValuesButton.setText("Выберите параметры");
        additionalSelectionButton.setText("Выберите параметр");
        thirdSelectionButton.setText("Выберите тип клиента");
        environmentButton.setText("Выбрать стенд");

        quantityField.clear();
        quantityField.setEnabled(true);
        loginField.clear();
        passwordField.clear();
        residentCheckbox.setValue(false);

        createCardCheckbox.setValue(false);
        createAccountCheckbox.setValue(false);

        // Удаляем все кнопки «Выбрать валюту»
        for (Button btn : currencyButtons) {
            currencyLayout.remove(btn);
        }
        currencyButtons.clear();

        // Скрываем layout с валютами и кнопку «+»
        addAccountButton.setVisible(false);
        currencyLayout.setVisible(false);
    }

    /**
     * Показать уведомление в верхней части экрана
     */
    private void showSingleNotification(String message, String themeName) {
        if (currentNotification != null) {
            currentNotification.close();
        }
        currentNotification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        currentNotification.addClassName("custom-notification");
        if (themeName != null && !themeName.isEmpty()) {
            currentNotification.addThemeName(themeName);
        }
    }

    /**
     * Проверка наличия процедуры в БД
     */
    private boolean procedureExists(String dbUrl, String user, String password, String procName) {
        String query = "SELECT COUNT(*) FROM user_objects " +
                "WHERE object_type = 'PROCEDURE' " +
                "AND object_name = UPPER('" + procName + "')";
        try {
            ArrayList<ArrayList> result = ConnectionData.executeQuerySelect(query, dbUrl, user, password);
            String rawValue = String.valueOf(result.get(0).get(0));
            double count = Double.parseDouble(rawValue);
            return count > 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Загрузка SQL из файла ресурсов (src/main/resources)
     */
    private String loadSQLFromFile(String fileName) throws IOException {
        InputStream is = getClass().getResourceAsStream("/" + fileName);
        if (is == null) {
            throw new FileNotFoundException("Файл не найден: " + fileName);
        }
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }


    /**
     * Проверка наличия пакета в БД
     */
    private boolean packageExists(String dbUrl, String user, String password, String packageName) {
        String query = "SELECT COUNT(*) FROM user_objects " +
                "WHERE object_type = 'PACKAGE' " +
                "AND object_name = UPPER('" + packageName + "')";
        try {
            ArrayList<ArrayList> result = ConnectionData.executeQuerySelect(query, dbUrl, user, password);
            String rawValue = String.valueOf(result.get(0).get(0));
            double count = Double.parseDouble(rawValue);
            return count > 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Проверка наличия таблицы в БД
     */
    private boolean tableExists(String dbUrl, String user, String password, String tableName) {
        String query = "SELECT COUNT(*) FROM user_tables WHERE table_name = UPPER('" + tableName + "')";
        try {
            ArrayList<ArrayList> result = ConnectionData.executeQuerySelect(query, dbUrl, user, password);
            String rawValue = String.valueOf(result.get(0).get(0));
            double count = Double.parseDouble(rawValue);
            return count > 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    /**
     * Диалог выбора типа клиента
     */
    private void openThirdSelectionDialog() {
        if (thirdSelectionDialog == null) {
            thirdSelectionDialog = new Dialog();
            thirdSelectionDialog.setModal(false);

            RadioButtonGroup<String> group = new RadioButtonGroup<>();
            group.setLabel("Выберите тип клиента");
            group.setItems("Физ лицо", "Юр лицо", "ИП");

            Button confirm = new Button("Применить", e -> {
                String sel = group.getValue();
                String typeIIN;
                if ("Юр лицо".equals(sel)) {
                    typeIIN = "JUR";
                } else if ("Физ лицо".equals(sel)) {
                    typeIIN = "FL";
                } else if ("ИП".equals(sel)) {
                    typeIIN = "PBOYUL";
                } else if ("ИПС".equals(sel)) {
                    typeIIN = "PBOYULS";
                } else {
                    typeIIN = "";
                }
                //thirdSelectionButton.setText("Выбрано: " + sel + " (" + typeIIN + ")");
                thirdSelectionButton.setText("Выбрано: " + sel);
                this.selectedTypeIIN = typeIIN;
                thirdSelectionDialog.close();
            });
            confirm.addClassName("custom-button");

            Button cancel = new Button("Отмена", e -> thirdSelectionDialog.close());
            cancel.addClassName("cancel-button");

            HorizontalLayout btnLayout = new HorizontalLayout(confirm, cancel);
            VerticalLayout layout = new VerticalLayout(group, btnLayout);
            thirdSelectionDialog.add(layout);
        }
        thirdSelectionDialog.open();
    }

    /**
     * Диалог выбора стенда
     */
    private void openEnvironmentDialog() {
        if (environmentDialog == null) {
            environmentDialog = new Dialog();
            environmentDialog.setModal(false);

            RadioButtonGroup<String> group = new RadioButtonGroup<>();
            group.setLabel("Выберите стенд");
            group.setItems("Dev (CBS3TP)", "Test (CBS3BT)", "Test (CBS3TEST)", "Preprod (CBS3YES)");

            Button confirm = new Button("Применить", e -> {
                String sel = group.getValue();
                if (sel != null && !sel.isEmpty()) {
                    if (sel.contains("CBS3TP")) {
                        selectedDbUrl = config.getTp_bd();
                    } else if (sel.contains("CBS3BT")) {
                        selectedDbUrl = config.getBt_bd();
                    } else if (sel.contains("CBS3TEST")) {
                        selectedDbUrl = config.getTest_bd();
                    } else if (sel.contains("CBS3YES")) {
                        selectedDbUrl = config.getYes_bd();
                    }
                    environmentButton.setText("Выбрано: " + sel);
                } else {
                    System.err.println("Стэнд не выбран");
                }
                environmentDialog.close();
            });
            confirm.addClassName("custom-button");

            Button cancel = new Button("Отмена", e -> environmentDialog.close());
            cancel.addClassName("cancel-button");

            HorizontalLayout btnLayout = new HorizontalLayout(confirm, cancel);
            VerticalLayout layout = new VerticalLayout(group, btnLayout);
            environmentDialog.add(layout);
        }
        environmentDialog.open();
    }

    /**
     * Диалог выбора параметров (MultiSelection)
     */
    private void openMultiSelectionDialog() {
        if (multiSelectionDialog == null) {
            multiSelectionDialog = new Dialog();
            multiSelectionDialog.setModal(false);

            RadioButtonGroup<String> group = new RadioButtonGroup<>();
            group.setLabel("Выберите параметры");
            group.setItems("Colvir");

            Button confirm = new Button("Применить", e -> {
                String sel = group.getValue();
                selectedValuesButton.setText("Выбрано: " + sel);
                multiSelectionDialog.close();
            });
            confirm.addClassName("custom-button");

            Button cancel = new Button("Отмена", e -> multiSelectionDialog.close());
            cancel.addClassName("cancel-button");

            HorizontalLayout btnLayout = new HorizontalLayout(confirm, cancel);
            VerticalLayout layout = new VerticalLayout(group, btnLayout);
            multiSelectionDialog.add(layout);
        }
        multiSelectionDialog.open();
    }

    /**
     * Диалог выбора одного параметра (SingleSelection)
     */
    private void openSingleSelectionDialog() {
        if (singleSelectionDialog == null) {
            singleSelectionDialog = new Dialog();
            singleSelectionDialog.setModal(false);

            RadioButtonGroup<String> group = new RadioButtonGroup<>();
            group.setLabel("Выберите параметры");
            group.setItems("Карточка Клиента");

            Button confirm = new Button("Применить", e -> {
                String sel = group.getValue();
                additionalSelectionButton.setText("Выбрано: " + sel);
                singleSelectionDialog.close();
            });
            confirm.addClassName("custom-button");

            Button cancel = new Button("Отмена", e -> singleSelectionDialog.close());
            cancel.addClassName("cancel-button");

            HorizontalLayout btnLayout = new HorizontalLayout(confirm, cancel);
            VerticalLayout layout = new VerticalLayout(group, btnLayout);
            singleSelectionDialog.add(layout);
        }
        singleSelectionDialog.open();
    }

}


