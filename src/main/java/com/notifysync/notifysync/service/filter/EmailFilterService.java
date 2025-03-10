package com.notifysync.notifysync.service.filter;

import com.notifysync.notifysync.model.Email;

public interface EmailFilterService {
    boolean isImportantEmail(Email email);
}
