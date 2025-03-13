package com.notifysync.notifysync.service.notification;


import com.notifysync.notifysync.model.Email;
import com.notifysync.notifysync.model.Notification;
import com.notifysync.notifysync.service.channel.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final List<NotificationChannel> channels;

    @Override
    public List<Notification> sendNotifications(Email email) {
        List<Notification> results = new ArrayList<>();

        if (email == null) {
            log.warn("Cannot send notification for null email");
            return results;
        }

        log.info("Sending notifications for email: {}", email.getSubject());

        // Try each channel in priority order (they are injected in the order defined by @Order)
        for (NotificationChannel channel : channels) {
            if (!channel.isAvailable()) {
                log.debug("Channel {} is not available", channel.getChannelType());
                continue;
            }

            Notification notification = Notification.builder()
                    .id(UUID.randomUUID().toString())
                    .title(email.getSubject())
                    .message(createNotificationMessage(email))
                    .channel(channel.getChannelType())
                    .priority(Notification.NotificationPriority.HIGH)
                    .createdAt(LocalDateTime.now())
                    .status(Notification.NotificationStatus.PENDING)
                    .build();

            try {
                boolean sent = channel.sendNotification(email);
                notification.setStatus(sent ?
                        Notification.NotificationStatus.SENT :
                        Notification.NotificationStatus.FAILED);

                if (sent) {
                    log.info("Successfully sent notification via {} for email: {}",
                            channel.getChannelType(), email.getSubject());
                    results.add(notification);


                    // break;  <-- commented out this line so that notif sent to all channels
                } else {
                    log.warn("Failed to send notification via {} for email: {}",
                            channel.getChannelType(), email.getSubject());
                }
            } catch (Exception e) {
                log.error("Error sending notification via {}", channel.getChannelType(), e);
                notification.setStatus(Notification.NotificationStatus.FAILED);
            }

            results.add(notification);
        }

        if (results.stream().noneMatch(n -> n.getStatus() == Notification.NotificationStatus.SENT)) {
            log.error("Failed to send notification via any channel for email: {}", email.getSubject());
        }

        return results;
    }

    private String createNotificationMessage(Email email) {
        return String.format("From: %s <%s>\nSubject: %s\nReceived: %s",
                email.getSender(),
                email.getSenderEmail(),
                email.getSubject(),
                email.getReceivedAt());
    }
}