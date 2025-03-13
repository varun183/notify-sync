package com.notifysync.notifysync.service.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class EmailTrackingServiceImpl implements EmailTrackingService {

    @Value("${notifysync.tracking.storage-file:processed_emails.json}")
    private String storageFile;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ProcessedEmailInfo> processedEmails = new ConcurrentHashMap<>();
    private final Map<String, List<UserFeedback>> userFeedback = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            // Create necessary ObjectMapper modules for Java 8 date/time
            objectMapper.findAndRegisterModules();

            // Load existing data if available
            File file = new File(storageFile);
            if (file.exists() && file.length() > 0) {
                StorageData data = objectMapper.readValue(file, StorageData.class);

                if (data.getProcessedEmails() != null) {
                    processedEmails.putAll(data.getProcessedEmails());
                }

                if (data.getUserFeedback() != null) {
                    userFeedback.putAll(data.getUserFeedback());
                }

                log.info("Loaded {} processed emails and feedback for {} senders from storage",
                        processedEmails.size(), userFeedback.size());
            } else {
                log.info("No existing email tracking data found, starting fresh");
            }
        } catch (Exception e) {
            log.error("Error initializing email tracking service", e);
        }
    }

    private void saveToFile() {
        try {
            StorageData data = new StorageData();
            data.setProcessedEmails(new HashMap<>(processedEmails));
            data.setUserFeedback(new HashMap<>(userFeedback));

            objectMapper.writeValue(new File(storageFile), data);
        } catch (IOException e) {
            log.error("Failed to save email tracking data", e);
        }
    }

    @Override
    public boolean isEmailProcessed(String emailId) {
        return processedEmails.containsKey(emailId);
    }

    @Override
    public void recordProcessedEmail(String emailId, String threadId, String subject,
                                     String senderEmail, boolean wasImportant, boolean wasNotified) {
        ProcessedEmailInfo info = new ProcessedEmailInfo();
        info.setEmailId(emailId);
        info.setThreadId(threadId);
        info.setSubject(subject);
        info.setSenderEmail(senderEmail);
        info.setProcessedTime(LocalDateTime.now());
        info.setWasImportant(wasImportant);
        info.setWasNotified(wasNotified);

        processedEmails.put(emailId, info);

        // Periodically clean up old entries (older than 30 days)
        if (processedEmails.size() % 100 == 0) {
            cleanupOldEntries();
        }

        // Save to file every 10 new emails
        if (processedEmails.size() % 10 == 0) {
            saveToFile();
        }
    }

    @Override
    public boolean wasThreadRecentlyProcessed(String threadId, int lookbackHours) {
        if (threadId == null || threadId.isEmpty()) {
            return false;
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(lookbackHours);

        // Look for any emails in this thread that were notified recently
        return processedEmails.values().stream()
                .anyMatch(info -> threadId.equals(info.getThreadId())
                        && info.isWasNotified()
                        && info.getProcessedTime().isAfter(cutoffTime));
    }

    @Override
    public void recordUserFeedback(String emailId, boolean isRelevant) {
        ProcessedEmailInfo emailInfo = processedEmails.get(emailId);
        if (emailInfo == null) {
            log.warn("Received feedback for unknown email ID: {}", emailId);
            return;
        }

        String senderEmail = emailInfo.getSenderEmail();
        if (senderEmail == null || senderEmail.isEmpty()) {
            return;
        }

        // Create feedback entry
        UserFeedback feedback = new UserFeedback();
        feedback.setEmailId(emailId);
        feedback.setFeedbackTime(LocalDateTime.now());
        feedback.setRelevant(isRelevant);

        // Add to sender's feedback list
        userFeedback.computeIfAbsent(senderEmail, k -> new ArrayList<>()).add(feedback);

        // Save to file after feedback
        saveToFile();
    }

    @Override
    public List<Object> getRecentFeedbackForSender(String senderEmail) {
        if (!userFeedback.containsKey(senderEmail)) {
            return Arrays.asList(0, 0); // No feedback yet
        }

        // Look at feedback from last 30 days
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);

        List<UserFeedback> recentFeedback = userFeedback.get(senderEmail).stream()
                .filter(fb -> fb.getFeedbackTime().isAfter(cutoffTime))
                .toList();

        int positiveCount = (int) recentFeedback.stream()
                .filter(UserFeedback::isRelevant)
                .count();

        return Arrays.asList(positiveCount, recentFeedback.size());
    }

    /**
     * Clean up entries older than 30 days to prevent unlimited growth
     */
    private void cleanupOldEntries() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);

        // Remove old processed emails
        processedEmails.entrySet().removeIf(entry ->
                entry.getValue().getProcessedTime().isBefore(cutoffTime));

        // Clean up old feedback
        for (List<UserFeedback> feedbackList : userFeedback.values()) {
            feedbackList.removeIf(fb -> fb.getFeedbackTime().isBefore(cutoffTime));
        }

        // Remove empty feedback lists
        userFeedback.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        log.debug("Cleaned up old entries, remaining: {} emails, {} feedback senders",
                processedEmails.size(), userFeedback.size());
    }

    /**
     * Data classes for storage
     */
    @Data
    public static class ProcessedEmailInfo {
        private String emailId;
        private String threadId;
        private String subject;
        private String senderEmail;
        private LocalDateTime processedTime;
        private boolean wasImportant;
        private boolean wasNotified;
    }

    @Data
    public static class UserFeedback {
        private String emailId;
        private LocalDateTime feedbackTime;
        private boolean isRelevant;
    }

    @Data
    public static class StorageData {
        private Map<String, ProcessedEmailInfo> processedEmails;
        private Map<String, List<UserFeedback>> userFeedback;
    }
}