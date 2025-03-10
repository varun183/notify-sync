package com.notifysync.notifysync.service.email;

import com.notifysync.notifysync.model.Email;

import java.util.List;

public interface EmailService {
    List<Email> fetchRecentEmails(int maxResults);
}