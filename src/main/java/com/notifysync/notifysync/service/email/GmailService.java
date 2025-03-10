package com.notifysync.notifysync.service.email;


import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.notifysync.notifysync.model.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailService implements EmailService {

    private final Gmail gmail;
    private static final String USER_ID = "me";

    @Override
    public List<Email> fetchRecentEmails(int maxResults) {
        List<Email> emails = new ArrayList<>();

        try {
            // Fetch recent messages
            ListMessagesResponse response = gmail.users().messages()
                    .list(USER_ID)
                    .setMaxResults((long) maxResults)
                    .execute();

            List<Message> messages = response.getMessages();
            if (messages == null || messages.isEmpty()) {
                log.info("No emails found.");
                return emails;
            }

            // Process each message
            for (Message message : messages) {
                try {
                    Message fullMessage = gmail.users().messages().get(USER_ID, message.getId()).execute();
                    emails.add(convertToEmail(fullMessage));
                } catch (Exception e) {
                    log.error("Error processing email with ID: {}", message.getId(), e);
                }
            }

        } catch (IOException e) {
            log.error("Failed to fetch emails from Gmail", e);
        }

        return emails;
    }

    private Email convertToEmail(Message message) {
        String subject = "";
        String sender = "";
        String senderEmail = "";
        String body = "";
        LocalDateTime receivedAt = null;

        // Get headers
        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            for (MessagePartHeader header : message.getPayload().getHeaders()) {
                switch (header.getName()) {
                    case "Subject":
                        subject = header.getValue();
                        break;
                    case "From":
                        String from = header.getValue();
                        if (from.contains("<") && from.contains(">")) {
                            sender = from.substring(0, from.indexOf("<")).trim();
                            senderEmail = from.substring(from.indexOf("<") + 1, from.indexOf(">")).trim();
                        } else {
                            senderEmail = from;
                            sender = from;
                        }
                        break;
                    case "Date":
                        try {
                            // Use a proper date parser instead of the deprecated constructor
                            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(
                                    "EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
                            Date date = format.parse(header.getValue());
                            receivedAt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                        } catch (Exception e) {
                            log.warn("Could not parse email date: {}", header.getValue());
                            receivedAt = LocalDateTime.now();
                        }
                        break;
                }
            }
        }

        // Get body
        if (message.getPayload() != null) {
            body = getTextFromMessagePart(message.getPayload());
        }

        return Email.builder()
                .id(message.getId())
                .subject(subject)
                .sender(sender)
                .senderEmail(senderEmail)
                .body(body)
                .receivedAt(receivedAt != null ? receivedAt : LocalDateTime.now())
                .isImportant(false) // Will be determined by the filter service
                .build();
    }

    private String getTextFromMessagePart(MessagePart part) {
        if (part.getBody() != null && part.getBody().getData() != null) {
            return decodeBase64(part.getBody().getData());
        }

        if (part.getParts() != null) {
            StringBuilder result = new StringBuilder();
            for (MessagePart subPart : part.getParts()) {
                if ("text/plain".equals(subPart.getMimeType()) && subPart.getBody() != null && subPart.getBody().getData() != null) {
                    return decodeBase64(subPart.getBody().getData());
                }
                result.append(getTextFromMessagePart(subPart));
            }
            return result.toString();
        }

        return "";
    }

    private String decodeBase64(String data) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(data.replace('-', '+').replace('_', '/'));
        return new String(decodedBytes);
    }
}
