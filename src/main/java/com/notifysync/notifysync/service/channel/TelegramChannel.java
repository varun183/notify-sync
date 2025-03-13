package com.notifysync.notifysync.service.channel;

import com.notifysync.notifysync.model.Email;
import com.notifysync.notifysync.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
@Slf4j
@Order(1)
public class TelegramChannel extends TelegramLongPollingBot implements NotificationChannel {

    @Value("${notifysync.telegram.chat-id}")
    private String chatId;

    @Value("${notifysync.telegram.bot-username:NotifySyncBot}")
    private String botUsername;

    @Value("${notifysync.telegram.bot-token}")
    private final String botToken;

    public TelegramChannel(@Value("${notifysync.telegram.bot-token}")String botToken) {
        super(botToken);
        this.botToken = botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // No need to handle incoming messages for this channel for now
        log.debug("Received update: {}", update);
    }

    @Override
    public boolean isAvailable() {
        return botToken != null && !botToken.isEmpty() && chatId != null && !chatId.isEmpty();
    }

    @Override
    public Notification.NotificationChannel getChannelType() {
        return Notification.NotificationChannel.TELEGRAM;
    }

    @Override
    public boolean sendNotification(Email email) {
        if (!isAvailable()) {
            log.warn("Telegram channel is not available");
            return false;
        }

        try {
            String message = formatEmailForTelegram(email);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.enableHtml(true);

            execute(sendMessage);
            log.info("Sent notification via Telegram for email: {}", email.getSubject());
            return true;
        } catch (TelegramApiException e) {
            log.error("Failed to send Telegram notification", e);
            return false;
        }
    }

    private String formatEmailForTelegram(Email email) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>New Important Email</b>\n\n");
        sb.append("<b>From:</b> ").append(email.getSender()).append(" &lt;").append(email.getSenderEmail()).append("&gt;\n");
        sb.append("<b>Subject:</b> ").append(email.getSubject()).append("\n");
        sb.append("<b>Received:</b> ").append(email.getReceivedAt()).append("\n\n");

        // Get and process body
        String body = email.getBody();
        if (body != null) {
            // First check if it looks like HTML content
            if (body.contains("<html") || body.contains("<!DOCTYPE") || body.contains("<style") ||
                    body.contains("<head") || body.contains("<body")) {

                // For HTML emails, extract just readable text or provide a summary
                body = "This email contains rich HTML content.\n\nPlease check your email inbox for the complete message.";
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

        return sb.toString();
    }
}