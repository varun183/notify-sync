package com.notifysync.notifysync.service.processor;

import com.notifysync.notifysync.model.Email;
import com.notifysync.notifysync.service.email.EmailService;
import com.notifysync.notifysync.service.email.GmailCategoryService;
import com.notifysync.notifysync.service.filter.EmailFilterService;
import com.notifysync.notifysync.service.notification.NotificationService;
import com.notifysync.notifysync.service.tracking.EmailTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProcessorServiceImpl implements EmailProcessorService {

    private final EmailService emailService;
    private final EmailFilterService emailFilterService;
    private final NotificationService notificationService;
    private final GmailCategoryService gmailCategoryService;
    private final EmailTrackingService emailTrackingService;

    @Value("${notifysync.email.max-emails-per-fetch:10}")
    private int maxEmailsPerFetch;

    @Value("${notifysync.email.max-notifications-per-day:20}")
    private int maxNotificationsPerDay;

    @Value("${notifysync.email.thread-deduplication-window-hours:2}")
    private int threadDeduplicationWindowHours;

    // Track notifications sent today
    private int notificationsSentToday = 0;
    private LocalDateTime notificationCountResetDate = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0);

    @Override
    @Scheduled(fixedDelayString = "${notifysync.email.check-interval-seconds:300}000")
    public void processEmails() {
        log.info("Starting scheduled email processing");

        try {
            // Reset notification counter if day changed
            resetDailyNotificationCounterIfNeeded();

            // Fetch recent emails
            List<Email> recentEmails = emailService.fetchRecentEmails(maxEmailsPerFetch);
            log.info("Fetched {} recent emails", recentEmails.size());

            int processedCount = 0;
            int importantCount = 0;
            int notifiedCount = 0;

            for (Email email : recentEmails) {
                try {
                    // Skip if email already processed
                    if (emailTrackingService.isEmailProcessed(email.getId())) {
                        log.debug("Skipping already processed email: {}", email.getSubject());
                        continue;
                    }

                    // Skip if not in PRIMARY or UPDATES category
                    if (gmailCategoryService.isInAllowedCategory(email.getId())) {
                        log.debug("Skipping email not in PRIMARY or UPDATES category: {}", email.getSubject());
                        emailTrackingService.recordProcessedEmail(
                                email.getId(),
                                email.getThreadId(),
                                email.getSubject(),
                                email.getSenderEmail(),
                                false,
                                false
                        );
                        continue;
                    }

                    // Check for thread-based duplicates
                    if (email.getThreadId() != null && !email.getThreadId().isEmpty() &&
                            emailTrackingService.wasThreadRecentlyProcessed(email.getThreadId(), threadDeduplicationWindowHours)) {
                        log.debug("Skipping email in recently notified thread: {}", email.getSubject());
                        emailTrackingService.recordProcessedEmail(
                                email.getId(),
                                email.getThreadId(),
                                email.getSubject(),
                                email.getSenderEmail(),
                                true, // Mark as important but no notification sent
                                false
                        );
                        continue;
                    }

                    processedCount++;

                    // Check if email is important
                    boolean isImportant = emailFilterService.isImportantEmail(email);
                    email.setImportant(isImportant);

                    if (isImportant) {
                        importantCount++;

                        // Check notification rate limit
                        if (notificationsSentToday < maxNotificationsPerDay) {
                            // Send notification
                            boolean notificationSent = !notificationService.sendNotifications(email).isEmpty();

                            if (notificationSent) {
                                notifiedCount++;
                                notificationsSentToday++;
                                log.info("Sent notification for important email: {}", email.getSubject());
                            }

                            // Record processed email
                            emailTrackingService.recordProcessedEmail(
                                    email.getId(),
                                    email.getThreadId(),
                                    email.getSubject(),
                                    email.getSenderEmail(),
                                    true,
                                    notificationSent
                            );
                        } else {
                            log.info("Daily notification limit reached. Skipping notification for: {}", email.getSubject());
                            emailTrackingService.recordProcessedEmail(
                                    email.getId(),
                                    email.getThreadId(),
                                    email.getSubject(),
                                    email.getSenderEmail(),
                                    true,
                                    false
                            );
                        }
                    } else {
                        // Record non-important email
                        emailTrackingService.recordProcessedEmail(
                                email.getId(),
                                email.getThreadId(),
                                email.getSubject(),
                                email.getSenderEmail(),
                                false,
                                false
                        );
                    }
                } catch (Exception e) {
                    log.error("Error processing email: {}", email.getId(), e);
                }
            }

            log.info("Completed processing emails: processed={}, important={}, notified={}",
                    processedCount, importantCount, notifiedCount);

        } catch (Exception e) {
            log.error("Error during email processing", e);
        }
    }

    /**
     * Reset the daily notification counter if the day has changed
     */
    private void resetDailyNotificationCounterIfNeeded() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(notificationCountResetDate)) {
            log.info("Resetting daily notification counter from {}", notificationsSentToday);
            notificationsSentToday = 0;
            notificationCountResetDate = now.plusDays(1).withHour(0).withMinute(0);
        }
    }
}