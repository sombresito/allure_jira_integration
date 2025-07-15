package ru.iopump.qa.allure.gui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import java.io.Serializable;
import ru.iopump.qa.allure.gui.view.AboutView;
import ru.iopump.qa.allure.gui.view.ReportsView;
import ru.iopump.qa.allure.gui.view.ReportsViewColvir;
import ru.iopump.qa.allure.gui.view.ReportsViewETE;
import ru.iopump.qa.allure.gui.view.ReportsViewDev;
import ru.iopump.qa.allure.gui.view.ReportsViewPreprod;
import ru.iopump.qa.allure.gui.view.ReportsViewTest;
import ru.iopump.qa.allure.gui.view.ResultsView;
import ru.iopump.qa.allure.gui.view.SwaggerView;
import ru.iopump.qa.allure.security.SecurityUtils;

@Route("reports-main")
@CssImport("./styles/styles.css")
@JsModule("./brands.js")
public class ReportsMain extends AppLayout {
    public static final String ALLURE_SERVER = "Reports bcc hub";
    private static final long serialVersionUID = 2881152775131362224L;

    public ReportsMain() {
        this.createHeader();
        this.createDrawer();
    }

    private void createHeader() {
        H3 logo = new H3("Reports bcc hub");
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
//        RouterLink reportsTest = new RouterLink("Отчеты Test окружения", ReportsViewTest.class);
//        reportsTest.setHighlightCondition(HighlightConditions.sameLocation());
//        reportsTest.addClassName("full-width-link");
        RouterLink reportsDev = new RouterLink("Отчеты Dev окружения", ReportsViewDev.class);
        reportsDev.setHighlightCondition(HighlightConditions.sameLocation());
        reportsDev.addClassName("full-width-link");
//        RouterLink reportsPreprod = new RouterLink("Отчеты Preprod окружения", ReportsViewPreprod.class);
//        reportsPreprod.setHighlightCondition(HighlightConditions.sameLocation());
//        reportsPreprod.addClassName("full-width-link");
        RouterLink reportsETE = new RouterLink("Отчеты E2E тестирования", ReportsViewETE.class);
        reportsETE.setHighlightCondition(HighlightConditions.sameLocation());
        reportsETE.addClassName("full-width-link");
        RouterLink reportsColvir = new RouterLink("Отчеты Colvir", ReportsViewColvir.class);
        reportsColvir.setHighlightCondition(HighlightConditions.sameLocation());
        reportsColvir.addClassName("full-width-link");
        RouterLink reports = new RouterLink("Хранилище отчетов", ReportsView.class);
        reports.setHighlightCondition(HighlightConditions.sameLocation());
        reports.addClassName("full-width-link");
        RouterLink results = new RouterLink("Результаты", ResultsView.class);
        results.setHighlightCondition(HighlightConditions.sameLocation());
        results.addClassName("full-width-link");
        RouterLink swagger = new RouterLink("Swagger", SwaggerView.class);
        swagger.setHighlightCondition(HighlightConditions.sameLocation());
        swagger.addClassName("full-width-link");
        RouterLink about = new RouterLink("О приложении", AboutView.class);
        about.setHighlightCondition(HighlightConditions.sameLocation());
        about.addClassName("full-width-link");
        VerticalLayout linksContainer = new VerticalLayout(new Component[]{reportsDev, reportsETE, reportsColvir, reports, results, about});
        linksContainer.setPadding(false);
        linksContainer.setSpacing(false);
        linksContainer.setSizeFull();
        if (SecurityUtils.hasRole("ROLE_ADMIN")) {
            linksContainer.add(new Component[]{swagger});
        }

        Div logoIcon = new Div();
        HorizontalLayout footer = new HorizontalLayout(new Component[]{logoIcon});
        VerticalLayout menu = new VerticalLayout(new Component[]{linksContainer, footer});
        menu.setSizeFull();
        menu.addClassName("menu-style");
        this.addToDrawer(new Component[]{menu});
        UI.getCurrent().getPage().executeJs("document.querySelectorAll('.full-width-link').forEach(link => {  link.addEventListener('click', function() {    document.querySelectorAll('.full-width-link').forEach(l => l.classList.remove('router-link-active'));    this.classList.add('router-link-active');  });  if (link.getAttribute('href') === window.location.pathname) {    link.classList.add('router-link-active');  }});", new Serializable[0]);
    }
}
