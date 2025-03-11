package com.notifysync.notifysync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import com.notifysync.notifysync.service.channel.TelegramChannel;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class TelegramConfig {

    private final ApplicationContext context;

    public TelegramConfig(ApplicationContext context) {
        this.context = context;
    }

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() {
        try {
            TelegramChannel telegramChannel = context.getBean(TelegramChannel.class);
            TelegramBotsApi telegramBotsApi = context.getBean(TelegramBotsApi.class);
            telegramBotsApi.registerBot(telegramChannel);
            log.info("Telegram bot registered successfully");
        } catch (TelegramApiException e) {
            log.error("Failed to register Telegram bot", e);
        }
    }
}