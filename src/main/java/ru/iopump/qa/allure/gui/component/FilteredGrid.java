package ru.iopump.qa.allure.gui.component;

import com.google.common.collect.Maps;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import ru.iopump.qa.allure.entity.ReportEntity;
import ru.iopump.qa.allure.security.SecurityUtils;
import ru.iopump.qa.util.Str;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ru.iopump.qa.allure.helper.Util.shortUrl;

public class FilteredGrid<T> {
    public static final String FONT_FAMILY = "font-family";
    public static final String GERMANIA_ONE = "Germania One";
    private final static String GRID_CLASS = "report-grid";
    private final ListDataProvider<T> dataProvider;
    @Getter
    private final Grid<T> grid;
    private final List<Col<T>> columnSpecList;
    private final Map<Grid.Column<T>, Supplier<String>> dynamicFooter = Maps.newHashMap();
    private SerializablePredicate<T> currentFilter; //тут удалить
    public FilteredGrid(
        @NonNull final ListDataProvider<T> dataProvider,
        @NonNull final List<Col<T>> columnSpecList
    ) {
        this.dataProvider = dataProvider;
        this.grid = new Grid<>();
        this.columnSpecList = columnSpecList;

        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        baseConfigurationGrid();
        filterConfiguration();

        updateFooters(); // Init footers
        dataProvider.addDataProviderListener(event -> updateFooters()); // Update footers on change
    }

    public FilteredGrid<T> addTo(HasComponents parent) {
        parent.add(grid);
        return this;
    }

    protected Grid.Column<T> addColumn(Col<T> columnSpec) {
        final Grid.Column<T> column;

        switch (columnSpec.getType()) {
            case LINK:
                column = grid.addColumn(link(columnSpec));
                break;
            case NUMBER:
                column = grid.addColumn(text(columnSpec));
                final Supplier<String> footer = () -> {
                    long amount = dataProvider.fetch(new Query<>(dataProvider.getFilter()))
                        .mapToLong(item -> Long.parseLong(Str.toStr(columnSpec.getValue().apply(item))))
                        .sum();
                    return "Итого: " + amount;
                };
                dynamicFooter.put(column, footer);
                break;
            default:
                column = grid.addColumn(text(columnSpec));
                break;
        }

        column.setKey(columnSpec.getName())
            .setHeader(columnSpec.getName())
            .setAutoWidth(true)
            .setSortable(columnSpec.isSortable());
        //noinspection unchecked,rawtypes
        column.setComparator((ValueProvider) columnSpec.getValue());
        return column;
    }


    private void filterConfiguration() {
    }

    private void addFilter(Col<T> spec, HeaderRow.HeaderCell headerCell, String pageKeyword) {

        final TextField filterField = new TextField();

        filterField.addValueChangeListener(event -> {
            applyGlobalFilter(event.getValue(), pageKeyword); // Используем значение из события и ключевое слово страницы
        });

        // Конфигурация filterField
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setValueChangeTimeout(3000);
        filterField.setClearButtonVisible(true);
        filterField.setPlaceholder("Поиск...");
        headerCell.setComponent(filterField);
    }


    public void applyGlobalFilter(String filterValue, String pageKeyword) {
        boolean isUser = SecurityUtils.hasRole("ROLE_USER");

        dataProvider.setFilter(row -> {
            ReportEntity reportEntity = (ReportEntity) row;

            // Проверяем, относится ли строка к текущей странице (например, test, dev, cfg)
            boolean isRelevantPageData = StringUtils.containsIgnoreCase(reportEntity.getPath(), pageKeyword) ||
                    StringUtils.containsIgnoreCase(reportEntity.getUrl(), pageKeyword);

            // Пропускаем строки, не относящиеся к текущей странице
            if (!isRelevantPageData) return false;

            boolean isActive = reportEntity.isActive();
            if (isUser && !isActive) return false; // Для USER показываем только активные строки

            if (StringUtils.isBlank(filterValue)) return true; // Если фильтр пустой, отображаем все строки, относящиеся к странице

            // Фильтрация по всем столбцам
            return columnSpecList.stream().anyMatch(columnSpec -> {
                var value = columnSpec.getValue().apply(row);
                return StringUtils.containsIgnoreCase(Str.toStr(value), filterValue);
            });
        });

        updateFooters();
    }




    private void baseConfigurationGrid() {
        grid.addClassName(GRID_CLASS);
        grid.setDataProvider(dataProvider);
        grid.removeAllColumns();
        grid.setHeightByRows(true);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        final List<Grid.Column<T>> cols = columnSpecList.stream()
            .map(this::addColumn)
            .collect(Collectors.toUnmodifiableList());
        cols.stream().findFirst()
            .ifPresent(c -> dynamicFooter.put(c, () -> "Количество: " + dataProvider
                .size(new Query<>(dataProvider.getFilter())))
            );
    }

    private Renderer<T> text(Col<T> columnSpec) {
        return new ComponentRenderer<>(row -> {
            var value = Str.toStr(columnSpec.getValue().apply(row));
            var res = new Span(value);
            res.getStyle().set(FONT_FAMILY, GERMANIA_ONE);
            return res;
        });
    }
    public void applyPathFilter(String... keywords) {
        dataProvider.setFilter(row -> {
            if (row instanceof ReportEntity) {
                ReportEntity reportEntity = (ReportEntity) row;
                for (String keyword : keywords) {
                    if (StringUtils.containsIgnoreCase(reportEntity.getPath(), keyword)) {
                        return true;
                    }
                }
            }
            return false;
        });
        updateFooters();
    }

    private Renderer<T> link(Col<T> columnSpec) {
        return new ComponentRenderer<>(row -> {
            var link = Str.toStr(columnSpec.getValue().apply(row));
            var res = new Anchor(link, StringUtils.defaultIfBlank(shortUrl(link), link));
            res.setTarget("_blank");
            res.getStyle().set(FONT_FAMILY, GERMANIA_ONE);
            return res;
        });
    }

    private void updateFooters() {
        dynamicFooter.forEach((col, sup) -> col.setFooter(sup.get()));
    }
//endregion
}
