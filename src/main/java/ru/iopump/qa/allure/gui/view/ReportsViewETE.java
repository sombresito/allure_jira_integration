package ru.iopump.qa.allure.gui.view;

import com.google.common.collect.ImmutableList;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import ru.iopump.qa.allure.controller.AllureReportController;
import ru.iopump.qa.allure.entity.ReportEntity;
import ru.iopump.qa.allure.gui.DateTimeResolver;
import ru.iopump.qa.allure.gui.ReportsMain;
import ru.iopump.qa.allure.gui.component.Col;
import ru.iopump.qa.allure.gui.component.FilteredGrid;
import ru.iopump.qa.allure.gui.component.ResultUploadDialog;
import ru.iopump.qa.allure.properties.AllureProperties;
import ru.iopump.qa.allure.properties.BasicProperties;
import ru.iopump.qa.allure.properties.ReportProperties;
import ru.iopump.qa.allure.security.SecurityUtils;
import ru.iopump.qa.allure.service.JpaReportService;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static ru.iopump.qa.allure.gui.MainLayout.ALLURE_SERVER;
import static ru.iopump.qa.allure.gui.component.Col.Type.LINK;
import static ru.iopump.qa.allure.gui.component.Col.Type.NUMBER;
import static ru.iopump.qa.allure.gui.component.Col.prop;
import static ru.iopump.qa.allure.gui.component.ResultUploadDialog.toMultiPartFile;
import static ru.iopump.qa.allure.helper.Util.url;


@Tag("reports-view-ete")
@PageTitle("ReportsETE | " + ALLURE_SERVER)
@Route(value = "reports-ete", layout = ReportsMain.class)


@Slf4j
public class ReportsViewETE extends VerticalLayout {
    private final ReportProperties reportProperties;
    private static final long serialVersionUID = 5822017036734476962L;
    private final DateTimeResolver dateTimeResolver;
    private final AllureProperties allureProperties;
    private final FilteredGrid<ReportEntity> reports;
    private final Button deleteSelection;
    private final ResultUploadDialog uploadDialog;
    private final BasicProperties basicProperties;
    private final Button uploadButton;
    private TextField filterField;


    public ReportsViewETE(final JpaReportService jpaReportService,
                          final AllureReportController allureReportController,
                          final DateTimeResolver dateTimeResolver,
                          final AllureProperties allureProperties,
                          final MultipartProperties multipartProperties, BasicProperties basicProperties, ReportProperties reportProperties) {
        this.reportProperties = reportProperties;
        this.dateTimeResolver = dateTimeResolver;
        this.allureProperties = allureProperties;
        this.basicProperties = basicProperties;
        this.dateTimeResolver.retrieve();
        this.uploadDialog = new ResultUploadDialog(
                (buffer, getNameFile) -> allureReportController.uploadReport("manual uploaded "+ getNameFile.substring(0, getNameFile.length() -4).toUpperCase(), toMultiPartFile(buffer)),
                (int) multipartProperties.getMaxFileSize().toBytes(),
                "отчета"
        );
        this.reports = new FilteredGrid<>(
                asProvider(jpaReportService, reportProperties),
                cols()
        );
        this.reports.getGrid().addClassName("custom-grid-background");
        this.uploadButton = new Button("Загрузить отчет", new Icon(VaadinIcon.UPLOAD)); // Измените текст на "Upload" для совпадения с примером
        uploadButton.addClassName("custom-upload-button"); // Применение нового стиля

        this.deleteSelection = new Button("Удалить выделенное",
                new Icon(VaadinIcon.TRASH),
                event -> {
                    Set<ReportEntity> selectedItems = new HashSet<>(reports.getGrid().getSelectedItems());
                    for (ReportEntity reportEntity : selectedItems) {
                        UUID uuid = reportEntity.getUuid();
                        try {
                            jpaReportService.internalDeleteByUUID(uuid);
                            Notification.show("Delete success: " + uuid, 2000, Notification.Position.TOP_START);
                        } catch (Exception e) { //NOPMD
                            Notification.show("Deleting error: " + e.getLocalizedMessage(),
                                    5000,
                                    Notification.Position.TOP_START);
                            log.error("Deleting error", e);
                        }
                    }
                    reports.getGrid().deselectAll();

                    // Пересоздаем и устанавливаем новый провайдер данных
                    ListDataProvider<ReportEntity> updatedProvider = asProvider(jpaReportService, reportProperties);
                    reports.getGrid().setDataProvider(updatedProvider);
                });
        deleteSelection.addClassName("custom-delete-button"); // Применяем новый CSS-класс
        this.dateTimeResolver.onClientReady(() -> reports.getGrid().getDataProvider().refreshAll());
        uploadDialog.onClose(event -> reports.getGrid().getDataProvider().refreshAll());
    }

    //// PRIVATE ////
    private static ListDataProvider<ReportEntity> asProvider(final JpaReportService jpaReportService,ReportProperties reportProperties) {
        // Получаем список данных
        Collection<ReportEntity> collection = jpaReportService.getAll();
        // Сортировка списка так, чтобы запись с интересующим ID была на верху
        List<ReportEntity> sortedList = collection.stream()
                .sorted(Comparator.comparing(reportEntity -> {
                    if (reportEntity.getUuid().equals(UUID.fromString(reportProperties.getYourETEReportProperty()))) {
                        return -1; // Запись с интересующим ID будет на верху
                    } else {
                        return 1; // Остальные записи будут ниже
                    }
                }))
                .collect(Collectors.toList());
        // Создаем новый ListDataProvider с отсортированным списком
        return new ListDataProvider<>(sortedList);
    }

