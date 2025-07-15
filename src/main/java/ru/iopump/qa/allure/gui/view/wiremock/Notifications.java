package ru.iopump.qa.allure.gui.view.wiremock;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE;

public class Notifications {
    public static Notification NotificationSuccess(String msgNotif) {
        Notification notification = new Notification();
        notification.setDuration(2000);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        Div statusText = new Div(new Text(msgNotif));

        var layout = new HorizontalLayout(statusText);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        notification.add(layout);
        notification.open();
        return notification;
        //Notification notification = new Notification();
        //notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        //Icon icon = VaadinIcon.CHECK_CIRCLE.create();
        //var layout = new HorizontalLayout(icon,
        //        new Text(msgNotif), createCloseBtn(notification));
        //layout.setAlignItems(FlexComponent.Alignment.CENTER);

        //notification.add(layout);

        //return notification;
    }
    public static Notification NotificationError(String msgNotif) {
        Notification notification = new Notification();
        notification.setDuration(2000);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

        Div statusText = new Div(new Text(msgNotif));

        var layout = new HorizontalLayout(statusText);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        notification.add(layout);
        notification.open();
        return notification;
        //Notification notification = new Notification();
        //notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        //Icon icon = VaadinIcon.WARNING.create();
        //var layout = new HorizontalLayout(icon,
        //        new Text(msgNotif), createCloseBtn(notification));
        //layout.setAlignItems(FlexComponent.Alignment.CENTER);
        //
        //notification.add(layout);

        //return notification;
    }
    public static Notification NotificationInform(String msgNotif) {
        Notification notification = new Notification();
        notification.setDuration(4000);
        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);

        Div statusText = new Div(new Text(msgNotif));

        var layout = new HorizontalLayout(statusText);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        notification.add(layout);
        notification.open();
        return notification;
    }
    public static Button createCloseBtn(Notification notification) {
        Button closeBtn = new Button(VaadinIcon.CLOSE_SMALL.create(),
                clickEvent -> notification.close());
        closeBtn.addThemeVariants(LUMO_TERTIARY_INLINE);

        return closeBtn;
    }
}
