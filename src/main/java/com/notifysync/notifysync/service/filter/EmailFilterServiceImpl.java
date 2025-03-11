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
            log.debug("Email is null");
            return false;
        }

        log.debug("Checking importance for email: Subject='{}', From='{}'",
                email.getSubject(), email.getSenderEmail());
        log.debug("Important domains configured: {}", importantDomains);
        log.debug("Important keywords configured: {}", importantKeywords);

        // Check if email is recent
        if (email.getReceivedAt() == null ||
                ChronoUnit.HOURS.between(email.getReceivedAt(), LocalDateTime.now()) > recencyHours) {
            log.debug("Email is too old or has no received date");
            return false;
        }

        // Check if sender domain is important
        if (isFromImportantDomain(email.getSenderEmail())) {
            log.debug("Email from {} is from an important domain", email.getSenderEmail());
            return true;
        }

        // Check if the subject contains important keywords
        if (containsImportantKeywords(email.getSubject())) {
            log.debug("Email subject '{}' contains important keywords", email.getSubject());
            return true;
        }

        // Check if the body contains important keywords
        if (containsImportantKeywords(email.getBody())) {
            log.debug("Email body contains important keywords");
            return true;
        }

        log.debug("Email is not important");
        return false;
    }

    private boolean isFromImportantDomain(String emailAddress) {
        if (emailAddress == null || emailAddress.isBlank() || importantDomains == null || importantDomains.isEmpty()) {
            log.debug("Email address is null/blank or no important domains configured");
            return false;
        }

        String lowerCaseEmail = emailAddress.toLowerCase(Locale.ROOT);
        log.debug("Checking if email '{}' matches any important domains: {}", lowerCaseEmail, importantDomains);

        for (String domain : importantDomains) {
            if (domain == null || domain.isBlank()) continue;

            String lowerCaseDomain = domain.toLowerCase(Locale.ROOT).trim();
            boolean matches = lowerCaseEmail.endsWith("@" + lowerCaseDomain) ||
                    lowerCaseEmail.endsWith("." + lowerCaseDomain);

            log.debug("Checking domain '{}' against '{}': {}", lowerCaseDomain, lowerCaseEmail, matches);
            if (matches) return true;
        }

        return false;
    }

    private boolean containsImportantKeywords(String content) {
        if (content == null || content.isBlank() || importantKeywords == null || importantKeywords.isEmpty()) {
            log.debug("Content is null/blank or no important keywords configured");
            return false;
        }

        String lowerCaseContent = content.toLowerCase(Locale.ROOT);
        log.debug("Checking if content contains any important keywords: {}", importantKeywords);

        for (String keyword : importantKeywords) {
            if (keyword == null || keyword.isBlank()) continue;

            String lowerCaseKeyword = keyword.toLowerCase(Locale.ROOT).trim();
            boolean contains = lowerCaseContent.contains(lowerCaseKeyword);

            log.debug("Checking keyword '{}' in content: {}", lowerCaseKeyword, contains);
            if (contains) return true;
        }

        return false;
    }
}
