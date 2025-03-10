package com.notifysync.notifysync.service.notification;


import com.notifysync.notifysync.model.Email;
import com.notifysync.notifysync.model.Notification;

import java.util.List;

public interface NotificationService {
    List<Notification> sendNotifications(Email email);
}
