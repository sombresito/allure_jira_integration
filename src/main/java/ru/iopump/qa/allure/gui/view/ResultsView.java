package ru.iopump.qa.allure.gui.view; //NOPMD

import com.google.common.collect.ImmutableList;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import ru.iopump.qa.allure.controller.AllureResultController;
import ru.iopump.qa.allure.entity.ReportEntity;
import ru.iopump.qa.allure.gui.DateTimeResolver;
import ru.iopump.qa.allure.gui.ReportsMain;
import ru.iopump.qa.allure.gui.component.Col;
import ru.iopump.qa.allure.gui.component.FilteredGrid;
import ru.iopump.qa.allure.gui.component.ReportGenerateDialog;
import ru.iopump.qa.allure.gui.component.ResultUploadDialog;
import ru.iopump.qa.allure.gui.dto.GenerateDto;
import ru.iopump.qa.allure.model.ResultResponse;
import ru.iopump.qa.util.StreamUtil;

import javax.annotation.PostConstruct;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;

import static ru.iopump.qa.allure.gui.ReportsMain.ALLURE_SERVER;
import static ru.iopump.qa.allure.gui.component.Col.prop;
import static ru.iopump.qa.allure.gui.component.ResultUploadDialog.toMultiPartFile;

@Tag("results-view")
@PageTitle("Results | " + ALLURE_SERVER)
@Route(value = "results", layout = ReportsMain.class)
@Slf4j
public class ResultsView extends VerticalLayout {
    private static final long serialVersionUID = 5822017036734416962L;


    /* COMPONENTS */
    private final FilteredGrid<ResultResponse> results;
    private final Button generateButton;
    private final ReportGenerateDialog generateDialog;
    private final Button uploadButton;
    private final ResultUploadDialog uploadDialog;

    private final Button deleteSelection;
    private final DateTimeResolver dateTimeResolver;
    private TextField filterField;
    public ResultsView(final AllureResultController allureResultController,
                       final AllureReportController allureReportController,
                       final DateTimeResolver dateTimeResolver,
                       final MultipartProperties multipartProperties) {
        this.dateTimeResolver = dateTimeResolver;
        this.dateTimeResolver.retrieve();

        this.results = new FilteredGrid<>(
                asProvider(allureResultController),
                cols()
        );
        this.generateButton = new Button("Создать отчет", new Icon(VaadinIcon.PLUS));
        generateButton.addClassName("create-report-button");

        this.uploadButton = new Button("Загрузить результаты", new Icon(VaadinIcon.UPLOAD)); // Измените текст на "Upload" для совпадения с примером
        uploadButton.addClassName("custom-upload-button"); // Применение нового стиля

        this.generateDialog = new ReportGenerateDialog(allureReportController);
        this.uploadDialog = new ResultUploadDialog(
                (buffer, nameFile) -> allureResultController.uploadResults(toMultiPartFile(buffer)),
                (int) multipartProperties.getMaxFileSize().toBytes(),
                "results"
        );

        uploadDialog.onClose(event -> results.getGrid().getDataProvider().refreshAll());

        this.results.getGrid().addClassName("custom-grid-background");
        this.deleteSelection = new Button("Удалить выделенное",
                new Icon(VaadinIcon.CLOSE_CIRCLE),
                event -> {
                    for (ResultResponse resultResponse : results.getGrid().getSelectedItems()) {
                        String uuid = resultResponse.getUuid();
                        try {
                            allureResultController.deleteResult(uuid);
                            Notification notification = Notification.show("Delete success: " + uuid, 2000, Notification.Position.TOP_START);
                            notification.addThemeName("success"); // Добавляем тему "success"
                            notification.addClassName("custom-notification"); // Применяем CSS-класс
                        } catch (Exception e) { //NOPMD
                            Notification notification = Notification.show("Deleting error: " + e.getLocalizedMessage(),
                                    5000,
                                    Notification.Position.TOP_START);
                            notification.addThemeName("error"); // Добавляем тему "error"
                            notification.addClassName("custom-notification"); // Применяем CSS-класс
                            log.error("Deleting error", e);
                        }
                    }
                    results.getGrid().deselectAll();
                    results.getGrid().getDataProvider().refreshAll();
                });
        //deleteSelection.addThemeVariants(ButtonVariant.LUMO_ERROR);
        // Темно-красный шрифт для кнопки "Удалить выделенное"
        deleteSelection.addClassName("custom-delete-button"); // Применяем новый CSS-класс

        generateDialog.addOpenedChangeListener(event -> {
            StreamUtil.stream(results.getGrid().getSelectedItems()).findFirst()
                    .ifPresentOrElse(resultResponse -> generateDialog.getPayload().getBinder()
                                    .setBean(new GenerateDto(resultResponse.getUuid(), null, null, false)),
                            () -> generateDialog.getPayload().getBinder().setBean(new GenerateDto())
                    );
        });

        this.dateTimeResolver.onClientReady(() -> results.getGrid().getDataProvider().refreshAll());
    }
    //// PRIVATE ////

