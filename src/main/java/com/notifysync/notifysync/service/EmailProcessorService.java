package com.notifysync.notifysync.service;


import com.notifysync.notifysync.model.Email;
import com.notifysync.notifysync.service.email.EmailService;
import com.notifysync.notifysync.service.filter.EmailFilterService;
import com.notifysync.notifysync.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProcessorService {

    private final EmailService emailService;
    private final EmailFilterService emailFilterService;
    private final NotificationService notificationService;

    @Value("${notifysync.email.max-emails-per-fetch:10}")
    private int maxEmailsPerFetch;

    @Scheduled(fixedDelayString = "${notifysync.email.check-interval-seconds:300}000")
    public void processEmails() {
        log.info("Starting scheduled email processing");

        try {
            // Fetch recent emails
            List<Email> recentEmails = emailService.fetchRecentEmails(maxEmailsPerFetch);
            log.info("Fetched {} recent emails", recentEmails.size());

            // Filter important emails
            List<Email> importantEmails = recentEmails.stream()
                    .filter(email -> {
                        boolean important = emailFilterService.isImportantEmail(email);
                        email.setImportant(important);
                        return important;
                    })
                    .toList();

            log.info("Found {} important emails", importantEmails.size());

            // Send notifications for each important email
            for (Email email : importantEmails) {
                notificationService.sendNotifications(email);
            }

            log.info("Completed processing emails");

        } catch (Exception e) {
            log.error("Error during email processing", e);
        }
    }
}
