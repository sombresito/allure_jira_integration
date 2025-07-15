package ru.iopump.qa.allure.gui.view;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import ru.iopump.qa.allure.gui.ReportsMain;

import javax.servlet.ServletContext;

import static ru.iopump.qa.allure.gui.ReportsMain.ALLURE_SERVER;
import static ru.iopump.qa.allure.helper.Util.concatParts;

@Tag("swagger-view")
@PageTitle("Swagger | " + ALLURE_SERVER)
@Route(value = "swagger", layout = ReportsMain.class)
@Slf4j
public class SwaggerView extends VerticalLayout {

    private static final long serialVersionUID = 5822077036734476962L;
    public SwaggerView(ServletContext context) {
        var frame = new IFrame(concatParts(context.getContextPath(), "swagger"));
        frame.setSizeFull();
        add(frame);

        // Устанавливаем фон напрямую
        getStyle().set("background-color", "#595959")
                .set("color", "#FFFFFF")  // Белый цвет текста
                .set("height", "100%")    // Полная высота
                .set("width", "100%");    // Полная ширина
    }
}
