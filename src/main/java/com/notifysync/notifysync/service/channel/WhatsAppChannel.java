package com.notifysync.notifysync.service.channel;


import com.notifysync.notifysync.model.Email;
import com.notifysync.notifysync.model.Notification;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@Slf4j
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

        // Truncate body if too long (WhatsApp has message size limits)
        String body = email.getBody();
        if (body != null && body.length() > 500) {
            body = body.substring(0, 500) + "...";
        }

        sb.append(body);

        return sb.toString();
    }
}
