package com.notifysync.notifysync.service.processor;

/**
 * Interface for email processing services.
 */
public interface EmailProcessorService {

    /**
     * Process emails to find important ones and send notifications.
     */
    void processEmails();
}