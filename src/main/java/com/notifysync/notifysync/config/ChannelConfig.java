package com.notifysync.notifysync.config;


import com.notifysync.notifysync.service.channel.NotificationChannel;
import com.notifysync.notifysync.service.channel.TelegramChannel;
import com.notifysync.notifysync.service.channel.WhatsAppChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class ChannelConfig {

    @Bean
    @Order(1) // First priority
    public NotificationChannel telegramNotificationChannel(TelegramChannel telegramChannel) {
        return telegramChannel;
    }

    @Bean
    @Order(2) // Second priority
    public NotificationChannel whatsAppNotificationChannel(WhatsAppChannel whatsAppChannel) {
        return whatsAppChannel;
    }
}
