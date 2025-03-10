package com.notifysync.notifysync.service.filter;


import com.notifysync.notifysync.model.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailFilterServiceImpl implements EmailFilterService {

    @Value("#{'${notifysync.filter.important-domains:}'.split(',')}")
    private List<String> importantDomains;

    @Value("#{'${notifysync.filter.important-keywords:}'.split(',')}")
    private List<String> importantKeywords;

    @Value("${notifysync.filter.recency-hours:24}")
    private int recencyHours;

    @Override
    public boolean isImportantEmail(Email email) {
        if (email == null) {
            return false;
        }

        // Check if email is recent
        if (email.getReceivedAt() == null ||
                ChronoUnit.HOURS.between(email.getReceivedAt(), LocalDateTime.now()) > recencyHours) {
            return false;
        }

        // Check if sender domain is important
        if (isFromImportantDomain(email.getSenderEmail())) {
            log.debug("Email from {} is from an important domain", email.getSenderEmail());
            return true;
        }

        // Check if the subject or body contains important keywords
        if (containsImportantKeywords(email.getSubject()) || containsImportantKeywords(email.getBody())) {
            log.debug("Email '{}' contains important keywords", email.getSubject());
            return true;
        }

        return false;
    }

    private boolean isFromImportantDomain(String emailAddress) {
        if (emailAddress == null || emailAddress.isBlank() || importantDomains == null || importantDomains.isEmpty()) {
            return false;
        }

        String lowerCaseEmail = emailAddress.toLowerCase(Locale.ROOT);

        return importantDomains.stream()
                .filter(domain -> !domain.isBlank())
                .anyMatch(domain -> lowerCaseEmail.endsWith("@" + domain.toLowerCase(Locale.ROOT)) ||
                        lowerCaseEmail.endsWith("." + domain.toLowerCase(Locale.ROOT)));
    }

    private boolean containsImportantKeywords(String content) {
        if (content == null || content.isBlank() || importantKeywords == null || importantKeywords.isEmpty()) {
            return false;
        }

        String lowerCaseContent = content.toLowerCase(Locale.ROOT);

        return importantKeywords.stream()
                .filter(keyword -> !keyword.isBlank())
                .anyMatch(keyword -> lowerCaseContent.contains(keyword.toLowerCase(Locale.ROOT)));
    }
}