    private List<Col<ReportEntity>> cols() {
        ImmutableList.Builder<Col<ReportEntity>> columnsBuilder = ImmutableList.builder();
        // Проверяем, имеет ли пользователь роль USER
        boolean isUser = SecurityUtils.hasRole("ROLE_USER");
        // Добавляем колонки в зависимости от роли пользователя
        if (!isUser) {
            // Для пользователей, не являющихся USER, добавляем колонку ID
            columnsBuilder.add(Col.<ReportEntity>with().name("ID").value(prop("uuid")).build());
        }
        // Колонки, которые отображаются для всех пользователей
        columnsBuilder.add(Col.<ReportEntity>with()
                .name("Created")
                .value(e -> dateTimeResolver.printDate(e.getCreatedDateTime()))
                .build());
        columnsBuilder.add(Col.<ReportEntity>with().name("Url").value(this::displayUrl).type(LINK).build());
        if (!isUser) {
            // Для пользователей, не являющихся USER, добавляем колонки
            columnsBuilder.add(Col.<ReportEntity>with().name("Path").value(prop("path")).build());
            columnsBuilder.add(Col.<ReportEntity>with().name("Active").value(prop("active")).build());
            columnsBuilder.add(Col.<ReportEntity>with().name("Size KB").value(prop("size")).type(NUMBER).build());
        }
        // Колонка Build отображается для всех пользователей
        columnsBuilder.add(Col.<ReportEntity>with().name("Pipeline").value(this::buildUrl).type(LINK).build());

        return columnsBuilder.build();
    }

    private String buildUrl(ReportEntity e) {
        return e.getBuildUrl();
    }

    private String displayUrl(ReportEntity e) {
        if (e.isActive()) {
            return e.generateLatestUrl(url(allureProperties), allureProperties.reports().path());
        } else {
            return e.generateUrl(url(allureProperties), allureProperties.reports().dir());
        }
    }

    private void applyUserRoleFilter() {
        // Устанавливаем фильтр на dataProvider отчетов
        ListDataProvider<ReportEntity> dataProvider = (ListDataProvider<ReportEntity>) reports.getGrid().getDataProvider();
        dataProvider.addFilter(reportEntity -> reportEntity.isActive());
    }

    @PostConstruct
    public void postConstruct() {
        reports.applyPathFilter("e2e");
        if (SecurityUtils.hasRole("ROLE_USER")) {
            applyUserRoleFilter();
        }
        // Настраиваем поле фильтра
        filterField = new TextField();
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setValueChangeTimeout(100);
        filterField.setClearButtonVisible(true);
        filterField.setPlaceholder("Поиск ...");
        filterField.getElement().setAttribute("title", "Введите текст для поиска по всем столбцам таблицы");
        // Ключевое слово текущей страницы
        String currentPageKeyword = "e2e"; // Задайте значение "cfg", "test" или "dev" в зависимости от текущей страницы
        filterField.addValueChangeListener(event ->
                reports.applyGlobalFilter(event.getValue(), currentPageKeyword)
        );
        filterField.addClassName("search-field"); // Применение класса для фона
        // Создаем макет для кнопок и поля фильтра
        HorizontalLayout buttonLayout = new HorizontalLayout();
        if (SecurityUtils.hasRole("ROLE_ADMIN")) {
            // Для роли "ROLE_ADMIN" добавляем все кнопки
            buttonLayout.add(filterField, uploadButton, deleteSelection);
            uploadDialog.addControlButton(uploadButton);
        } else if (SecurityUtils.hasRole("ROLE_USER")) {
            // Для роли "ROLE_USER" скрываем кнопку удаления
            buttonLayout.add(filterField, uploadButton);
            uploadDialog.addControlButton(uploadButton);
        } else {
            // Для других ролей показываем только фильтр
            buttonLayout.add(filterField, uploadButton);
            uploadDialog.addControlButton(uploadButton);
        }
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(true);
        buttonLayout.setWidthFull();
        buttonLayout.setAlignItems(FlexComponent.Alignment.CENTER); // Центрирует элементы по вертикали
        filterField.getStyle().set("margin-right", "14px");
        uploadButton.getStyle().set("margin-right", "14px");



        // Настраиваем сетку отчетов через Grid
        Grid<ReportEntity> reportGrid = reports.getGrid();
        reportGrid.setHeight("100%"); // Устанавливаем высоту 100% для заполнения доступного пространства

        // Оборачиваем сетку в прокручиваемый контейнер и задаем фиксированную высоту
        VerticalLayout gridContainer = new VerticalLayout(reportGrid);
        gridContainer.setSizeFull();
        gridContainer.setPadding(false);
        gridContainer.setSpacing(false);
        gridContainer.getStyle().set("overflow", "auto"); // Включаем прокрутку для контейнера

        // Создаем основной макет с закрепленным блоком кнопок и прокручиваемой сеткой
        VerticalLayout mainLayout = new VerticalLayout(buttonLayout, gridContainer);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);
        mainLayout.setFlexGrow(1, gridContainer); // Позволяет gridContainer занимать оставшуюся часть пространства

        add(mainLayout);
    }

}

