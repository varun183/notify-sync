package com.notifysync.notifysync.service.tracking;

import java.util.List;

/**
 * Service for tracking processed emails to avoid duplicates
 */
public interface EmailTrackingService {

    /**
     * Check if an email has been processed already
     *
     * @param emailId The email ID to check
     * @return true if the email has been processed before
     */
    boolean isEmailProcessed(String emailId);

    /**
     * Record that an email has been processed
     *
     * @param emailId The email ID
     * @param threadId The thread ID
     * @param subject The email subject
     * @param senderEmail The sender's email address
     * @param wasImportant Whether the email was deemed important
     * @param wasNotified Whether a notification was sent
     */
    void recordProcessedEmail(String emailId, String threadId, String subject,
                              String senderEmail, boolean wasImportant, boolean wasNotified);

    /**
     * Check if a similar email in the same thread was recently processed
     *
     * @param threadId The thread ID
     * @param lookbackHours Hours to look back for similar emails
     * @return true if a similar email was recently processed
     */
    boolean wasThreadRecentlyProcessed(String threadId, int lookbackHours);

    /**
     * Record a user feedback for an email
     *
     * @param emailId The email ID
     * @param isRelevant Whether the user found it relevant
     */
    void recordUserFeedback(String emailId, boolean isRelevant);

    /**
     * Get the recent feedback count
     *
     * @param senderEmail The sender's email address
     * @return A pair containing the positive feedback count and total feedback count
     */
    List<Object> getRecentFeedbackForSender(String senderEmail);
}