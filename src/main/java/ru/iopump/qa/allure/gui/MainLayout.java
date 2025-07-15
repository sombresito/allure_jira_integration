package ru.iopump.qa.allure.gui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Anchor;
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
import ru.iopump.qa.allure.gui.view.ArtificialIntelligence;
import ru.iopump.qa.allure.gui.view.ReportsViewPreprod;
import ru.iopump.qa.allure.gui.view.SyntheticGenerationView;
import ru.iopump.qa.allure.security.SimpleHttpsServer;
import ru.iopump.qa.allure.gui.view.wiremock.WireMockMain;

//@Route("")
@CssImport("./styles/styles.css")
@JsModule("./brands.js")
public class MainLayout extends AppLayout {
    public static final String ALLURE_SERVER = "Allure Server bcc hub";
    private static final long serialVersionUID = 2881152775131362224L;

    public static final String FONT_FAMILY = "font-family";
    public static final String GERMANIA_ONE = "Cambria";



    public MainLayout() {
        this.createHeader();
        this.createDrawer();
        this.createDescription();
    }


    private void createHeader() {
        H3 logo = new H3("Allure Server bcc hub");
        logo.addClassName("logo-text");
        Div spacer = new Div();
        logo.getElement().getStyle().set("color", "#FDFEFE");
        logo.getStyle().set("margin-top", "1px");
        HorizontalLayout header = new HorizontalLayout(new Component[]{new DrawerToggle(), logo, spacer});
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.expand(new Component[]{spacer});
        header.setWidth("100%");
        header.addClassName("header");
        this.addToNavbar(new Component[]{header});
    }

    private void createDrawer() {
        RouterLink reportsMain = new RouterLink("Отчеты автотестов", ReportsViewPreprod.class);
        reportsMain.setHighlightCondition(HighlightConditions.sameLocation());
        reportsMain.addClassName("full-width-link");
        RouterLink load = new RouterLink("Нагрузочное тестирование", LoadLayout.class);
        load.setHighlightCondition(HighlightConditions.sameLocation());
        load.addClassName("full-width-link");
        RouterLink generation = new RouterLink("Генератор синтетики", SyntheticGenerationView.class);
        generation.setHighlightCondition(HighlightConditions.sameLocation());
        generation.addClassName("full-width-link");
        RouterLink wireMock = new RouterLink("Mock Server", WireMockMain.class);
        wireMock.setHighlightCondition(HighlightConditions.sameLocation());
        wireMock.addClassName("full-width-link");
        Anchor auto = new Anchor("http://10.15.123.137:9090/", "Помощник NoMadAI");
        //auto.setHighlightCondition(HighlightConditions.sameLocation());
        auto.addClassName("full-width-link");
        VerticalLayout linksContainer = new VerticalLayout(new Component[]{reportsMain, load, generation, auto, wireMock}); //auto, generation, auto
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
                new Paragraph("На странице отчётов:\n" +
                        "предоставляются подробные отчёты по результатам автоматизированных и мануальных тестов. Пользователь может анализировать выполненные тесты, их успешность, длительность и обнаруженные ошибки."),
                new Paragraph("На странице нагрузочного тестирования:\n" +
                        "можно сделать заказ на проведение нагрузочного тестирования с помощью анкеты, а также создать и настроить скрипты JMX для моделирования различных сценариев нагрузки."),
                new Paragraph("На странице Mock Server\n" +
                        "можно настраивать, запускать и создавать заглушки.")
                //new Paragraph("НА СТАДИИ РЕАЛИЗАЦИИ: Страница Помогайка ИИ:\n" +
                //        "На этой странице В ПРОЦЕССЕ реализации функции искусственного интеллекта, которые будут помогать в написание автотестов и предоставлять рекомендации для улучшения кода или процессов тестирования.")
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

    private void startHttpsServer() {
        // Запуск HTTPS-сервера в отдельном потоке
        Thread serverThread = new Thread(() -> {
            try {
                SimpleHttpsServer.startHttpsServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true); // Сервер завершится вместе с приложением
        serverThread.start();
    }

}
