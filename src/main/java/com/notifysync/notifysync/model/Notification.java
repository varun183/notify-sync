package com.notifysync.notifysync.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Notification {
    private String id;
    private String title;
    private String message;
    private NotificationChannel channel;
    private NotificationPriority priority;
    private LocalDateTime createdAt;
    private NotificationStatus status;

    public enum NotificationChannel {
        TELEGRAM,
        WHATSAPP,
        EMAIL
    }

    public enum NotificationPriority {
        HIGH,
        MEDIUM,
        LOW
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED
    }
}
