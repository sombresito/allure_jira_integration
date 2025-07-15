package ru.iopump.qa.allure.gui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import ru.iopump.qa.allure.gui.loadView.ClientFormView;
import ru.iopump.qa.allure.gui.loadView.LoadJMXView;
import ru.iopump.qa.allure.gui.loadView.ManualLoadView;

@Route("load-main")
@CssImport("./styles/styles.css")
@JsModule("./brands.js")
public class LoadLayout extends AppLayout {
    public static final String ALLURE_SERVER = "Load Testing bcc hub";
    private static final long serialVersionUID = 2881152775131362224L;

    public static final String FONT_FAMILY = "font-family";
    public static final String GERMANIA_ONE = "Cambria";


    public LoadLayout() {
        this.createHeader();
        this.createDrawer();
        this.createDescription();
    }

    private void createHeader() {
        H3 logo = new H3("Load Testing bcc hub");
        logo.addClassName("logo-text");
        Div spacer = new Div();
        logo.getElement().getStyle().set("color", "#FDFEFE");
        logo.getStyle().set("margin-top", "1px");
        Button backButton = new Button("Вернуться на главную страницу", e ->
                UI.getCurrent()                 // текущее UI
                        .getPage()                   // страница-браузер
                        .setLocation("/ui/")         // <-- абсолютный путь к React-SPA
        );
        backButton.addClassName("header-button");
        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, spacer, backButton);
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.expand(spacer);
        header.setWidth("100%");
        header.addClassName("header");
        this.addToNavbar(header);
    }

    private void createDrawer() {
        RouterLink load = new RouterLink("Создать JMX (Auto)", LoadJMXView.class);
        load.setHighlightCondition(HighlightConditions.sameLocation());
        load.addClassName("full-width-link");
        RouterLink loadManual = new RouterLink("Создать JMX (Manual)", ManualLoadView.class);
        loadManual.setHighlightCondition(HighlightConditions.sameLocation());
        loadManual.addClassName("full-width-link");
        RouterLink clientForm = new RouterLink("Заполнить анкету клиента", ClientFormView.class);
        clientForm.setHighlightCondition(HighlightConditions.sameLocation());
        clientForm.addClassName("full-width-link");
        VerticalLayout linksContainer = new VerticalLayout(load, loadManual, clientForm);
        linksContainer.setPadding(false);
        linksContainer.setSpacing(false);
        linksContainer.setSizeFull();
        Div logoIcon = new Div();
        HorizontalLayout footer = new HorizontalLayout(logoIcon);
        VerticalLayout menu = new VerticalLayout(linksContainer, footer);
        menu.setSizeFull();
        menu.addClassName("menu-style");
        this.addToDrawer(menu);
        UI.getCurrent().getPage().executeJs("document.querySelectorAll('.full-width-link').forEach(link => {  link.addEventListener('click', function() {    document.querySelectorAll('.full-width-link').forEach(l => l.classList.remove('router-link-active'));    this.classList.add('router-link-active');  });  if (link.getAttribute('href') === window.location.pathname) {    link.classList.add('router-link-active');  }});");
    }

    private void createDescription() {
        // Добавляем описание на главную страницу
        Div description = new Div(
                new Paragraph("Страница создать JMX файл:\n" +
                        "Позволяет автоматически создать JMX файл на основе загруженной Postman коллекции. Включает различные возможности настроить дополнительные параметры."),
                new Paragraph("Страница создать JMX файл в ручную:\n" +
                        "Позволяет создать JMX файл вручную, указывая различные параметры."),
                new Paragraph("Страница заполнить анкету клиента:\n" +
                        "Дает возможность заказать нагрузочное тестирование, заполнив анкету с различными полями. После заполнения анкета отправляется ответственным лицам для дальнейшей обработки и подготовки тестирования.")
        );
        description.getStyle()
                .set(FONT_FAMILY, GERMANIA_ONE)
                .set("color", "black")
                .set("padding", "20px")
                .set("border", "2px solid white")
                .set("background-color", "#BFBFBF")
                .set("border-radius", "10px")
                .set("width", "calc(100% - 12cm)") // Учитываем отступы по 6см с каждой стороны
                .set("margin", "5cm auto 0 auto"); // Отступ сверху 5см и центрирование по горизонтали


        VerticalLayout mainLayout = new VerticalLayout(description);
        mainLayout.setAlignItems(Alignment.CENTER);
        mainLayout.setPadding(false);
        mainLayout.setSpacing(false);
        mainLayout.setSizeFull();

        this.setContent(mainLayout); // Устанавливаем описание как основное содержимое
    }
}