    private static ListDataProvider<ResultResponse> asProvider(final AllureResultController allureResultController) {
        //noinspection unchecked
        final Collection<ResultResponse> collection = (Collection<ResultResponse>) Proxy
                .newProxyInstance(Thread.currentThread().getContextClassLoader(),
                        new Class[]{Collection.class},
                        (proxy, method, args) -> method.invoke(allureResultController.getAllResult(), args));

        return new ListDataProvider<>(collection);
    }

    private List<Col<ResultResponse>> cols() {
        return ImmutableList.<Col<ResultResponse>>builder()
                .add(Col.<ResultResponse>with().name("ID").value(prop("uuid")).build())
                .add(Col.<ResultResponse>with()
                        .name("Created")
                        .value(e -> dateTimeResolver.printDate(e.getCreated()))
                        .build())
                .add(Col.<ResultResponse>with().name("Size KB").value(prop("size")).type(Col.Type.NUMBER).build())
                .build();
    }


    @PostConstruct
    public void postConstruct() {
        // Настройка текстового поля фильтра
        filterField = new TextField();
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setValueChangeTimeout(100);
        filterField.setClearButtonVisible(true);
        filterField.setPlaceholder("Поиск ...");
        filterField.getElement().setAttribute("title", "Введите текст для поиска по всем столбцам таблицы");

// Ключевое слово текущей страницы
        String currentPageKeyword = "colvir"; // Задайте значение "cfg", "test" или "dev" в зависимости от текущей страницы

        filterField.addValueChangeListener(event ->
                results.applyGlobalFilter(event.getValue(), currentPageKeyword)
        );
        filterField.addClassName("search-field"); // Применение класса для фона
        // Настройка кнопок управления
        generateDialog.addControlButton(generateButton);
        uploadDialog.addControlButton(uploadButton);

        // Создаем макет для фиксированных элементов управления
        HorizontalLayout controlLayout = new HorizontalLayout(filterField, generateButton, uploadButton, deleteSelection);
        controlLayout.setSpacing(true);
        controlLayout.setWidthFull();
        controlLayout.setPadding(true);
        controlLayout.setAlignItems(FlexComponent.Alignment.CENTER); // Центрирует элементы по вертикали
        // Оборачиваем компонент results в контейнер с прокруткой
        VerticalLayout resultsContainer = new VerticalLayout(results.getGrid());
        resultsContainer.setSizeFull();
        resultsContainer.setPadding(false);
        resultsContainer.setSpacing(false);
        resultsContainer.getStyle().set("overflow", "auto"); // Включаем прокрутку только для results


        // Основной макет с зафиксированными элементами управления и прокручиваемым контейнером
        VerticalLayout mainLayout = new VerticalLayout(controlLayout, resultsContainer);
        mainLayout.setSizeFull();
        mainLayout.setPadding(false);

        mainLayout.setFlexGrow(1, resultsContainer); // Устанавливаем resultsContainer, чтобы занимать оставшееся пространство

        add(mainLayout, generateDialog);
    }


}
