package com.notifysync.notifysync.controller;


import com.notifysync.notifysync.service.processor.EmailProcessorService;
import com.notifysync.notifysync.service.channel.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
@Slf4j
public class StatusController {

    private final List<NotificationChannel> channels;
    private final EmailProcessorService emailProcessorService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("status", "OK");
        status.put("timestamp", System.currentTimeMillis());

        List<Map<String, Object>> channelStatus = channels.stream()
                .map(channel -> {
                    Map<String, Object> channelInfo = new HashMap<>();
                    channelInfo.put("type", channel.getChannelType());
                    channelInfo.put("available", channel.isAvailable());
                    return channelInfo;
                })
                .collect(Collectors.toList());

        status.put("channels", channelStatus);

        return ResponseEntity.ok(status);
    }

    @PostMapping("/process-now")
    public ResponseEntity<Map<String, String>> processNow() {
        log.info("Manual processing of emails triggered");
        emailProcessorService.processEmails();

        Map<String, String> response = new HashMap<>();
        response.put("status", "Processing triggered");

        return ResponseEntity.ok(response);
    }
}