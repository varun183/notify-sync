package com.notifysync.notifysync.service.channel;

import com.notifysync.notifysync.model.Email;
import com.notifysync.notifysync.model.Notification;

public interface NotificationChannel {
    boolean isAvailable();
    Notification.NotificationChannel getChannelType();
    boolean sendNotification(Email email);
}
