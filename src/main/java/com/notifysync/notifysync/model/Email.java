package com.notifysync.notifysync.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class Email {
    private String id;
    private String threadId;
    private String subject;
    private String sender;
    private String senderEmail;
    private String body;
    private LocalDateTime receivedAt;
    private List<String> attachments;
    private boolean isImportant;
}