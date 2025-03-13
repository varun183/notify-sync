package com.notifysync.notifysync.service.channel;

import com.notifysync.notifysync.model.Email;
import com.notifysync.notifysync.model.Notification;
import com.notifysync.notifysync.service.tracking.EmailTrackingService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@Order(1)
public class WhatsAppChannel implements NotificationChannel {

    @Value("${notifysync.whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${notifysync.whatsapp.account-sid:}")
    private String accountSid;

    @Value("${notifysync.whatsapp.auth-token:}")
    private String authToken;

    @Value("${notifysync.whatsapp.from-number:}")
    private String fromNumber;

    @Value("${notifysync.whatsapp.to-number:}")
    private String toNumber;



    @PostConstruct
    public void init() {
        if (isAvailable()) {
            Twilio.init(accountSid, authToken);
            log.info("WhatsApp (Twilio) service initialized");
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled &&
                accountSid != null && !accountSid.isEmpty() &&
                authToken != null && !authToken.isEmpty() &&
                fromNumber != null && !fromNumber.isEmpty() &&
                toNumber != null && !toNumber.isEmpty();
    }

    @Override
    public Notification.NotificationChannel getChannelType() {
        return Notification.NotificationChannel.WHATSAPP;
    }

    @Override
    public boolean sendNotification(Email email) {
        if (!isAvailable()) {
            log.warn("WhatsApp channel is not available");
            return false;
        }

        try {
            /*
            // Generate a tracking code for this email
            String trackingCode = generateTrackingCode();
            trackingCodeToEmailMap.put(trackingCode, email.getId());

            // Clean up tracking codes if map gets too large
            if (trackingCodeToEmailMap.size() > 50) {
                // Simplistic approach - just keep most recent ones
                while (trackingCodeToEmailMap.size() > 25) {
                    String firstKey = trackingCodeToEmailMap.keySet().iterator().next();
                    trackingCodeToEmailMap.remove(firstKey);
                }
            }

            String messageBody = formatEmailForWhatsApp(email, trackingCode);
            */

            String messageBody = formatEmailForWhatsApp(email);
            PhoneNumber from = new PhoneNumber("whatsapp:" + fromNumber);
            PhoneNumber to = new PhoneNumber("whatsapp:" + toNumber);

            Message message = Message.creator(to, from, messageBody).create();
            log.info("Sent notification via WhatsApp for email: {}, SID: {}", email.getSubject(), message.getSid());
            return true;
        } catch (Exception e) {
            log.error("Failed to send WhatsApp notification", e);
            return false;
        }
    }

    private String formatEmailForWhatsApp(Email email) {
        StringBuilder sb = new StringBuilder();
        sb.append("📥 *New Important Email*\n\n");
        sb.append("*From:* ").append(email.getSender()).append(" (").append(email.getSenderEmail()).append(")\n");
        sb.append("*Subject:* ").append(email.getSubject()).append("\n");
        sb.append("*Received:* ").append(email.getReceivedAt()).append("\n\n");

        // Get and process body
        String body = email.getBody();
        if (body != null) {
            // First check if it looks like HTML content
            if (body.contains("<html") || body.contains("<!DOCTYPE") || body.contains("<style") ||
                    body.contains("<head") || body.contains("<body")) {

                // For HTML emails, extract just readable text or provide a summary
                body = "This email contains rich HTML content that can't be displayed in WhatsApp.\n\nPlease check your email inbox for the complete message.";
            } else {
                // For regular emails, strip any HTML tags that might be present
                body = body.replaceAll("<[^>]*>", "");

                // Convert common HTML entities
                body = body.replace("&nbsp;", " ")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'");

                // Clean up excessive whitespace
                body = body.replaceAll("(?m)^[ \t]*\r?\n", ""); // Remove empty lines
                body = body.replaceAll("[ \t]+\r?\n", "\n");    // Remove trailing spaces
                body = body.replaceAll("\n{3,}", "\n\n");       // Replace 3+ consecutive newlines with just 2
                body = body.trim();                            // Trim leading/trailing whitespace

                // Truncate if too long
                if (body.length() > 500) {
                    body = body.substring(0, 500) + "...";
                }
            }
        }

        sb.append(body);

        // Add feedback instructions
//        sb.append("\n\n---\n");
//        sb.append("Was this notification useful?\n");
//        sb.append("Reply with: *YES-").append(trackingCode).append("* if relevant\n");
//        sb.append("Reply with: *NO-").append(trackingCode).append("* if not important");

        return sb.toString();
    }

//    /**
//     * Process incoming messages from WhatsApp for feedback
//     * This is called by the webhook controller when a message is received
//     */
//    public void processIncomingMessage(String fromNumber, String messageBody) {
//        if (messageBody == null || messageBody.isEmpty()) {
//            return;
//        }
//
//        try {
//            messageBody = messageBody.trim().toUpperCase();
//
//            // Check for YES feedback
//            if (messageBody.startsWith("YES-")) {
//                String trackingCode = extractTrackingCode(messageBody, "YES-");
//                processFeedback(trackingCode, true);
//            }
//            // Check for NO feedback
//            else if (messageBody.startsWith("NO-")) {
//                String trackingCode = extractTrackingCode(messageBody, "NO-");
//                processFeedback(trackingCode, false);
//            }
//
//        } catch (Exception e) {
//            log.error("Error processing incoming WhatsApp message", e);
//        }
//    }
//
//    /**
//     * Process feedback from user
//     */
//    private void processFeedback(String trackingCode, boolean isPositive) {
//        if (trackingCode == null || !trackingCodeToEmailMap.containsKey(trackingCode)) {
//            log.warn("Received feedback with unknown tracking code: {}", trackingCode);
//            return;
//        }
//
//        String emailId = trackingCodeToEmailMap.get(trackingCode);
//        emailTrackingService.recordUserFeedback(emailId, isPositive);
//
//        // Send acknowledgment
//        try {
//            String responseMessage = isPositive ?
//                    "Thank you for your feedback! We'll continue to notify you about similar emails." :
//                    "Thank you for your feedback! We'll adjust future notifications accordingly.";
//
//            PhoneNumber from = new PhoneNumber("whatsapp:" + fromNumber);
//            PhoneNumber to = new PhoneNumber("whatsapp:" + toNumber);
//
//            Message.creator(to, from, responseMessage).create();
//
//            log.info("Recorded {} feedback for email {}",
//                    isPositive ? "positive" : "negative", emailId);
//
//        } catch (Exception e) {
//            log.error("Error sending feedback acknowledgment", e);
//        }
//    }
//
//    /**
//     * Extract tracking code from feedback message
//     */
//    private String extractTrackingCode(String message, String prefix) {
//        if (message.startsWith(prefix) && message.length() > prefix.length()) {
//            return message.substring(prefix.length()).trim();
//        }
//        return null;
//    }
//
//    /**
//     * Generate a short unique tracking code
//     */
//    private String generateTrackingCode() {
//        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
//    }
}