package com.notifysync.notifysync.service.email;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class GmailCategoryService {

    private final Gmail gmail;
    private static final String USER_ID = "me";
    private static final String CATEGORY_PREFIX = "CATEGORY_";

    // Gmail categories
    public enum GmailCategory {
        PRIMARY,
        SOCIAL,
        PROMOTIONS,
        UPDATES,
        FORUMS,
        UNKNOWN;

        public static GmailCategory fromLabelName(String labelName) {
            if (labelName == null || !labelName.startsWith(CATEGORY_PREFIX)) {
                return UNKNOWN;
            }

            String category = labelName.substring(CATEGORY_PREFIX.length());
            try {
                return GmailCategory.valueOf(category);
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }
    }

    private Map<String, String> labelIdToNameMap;

    public GmailCategoryService(@Qualifier("gmailApiService") Gmail gmail) {
        this.gmail = gmail;
    }

    /**
     * Gets the Gmail category for a specific message
     *
     * @param messageId The Gmail message ID
     * @return The Gmail category
     */
    public GmailCategory getMessageCategory(String messageId) {
        try {
            // Ensure we have the label mappings
            if (labelIdToNameMap == null) {
                initLabelMap();
            }

            // Get the message and its label IDs
            Message message = gmail.users().messages().get(USER_ID, messageId).execute();
            List<String> labelIds = message.getLabelIds();

            if (labelIds == null || labelIds.isEmpty()) {
                return GmailCategory.UNKNOWN;
            }

            // Look for category labels
            for (String labelId : labelIds) {
                String labelName = labelIdToNameMap.get(labelId);
                if (labelName != null && labelName.startsWith(CATEGORY_PREFIX)) {
                    return GmailCategory.fromLabelName(labelName);
                }
            }

            return GmailCategory.UNKNOWN;

        } catch (IOException e) {
            log.error("Error getting Gmail category for message {}: {}", messageId, e.getMessage());
            return GmailCategory.UNKNOWN;
        }
    }

    /**
     * Checks if a message belongs to one of the allowed categories (PRIMARY or UPDATES)
     */
    public boolean isInAllowedCategory(String messageId) {
        GmailCategory category = getMessageCategory(messageId);
        return category == GmailCategory.PRIMARY || category == GmailCategory.UPDATES;
    }

    /**
     * Initialize the label ID to name mapping
     */
    private void initLabelMap() {
        labelIdToNameMap = new HashMap<>();

        try {
            List<Label> labels = gmail.users().labels().list(USER_ID).execute().getLabels();
            if (labels != null) {
                for (Label label : labels) {
                    labelIdToNameMap.put(label.getId(), label.getName());
                }
            }
            log.debug("Initialized Gmail label map with {} labels", labelIdToNameMap.size());
        } catch (IOException e) {
            log.error("Error fetching Gmail labels: {}", e.getMessage());
            labelIdToNameMap = Collections.emptyMap();
        }
    }
}