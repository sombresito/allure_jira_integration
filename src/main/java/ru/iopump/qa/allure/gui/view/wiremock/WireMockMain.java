package ru.iopump.qa.allure.gui.view.wiremock;

import com.vaadin.flow.component.Component;
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
import java.io.Serializable;
import java.util.List;

import ru.iopump.qa.allure.gui.LoadLayout;
import ru.iopump.qa.allure.gui.loadView.ClientFormView;
import ru.iopump.qa.allure.gui.loadView.LoadJMXView;
import ru.iopump.qa.allure.gui.loadView.ManualLoadView;

import static ru.iopump.qa.allure.gui.view.wiremock.CreateTeamFromJson.loadConfigsFromDirectory;

@Route("mock-main")
@CssImport("./styles/styles.css")
@JsModule("./brands.js")
public class WireMockMain extends AppLayout {
    public static final String ALLURE_SERVER = "MockServer bcc hub";
    private static final long serialVersionUID = 2881152775131362224L;

    public static final String FONT_FAMILY = "font-family";
    public static final String GERMANIA_ONE = "Cambria";

    private final String configDir = "/allure/src/main/resources/Teams";

    public WireMockMain() {
        this.createHeader();
        this.createDrawer();
        this.createDescription();
    }

    private void createHeader() {
        H3 logo = new H3("MockServer bcc hub");
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
        HorizontalLayout header = new HorizontalLayout(new Component[]{new DrawerToggle(), logo, spacer, backButton});
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.expand(new Component[]{spacer});
        header.setWidth("100%");
        header.addClassName("header");
        this.addToNavbar(new Component[]{header});
    }

    private void createDrawer() {
        RouterLink runWireMock = new RouterLink("Запуск MockServer", StartWireMock.class);
        runWireMock.setHighlightCondition(HighlightConditions.sameLocation());
        runWireMock.addClassName("full-width-link");
        RouterLink creStub = new RouterLink("Создать заглушку", Mocks.class);
        creStub.setHighlightCondition(HighlightConditions.sameLocation());
        creStub.addClassName("full-width-link");
        RouterLink creTeam = new RouterLink("Создать команду", CreateTeam.class);
        creTeam.setHighlightCondition(HighlightConditions.sameLocation());
        creTeam.addClassName("full-width-link");
        VerticalLayout linksContainer = new VerticalLayout(new Component[]{runWireMock, creStub, creTeam});
        linksContainer.setPadding(false);
        linksContainer.setSpacing(false);
        linksContainer.setSizeFull();
        Div logoIcon = new Div();
        HorizontalLayout footer = new HorizontalLayout(new Component[]{logoIcon});
        VerticalLayout menu = new VerticalLayout(new Component[]{linksContainer, footer});
        menu.setSizeFull();
        menu.addClassName("menu-style");
        this.addToDrawer(new Component[]{menu});
        UI.getCurrent().getPage().executeJs("document.querySelectorAll('.full-width-link').forEach(link => {  link.addEventListener('click', function() {    document.querySelectorAll('.full-width-link').forEach(l => l.classList.remove('router-link-active'));    this.classList.add('router-link-active');  });  if (link.getAttribute('href') === window.location.pathname) {    link.classList.add('router-link-active');  }});", new Serializable[0]);
    }

    private void createDescription() {
        // Добавляем описание на главную страницу
        Div description = new Div(
                new Paragraph("Страница запуска MockServer:\n" +
                        "Позволяет настраивать и запускать MockServer."),
                new Paragraph("Страница создания заглушек:\n" +
                        "Позволяет создавать заглушки сервисов при помощи MockServer."),
                new Paragraph("Страница создания команды:\n" +
                        "Позволяет создавать докер-контейнер для команды")
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

