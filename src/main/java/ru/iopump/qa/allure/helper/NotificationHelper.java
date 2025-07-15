package ru.iopump.qa.allure.helper;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class NotificationHelper {
    private static final Object NOTIFICATION_LOCK = new Object();
    private static volatile boolean isNotificationActive = false;

    public static void showDebouncedNotification(String message, NotificationVariant variant) {
        synchronized (NOTIFICATION_LOCK) {
            if (isNotificationActive) {
                return;
            }
            isNotificationActive = true;
        }

        Notification notification = Notification.show(
                message,
                3000,
                Notification.Position.TOP_CENTER
        );
        notification.addThemeVariants(variant);

        notification.addOpenedChangeListener(event -> {
            if (!event.isOpened()) {
                synchronized (NOTIFICATION_LOCK) {
                    isNotificationActive = false;
                }
            }
        });

        // Принудительный сброс через CompletableFuture
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(4);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                synchronized (NOTIFICATION_LOCK) {
                    isNotificationActive = false;
                }
            }
        });
    }
}